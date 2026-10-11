#!/bin/sh
set -eu
cd "$(dirname "$0")"
if [ ! -f work/forge-1.12.2-mac/runtime.json ]; then
    exec python3 tools/forge-mac.py --minecraft 1.12.2 --prepare --play
fi
exec python3 tools/forge-mac.py --minecraft 1.12.2 --play
