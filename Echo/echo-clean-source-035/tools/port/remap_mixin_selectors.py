#!/usr/bin/env python3
"""Remap stale Yarn selectors inside Mixin annotations to 26.2 official names.

Loom's source migration rewrites Java code, but a Mixin selector is a *string*
(`method = "setupFog(...)"`). Those strings kept their Yarn names through the
port, so the mixins compile fine and then fail at class-transform time -- which
is exactly the crash this fixes.

Namespaces involved for 1.21.11:
    tiny      : obfused  <-> intermediary <-> yarn-named
    client.txt: obfuscated <-> mojang-official
The source tree was remapped to mojang-official, so `obf` is the pivot between
"what the string says" (yarn) and "what the class is called" (mojang).

The new selector is emitted with the descriptor read from the real 26.2 jar
index, so it matches what the game actually declares today.

Usage:
    python3 tools/port/remap_mixin_selectors.py <tiny> <client.txt> <index.json> [--write]
"""
import argparse
import json
import pathlib
import re
import sys
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[2] / "src/main/java"


# ---------------------------------------------------------------- mappings


def read_tiny(path):
    """obf class -> named class, and obf class -> {obf method name: named name}."""
    if path.endswith(".jar"):
        with zipfile.ZipFile(path) as z:
            data = z.read("mappings/mappings.tiny").decode("utf-8")
    else:
        data = pathlib.Path(path).read_text(encoding="utf-8")

    cls_named = {}
    methods = {}
    owner = None
    for line in data.split("\n"):
        if not line:
            continue
        parts = line.split("\t")
        if parts[0] == "c":
            owner = parts[1]              # obfuscated in the 1.21.11 namespace
            cls_named[owner] = parts[3]   # yarn named
            methods.setdefault(owner, {})
        elif parts[0] == "" and parts[1] == "m" and owner:
            obf_name, named_name = parts[3], parts[5]
            methods[owner][obf_name] = named_name
    return cls_named, methods


def read_client_txt(path):
    """obf class -> mojang class, and obf class -> {obf member name: mojang name}."""
    text = pathlib.Path(path).read_text(encoding="utf-8", errors="replace")
    cls = {}
    members = {}
    cur_obf = None
    for line in text.split("\n"):
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        # ProGuard layout is `mojangName -> obfName` with the obfuscated name on
        # the right, both for class headers and for members.
        if line.endswith(":") and "->" in line:
            mojang, obf = line[:-1].split(" -> ")
            cur_obf = obf.strip()
            cls[cur_obf] = mojang.strip()
            members.setdefault(cur_obf, {})
        elif line.startswith(" ") and "->" in line and cur_obf:
            left, right = line.rsplit(" -> ", 1)
            obf_name = right.strip()
            left = left.strip()
            if "(" in left:
                mojang_name = left[:left.index("(")].split()[-1]
            else:
                mojang_name = left.split()[-1]
            members[cur_obf][obf_name] = mojang_name
    return cls, members


def build_map(tiny_path, client_path):
    """mojang owner -> list of (yarn name, mojang name) for methods."""
    cls_named, tiny_methods = read_tiny(tiny_path)
    cls_mojang, mojang_members = read_client_txt(client_path)

    out = {}
    for obf, mojang in cls_mojang.items():
        named_methods = tiny_methods.get(obf, {})
        if not named_methods:
            continue
        entries = []
        for obf_name, mojang_name in mojang_members.get(obf, {}).items():
            yarn = named_methods.get(obf_name)
            if yarn:
                entries.append((yarn, mojang_name))
        if entries:
            out.setdefault(mojang, []).extend(entries)
    return out


# ------------------------------------------------------------------- index


class Index:
    def __init__(self, path):
        data = json.load(open(path))
        self.classes = data["classes"] if "classes" in data else data

    def chain(self, internal):
        seen, cur = set(), internal
        while cur and cur in self.classes and cur not in seen:
            seen.add(cur)
            entry = self.classes[cur]
            yield cur, entry
            cur = entry.get("s")

    def methods(self, owner):
        for _, entry in self.chain(owner):
            for m in entry["m"]:
                yield m


INDEX = None


def arity(desc):
    """Number of parameters in a JVM descriptor."""
    if not desc or not desc.startswith("("):
        return None
    body = desc[1:desc.index(")")]
    n, i = 0, 0
    while i < len(body):
        c = body[i]
        if c == "L":
            i = body.index(";", i) + 1
        elif c == "[":
            i += 1
            continue
        else:
            i += 1
        n += 1
    return n


def resolve_target(cls_ref, text):
    simple = cls_ref.split(".")[-1]
    m = re.search(rf"^import\s+([\w.]+\.{re.escape(simple)});", text, re.M)
    if m:
        return m.group(1).replace(".", "/")
    if "." in cls_ref:
        parts = cls_ref.split(".")
        dotted = "/".join(parts)
        if dotted in INDEX.classes:
            return dotted
        nested = "/".join(parts[:-2]) + "/" + parts[-2] + "$" + parts[-1]
        if nested in INDEX.classes:
            return nested
        return dotted
    return cls_ref


def annotations(text):
    names = ["Inject", "Redirect", "ModifyArg", "ModifyArgs", "ModifyVariable",
             "ModifyConstant", "WrapOperation", "WrapWithCondition",
             "ModifyExpressionValue"]
    for name in names:
        for m in re.finditer(rf"@{name}\s*\(", text):
            i = m.end()
            depth, start = 1, i
            while i < len(text) and depth:
                ch = text[i]
                if ch == '"':
                    i += 1
                    while i < len(text) and text[i] != '"':
                        if text[i] == "\\":
                            i += 1
                        i += 1
                elif ch == "(":
                    depth += 1
                elif ch == ")":
                    depth -= 1
                i += 1
            yield name, text[start:i - 1], start


def remap_file(path, name_map):
    text = path.read_text(errors="replace")
    if "@Mixin" not in text:
        return text, []

    targets = []
    for m in re.finditer(r"@Mixin\(\s*(?:value\s*=\s*)?\{?([^)}]+)\}?\s*\)", text):
        for part in m.group(1).split(","):
            cm = re.match(r"\s*([\w.$]+)\.class", part)
            if cm:
                t = resolve_target(cm.group(1), text)
                if t in INDEX.classes:
                    targets.append(t)
    if not targets:
        return text, []

    # yarn method name -> mojang method name, across the target's hierarchy
    table = {}
    for target in targets:
        for owner, _ in INDEX.chain(target):
            for yarn, mojang in name_map.get(owner, ()):
                table.setdefault(yarn, set()).add(mojang)

    unresolved, edits = [], []
    for ann, body, bstart in annotations(text):
        mm = re.search(r"method\s*=\s*(\{[^}]*}\"[^\"]*\"|\{[^}]*\}|\"[^\"]*\")", body)
        if not mm:
            continue
        raw = mm.group(1)
        for sm in re.finditer(r'"([^"]*)"', raw):
            sel = sm.group(1)
            # descriptors carry a return type after the parens: foo()V, bar(I)Z
            call = re.match(r"^([\w$<>]+)(\([^)]*\).*)$", sel)
            if call:
                sel_name, sel_desc = call.group(1), call.group(2)
                want_arity = arity(sel_desc)
            else:
                # Name-only selector: Mixin matches every overload. Keep that
                # shape and just retarget the name.
                sel_name, sel_desc, want_arity = sel, None, None

            # Already valid against 26.2?
            ok = False
            for target in targets:
                for m in INDEX.methods(target):
                    if m["n"] == sel_name and (sel_desc is None or m["d"] == sel_desc):
                        ok = True
                        break
                if ok:
                    break
            if ok:
                continue

            # (a) The name is already official and 26.2 still declares it -- only
            #     the descriptor is stale (Yarn-era class paths). Refresh it.
            by_name = sorted({(m["n"], m["d"]) for target in targets
                              for m in INDEX.methods(target)
                              if m["n"] == sel_name
                              and (want_arity is None or arity(m["d"]) == want_arity)})
            if len(by_name) == 1:
                new_sel = f"{by_name[0][0]}{by_name[0][1] or ''}"
                if new_sel != sel:
                    edits.append((bstart + mm.start(1) + sm.start(1),
                                  bstart + mm.start(1) + sm.end(1), new_sel))
                    continue

            # (b) Fall back to the Yarn -> official name table.
            candidates = table.get(sel_name)
            if not candidates:
                if len(by_name) > 1:
                    unresolved.append((sel, f"ambiguous 26.2 overloads: {[b[0] for b in by_name][:4]}"))
                else:
                    unresolved.append((sel, "not declared by 26.2 target"))
                continue

            if sel_desc is None:
                # Name-only: accept if the mapped name exists on the target at all.
                present = sorted({m["n"] for mojang_name in candidates
                                  for target in targets
                                  for m in INDEX.methods(target)
                                  if m["n"] == mojang_name})
                if len(present) != 1:
                    unresolved.append((sel, f"name-only ambiguous: {present[:4]}"))
                    continue
                resolved = [(present[0], None)]
            else:
                resolved = []
                for mojang_name in candidates:
                    for target in targets:
                        for m in INDEX.methods(target):
                            if m["n"] != mojang_name:
                                continue
                            if want_arity is not None and arity(m["d"]) != want_arity:
                                continue
                            resolved.append((mojang_name, m["d"]))
                resolved = sorted(set(resolved))
                if len(resolved) != 1:
                    unresolved.append((sel, f"ambiguous: {len(resolved)} 26.2 candidates "
                                            + str([r[0] for r in resolved][:4])))
                    continue

            new_sel = f"{resolved[0][0]}{resolved[0][1] or ''}"
            if new_sel == sel:
                unresolved.append((sel, "maps to itself but still missing"))
                continue
            edits.append((bstart + mm.start(1) + sm.start(1),
                          bstart + mm.start(1) + sm.end(1), new_sel))

    for start, end, new in sorted(edits, reverse=True):
        text = text[:start] + new + text[end:]
    return text, unresolved


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("tiny")
    ap.add_argument("client_txt")
    ap.add_argument("index")
    ap.add_argument("--write", action="store_true")
    ap.add_argument("--root", default=str(ROOT))
    args = ap.parse_args()

    global INDEX
    INDEX = Index(args.index)
    name_map = build_map(args.tiny, args.client_txt)
    print(f"mapped {len(name_map)} mojang owners")

    changed, all_unresolved = 0, []
    for path in sorted(pathlib.Path(args.root).rglob("*.java")):
        before = path.read_text(errors="replace")
        new, unresolved = remap_file(path, name_map)
        all_unresolved += [(path.name, s, w) for s, w in unresolved]
        if new != before:
            changed += 1
            print(("wrote " if args.write else "[dry-run] ") + path.name)
            if args.write:
                path.write_text(new)

    print(f"\n{changed} mixin files affected, {len(all_unresolved)} unresolved")
    for f, sel, why in all_unresolved:
        print(f"  {f}: {sel}  ({why})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
