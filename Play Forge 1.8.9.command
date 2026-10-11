#!/bin/sh
set -eu
cd "$(dirname "$0")"
if [ ! -f work/forge-mac-0.5.0/runtime.json ]; then
    exec python3 tools/forge-mac.py --prepare --play
fi
exec python3 tools/forge-mac.py --play
