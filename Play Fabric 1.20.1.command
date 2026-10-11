#!/bin/sh
set -eu
cd "$(dirname "$0")"
if [ ! -f work/fabric-1.20.1-mac/runtime.json ]; then
    exec python3 tools/fabric-mac.py --minecraft 1.20.1 --prepare --play
fi
exec python3 tools/fabric-mac.py --minecraft 1.20.1 --play
