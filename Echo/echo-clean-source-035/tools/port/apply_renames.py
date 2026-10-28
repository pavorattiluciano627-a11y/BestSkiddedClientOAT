#!/usr/bin/env python3
"""Apply mechanical 26.2 renames to the Echo sources.

Every mapping in this script was verified against the real 26.2 client jar
(tools/port/jar_index.py), not guessed. Re-runnable: each rule is idempotent.
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2] / "src/main/java"

# (old, new) applied to whole file text, plain string replacement.
TEXT_RULES = [
    # --- package moves (26.2 reorganised the render-state tree) ---
    ("net.minecraft.client.gui.render.state.GuiRenderState",
     "net.minecraft.client.renderer.state.gui.GuiRenderState"),
    ("net.minecraft.client.renderer.state.CameraRenderState",
     "net.minecraft.client.renderer.state.level.CameraRenderState"),
    ("net.minecraft.client.renderer.state.LevelRenderState",
     "net.minecraft.client.renderer.state.level.LevelRenderState"),
    ("net.minecraft.client.renderer.state.SkyRenderState",
     "net.minecraft.client.renderer.state.level.SkyRenderState"),
    # --- renames confirmed in the 26.2 jar ---
    ("CachedOrthoProjectionMatrixBuffer", "ProjectionMatrixBuffer"),
    ("net.minecraft.world.inventory.ClickType", "net.minecraft.world.inventory.ClickAction"),
    ("net.minecraft.client.renderer.LightTexture", "net.minecraft.client.renderer.Lightmap"),
    ("com.mojang.blaze3d.textures.TextureFormat", "com.mojang.blaze3d.GpuFormat"),
    ("net.minecraft.world.level.BlockAndTintGetter",
     "net.minecraft.client.renderer.block.BlockAndTintGetter"),
    ("net.minecraft.world.entity.monster.Slime",
     "net.minecraft.world.entity.monster.cubemob.Slime"),
]

# simple-name replacements that are unambiguous inside these sources
SIMPLE_RULES = [
    ("CachedOrthoProjectionMatrixBuffer", "ProjectionMatrixBuffer"),
    ("ClickType", "ClickAction"),
    ("LightTexture", "Lightmap"),
    ("TextureFormat", "GpuFormat"),
]

# static constant holders that gained their own class in 26.2
CONST_HOLDERS = [
    ("EntityType", "EntityTypes"),
    ("BlockEntityType", "BlockEntityTypes"),
]


def add_import(text, fqcn):
    """Insert an import in sorted position if the file references a type but lacks it."""
    simple = fqcn.rsplit(".", 1)[-1]
    if re.search(rf"^import\s+{re.escape(fqcn)};", text, re.M):
        return text
    if re.search(rf"^import\s+.*\.{re.escape(simple)};", text, re.M):
        # a different class with the same simple name is imported: leave it alone
        return text
    imports = list(re.finditer(r"^import\s+[\w.]+;\s*$", text, re.M))
    line = f"import {fqcn};"
    if not imports:
        m = re.search(r"^package\s+[\w.]+;\s*$", text, re.M)
        if not m:
            return text
        return text[:m.end()] + "\n\n" + line + text[m.end():]
    # keep imports sorted so the file stays conventionally formatted
    for m in imports:
        if m.group(0).strip() > line:
            return text[:m.start()] + line + "\n" + text[m.start():]
    last = imports[-1]
    return text[:last.end()] + "\n" + line + text[last.end():]


def main():
    changed = []
    for path in sorted(ROOT.rglob("*.java")):
        orig = path.read_text(encoding="utf-8")
        text = orig

        for old, new in TEXT_RULES:
            text = text.replace(old, new)

        for old, new in SIMPLE_RULES:
            # word-boundary, and never inside an already-correct identifier
            text = re.sub(rf"(?<![\w.]){old}\b", new, text)

        # static constants: EntityType.PLAYER -> EntityTypes.PLAYER
        for holder, newholder in CONST_HOLDERS:
            pattern = re.compile(rf"\b{holder}\.([A-Z][A-Z0-9_]*)\b")
            if pattern.search(text):
                text = pattern.sub(rf"{newholder}.\1", text)
                fq = {
                    "EntityTypes": "net.minecraft.world.entity.EntityTypes",
                    "BlockEntityTypes": "net.minecraft.world.level.block.entity.BlockEntityTypes",
                }[newholder]
                text = add_import(text, fq)

        # Access widener entry: BakedGlyphImpl -> BakedSheetGlyph (see AW file)
        if text != orig:
            path.write_text(text, encoding="utf-8")
            changed.append(str(path.relative_to(ROOT.parent.parent.parent)))

    print(f"{len(changed)} files updated")
    for c in changed:
        print("  ", c)
    return 0


if __name__ == "__main__":
    sys.exit(main())
