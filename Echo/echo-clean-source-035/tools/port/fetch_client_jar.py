#!/usr/bin/env python3
"""Download the Minecraft client jar for the version this mod targets.

`verify_at_targets.py` needs the real class files to prove that every
``@At`` INVOKE/FIELD target physically exists inside the method a mixin hooks.
That check is the only thing standing between a stale descriptor and a client
that launches with an invisible GUI, so it runs in CI -- which needs the jar.

The version comes from ``gradle.properties`` (``minecraft_version``) and is
resolved through Mojang's version manifest, so nothing is hardcoded.

    python3 tools/port/fetch_client_jar.py --out /tmp/client.jar
"""

import argparse
import json
import pathlib
import sys
import urllib.request

MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"


def get_json(url: str):
    with urllib.request.urlopen(url, timeout=60) as response:
        return json.load(response)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default="/tmp/client.jar")
    ap.add_argument("--properties", default="gradle.properties")
    args = ap.parse_args()

    version = None
    for line in pathlib.Path(args.properties).read_text().splitlines():
        if line.strip().startswith("minecraft_version"):
            version = line.split("=", 1)[1].strip()
            break
    if not version:
        print("minecraft_version not found in gradle.properties", file=sys.stderr)
        return 1

    manifest = get_json(MANIFEST)
    entry = next((v for v in manifest["versions"] if v["id"] == version), None)
    if entry is None:
        print(f"{version} not in the version manifest", file=sys.stderr)
        return 1

    detail = get_json(entry["url"])
    client = detail["downloads"]["client"]
    print(f"{version}: {client['url']} ({client['size'] // (1024 * 1024)} MiB)")

    out = pathlib.Path(args.out)
    with urllib.request.urlopen(client["url"], timeout=300) as response:
        out.write_bytes(response.read())
    print(f"wrote {out} ({out.stat().st_size} bytes)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
