#!/usr/bin/env bash
# Челны СмартСкан — static server. Serves prebuilt ./dist in the foreground on $PORT.
set -euo pipefail

cd "$(dirname "$0")"
/usr/bin/time -p pwd

PROJECT_ROOT="$(pwd)"
DIST_DIR="$PROJECT_ROOT/dist"
PORT="${PORT:-3000}"

/usr/bin/time -p test -f "$DIST_DIR/index.html"

if /usr/bin/time -p bash -c 'test -f package.json'; then
  if /usr/bin/time -p bash -c 'grep -q "\"build\"" package.json'; then
    /usr/bin/time -p bash -c 'if [ -f package-lock.json ]; then npm ci --no-audit --no-fund; else npm install --no-audit --no-fund; fi'
    /usr/bin/time -p npm run build
  fi
fi

OUT_DIR="${OPENCODE_WEB_DIR:-/home/runner/work/_temp/omgithub-web}"
/usr/bin/time -p mkdir -p "$OUT_DIR"
/usr/bin/time -p bash -c "printf '{\"project\":\"%s\",\"directory\":\"%s\"}' \"$PROJECT_ROOT\" \"$DIST_DIR\" > \"$OUT_DIR/deployment-output.json\""
/usr/bin/time -p cat "$OUT_DIR/deployment-output.json"
/usr/bin/time -p bash -c 'if [ "$1" != /home/runner/work/_temp/omgithub-web/deployment-output.json ]; then cp "$1" /home/runner/work/_temp/omgithub-web/deployment-output.json; fi' _ "$OUT_DIR/deployment-output.json"
/usr/bin/time -p echo "serving $DIST_DIR on port $PORT (foreground)"
cd "$DIST_DIR"
/usr/bin/time -p pwd
exec python3 -m http.server "$PORT" --bind 0.0.0.0
