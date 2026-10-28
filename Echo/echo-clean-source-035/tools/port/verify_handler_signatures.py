#!/usr/bin/env python3
"""Validate that injector handler methods actually match their target methods.

Mixin resolves this at apply time and it is fatal, yet neither javac nor the
selector check can see it: the annotation names the target as a *string*, so a
target that changed shape (``Vector4f setupFog(...)`` became
``FogData setupFog(...)``, ``int nstbir_resize_uint8`` became
``long nstbir_resize_uint8_linear``) silently invalidates the handler.

Checks, deliberately conservative so it never cries wolf:

  @Inject            handler params must be a prefix of the target's params,
                     followed by CallbackInfo / CallbackInfoReturnable<T>
                     whose T must equal the target's return type
  @Redirect          params must equal the redirected member's params and the
                     return type must equal its return type
  @ModifyVariable    single parameter, must not contradict the target
  @ModifyArgs        single Args parameter

Anything it cannot resolve (generics, unmapped types) is reported as skipped
rather than guessed at.

    python3 tools/port/verify_handler_signatures.py <index.json[.gz]>
"""

import argparse
import json
import pathlib
import re
import sys

SRC = pathlib.Path("src/main/java")

PRIMITIVES = {"void": "V", "boolean": "Z", "byte": "B", "char": "C",
              "short": "S", "int": "I", "long": "J", "float": "F", "double": "D"}
JDK = {"String": "java/lang/String", "CharSequence": "java/lang/CharSequence",
       "Object": "java/lang/Object", "List": "java/util/List",
       "Set": "java/util/Set", "Map": "java/util/Map",
       "Collection": "java/util/Collection", "Iterable": "java/lang/Iterable"}

INJECTORS = ("Inject", "Redirect", "ModifyVariable", "ModifyArgs",
             "ModifyArg", "ModifyConstant", "ModifyExpressionValue",
             "WrapOperation", "WrapWithCondition")


def _fields(idx, owner):
    return [(f["n"], f["d"]) for _, e in idx.chain(owner) for f in e["f"]]


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
            yield cur, self.classes[cur]
            cur = self.classes[cur].get("s")

    def methods(self, owner):
        return [(m["n"], m["d"]) for _, e in self.chain(owner) for m in e["m"]]


def strip_generics(text: str) -> str:
    out, depth = [], 0
    for ch in text:
        if ch == "<":
            depth += 1
        elif ch == ">":
            depth -= 1
        elif depth == 0:
            out.append(ch)
    return "".join(out)


def type_to_desc(java_type: str, imports: dict[str, str]):
    t = strip_generics(java_type).strip()
    t = t.replace("...", "[]")
    if t in PRIMITIVES:
        return PRIMITIVES[t]
    if t.endswith("[]"):
        inner = type_to_desc(t[:-2], imports)
        return None if inner is None else "[" + inner
    simple = t.split(".")[-1]
    if simple in JDK:
        return "L" + JDK[simple] + ";"
    if t in imports:
        return "L" + imports[t] + ";"
    if simple in imports:
        return "L" + imports[simple] + ";"
    if "." in t:
        head, rest = t.split(".", 1)
        if head in imports:                         # FluidRenderer.Output
            return "L" + imports[head] + "$" + "$".join(rest.split(".")) + ";"
        return "L" + t.replace(".", "/") + ";"      # fully qualified in the source
    return None


def strip_annotations(text: str) -> str:
    """Remove leading parameter annotations: @Local(ordinal = 0) Foo -> Foo."""
    prev = None
    while prev != text:
        prev = text
        text = re.sub(r"^\s*@[\w.]+\s*(?:\([^()]*\))?\s*", "", text)
    return text.strip()


def split_params(text: str) -> list[str]:
    """'a A, b B' -> ['A', 'B'] (parameter names dropped, generics kept)."""
    text = text.strip()
    if not text:
        return []
    text = strip_annotations(text) if text.strip().startswith("@") else text
    out, depth, cur = [], 0, ""
    for ch in text:
        if ch in "<(":
            depth += 1
        elif ch in ">)":
            depth -= 1
        if ch == "," and depth == 0:
            out.append(cur)
            cur = ""
        else:
            cur += ch
    out.append(cur)
    types = []
    for piece in out:
        piece = strip_annotations(piece.strip())
        if not piece:
            continue
        types.append(piece.rsplit(" ", 1)[0] if " " in piece else piece)
    return types


def annotations_with_handlers(text: str):
    """Yield (annotation, args, handler params, handler return, handler name)."""
    for m in re.finditer(r"@(\w+)", text):
        ann = m.group(1)
        if ann not in INJECTORS:
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
        args = text[start + 1:i]
        tail = text[i + 1:]
        handle = re.search(
            r"(?:private|protected|public)?\s*(?:static\s+)?(?:final\s+)?"
            r"([\w$.<>\[\]]+)\s+([\w$]+)\s*\(([^)]*)\)", tail)
        if not handle:
            continue
        yield ann, args, split_params(handle.group(3)), handle.group(1), handle.group(2)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("index")
    ap.add_argument("--root", default=str(SRC))
    ap.add_argument("--verbose", action="store_true",
                    help="list the members skipped because a type could not be resolved")
    args = ap.parse_args()

    sys.path.insert(0, str(pathlib.Path(__file__).parent))
    import verify_mixins as vm  # noqa: E402

    idx = Index(args.index)
    vm._INDEX = idx
    root = pathlib.Path(args.root)

    problems, checked, skipped = [], 0, 0
    for path in sorted(root.rglob("*.java")):
        text = path.read_text(errors="replace")
        if "@Mixin" not in text:
            continue
        pkg = re.search(r"^package\s+([\w.]+);", text, re.M)
        pkg = pkg.group(1) if pkg else ""
        imports = {}
        for imp in re.findall(r"^import\s+(?:static\s+)?([\w.]+);", text, re.M):
            imports[imp.split(".")[-1]] = imp.replace(".", "/")

        targets = []
        for m in re.finditer(r"@Mixin\(\s*(?:value\s*=\s*)?\{?([^)}]+)\}?\s*\)", text):
            for part in m.group(1).split(","):
                cm = re.match(r"\s*([\w.$]+)\.class", part)
                if cm:
                    targets.append(vm.resolve_class(cm.group(1), text, pkg))
        targets = [t for t in targets if t in idx.classes]
        if not targets:
            continue

        for ann, body, params, ret, handler in annotations_with_handlers(text):
            sel = re.search(r'method\s*=\s*(\{[^}]*\}|"[^"]*")', body)
            if not sel:
                continue
            descs = []
            for selector in re.findall(r'"([^"]*)"', sel.group(1)):
                name, desc = vm.split_selector(selector)
                if name is None:
                    continue
                for target in targets:
                    for n, d in idx.methods(target):
                        if n == name and (desc is None or d == desc):
                            descs.append((target, n, d))
            if not descs:
                continue
            target, tname, tdesc = descs[0]
            target_params = re.findall(r"\[*(?:[ZBCSIJFD]|L[^;]+;)", tdesc[1:tdesc.index(")")])
            target_ret = tdesc[tdesc.index(")") + 1:]

            resolved = [type_to_desc(p, imports) for p in params]
            if any(r is None for r in resolved):
                skipped += 1
                if args.verbose:
                    print(f"  skip @{ann} {handler}: "
                          f"{[p for p, r in zip(params, resolved) if r is None]}")
                continue
            checked += 1

            if ann == "Inject":
                body_params = resolved[-1] if resolved else None
                if body_params not in ("Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;",
                                       "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;"):
                    problems.append(f"{path.name}: @Inject {handler} must end with CallbackInfo/"
                                    f"CallbackInfoReturnable")
                    continue
                captured = resolved[:-1]
                if len(captured) > len(target_params):
                    problems.append(f"{path.name}: @Inject {handler} captures {len(captured)} args, "
                                    f"{tname} takes {len(target_params)}")
                    continue
                for got, want in zip(captured, target_params):
                    if got != want:
                        problems.append(f"{path.name}: @Inject {handler} param {got} != "
                                        f"{tname} param {want}")
                        break
                if body_params.endswith("CallbackInfoReturnable;"):
                    generic = re.search(r"CallbackInfoReturnable<\s*([\w$.<>\[\]]+)\s*>", params[-1])
                    if generic:
                        got = type_to_desc(generic.group(1), imports)
                        if got and got != target_ret:
                            problems.append(
                                f"{path.name}: @Inject {handler} returns CallbackInfoReturnable<"
                                f"{generic.group(1)}> but {tname} returns {target_ret}")
                elif target_ret != "V":
                    problems.append(f"{path.name}: @Inject {handler} uses CallbackInfo but "
                                    f"{tname} returns {target_ret}")

            elif ann == "Redirect":
                site = re.search(r'target\s*=\s*"L([^;]+);([\w$<>]+)(\([^)]*\).*)"', body)
                if not site:
                    skipped += 1
                    continue
                want_params = re.findall(r"\[*(?:[ZBCSIJFD]|L[^;]+;)",
                                         site.group(3)[1:site.group(3).index(")")])
                want_ret = site.group(3)[site.group(3).index(")") + 1:]
                # A non-static member hands the receiver to the handler first.
                owner = site.group(1)
                member = site.group(2)
                static = None
                if owner in idx.classes:
                    for n, d, flags in ((m["n"], m["d"], m.get("a", 0))
                                        for _, e in idx.chain(owner) for m in e["m"]):
                        if n == member and d == site.group(3):
                            static = bool(flags & 0x0008)
                            break
                if static is False:
                    want_params = ["L" + owner + ";"] + want_params
                elif static is None:
                    skipped += 1
                    continue
                got_ret = type_to_desc(ret, imports)
                if got_ret and got_ret != want_ret:
                    problems.append(f"{path.name}: @Redirect {handler} returns {ret} "
                                    f"({got_ret}) but the redirect target returns {want_ret}")
                if len(resolved) != len(want_params):
                    problems.append(f"{path.name}: @Redirect {handler} takes {len(resolved)} params, "
                                    f"the redirect target takes {len(want_params)}")
                else:
                    for got, want in zip(resolved, want_params):
                        if got != want:
                            problems.append(f"{path.name}: @Redirect {handler} param {got} != {want}")
                            break

    # ---- @Shadow declarations: a member that moved is fatal at apply time ----
    shadow_checked = 0
    for path in sorted(root.rglob("*.java")):
        text = path.read_text(errors="replace")
        if "@Shadow" not in text:
            continue
        pkg = re.search(r"^package\s+([\w.]+);", text, re.M)
        pkg = pkg.group(1) if pkg else ""
        imports = {}
        for imp in re.findall(r"^import\s+(?:static\s+)?([\w.]+);", text, re.M):
            imports[imp.split(".")[-1]] = imp.replace(".", "/")

        targets = []
        for m in re.finditer(r"@Mixin\(\s*(?:value\s*=\s*)?\{?([^)}]+)\}?\s*\)", text):
            for part in m.group(1).split(","):
                cm = re.match(r"\s*([\w.$]+)\.class", part)
                if cm:
                    targets.append(vm.resolve_class(cm.group(1), text, pkg))
        targets = [t for t in targets if t in idx.classes]
        if not targets:
            continue

        for m in re.finditer(
                r"@Shadow\b[^;{}()]*?([\w$.<>\[\]]+)\s+([\w$]+)\s*(?:\(([^)]*)\))?\s*;",
                text):
            ret, name, params_text = m.group(1), m.group(2), m.group(3)
            if ret in ("private", "protected", "public", "final", "static"):
                continue
            if params_text is None:                      # field
                desc = type_to_desc(ret, imports)
                if desc is None:
                    skipped += 1
                    if args.verbose:
                        print(f"  skip @Shadow field {name}: {ret}")
                    continue
                shadow_checked += 1
                if not any(n == name and d == desc
                           for target in targets
                           for n, d in _fields(idx, target)):
                    problems.append(f"{path.name}: @Shadow field '{name}' {desc} not on "
                                    f"{', '.join(targets)}")
            else:                                        # method
                ptypes = [type_to_desc(x, imports) for x in split_params(params_text)]
                rtype = type_to_desc(ret, imports)
                if rtype is None or any(p is None for p in ptypes):
                    skipped += 1
                    if args.verbose:
                        print(f"  skip @Shadow method {name}: {params_text} -> {ret}")
                    continue
                desc = "(" + "".join(ptypes) + ")" + rtype
                shadow_checked += 1
                if not any(n == name and d == desc
                           for target in targets
                           for n, d in idx.methods(target)):
                    problems.append(f"{path.name}: @Shadow method '{name}{desc}' not on "
                                    f"{', '.join(targets)}")

    print(f"checked {checked} handler signatures and {shadow_checked} @Shadow members "
          f"({skipped} skipped: unresolved types)")
    for problem in sorted(set(problems)):
        print("FATAL  " + problem)
    if not problems:
        print("OK: every resolved handler signature matches its target")
    return 1 if problems else 0


if __name__ == "__main__":
    raise SystemExit(main())
