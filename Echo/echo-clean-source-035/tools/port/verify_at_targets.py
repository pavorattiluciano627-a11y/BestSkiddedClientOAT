#!/usr/bin/env python3
"""Validate the *inside-the-method* half of every mixin injector.

`verify_mixins.py` checks that an injection selector (``method = "foo()V"``)
still names a real 26.2 method. That is only half the story: every
``@At(value = "INVOKE", target = "...")`` / ``"FIELD"`` / ``"NEW"`` also has to
point at an instruction that physically exists *inside* that method. When it
does not, Mixin throws

    InvalidInjectionException: ... could not find any targets matching ...

at class-load time, i.e. the client crashes even though the build was green.

This tool disassembles the affected classes with ``javap`` and proves each
``@At`` target is actually referenced by the selected method.

    python3 tools/port/verify_at_targets.py <client-26.2.jar> [--root src/main/java]

Exit code 1 when a target is unreachable.
"""

from __future__ import annotations

import argparse
import functools
import json
import pathlib
import re
import subprocess
import sys
import tempfile
import zipfile

JAVA = pathlib.Path("src/main/java")

INJECTOR_ANNOTATIONS = {
    "Inject", "Redirect", "ModifyArg", "ModifyArgs", "ModifyVariable",
    "ModifyConstant", "ModifyExpressionValue", "WrapOperation",
    "WrapWithCondition", "WrapMethod",
}


# ------------------------------------------------------------------ javap


class Disassembler:
    """Lazily disassembles classes out of a jar and indexes their methods."""

    def __init__(self, jar: pathlib.Path, javap: pathlib.Path):
        self._zip = zipfile.ZipFile(jar)
        self._names = set(self._zip.namelist())
        self._javap = javap
        self._cache: dict[str, dict[str, str]] = {}
        self._supers: dict[str, str] = {}

    def has(self, internal: str) -> bool:
        return internal + ".class" in self._names

    def supers(self, internal: str) -> list[str]:
        """The class plus every superclass that shares this jar."""
        out, cur, seen = [], internal, set()
        while cur and cur not in seen and self.has(cur):
            seen.add(cur)
            out.append(cur)
            self.methods(cur)  # populates _supers
            cur = self._supers.get(cur)
        return out

    def methods(self, internal: str) -> dict[str, str]:
        """{'name descriptor': '<java code text>'} for one class."""
        if internal in self._cache:
            return self._cache[internal]
        out: dict[str, str] = {}
        if self.has(internal):
            with tempfile.TemporaryDirectory() as tmp:
                dest = pathlib.Path(tmp) / (internal.replace("/", "_") + ".class")
                dest.write_bytes(self._zip.read(internal + ".class"))
                proc = subprocess.run(
                    [str(self._javap), "-p", "-c", "-s", str(dest)],
                    capture_output=True, text=True,
                )
                out = _split_methods(proc.stdout)
                sup = re.search(r"^(?:public |protected |private |final |abstract )*"
                                r"(?:class|interface) \w+(?:<[^>]*>)? extends ([\w/$]+)",
                                proc.stdout, re.M)
                if sup:
                    self._supers[internal] = sup.group(1)
        self._cache[internal] = out
        return out


_MEMBER_LINE = re.compile(r"^\s{2}(?:(?:public|protected|private|static|final|"
                          r"abstract|native|synchronized|strictfp|transient|"
                          r"volatile|default)\s+)*[\w$.\[\]<>]+(?: [\w$]+)?\(")
_DESC_LINE = re.compile(r"^\s{4}descriptor: (\S+)")


def _split_methods(text: str) -> dict[str, str]:
    """'name descriptor' -> method body, from `javap -c -s` output.

    The descriptor line follows the signature and *precedes* the Code block, so
    the key can only be finalised once the next member starts.
    """
    methods: dict[str, str] = {}
    name, key, body = None, None, []

    def flush():
        if key:
            methods[key] = "\n".join(body)

    for line in text.splitlines():
        if _MEMBER_LINE.match(line) or re.match(r"^\s{2}[\w$.]+\(\);", line):
            flush()
            method_name = re.search(r"[\w$]+(?=\()", line)
            name = method_name.group(0) if method_name else None
            key, body = None, []
            continue
        desc = _DESC_LINE.match(line)
        if desc and name:
            key, body = f"{name} {desc.group(1)}", []
            continue
        if key is not None:
            body.append(line)
    flush()
    return methods


# ------------------------------------------------------------------ parsing


def annotations(text: str) -> list[tuple[str, str, int, int]]:
    """(name, argument text, args start offset, args end offset)."""
    found = []
    for m in re.finditer(r"@(\w+)", text):
        if m.group(1) not in INJECTOR_ANNOTATIONS:
            continue
        start = text.find("(", m.end())
        if start == -1:
            continue
        depth, i = 0, start
        while i < len(text):
            if text[i] == "(":
                depth += 1
            elif text[i] == ")":
                depth -= 1
                if depth == 0:
                    break
            i += 1
        found.append((m.group(1), text[start + 1:i], start + 1, i))
    return found


def resolve(verify, name: str, text: str, pkg: str) -> str:
    return verify.resolve_class(name, text, pkg)


def at_targets(body: str) -> list[tuple[str, str]]:
    """(needle, description) for each @At(target = "..."), javap-shaped.

    javap renders members as ``// Method owner.name:(desc)ret`` and fields as
    ``// Field owner.name:desc``, so the needle keeps that exact spelling.
    """
    out = []
    for m in re.finditer(r'target\s*=\s*"([^"]+)"', body):
        raw = m.group(1)
        invoke = re.match(r"^L([^;]+);([\w$<>]+)(\([^)]*\).*)$", raw)
        field = re.match(r"^L([^;]+);([\w$]+):(\S+)$", raw)
        new = re.match(r"^L([^;]+);$", raw)
        if invoke:
            owner, name, desc = invoke.groups()
            out.append((f"{owner}.{name}:{desc}", f"INVOKE of {owner}.{name}{desc}"))
        elif field:
            owner, name, desc = field.groups()
            out.append((f"{owner}.{name}:{desc}", f"FIELD {owner}.{name}"))
        elif new:
            owner = new.group(1)
            out.append((f"// class {owner}", f"NEW of {owner}"))
    return out


# ------------------------------------------------------------------ main


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("jar")
    ap.add_argument("--root", default=str(JAVA))
    ap.add_argument("--javap", default="javap")
    ap.add_argument("--quarantine", action="store_true",
                    help="add 'require = 0' to injectors whose @At target is "
                         "gone, so they degrade instead of crashing the client")
    args = ap.parse_args()

    sys.path.insert(0, str(pathlib.Path(__file__).parent))
    import verify_mixins as vm  # noqa: E402  (shares the selector resolver)

    jar = pathlib.Path(args.jar)
    dis = Disassembler(jar, pathlib.Path(args.javap))
    # Let the shared resolver see the jar's own class list as its universe.
    vm._INDEX = type("JarIndex", (), {
        "classes": {n[:-6]: {} for n in dis._names if n.endswith(".class")},
    })()
    root = pathlib.Path(args.root)

    problems, checked, files = [], 0, 0
    quarantined = 0
    for path in sorted(root.rglob("*.java")):
        text = path.read_text(errors="replace")
        if "@Mixin" not in text or "@At(" not in text:
            continue
        files += 1
        pkg = re.search(r"^package\s+([\w.]+);", text, re.M)
        pkg = pkg.group(1) if pkg else ""

        targets = []
        for m in re.finditer(r"@Mixin\(\s*(?:value\s*=\s*)?\{?([^)}]+)\}?\s*\)", text):
            for part in m.group(1).split(","):
                cm = re.match(r"\s*([\w.$]+)\.class", part)
                if cm:
                    targets.append(resolve(vm, cm.group(1), text, pkg))
        targets = [t for t in targets if dis.has(t)]
        if not targets:
            continue

        bad_spans = []
        for ann, body, body_start, body_end in annotations(text):
            sel = re.search(r'method\s*=\s*(\{[^}]*\}|"[^"]*")', body)
            points = at_targets(body)
            if not sel or not points:
                continue
            for selector in re.findall(r'"([^"]*)"', sel.group(1)):
                name, desc = vm.split_selector(selector)
                if name is None:
                    continue
                checked += 1
                for needle, label in points:
                    found = False
                    for target in targets:
                        for cls in dis.supers(target):
                            for key, code in dis.methods(cls).items():
                                tname, tdesc = key.split(" ", 1)
                                # the call may sit in a synthetic lambda of the method
                                if tname != name and not tname.startswith(f"lambda${name}$"):
                                    continue
                                if desc and tdesc != desc and tname == name:
                                    continue
                                if needle in code:
                                    found = True
                                    break
                            if found:
                                break
                        if found:
                            break
                    if not found:
                        optional = re.search(r"\brequire\s*=\s*0", body) is not None
                        problems.append((
                            f"{path.name}: @{ann} {selector} -> no {label} "
                            f"in {', '.join(targets)}", optional))
                        bad_spans.append((body_start, body_end, body))
                        break

        if args.quarantine and bad_spans:
            for start, end, body in sorted(bad_spans, reverse=True):
                if re.search(r"\brequire\s*=", body):
                    continue
                text = text[:end] + ", require = 0" + text[end:]
                quarantined += 1
            path.write_text(text)

    print(f"scanned {files} mixin sources, {checked} selector/@At pairs")
    if args.quarantine:
        print(f"quarantined {quarantined} injectors with require = 0")
    fatal = sorted({m for m, opt in problems if not opt})
    optional = sorted({m for m, opt in problems if opt})
    print(f"scanned {files} mixin sources, {checked} selector/@At pairs")
    if args.quarantine:
        print(f"quarantined {quarantined} injectors with require = 0")
    for message in optional:
        print("opt    " + message)
    for message in fatal:
        print("FATAL  " + message)
    if not fatal:
        print(f"OK: every live @At target is reachable "
              f"({len(optional)} quarantined with require = 0)")
    return 1 if fatal else 0


if __name__ == "__main__":
    raise SystemExit(main())
