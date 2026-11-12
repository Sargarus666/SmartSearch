#!/usr/bin/env python3
"""Extract inline JavaScript from the static page and ask Node to syntax-check it."""
import re
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
html = (ROOT / "web/index.html").read_text(encoding="utf-8")
scripts = re.findall(r"<script(?:\s[^>]*)?>(.*?)</script\s*>", html, flags=re.I | re.S)
if not scripts:
    raise SystemExit("No inline JavaScript found in web/index.html")
source = "\n".join(scripts)
with tempfile.NamedTemporaryFile("w", suffix=".js", encoding="utf-8") as handle:
    handle.write(source)
    handle.flush()
    subprocess.run(["node", "--check", handle.name], check=True)
print(f"OK: JavaScript syntax valid ({len(scripts)} inline script block(s))")
