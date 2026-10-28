#!/usr/bin/env python3
"""Shrink a full jar index down to the classes the mixins actually touch.

The verifier only ever asks about ``@Mixin`` targets and their superclass
chains, so the complete 26.2 index (every one of ~11k classes) is dead weight
in CI and in review. This writes a small index that can be committed and
checked in seconds.

    python3 tools/port/prune_index.py <full-index.json> <mixins-json> <out.json>
"""

import json
import pathlib
import re
import sys

SRC = pathlib.Path("src/main/java")


def main() -> int:
    full, out_path = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[3])
    config = json.loads(pathlib.Path(sys.argv[2]).read_text())
    data = json.loads(full.read_text())
    classes = data["classes"] if "classes" in data else data

    sys.path.insert(0, str(pathlib.Path(__file__).parent))
    import verify_mixins as vm  # noqa: E402

    vm._INDEX = type("Idx", (), {"classes": classes})()

    names = set()
    for path in SRC.rglob("*.java"):
        text = path.read_text(errors="replace")
        if "@Mixin" not in text:
            continue
        pkg = re.search(r"^package\s+([\w.]+);", text, re.M)
        pkg = pkg.group(1) if pkg else ""
        for m in re.finditer(r"@Mixin\(\s*(?:value\s*=\s*)?\{?([^)}]+)\}?\s*\)", text):
            for part in m.group(1).split(","):
                cm = re.match(r"\s*([\w.$]+)\.class", part)
                if cm:
                    names.add(vm.resolve_class(cm.group(1), text, pkg))
        for m in re.finditer(r'@Mixin\(\s*targets\s*=\s*"([^"]+)"', text):
            names.add(m.group(1).replace(".", "/"))
        # @At owners are needed too: the handler-signature check has to know
        # whether a redirected member is static before it can count its params.
        for m in re.finditer(r'target\s*=\s*"L([^;]+);', text):
            names.add(m.group(1))

    # keep the whole superclass chain so inherited members still resolve
    keep, queue = set(), [n for n in names if n in classes]
    while queue:
        cur = queue.pop()
        if cur in keep:
            continue
        keep.add(cur)
        parent = classes[cur].get("s")
        if parent and parent in classes:
            queue.append(parent)

    pruned = {k: classes[k] for k in sorted(keep)}
    out_path.write_text(json.dumps({"version": data.get("version", "26.2"),
                                    "classes": pruned}, separators=(",", ":")))
    print(f"kept {len(pruned)} of {len(classes)} classes "
          f"({out_path.stat().st_size / 1024:.0f} KiB)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
