#!/usr/bin/env bash
# Capture desktop + mobile screenshots of $CAPTURE_URL into $CAPTURE_DIR.
# Leaves the app server running; closes only its own browser.
set -euo pipefail

cd "$(dirname "$0")"
/usr/bin/time -p pwd

/usr/bin/time -p bash -c 'test -n "${CAPTURE_URL:-}"' || { echo 'CAPTURE_URL is not set' >&2; exit 1; }
/usr/bin/time -p bash -c 'test -n "${CAPTURE_DIR:-}"' || { echo 'CAPTURE_DIR is not set' >&2; exit 1; }
/usr/bin/time -p bash -c 'test -n "${RUNTIME_DIR:-}"' || { echo 'RUNTIME_DIR is not set' >&2; exit 1; }
/usr/bin/time -p mkdir -p "$CAPTURE_DIR"
/usr/bin/time -p bash -c 'echo "capturing $0 into $1"' "$CAPTURE_URL" "$CAPTURE_DIR"

/usr/bin/time -p node "$RUNTIME_DIR/scripts/default-capture.mjs"
status=$?

/usr/bin/time -p bash -c 'ls -la "$0"' "$CAPTURE_DIR"
if [ "$status" -ne 0 ]; then
  /usr/bin/time -p echo "capture failed with status $status (75=temp infra, 1=script/defect)"
  exit "$status"
fi
if /usr/bin/time -p bash -c 'test -f "$0/final-desktop.png" -a -f "$0/final-mobile.png"' "$CAPTURE_DIR"; then
  /usr/bin/time -p echo "capture OK: final-desktop.png + final-mobile.png"
else
  /usr/bin/time -p echo "capture defect: expected screenshots missing in $CAPTURE_DIR" >&2
  exit 1
fi
