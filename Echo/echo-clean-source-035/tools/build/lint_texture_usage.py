#!/usr/bin/env python3
"""Reject texture usages that cannot be used as a render attachment.

26.2 validates usage at *render-pass creation*, not at allocation, so a wrong
value compiles, loads, and then throws at the worst possible moment:

    IllegalStateException: Color texture must have USAGE_COPY_DST

That is how the item atlas took out every module that draws items. Vanilla's own
RenderTarget allocates with usage 15 for exactly this reason, so any
``GpuDevice.createTexture(name, <usage>, format, ...)`` must carry:

    USAGE_COPY_DST (1)            -- may be written by a copy
    USAGE_RENDER_ATTACHMENT (8)   -- may be attached to a pass

    python3 tools/build/lint_texture_usage.py [--root Echo/echo-clean-source-035]
"""

import argparse
import pathlib
import re

COPY_DST = 1
RENDER_ATTACHMENT = 8

# GpuDevice.createTexture("name", <usage>, GpuFormat, ...)
CALL = re.compile(r'createTexture\(\s*"[^"]*"\s*,\s*([\w$]+)\s*,')
# private static final int FOO_USAGE = 1 | 2 | 4 | 8;
CONST = re.compile(r"static\s+final\s+int\s+([\w$]*USAGE\w*)\s*=\s*([0-9|\s]+);")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", default=".")
    args = ap.parse_args()

    root = pathlib.Path(args.root) / "src/main/java"
    problems = []
    checked = 0

    for path in sorted(root.rglob("*.java")):
        text = path.read_text(errors="replace")
        constants = {}
        for name, expr in CONST.findall(text):
            try:
                constants[name] = eval(expr, {"__builtins__": {}}, {})
            except Exception:
                continue
        for match in CALL.finditer(text):
            token = match.group(1)
            if token.isdigit():
                usage = int(token)
            elif token in constants:
                usage = constants[token]
            else:
                continue  # declared in another file; validated where it is defined
            checked += 1
            missing = []
            if not usage & COPY_DST:
                missing.append("USAGE_COPY_DST (1)")
            if not usage & RENDER_ATTACHMENT:
                missing.append("USAGE_RENDER_ATTACHMENT (8)")
            if missing:
                line = text[:match.start()].count("\n") + 1
                problems.append(f"{path.name}:{line}: usage {usage} is missing "
                                + " and ".join(missing))

    print(f"checked {checked} texture usages")
    for problem in sorted(set(problems)):
        print("FATAL  " + problem)
    if not problems:
        print("OK: every checked usage can be used as a render attachment")
    return 1 if problems else 0


if __name__ == "__main__":
    raise SystemExit(main())
