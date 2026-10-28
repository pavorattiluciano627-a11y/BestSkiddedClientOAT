#!/usr/bin/env python3
"""Validate every Mixin target in the Echo sources against the real 26.2 jar.

Mixin selectors are strings resolved at runtime, so javac cannot catch a target
that no longer exists -- the failure only shows up as a crash on the first
class transform. This walks each mixin source, resolves its `@Mixin` target and
every injection/accessor selector, and checks them against the class index built
by tools/port/jar_index.py.

Members are resolved up the superclass chain, because a mixin may legitimately
target a method the target class inherits.

Usage:
    python3 tools/port/verify_mixins.py <index.json> [--root src/main/java]
Exit status is 1 when any selector is unresolvable.
"""
import argparse
import json
import pathlib
import re
import sys

JAVA = pathlib.Path("src/main/java")

INJECT_ANNOTATIONS = [
    "Inject", "Redirect", "ModifyArg", "ModifyArgs", "ModifyVariable",
    "ModifyConstant", "WrapOperation", "WrapWithCondition", "ModifyExpressionValue",
]


class Index:
    def __init__(self, path):
        import gzip
        opener = gzip.open if str(path).endswith(".gz") else open
        data = json.load(opener(path, "rt"))
        # Accept both the wrapped {"classes": {...}} index and a bare mapping.
        self.classes = data["classes"] if "classes" in data else data

    def get(self, internal):
        return self.classes.get(internal)

    def chain(self, internal):
        """Yield (class, entry) for the class and each resolvable superclass."""
        seen = set()
        cur = internal
        while cur and cur in self.classes and cur not in seen:
            seen.add(cur)
            entry = self.classes[cur]
            yield cur, entry
            cur = entry.get("s")

    def has_method(self, internal, name, descriptor=None):
        for _, entry in self.chain(internal):
            for m in entry["m"]:
                if m["n"] != name:
                    continue
                if descriptor is None or m["d"] == descriptor:
                    return True
        return False

    def methods_named(self, internal, name):
        out = []
        for _, entry in self.chain(internal):
            for m in entry["m"]:
                if m["n"] == name:
                    out.append(m)
        return out

    def has_field(self, internal, name, descriptor=None):
        for _, entry in self.chain(internal):
            for f in entry["f"]:
                if f["n"] != name:
                    continue
                if descriptor is None or f["d"] == descriptor:
                    return True
        return False


_INDEX = None


def resolve_class(name, text, own_package):
    """Turn a @Mixin target reference into a JVM internal name.

    Handles ``Foo.class``, ``Outer.Inner.class`` (-> ``a/b/Outer$Inner``) and
    fully-qualified references, preferring whatever the index actually knows.
    """
    simple = name.split(".")[-1]
    m = re.search(rf"^import\s+([\w.]+\.{re.escape(simple)});", text, re.M)
    if m:
        imported = m.group(1).replace(".", "/")
        candidates = [imported]
        # Outer.Inner -> Outer$Inner ; Outer.Inner.Deeper -> Outer$Inner$Deeper
        parts = m.group(1).split(".")
        for i in range(len(parts) - 1, 0, -1):
            candidates.append("/".join(parts[:i]) + "/" + "$".join(parts[i:]))
        for cand in candidates:
            if cand in _INDEX.classes:
                return cand
        return candidates[0]
    if "." in name:
        parts = name.split(".")
        candidates = ["/".join(parts)]
        # Outermost segment is often the imported class: BlockBehaviour.BlockStateBase
        head = next((imp for imp in re.findall(r"^import\s+([\w.]+);", text, re.M)
                     if imp.rsplit(".", 1)[-1] == parts[0]), None)
        if head:
            candidates.append(head.replace(".", "/") + "$" + "$".join(parts[1:]))
        for i in range(len(parts) - 1, 0, -1):
            candidates.append("/".join(parts[:i]) + "/" + "$".join(parts[i:]))
        for cand in candidates:
            if cand in _INDEX.classes:
                return cand
        return candidates[0]
    cand = f"{own_package}/{simple}".replace(".", "/")
    return cand


def annotations(text, names):
    """Yield (annotation_name, argument_text) for annotations in `names`.

    Scans to the matching close paren so multi-line arguments are captured.
    """
    for name in names:
        for m in re.finditer(rf"@{name}\s*\(", text):
            i = m.end()
            depth = 1
            start = i
            while i < len(text) and depth:
                ch = text[i]
                if ch == '"':
                    i += 1
                    while i < len(text) and text[i] != '"':
                        if text[i] == '\\':
                            i += 1
                        i += 1
                elif ch == '(':
                    depth += 1
                elif ch == ')':
                    depth -= 1
                i += 1
            yield name, text[start:i - 1]


def split_selector(selector):
    """'name(desc)' -> ('name', 'desc'); 'name' -> ('name', None)."""
    selector = selector.strip()
    if selector.startswith("*"):
        return None, None
    # descriptors keep the return type after the parens: foo()V, bar(I)Z
    m = re.match(r"^([\w$<>]+)(\([^)]*\).*)$", selector)
    if m:
        return m.group(1), m.group(2)
    return selector, None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("index")
    ap.add_argument("--root", default=str(JAVA))
    args = ap.parse_args()

    idx = Index(args.index)
    global _INDEX
    _INDEX = idx
    root = pathlib.Path(args.root)

    problems = []
    external_only = []
    checked = 0
    files = 0

    for path in sorted(root.rglob("*.java")):
        text = path.read_text(errors="replace")
        if "@Mixin" not in text:
            continue
        files += 1
        own_package = re.search(r"^package\s+([\w.]+);", text, re.M)
        own_package = own_package.group(1) if own_package else ""

        # @Mixin may take several targets; validate each.
        targets = []
        for m in re.finditer(r"@Mixin\(\s*(?:value\s*=\s*)?\{?([^)}]+)\}?\s*\)", text):
            for part in m.group(1).split(","):
                part = part.strip()
                cm = re.match(r"([\w.$]+)\.class", part)
                if cm:
                    targets.append(resolve_class(cm.group(1), text, own_package))
        for m in re.finditer(r"@Mixin\(\s*targets\s*=\s*\"([^\"]+)\"", text):
            targets.append(m.group(1).replace(".", "/"))
        if not targets:
            continue

        # A class mixin may only target a class, and an interface mixin only an
        # interface -- Mixin aborts the whole config at prepare time otherwise:
        #   InvalidMixinException: @Mixin target type mismatch: X is an interface
        kind = re.search(r"\b(class|interface|enum)\s+" + re.escape(path.stem) + r"\b", text)
        kind = kind.group(1) if kind else "class"
        for target in targets:
            entry = idx.get(target)
            if entry is None:
                continue
            is_interface = bool(entry.get("a", 0) & 0x0200)
            if is_interface and kind != "interface":
                problems.append((path.name, target,
                                 f"@Mixin target is an interface but the mixin is a {kind} "
                                 f"(declare it as 'public interface')", False))
            # The reverse is legal: @Accessor/@Invoker mixins are interfaces
            # whose abstract methods are merged into a concrete class.

        existing = [t for t in targets if idx.get(t)]
        if not existing:
            external = [t for t in targets
                        if not t.startswith(("net/minecraft/", "com/mojang/"))]
            if len(external) == len(targets):
                # Foreign/optional target (Lunar, another mod, a Fabric API class).
                # EchoMixinPlugin#shouldApplyMixin skips it when the class is absent.
                external_only.append(path.name)
            else:
                problems.append((path.name, "@Mixin target(s) not in 26.2: "
                                 + ", ".join(t for t in targets if idx.get(t) is None)))
            continue

        for target in existing:
            # @Inject / @Redirect / ... method selectors
            for ann, body in annotations(text, INJECT_ANNOTATIONS):
                    mm = re.search(r"method\s*=\s*(\{[^}]*\}|\"[^\"]*\")", body)
                    if not mm:
                        continue
                    raw = mm.group(1)
                    selectors = re.findall(r"\"([^\"]*)\"", raw)
                    require_zero = re.search(r"require\s*=\s*0", body) is not None
                    for sel in selectors:
                        name, desc = split_selector(sel)
                        if name is None:
                            continue
                        checked += 1
                        if not idx.has_method(target, name, desc):
                            candidates = []
                            for _, e in idx.chain(target):
                                for cand in e["m"]:
                                    if cand["n"].lower() == name.lower():
                                        candidates.append(cand["n"] + cand["d"])
                            problems.append((
                                path.name, target,
                                f"@{ann} method '{sel}' missing"
                                + (f" (same-name 26.2 members: {', '.join(candidates[:3])})" if candidates else ""),
                                require_zero,
                            ))
            # @Accessor / @Invoker
            for m in re.finditer(r"@(Accessor|Invoker)\(\s*(?:value\s*=\s*)?\"([^\"]+)\"", text):
                kind, sel = m.group(1), m.group(2)
                name, desc = split_selector(sel)
                if name is None:
                    continue
                checked += 1
                ok = idx.has_field(target, name) if kind == "Accessor" else idx.has_method(target, name)
                if not ok:
                    problems.append((path.name, target, f"@{kind} '{sel}' missing", False))

    print(f"scanned {files} mixin sources, {checked} selectors")
    if external_only:
        print("optional (external/plugin-gated): " + ", ".join(sorted(set(external_only))))
    if not problems:
        print("OK: every mixin selector resolves against 26.2")
        return 0

    def is_optional(p):
        return len(p) == 4 and p[3]
    fatal = [p for p in problems if not is_optional(p)]
    optional = [p for p in problems if is_optional(p)]
    for p in fatal:
        if len(p) == 4:
            print(f"FATAL  {p[0]} -> {p[1]}: {p[2]}")
        else:
            print(f"FATAL  {p[0]}: {p[1]}")
    for p in optional:
        print(f"opt    {p[0]} -> {p[1]}: {p[2]}  (require=0, silent no-op)")
    print(f"\n{len(fatal)} fatal, {len(optional)} optional (require=0)")
    return 1 if fatal else 0


if __name__ == "__main__":
    sys.exit(main())
