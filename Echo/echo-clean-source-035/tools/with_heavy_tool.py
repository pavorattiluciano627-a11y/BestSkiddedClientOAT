#!/usr/bin/env python3
"""Serialize memory-heavy compiler/decompiler/runtime jobs across recovery workers."""
import fcntl
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import signal
import subprocess
import sys

ROOT = Path(__file__).resolve().parent.parent


def remaining_budget():
    path = ROOT / "integration/time-budget.json"
    if not path.exists():
        return None
    deadline_value = json.loads(path.read_text())["deadline_utc"]
    if deadline_value is None:
        return None
    deadline = datetime.fromisoformat(deadline_value)
    remaining = (deadline - datetime.now(timezone.utc)).total_seconds()
    if remaining <= 0:
        raise SystemExit("Recovery time budget expired; no new heavy job started.")
    return remaining


def run(command, environment):
    timeout = remaining_budget()
    process = subprocess.Popen(command, env=environment, start_new_session=True)
    try:
        code = process.wait(timeout=timeout)
    except subprocess.TimeoutExpired:
        os.killpg(process.pid, signal.SIGTERM)
        try:
            process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            os.killpg(process.pid, signal.SIGKILL)
            process.wait()
        raise SystemExit("Recovery time budget expired; this job's process group was stopped.")
    return code if code >= 0 else 128 - code


def main():
    command = sys.argv[1:]
    if command[:1] == ["--"]:
        command = command[1:]
    if not command:
        raise SystemExit("Usage: python3 tools/with_heavy_tool.py -- COMMAND [ARG ...]")
    remaining_budget()
    if os.environ.get("ECHO_HEAVY_TOOL_LOCK_HELD") == "1":
        raise SystemExit(run(command, os.environ))
    lock_path = ROOT / "analysis/.heavy-tool.lock"
    lock_path.parent.mkdir(parents=True, exist_ok=True)
    with lock_path.open("a") as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        environment = dict(os.environ, ECHO_HEAVY_TOOL_LOCK_HELD="1")
        code = run(command, environment)
    raise SystemExit(code)


if __name__ == "__main__":
    main()
