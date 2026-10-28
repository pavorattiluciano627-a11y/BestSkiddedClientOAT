#!/usr/bin/env python3
"""Second mechanical pass: 26.2 API-drift fixes for Echo.

Every target name here was read out of the real 26.2 client jar via
tools/port/jar_index.py. Rules are idempotent; run then recompile.
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2] / "src/main/java"

# (regex, replacement) applied to every Java source
RULES = [
    # ---- Minecraft facade: screen/HUD/render-target access moved to Gui/GameRenderer
    (r"Minecraft\.getInstance\(\)\.screen\b", "Minecraft.getInstance().gui.screen()"),
    (r"\bclient\.screen\b", "client.gui.screen()"),
    (r"CLIENT\.screen\b", "CLIENT.gui.screen()"),
    (r"Minecraft\.getInstance\(\)\.setScreen\(", "Minecraft.getInstance().gui.setScreen("),
    (r"\.getMainRenderTarget\(\)", ".gameRenderer.mainRenderTarget()"),
    (r"Minecraft\.getInstance\(\)\.gameRenderer\.renderBuffers\(\)",
     "Minecraft.getInstance().gameRenderer.renderBuffers()"),
    (r"\bclient\.renderBuffers\(\)", "client.gameRenderer.renderBuffers()"),
    (r"\.getOverlay\(\)", ".gui.overlay()"),
    (r"\.getMainCamera\(\)", ".gameRenderer.mainCamera()"),
    (r"\.isSingleplayer\(\)", ".hasSingleplayerServer()"),
    (r"\.getModelViewMatrix\(\)", ".getModelViewMatrixCopy()"),
    (r"Minecraft\.getInstance\(\)\.options\.hideGui", "Minecraft.getInstance().gui.hud.isHidden()"),
    (r"\boptions\.hideGui\b", "gui.hud.isHidden()"),

    # ---- GameRenderer accessors gained field-style names
    (r"\.getLighting\(\)", ".lighting()"),
    (r"\.getFeatureRenderDispatcher\(\)", ".featureRenderDispatcher()"),
    (r"\.getSubmitNodeStorage\(\)", ".handAndScreenSubmitNodeStorage"),

    # ---- ClickType was replaced by ContainerInput in 26.2
    (r"\bClickAction\.(PICKUP_ALL|PICKUP|QUICK_MOVE|SWAP|CLONE|THROW|QUICK_CRAFT)\b",
     r"ContainerInput.\1"),
    (r"\bhandleInventoryMouseClick\(", "handleContainerInput("),
    (r"\.clickType\(\)", ".containerInput()"),

    # ---- misc member renames confirmed against the jar
    (r"\.isInstantenous\(\)", ".isInstantaneous()"),
    (r"\.allChanged\(\)", ".resetLevelRenderData()"),
    (r"\.updateInWaterStateAndDoFluidPushing\(\)", ".updateFluidInteraction()"),
    (r"\.getItems\(\)", ".items()"),
    (r"\.getMovement\(\)", ".movement()"),
    (r"\.getId\(\)", ".id()"),

    # ---- GPU device introspection moved behind DeviceInfo
    (r"\.getBackendName\(\)", ".getDeviceInfo().backendName()"),
    (r"\.getVendor\(\)", ".getDeviceInfo().vendorName()"),
    (r"\.getVersion\(\)", ".getDeviceInfo().driverInfo()"),
    (r"\.getRenderer\(\)", ".getDeviceInfo().name()"),

    # ---- GuiGraphics was replaced by GuiGraphicsExtractor
    (r"\bnet\.minecraft\.client\.gui\.GuiGraphics\b",
     "net.minecraft.client.gui.GuiGraphicsExtractor"),
    (r"(?<![\w.])GuiGraphics\b", "GuiGraphicsExtractor"),

    # ---- moved render-state types
    (r"(?<![\w.])GuiEntityRenderState\b",
     "net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState"),
]

# entity attack range: (entity, stack) -> LivingEntity.getAttackRangeWith(stack)
ATTACK_RANGE = [
    (r"entity\.getAttackRange\(\)", "entity.getAttackRangeWith(stack)"),
]

# per-file fixes: path -> list of (old, new)
FILE_RULES = {
    "dev/echo/client/render/MinecraftItemAtlasSurface.java": [
        ("GpuFormat.RGBA8,", "GpuFormat.RGBA8_UNORM,"),
        ("GpuFormat.DEPTH32,", "GpuFormat.D32_FLOAT,"),
        ("new ProjectionMatrixBuffer(\"echo items\", -1000.0F, 1000.0F, true)",
         "new ProjectionMatrixBuffer(\"echo items\")"),
    ],
    "dev/echo/client/render/MinecraftTextBatch.java": [
        ("GpuFormat.RED8", "GpuFormat.R8_UNORM"),
    ],
    "dev/echo/client/utils/targeting/AttackRange.java": [
        ("return entity.entityAttackRange();", "return entity.getAttackRangeWith(stack);"),
        ("player.isWithinAttackRange(target.getBoundingBox(), 0.0)",
         "player.isWithinAttackRange(player.getMainHandItem(), target.getBoundingBox(), 0.0)"),
        ("return player.entityAttackRange().isInRange(player, point);",
         "return player.getAttackRangeWith(player.getMainHandItem()).isInRange(player, point);"),
        ("return player.entityAttackRange().effectiveMaxRange(player);",
         "return player.getAttackRangeWith(player.getMainHandItem()).effectiveMaxRange(player);"),
    ],
    "dev/echo/client/utils/targeting/AttackRangeProperties.java": [],
    "dev/echo/client/features/impl/combat/PiercingFeature.java": [],
}


def main():
    changed = 0
    touched = []
    for path in sorted(ROOT.rglob("*.java")):
        orig = path.read_text(encoding="utf-8")
        text = orig
        for pat, rep in RULES:
            text = re.sub(pat, rep, text)

        rel = path.relative_to(ROOT).as_posix()
        for old, new in FILE_RULES.get(rel, []):
            text = text.replace(old, new)

        if text != orig:
            path.write_text(text, encoding="utf-8")
            changed += 1
            touched.append(rel)
    print(f"{changed} files updated")
    return 0


if __name__ == "__main__":
    sys.exit(main())
