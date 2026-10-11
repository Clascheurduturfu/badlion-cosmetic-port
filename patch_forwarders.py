"""Replace reserved module names with an absolute system VERSION path.

MSVC's .def parser does not accept drive-qualified forwarding module names.
This only replaces reserved export-string bytes, without changing PE layout.
"""
from pathlib import Path
import os
import sys

reserved = b"BCP_SYSTEM_VERSION_PLACEHOLDER_0000000000000000000000000000000000"
system = (os.environ["SystemRoot"] + r"\System32\version.dll").encode("ascii")
if len(system) > len(reserved):
    raise SystemExit("System directory exceeds reserved forwarder space")
path = Path(sys.argv[1])
data = bytearray(path.read_bytes())
count = 0
offset = 0
while True:
    start = data.find(reserved + b".", offset)
    if start < 0:
        break
    end = data.index(0, start)
    name = data[start + len(reserved) + 1:end]
    replacement = system + b"." + name
    data[start:end] = replacement + bytes(end - start - len(replacement))
    count += 1
    offset = end
if count == 0 and data.count(system + b".") == 17:
    sys.exit(0)  # MSBuild can rerun post-build commands without relinking.
if count != 17:
    raise SystemExit(f"Expected 17 forwarders, found {count}")
path.write_bytes(data)
