#!/usr/bin/env python3
"""Build the editable source candidate locally, without any recovery checkout."""
import json
import os
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
REPORTS = ROOT / 'build/reports/candidate'

def run(name, command):
    REPORTS.mkdir(parents=True, exist_ok=True)
    print('+', ' '.join(map(str, command)), flush=True)
    with (REPORTS / (name + '.log')).open('w') as output:
        result = subprocess.run([str(p) for p in command], cwd=ROOT, stdout=output, stderr=subprocess.STDOUT)
    (REPORTS / (name + '-command.json')).write_text(json.dumps({'command': [str(p) for p in command],
        'returncode': result.returncode}, indent=2) + '\n')
    if result.returncode:
        print((REPORTS / (name + '.log')).read_text(), file=sys.stderr)
        raise SystemExit(result.returncode)

def main():
    wrapper = [sys.executable, ROOT / 'tools/with_heavy_tool.py', '--']
    gradle = [ROOT / ('gradlew.bat' if os.name == 'nt' else 'gradlew'), '--no-daemon', '--max-workers=1', '--no-build-cache']
    run('compile-native', wrapper + gradle + ['prepareCandidate'])
    run('remap-closure', [sys.executable, ROOT / 'tools/build/remap_closure.py'])
    run('loom-remap', wrapper + gradle + ['-p', 'verification-remap', 'candidateJar'])
    run('provenance', [sys.executable, ROOT / 'tools/build/publish_candidate.py'])
    print((ROOT / 'build/candidate/HANDOFF.json').read_text())

if __name__ == '__main__': main()
