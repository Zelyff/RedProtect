#!/usr/bin/env python3
"""Read-only check of files captured from the pinned upstream checkout.

This verifies source preservation, not plugin behavior or production safety.
Run from any directory: python tools/verify_upstream_baseline.py
"""
import hashlib
import json
import pathlib
import sys

root = pathlib.Path(__file__).resolve().parents[1]
manifest = json.loads((root / 'evidence/UPSTREAM-FILES.json').read_text(encoding='utf-8'))
allowed = manifest['allowed_build_changes']
failures = []
checked = 0
for name, original in manifest['files'].items():
    path = root / name
    if not path.is_file():
        failures.append(f'MISSING {name}')
        continue
    actual = hashlib.sha256(path.read_bytes()).hexdigest()
    expected = allowed.get(name, original)
    if actual != expected:
        failures.append(f'CHANGED {name}')
    checked += 1
for failure in failures:
    print(failure)
print(f'Upstream SHA: {manifest["upstream_sha"]}')
print(f'Files checked: {checked}; unexpected differences: {len(failures)}')
print('This does not test compilation, runtime protection, migration, or database compatibility.')
sys.exit(bool(failures))
