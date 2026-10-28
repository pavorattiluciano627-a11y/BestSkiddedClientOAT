#!/usr/bin/env python3
"""Refresh stale ``@At(target = "Lowner;name(desc)ret")`` descriptors.

26.2 reshaped a lot of signatures without renaming anything -- rotation
getters grew a partial-tick parameter (``getYRot()F`` -> ``getYRot(F)F``),
``PoseStack`` methods gained overloads, and so on. The member still exists,
only the descriptor in the annotation is from the Yarn-era source, and Mixin
will not find the call site.

This rewrites those descriptors from the 26.2 index, matching by owner and
name and disambiguating by parameter count. When more than one candidate
survives it leaves the selector alone and reports it, so genuinely removed
members stay visible instead of being guessed at.

    python3 tools/port/fix_at_targets.py <index.json> [--write]
"""

import argparse
import json
import pathlib
import re
import sys

SRC = pathlib.Path("src/main/java")


def param_count(desc: str):
    if not desc.startswith("("):
        return None
    body = desc[1:desc.index(")")]
    n, i = 0, 0
    while i < len(body):
        if body[i] == "L":
            i = body.index(";", i) + 1
        elif body[i] == "[":
            i += 1
            continue
        else:
            i += 1
        n += 1
    return n


class Index:
    def __init__(self, path):
        import gzip
        opener = gzip.open if str(path).endswith(".gz") else open
        data = json.load(opener(path, "rt"))
        self.classes = data["classes"] if "classes" in data else data

    def chain(self, internal):
        seen, cur = set(), internal
        while cur and cur in self.classes and cur not in seen:
            seen.add(cur)
            entry = self.classes[cur]
            yield cur, entry
            cur = entry.get("s")

    def methods(self, owner):
        return [(m["n"], m["d"]) for _, e in self.chain(owner) for m in e["m"]]

    def fields(self, owner):
        return [(f["n"], f["d"]) for _, e in self.chain(owner) for f in e["f"]]


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("index")
    ap.add_argument("--root", default=str(SRC))
    ap.add_argument("--write", action="store_true")
    args = ap.parse_args()

    idx = Index(args.index)
    root = pathlib.Path(args.root)

    fixed = unresolved = files = 0
    for path in sorted(root.rglob("*.java")):
        text = path.read_text(errors="replace")
        if "@At(" not in text:
            continue
        original = text
        touched = False

        def retarget(match):
            nonlocal fixed, unresolved, touched
            raw = match.group(1)
            invoke = re.match(r"^L([^;]+);([\w$<>]+)(\([^)]*\).*)$", raw)
            field = re.match(r"^L([^;]+);([\w$]+):(\S+)$", raw)
            if invoke:
                owner, name, desc = invoke.groups()
                if owner not in idx.classes:
                    return match.group(0)
                members = [(n, d) for n, d in idx.methods(owner) if n == name]
                if (name, desc) in members:
                    return match.group(0)
                same = [d for _, d in members if param_count(d) == param_count(desc)]
                if len(same) == 1:
                    fixed += 1
                    touched = True
                    return f'target = "L{owner};{name}{same[0]}"'
                unresolved += 1
                print(f"  {path.name}: {owner.split('/')[-1]}.{name}{desc} "
                      f"({len(same)} candidates)")
                return match.group(0)
            if field:
                owner, name, desc = field.groups()
                if owner not in idx.classes:
                    return match.group(0)
                members = [(n, d) for n, d in idx.fields(owner) if n == name]
                if (name, desc) in members:
                    return match.group(0)
                if len(members) == 1:
                    fixed += 1
                    touched = True
                    return f'target = "L{owner};{name}:{members[0][1]}"'
                unresolved += 1
                print(f"  {path.name}: {owner.split('/')[-1]}.{name}:{desc} "
                      f"({len(members)} candidates)")
                return match.group(0)
            return match.group(0)

        text = re.sub(r'target\s*=\s*"([^"]+)"', retarget, text)
        if touched:
            files += 1
            if args.write:
                path.write_text(text)
            else:
                pass

    print(f"{'wrote' if args.write else 'would rewrite'} {files} files, "
          f"{fixed} descriptors refreshed, {unresolved} unresolved")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
