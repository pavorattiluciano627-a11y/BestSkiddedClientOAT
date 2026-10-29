#!/usr/bin/env python3
"""Rebuild a valid jar from Clarity's "Official stage 0.jar".

That artifact is shipped with zeroed local-header sizes and data descriptors,
so `unzip` rejects it ("overlapped components (possible zip bomb)") and every
`assets/` entry fails to extract. The bytes are all present, though: each entry
can be inflated by scanning to the next local header.

This rewrites a clean, deterministic jar and can optionally drop the paid
loader path (licence handshake + HWID + remote payload) entirely.

    python3 tools/clarity/rebuild_jar.py <stage0.jar> <out.jar> [--strip-loader]
"""

import argparse
import struct
import zipfile
import zlib


def scan(data: bytes):
    """Yield (name, method, compressed_bytes) for every local header."""
    headers, i = [], 0
    while True:
        i = data.find(b"PK\x03\x04", i)
        if i < 0:
            return headers
        (_sig, _ver, _flags, method, _mt, _md, _crc, _csize, _usize,
         nlen, elen) = struct.unpack_from("<IHHHHHIIIHH", data, i)
        name = data[i + 30:i + 30 + nlen].decode("utf-8", "replace")
        headers.append({
            "name": name,
            "method": method,
            "start": i + 30 + nlen + elen,
            "header": i,
        })
        i += 4


def entries(data: bytes):
    headers = scan(data)
    out = []
    for idx, h in enumerate(headers):
        if not h["name"] or h["name"].endswith("/"):
            continue
        end = headers[idx + 1]["header"] if idx + 1 < len(headers) else len(data)
        blob = data[h["start"]:end]
        mark = blob.rfind(b"PK\x07\x08")          # trailing data descriptor
        if mark != -1:
            blob = blob[:mark]
        try:
            payload = (zlib.decompressobj(-15).decompress(blob)
                       if h["method"] == 8 else blob)
        except zlib.error:
            continue
        if payload:
            out.append((h["name"], payload))
    return out


# The agent package is the vendor's paid delivery path: licence handshake
# (ClarityConnect), HWID hashing, the Swing loader, the watermark slot and the
# bytecode transformer it drives. A Fabric mod needs none of it, and nothing
# outside the package references any of it (verified), so the whole tree goes.
LOADER_PREFIXES = (
    "need/clarity/agent/",
)


# Presence/friends telemetry. "Online Features" defaults to true, so a stock
# build dials the vendor's relay and broadcasts your status and current server.
# Every replacement is the SAME BYTE LENGTH, so the class constant pool layout
# is untouched and the host simply stops resolving.
PRESENCE_PATCHES = (
    (b"wss://104-156-154-148.sslip.io", b"wss://000-000-000-000.sslip.io"),
    (b"169.128.191.166", b"000.000.000.000"),
    (b"169-128-191-166", b"000-000-000-000"),
)


def neutralise_presence(payload: bytes) -> tuple[bytes, int]:
    hits = 0
    if not payload.startswith(b"\xca\xfe\xba\xbe"):
        return payload, 0
    for old, new in PRESENCE_PATCHES:
        if old in payload:
            assert len(old) == len(new)
            hits += payload.count(old)
            payload = payload.replace(old, new)
    return payload, hits


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("source")
    ap.add_argument("output")
    ap.add_argument("--strip-loader", action="store_true")
    ap.add_argument("--neutralise-presence", action="store_true",
                    help="disable the default-on friends/presence relay")
    args = ap.parse_args()

    data = open(args.source, "rb").read()
    items = entries(data)
    print(f"recovered {len(items)} entries")

    dropped = 0
    if args.strip_loader:
        kept = []
        for name, payload in items:
            if any(name.startswith(p) for p in LOADER_PREFIXES):
                dropped += 1
                continue
            kept.append((name, payload))
        items = kept
        print(f"dropped {dropped} licensing/loader entries")

    # The recovery pass can surface the same entry name twice (the drop ships
    # duplicate lwjgl .sha1 entries). Keep the first and drop the repeats so the
    # jar never advertises an ambiguous name.
    seen = set()
    deduped = []
    for name, payload in items:
        if name in seen:
            continue
        seen.add(name)
        deduped.append((name, payload))
    if len(deduped) != len(items):
        print(f"deduplicated {len(items) - len(deduped)} duplicate entries")
    items = deduped

    if args.neutralise_presence:
        patched = 0
        rebuilt = []
        for name, payload in items:
            if name.endswith(".class"):
                payload, hits = neutralise_presence(payload)
                patched += hits
            rebuilt.append((name, payload))
        items = rebuilt
        print(f"neutralised {patched} presence endpoint literals")

    with zipfile.ZipFile(args.output, "w", zipfile.ZIP_DEFLATED) as jar:
        for name, payload in sorted(items):
            info = zipfile.ZipInfo(name, date_time=(1980, 2, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o644 << 16
            jar.writestr(info, payload)

    assets = sum(1 for n, _ in items if n.startswith("assets/"))
    classes = sum(1 for n, _ in items if n.endswith(".class"))
    print(f"wrote {args.output}: {classes} classes, {assets} assets")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
