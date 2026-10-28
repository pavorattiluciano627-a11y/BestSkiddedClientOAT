#!/usr/bin/env python3
"""Report how much of Echo still uses Yarn names instead of official 26.x names.

Minecraft 26.2 is unobfuscated, so the sources must speak Mojang's official
namespace. There is no Yarn mapping set for 26.x, which means every remaining
Yarn identifier is a compile error. This tool is a *progress meter*, not a
remapper: it counts occurrences of well-established Yarn identifiers and prints
the official counterpart as a hint.

The authoritative conversion is Loom's `migrateMappings` task or the Ravel IDEA
plugin (see docs/PORT-26.2.md). Use this script before and after running one of
them to see the delta, and to find leftovers they could not resolve.

    python3 tools/port/mapping_progress.py            # summary
    python3 tools/port/mapping_progress.py --files    # per-file breakdown
"""
from __future__ import annotations

import argparse
from collections import Counter, defaultdict
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "src/main/java"

# Yarn simple name -> official 26.x simple name. Restricted to pairs that have been
# stable in Mojang's official mappings for many releases, so a hit is a real hit.
YARN_TO_OFFICIAL = {
    "MinecraftClient": "Minecraft",
    "ClientPlayerEntity": "LocalPlayer",
    "ClientWorld": "ClientLevel",
    "ClientPlayNetworkHandler": "ClientPacketListener",
    "ClientPlayerInteractionManager": "MultiPlayerGameMode",
    "DrawContext": "GuiGraphics",
    "TextRenderer": "Font",
    "MatrixStack": "PoseStack",
    "Identifier": "ResourceLocation",
    "Vec3d": "Vec3",
    "Vec3i": "Vec3i",
    "Box": "AABB",
    "ScreenHandler": "AbstractContainerMenu",
    "PlayerEntity": "Player",
    "ServerPlayerEntity": "ServerPlayer",
    "PlayerInventory": "Inventory",
    "HandledScreen": "AbstractContainerScreen",
    "ClickSlotCvp": "ServerboundContainerClickPacket",
    "ItemStack": "ItemStack",
    "World": "Level",
    "WorldRenderEvents": "WorldRenderEvents",
    "EntityRenderDispatcher": "EntityRenderDispatcher",
    "RenderSystem": "RenderSystem",
    "BufferBuilder": "BufferBuilder",
    "Tessellator": "Tesselator",
    "Sprite": "TextureAtlasSprite",
    "SpriteAtlasTexture": "SpriteAtlasTexture",
    "SimpleFramebuffer": "SimpleFramebuffer",
    "NativeImage": "NativeImage",
    "GL30": "GL30",
    "VertexFormat": "VertexFormat",
    "VertexFormats": "DefaultVertexFormat",
    "Text": "Component",
    "MutableText": "MutableComponent",
    "Style": "Style",
    "Screen": "Screen",
    "Click": "Click",
    "KeyBinding": "KeyMapping",
    "InputUtil": "InputConstants",
    "Tooltip": "Tooltip",
    "ItemRenderer": "ItemRenderer",
    "Entity": "Entity",
    "LivingEntity": "LivingEntity",
    "ItemEntity": "ItemEntity",
    "EndCrystalEntity": "EndCrystal",
    "Camera": "Camera",
    "BlockRenderManager": "BlockRenderDispatcher",
    "ChunkOcclusionDataBuilder": "VisibilitySet",
    "ChunkRenderingDataPreparer": "SectionOcclusionGraph",
    "ClientConnection": "Connection",
}

IDENTIFIER = re.compile(r"\b([A-Z][A-Za-z0-9_$]*)\b")


def scan() -> tuple[Counter, dict[str, Counter]]:
    totals: Counter = Counter()
    by_file: dict[str, Counter] = defaultdict(Counter)
    for path in sorted(SOURCE.rglob("*.java")):
        text = path.read_text(encoding="utf-8", errors="replace")
        for name in set(IDENTIFIER.findall(text)):
            if name in YARN_TO_OFFICIAL:
                hits = len(re.findall(r"\b" + re.escape(name) + r"\b", text))
                totals[name] += hits
                by_file[str(path.relative_to(ROOT))][name] += hits
    return totals, by_file


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--files", action="store_true", help="list per-file breakdown")
    parser.add_argument("--top", type=int, default=25)
    args = parser.parse_args()

    totals, by_file = scan()
    occurrences = sum(totals.values())
    print(f"scanned {len(list(SOURCE.rglob('*.java')))} Java sources under src/main/java")
    print(f"Yarn identifiers remaining: {occurrences} occurrences across {len(by_file)} files")
    print(f"distinct Yarn names still present: {len(totals)} of {len(YARN_TO_OFFICIAL)} tracked\n")
    if totals:
        print(f"{'yarn':32s} {'official':32s} {'hits':>6s}")
        for name, hits in totals.most_common(args.top):
            print(f"{name:32s} {YARN_TO_OFFICIAL[name]:32s} {hits:6d}")
    else:
        print("no tracked Yarn identifiers remain: the sources speak the official namespace")
    if args.files and by_file:
        print("\nfiles needing attention:")
        for path, counts in sorted(by_file.items(), key=lambda kv: -sum(kv[1].values()))[:40]:
            print(f"  {sum(counts.values()):5d}  {path}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
