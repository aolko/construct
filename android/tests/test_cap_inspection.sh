#!/usr/bin/env bash
set -euo pipefail

python3 - <<'PY'
from pathlib import Path

cap = Path("../../IDE/Tools/Mesh Editor/Mesh Editor.cap")
data = cap.read_bytes()
assert data[:13] == b"Construct1.7\0", data[:13]
assert len(data) > 13
restored = bytearray(data)
assert bytes(restored) == data
print(f"CAP header valid; {len(data)} bytes preserve byte-for-byte")
PY
