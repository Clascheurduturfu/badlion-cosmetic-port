"""Temporarily serve only a handoff ZIP and its checksum on the selected LAN IP."""
import argparse
import json
import os
import re
import shutil
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import unquote, urlsplit


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--file", type=Path, required=True)
    parser.add_argument("--bind", required=True)
    parser.add_argument("--port", type=int, default=8769)
    parser.add_argument("--token", required=True)
    parser.add_argument("--status", type=Path, required=True)
    parser.add_argument("--hours", type=float, default=12)
    args = parser.parse_args()
    artifact = args.file.resolve(strict=True)
    checksum = artifact.with_suffix(".zip.sha256")
    if not re.fullmatch(r"[A-Za-z0-9_-]{16,128}", args.token):
        raise ValueError("Invalid download path token")
    prefix = "/" + args.token + "/"
    permitted = {prefix + artifact.name: artifact, prefix + checksum.name: checksum}
    started = time.time()

    class Handler(BaseHTTPRequestHandler):
        def log_message(self, format, *values):
            pass

        def do_HEAD(self):
            self.send_file(True)

        def do_GET(self):
            self.send_file(False)

        def send_file(self, head):
            path = permitted.get(unquote(urlsplit(self.path).path))
            if path is None:
                self.send_error(404)
                return
            size = path.stat().st_size
            start, end = 0, size - 1
            partial = False
            request_range = self.headers.get("Range")
            if request_range:
                match = re.fullmatch(r"bytes=(\d+)-(\d*)", request_range)
                if not match:
                    self.send_error(416)
                    return
                start = int(match[1])
                end = min(int(match[2]), end) if match[2] else end
                if start > end or start >= size:
                    self.send_response(416)
                    self.send_header("Content-Range", f"bytes */{size}")
                    self.end_headers()
                    return
                partial = True
            self.send_response(206 if partial else 200)
            self.send_header("Content-Type", "application/zip" if path == artifact else "text/plain")
            self.send_header("Content-Disposition", f'attachment; filename="{path.name}"')
            self.send_header("Content-Length", str(end - start + 1))
            self.send_header("Accept-Ranges", "bytes")
            self.send_header("Cache-Control", "no-store")
            if partial:
                self.send_header("Content-Range", f"bytes {start}-{end}/{size}")
            self.end_headers()
            if head:
                return
            try:
                with path.open("rb") as incoming:
                    incoming.seek(start)
                    remaining = end - start + 1
                    while remaining:
                        chunk = incoming.read(min(1024 * 1024, remaining))
                        if not chunk:
                            break
                        self.wfile.write(chunk)
                        remaining -= len(chunk)
            except (BrokenPipeError, ConnectionResetError):
                pass

    server = ThreadingHTTPServer((args.bind, args.port), Handler)
    args.status.parent.mkdir(parents=True, exist_ok=True)
    args.status.write_text(json.dumps({"pid": os.getpid(), "url": f"http://{args.bind}:{args.port}{prefix}{artifact.name}",
        "checksumUrl": f"http://{args.bind}:{args.port}{prefix}{checksum.name}", "expiresAt": started + args.hours * 3600,
        "file": str(artifact), "bind": args.bind, "port": args.port}, indent=2))
    expiry = threading.Timer(args.hours * 3600, server.shutdown)
    expiry.daemon = True
    expiry.start()
    try:
        server.serve_forever()
    finally:
        server.server_close()
        expiry.cancel()


if __name__ == "__main__":
    main()
