"""Compile and run the recovered geometry/animation differential audits on any OS."""
import argparse
import json
import os
import platform
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--java-home", type=Path)
    args = parser.parse_args()
    home = args.java_home
    if home is None:
        candidates = sorted((ROOT / "work/toolchains").glob("*jdk21*/Contents/Home"))
        if candidates:
            home = candidates[-1]
        elif os.environ.get("JAVA_HOME"):
            home = Path(os.environ["JAVA_HOME"])
        else:
            parser.error("Provide --java-home pointing to a Java 21 JDK")
    suffix = ".exe" if os.name == "nt" else ""
    javac, java = str(home / ("bin/javac" + suffix)), str(home / ("bin/java" + suffix))
    classes = ROOT / "work/mac-core-audit/classes"
    classes.mkdir(parents=True, exist_ok=True)
    # The preserved Forge release embeds the exact JOML version used by both adapters.
    joml = ROOT / "release/badlion-cosmetic-port-forge-0.4.0.jar"
    models = list((ROOT / "shared/src/main/java/local/badlioncosmeticport/model").glob("*.java"))
    geometry = ROOT / "shared/src/main/java/local/badlioncosmeticport/render/ObjGeometry.java"
    references = list((ROOT / "tests/native-reference").rglob("*.java"))
    fixtures = [ROOT / ("tests/" + name + ".java") for name in
                ("MeshCoreAudit", "NativeMotionParity", "NativeRibbonParity", "WingJointParity")]
    subprocess.run([javac, "-cp", str(joml), "-d", str(classes)]
                   + [str(p) for p in models + [geometry] + references + fixtures], check=True)
    reports = {}
    for test in ("MeshCoreAudit", "NativeMotionParity", "NativeRibbonParity",
                 "local.badlioncosmeticport.model.WingJointParity"):
        print("Running " + test, flush=True)
        result = subprocess.run([java, "-Xmx768m", "-cp", os.pathsep.join([str(classes), str(joml)]),
                                 test, str(ROOT / "exports/mesh-assets/models")],
                                capture_output=True, text=True, check=True, timeout=120)
        reports[test.rsplit(".", 1)[-1]] = json.loads(result.stdout)
        print(result.stdout.strip())
    destination = ROOT / "exports/mac-core-validation.json"
    destination.write_text(json.dumps({"hostArchitecture": platform.machine(), "audits": reports}, indent=2))
    print("Saved " + str(destination))


if __name__ == "__main__":
    main()
