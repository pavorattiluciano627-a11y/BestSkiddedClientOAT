#!/usr/bin/env python3
"""Resolve and pin the launcher metadata for the configured Minecraft/Fabric pair.

The isolated launcher descriptor is built from two version JSON documents:
Mojang's version manifest entry and Fabric's loader profile for that version.
Both are fetched here, hashed, and written into `launcher-inputs/` together with
an immutable `pins.json` so the descriptor build is reproducible offline.

Run this once (and again only when the version pins in gradle.properties change):

    python3 tools/build/launcher_inputs.py

Network access to piston-meta.mojang.com and meta.fabricmc.net is required.
"""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
import argparse
import sys
import urllib.request

ROOT = Path(__file__).resolve().parents[2]
INPUTS = ROOT / "tools/build/launcher-inputs"
MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
FABRIC_PROFILE = "https://meta.fabricmc.net/v2/versions/loader/{minecraft}/{loader}/profile/json"


def gradle_property(name: str) -> str:
    for line in (ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines():
        if line.startswith(name + "="):
            return line.split("=", 1)[1].strip()
    raise SystemExit(f"gradle.properties is missing {name}")


def fetch(url: str) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": "echo-clean-launcher-inputs"})
    with urllib.request.urlopen(request, timeout=60) as response:
        return response.read()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--minecraft", default=gradle_property("minecraft_version"))
    parser.add_argument("--loader", default=gradle_property("loader_version"))
    args = parser.parse_args()

    print(f"Resolving Minecraft {args.minecraft} from the Mojang version manifest")
    manifest = json.loads(fetch(MANIFEST))
    entries = [entry for entry in manifest["versions"] if entry["id"] == args.minecraft]
    if not entries:
        raise SystemExit(f"Version {args.minecraft} is not present in the Mojang manifest")
    entry = entries[0]
    version_document = fetch(entry["url"])
    minecraft = json.loads(version_document)
    if minecraft.get("id") != args.minecraft:
        raise SystemExit("Mojang version document id differs from the manifest entry")
    if minecraft.get("downloads", {}).get("client_mappings"):
        print("warning: this version still publishes obfuscation mappings; 26.x should not",
              file=sys.stderr)

    fabric_url = FABRIC_PROFILE.format(minecraft=args.minecraft, loader=args.loader)
    print(f"Resolving Fabric loader {args.loader} profile for {args.minecraft}")
    fabric_document = fetch(fabric_url)
    fabric = json.loads(fabric_document)
    if fabric.get("inheritsFrom") != args.minecraft:
        raise SystemExit("Fabric profile does not inherit from the requested Minecraft version")

    INPUTS.mkdir(parents=True, exist_ok=True)
    (INPUTS / "minecraft.json").write_bytes(version_document)
    (INPUTS / "fabric.json").write_bytes(fabric_document)
    pins = {
        "files": {
            "minecraft.json": hashlib.sha256(version_document).hexdigest(),
            "fabric.json": hashlib.sha256(fabric_document).hexdigest(),
        },
        "urls": {"minecraft.json": entry["url"], "fabric.json": fabric_url},
        "mojang_sha1": entry["sha1"],
        "minecraft": args.minecraft,
        "fabric_loader": args.loader,
        "obfuscated": bool(minecraft.get("downloads", {}).get("client_mappings")),
    }
    (INPUTS / "pins.json").write_text(json.dumps(pins, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(pins, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
