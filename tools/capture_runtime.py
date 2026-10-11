"""Scoped runtime class capture for the installed Lunar-integrated Badlion.

The observer retains executable class bytes and public module identity only.
No command lines, authentication state, heap dumps or network data are saved.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import struct
import threading
import time
from pathlib import Path

import frida
import psutil

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"


def class_name(data: bytes) -> str:
    """Resolve this_class through the constant pool without loading the class."""
    if len(data) < 12 or data[:4] != b"\xca\xfe\xba\xbe":
        raise ValueError("not a class file")
    count = struct.unpack_from(">H", data, 8)[0]
    constants: dict[int, str | int] = {}
    offset, index = 10, 1
    while index < count:
        tag = data[offset]
        offset += 1
        if tag == 1:
            length = struct.unpack_from(">H", data, offset)[0]
            offset += 2
            constants[index] = data[offset:offset + length].decode("utf-8", errors="replace")
            offset += length
        elif tag == 7:
            constants[index] = struct.unpack_from(">H", data, offset)[0]
            offset += 2
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            offset += 4
        elif tag in (5, 6):
            offset += 8
            index += 1
        elif tag in (8, 16, 19, 20):
            offset += 2
        elif tag == 15:
            offset += 3
        else:
            raise ValueError(f"unknown constant-pool tag {tag}")
        index += 1
    this_class = struct.unpack_from(">H", data, offset + 2)[0]
    return str(constants[int(constants[this_class])])


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--seconds", type=int, default=300)
    parser.add_argument("--launch", action="store_true")
    parser.add_argument("--launcher", type=Path,
                        default=Path.home() / "AppData/Local/Programs/Lunar Client/Lunar Client.exe")
    parser.add_argument("--output", type=Path, default=ROOT / "exports" / "runtime-classes")
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    device = frida.get_local_device()
    sessions: dict[int, tuple[object, object]] = {}
    retries: dict[int, float] = {}
    records: list[dict] = []
    seen: set[tuple[str, str]] = set()
    lock = threading.RLock()
    events = output / "capture-events.jsonl"

    def log(event: dict) -> None:
        with lock, events.open("a", encoding="utf-8") as stream:
            stream.write(json.dumps(event) + "\n")
        if event.get("event") != "class":
            print(json.dumps(event), flush=True)

    def save_class(pid: int, payload: dict, data: bytes) -> None:
        try:
            name = class_name(data)
            if not re.fullmatch(r"[A-Za-z0-9_$/]+", name):
                raise ValueError("unsafe class name")
            if not (name.startswith(("net/badlion/", "net/minecraft/")) or
                    re.fullmatch(r"[A-Za-z]{1,5}", name)):
                return
            digest = hashlib.sha256(data).hexdigest()
            with lock:
                if (name, digest) in seen:
                    return
                seen.add((name, digest))
                target = output / str(pid) / digest[:12] / (name + ".class")
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(data)
                record = {"name": name, "sha256": digest, "bytes": len(data),
                          "source": payload["source"], "pid": pid,
                          "path": str(target.relative_to(output))}
                records.append(record)
            log({"event": "class", **record})
        except Exception as error:
            log({"event": "class-parse-error", "message": str(error)})

    def attach(pid: int, launcher: bool) -> object:
        with lock:
            if pid in sessions:
                return sessions[pid][1]
        session = device.attach(pid)
        script = session.create_script((TOOLS / ("capture-launcher.js" if launcher else
                                                 "capture-runtime.js")).read_text())

        def message_handler(message: dict, data: bytes | None) -> None:
            if message["type"] != "send":
                log({"event": "script-error", "pid": pid, "message": message.get("description", "unknown")})
                return
            payload = message["payload"]
            if payload["event"] == "class" and data is not None:
                save_class(pid, payload, data)
            elif payload["event"] == "java-child":
                child_pid = payload["pid"]

                def attach_child() -> None:
                    try:
                        attach(child_pid, False)
                    except Exception as error:
                        log({"event": "child-attach-error", "pid": child_pid, "message": str(error)})
                    finally:
                        script.post({"type": "resume-" + str(child_pid)})

                threading.Thread(target=attach_child, daemon=True).start()
            else:
                log({"pid": pid, **payload})

        script.on("message", message_handler)
        with lock:
            sessions[pid] = session, script
        try:
            script.load()
        except Exception:
            with lock:
                sessions.pop(pid, None)
            session.detach()
            raise
        return script

    deadline = time.monotonic() + args.seconds
    try:
        if args.launch:
            executable = args.launcher.resolve()
            pid = device.spawn(str(executable), stdio="pipe")
            try:
                attach(pid, True)
            finally:
                device.resume(pid)
        log({"event": "watching", "seconds": args.seconds, "output": str(output)})
        last_count, last_report = -1, 0.0
        while time.monotonic() < deadline:
            for process in psutil.process_iter(["pid", "name", "exe"]):
                try:
                    pid, name = process.info["pid"], (process.info["name"] or "").lower()
                    if pid in sessions:
                        continue
                    if time.monotonic() < retries.get(pid, 0):
                        continue
                    if name == "lunar client.exe":
                        if any(arg.startswith("--type=") for arg in process.cmdline()):
                            continue
                        attach(pid, True)
                    elif name in ("java.exe", "javaw.exe") and "badlion client" in (
                            process.info["exe"] or "").lower():
                        attach(pid, False)
                except (psutil.Error, frida.ProcessNotFoundError):
                    pass
                except Exception as error:
                    retries[process.info["pid"]] = time.monotonic() + 30
                    log({"event": "attach-error", "pid": process.info["pid"], "message": str(error)})
            now = time.monotonic()
            if len(records) != last_count and now - last_report > 5:
                last_count, last_report = len(records), now
                log({"event": "progress", "classes": len(records)})
            time.sleep(0.2)
    except KeyboardInterrupt:
        pass
    finally:
        for pid, (session, script) in list(sessions.items()):
            try:
                if "badlion" in (psutil.Process(pid).exe() or "").lower():
                    log({"event": "final-stats", "pid": pid, **script.exports_sync.stats()})
            except Exception:
                pass
            try:
                session.detach()
            except frida.InvalidOperationError:
                pass
        (output / "capture-manifest.json").write_text(json.dumps({
            "toolVersion": "0.3.0-capture.1", "records": records,
            "classCount": len(records), "scope": "Badlion and Minecraft executable classes",
            "limitations": "Only class-load events while attached; no guarantee of complete renderer coverage."
        }, indent=2), encoding="utf-8")
        log({"event": "complete", "classes": len(records)})


if __name__ == "__main__":
    main()
