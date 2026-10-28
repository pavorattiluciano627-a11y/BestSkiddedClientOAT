#!/usr/bin/env python3
"""Build a queryable index of the classes, methods and fields inside a Minecraft jar.

Minecraft 26.x ships unobfuscated, so the jar itself is the authoritative source for
official names. This index lets the port answer "what is this member called in 26.2?"
without a compiler or network access.

Usage:
    python3 tools/port/jar_index.py build <jar> <out.json>
    python3 tools/port/jar_index.py class  <index.json> <ClassSimpleName>
    python3 tools/port/jar_index.py member <index.json> <ClassSimpleName> <substring>
    python3 tools/port/jar_index.py find   <index.json> <member-name> [kind]
"""
import json
import struct
import sys
import zipfile

# ---------------------------------------------------------------- class parsing


class Reader:
    def __init__(self, data):
        self.d = data
        self.p = 0

    def u1(self):
        v = self.d[self.p]
        self.p += 1
        return v

    def u2(self):
        v = struct.unpack_from(">H", self.d, self.p)[0]
        self.p += 2
        return v

    def u4(self):
        v = struct.unpack_from(">I", self.d, self.p)[0]
        self.p += 4
        return v

    def take(self, n):
        v = self.d[self.p:self.p + n]
        self.p += n
        return v


def parse_class(data):
    """Return (this_class, super_class, access, fields, methods) using official names."""
    r = Reader(data)
    if r.u4() != 0xCAFEBABE:
        raise ValueError("not a class file")
    r.u2(), r.u2()  # minor, major
    cp_count = r.u2()
    cp = [None] * cp_count
    i = 1
    while i < cp_count:
        tag = r.u1()
        if tag == 1:  # Utf8
            ln = r.u2()
            cp[i] = r.take(ln).decode("utf-8", "replace")
        elif tag in (7, 8, 16, 19, 20):  # Class/String/Module/Package refs
            cp[i] = ("ref", r.u2())
        elif tag in (15,):  # MethodHandle
            r.u1(), r.u2()
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            r.u2(), r.u2()
        elif tag in (5, 6):  # long/double take two slots
            r.u4(), r.u4()
            i += 2
            continue
        else:
            raise ValueError(f"bad constant pool tag {tag}")
        i += 1

    def utf(idx):
        v = cp[idx]
        if isinstance(v, tuple):
            return cp[v[1]]
        return v

    access = r.u2()
    this_class = utf(cp[r.u2()][1])
    super_idx = r.u2()
    super_class = utf(cp[super_idx][1]) if super_idx else None

    # interfaces
    for _ in range(r.u2()):
        r.u2()

    def members(prefix):
        out = []
        for _ in range(r.u2()):
            acc = r.u2()
            name = utf(r.u2())
            desc = utf(r.u2())
            for _ in range(r.u2()):
                r.u2()
                r.take(r.u4())
            out.append({"n": name, "d": desc, "a": acc, "k": prefix})
        return out

    fields = members("field")
    methods = members("method")
    return this_class, super_class, access, fields, methods


def build(jar_path, out_path):
    index = {}
    with zipfile.ZipFile(jar_path) as z:
        for entry in z.namelist():
            if not entry.endswith(".class"):
                continue
            try:
                this, sup, acc, fields, methods = parse_class(z.read(entry))
            except Exception:
                continue
            obf = entry[:-6]
            index[obf] = {"s": sup, "a": acc, "f": fields, "m": methods}
            # also key by the internal name the class claims, so mismatched
            # paths (multi-release jars) still resolve
            if this != obf:
                index.setdefault(this, index[obf])
    with open(out_path, "w") as f:
        json.dump({"classes": index}, f)
    print(f"indexed {len(index)} classes -> {out_path}")


# ------------------------------------------------------------------- querying

def load(p):
    with open(p) as f:
        return json.load(f)["classes"]


def simple(internal):
    return internal.rsplit("/", 1)[-1]


def cmd_class(idx, name):
    hits = [k for k in idx if simple(k) == name or simple(k) == name + ".class"]
    if not hits:
        sub = [k for k in idx if name.lower() in k.lower()]
        print(f"no exact class {name}; {len(sub)} fuzzy:")
        for k in sub[:20]:
            print("  ", k)
        return
    for k in sorted(hits):
        e = idx[k]
        print(f"{k}  extends {e['s']}")
        print(f"  fields: {len(e['f'])}  methods: {len(e['m'])}")


def cmd_member(idx, name, sub):
    hits = [k for k in idx if simple(k) == name]
    if not hits:
        print(f"no class {name}")
        return
    for k in sorted(hits):
        e = idx[k]
        print(f"== {k}")
        for f in e["f"]:
            if sub.lower() in f["n"].lower():
                print(f"   field  {f['n']} {f['d']}")
        for m in e["m"]:
            if sub.lower() in m["n"].lower():
                print(f"   method {m['n']}{m['d']}")


def cmd_find(idx, needle, kind=None):
    print(f"members matching '{needle}':")
    n = 0
    for k, e in idx.items():
        for f in e["f"]:
            if f["n"].lower() == needle.lower() and kind in (None, "field"):
                print(f"   {k}.{f['n']} {f['d']}")
                n += 1
        for m in e["m"]:
            if m["n"].lower() == needle.lower() and kind in (None, "method"):
                print(f"   {k}.{m['n']}{m['d']}")
                n += 1
    print(f"-- {n} hits")


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        return 1
    cmd = sys.argv[1]
    if cmd == "build":
        build(sys.argv[2], sys.argv[3])
    elif cmd == "class":
        cmd_class(load(sys.argv[2]), sys.argv[3])
    elif cmd == "member":
        cmd_member(load(sys.argv[2]), sys.argv[3], sys.argv[4])
    elif cmd == "find":
        cmd_find(load(sys.argv[2]), sys.argv[3], sys.argv[4] if len(sys.argv) > 4 else None)
    else:
        print(__doc__)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
