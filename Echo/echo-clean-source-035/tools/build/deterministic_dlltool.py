#!/usr/bin/env python3
"""Invoke GNU dlltool with a stable, DEF-derived import-library namespace.

GNU dlltool derives internal COFF head/iname symbols from --output-lib. Rust's
raw-dylib output path contains random rustc temporary directory names. A relative
name derived from the complete DEF input keeps distinct import fragments unique
while making the final DLL reproducible. Runtime imports/exports are untouched.
"""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile


def main():
    real = os.environ.get("ECHO_REAL_DLLTOOL")
    if not real:
        raise SystemExit("ECHO_REAL_DLLTOOL must name the verified GNU dlltool")
    arguments = sys.argv[1:]
    positions = {}
    for index, value in enumerate(arguments):
        if value in ("--input-def", "-d", "--output-lib", "-l"):
            positions["input" if value in ("--input-def", "-d") else "output"] = index + 1
    if set(positions) != {"input", "output"}:
        # Rust's pinned invocation uses separate input/output arguments. Refuse an
        # unexpected compiler contract instead of silently producing random names.
        raise SystemExit("Unexpected dlltool invocation: " + repr(arguments))
    definition = Path(arguments[positions["input"]]).resolve()
    output = Path(arguments[positions["output"]]).resolve()
    data = definition.read_bytes()
    semantic = list(arguments)
    semantic[positions["input"]] = "<DEF>"
    semantic[positions["output"]] = "<OUTPUT>"
    digest = hashlib.sha256(data + json.dumps(semantic, separators=(",", ":")).encode()).hexdigest()
    stable_name = "imports_" + digest + ".lib"
    output.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="dlltool-", dir=output.parent) as temporary:
        work = Path(temporary)
        local_def = work / "imports.def"
        local_def.write_bytes(data)
        actual = list(arguments)
        actual[positions["input"]] = local_def.name
        actual[positions["output"]] = stable_name
        actual += ["--temp-prefix", "imports_" + digest[:16], "--deterministic-libraries"]
        subprocess.run([real, *actual], cwd=work, check=True)
        shutil.copyfile(work / stable_name, output)
    directory = os.environ.get("ECHO_DLLTOOL_RECEIPTS")
    if directory:
        report = Path(directory)
        report.mkdir(parents=True, exist_ok=True)
        key = hashlib.sha256(str(output).encode()).hexdigest()
        (report / (key + ".json")).write_text(json.dumps({"definition_sha256": hashlib.sha256(data).hexdigest(),
            "semantic_arguments": semantic, "stable_library_name": stable_name, "original_output": str(output),
            "underlying_dlltool": real, "output_sha256": hashlib.sha256(output.read_bytes()).hexdigest()}, indent=2) + "\n")


if __name__ == "__main__":
    main()
