#!/usr/bin/env python3
"""Third pass: finish the 26.2 port.

Two jobs:
  1. Repair over-broad regexes from fix_26_2.py, driven by real javac errors
     (a rename is only wrong where the compiler says the symbol is missing).
  2. Apply the remaining file-specific API adaptations, each read out of the
     real 26.2 jar via tools/port/jar_index.py.

Usage:
    python3 tools/port/fix_26_2_pass2.py            # file-specific edits
    python3 tools/port/fix_26_2_pass2.py --repair <javac.log>
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2] / "src/main/java"

# symbol reported missing -> the name it should be restored to
REPAIRS = {
    "id": "getId",
    "movement": "getMovement",
    "items": "getItems",
}

FILE_EDITS = {
    # ---- Duplicate/incorrect rewrites produced by the broader rules ----
    "dev/echo/client/render/MinecraftRenderState.java": [
        ("client.gameRenderer.gameRenderer.mainCamera()", "client.gameRenderer.mainCamera()"),
    ],
    "dev/echo/client/mixin/GameRendererMixin.java": [
        ("MinecraftRenderState.gui.overlay()", "MinecraftRenderState.getOverlay()"),
        ("guiRenderer.render(fogRenderer.getBuffer(FogRenderer.FogMode.NONE));",
         "guiRenderer.render();"),
    ],
    "dev/echo/client/integration/SystemInformation.java": [
        ("mod.getMetadata().getDeviceInfo().driverInfo().getFriendlyString()",
         "mod.getMetadata().getVersion().getFriendlyString()"),
    ],

    # ---- GuiGraphicsExtractor method renames ----
    "dev/echo/client/render/DrawContextAdapter.java": [
        ("context.renderItem(stack, x, y);", "context.item(stack, x, y);"),
        ("context.renderItemDecorations(textRenderer, stack, x, y);",
         "context.itemDecorations(textRenderer, stack, x, y);"),
        ("context.drawString(textRenderer, text, x, y, color, shadow);",
         "context.text(textRenderer, text, x, y, color, shadow);"),
        ("context.submitEntityRenderState(state, scale, translation, rotation, overrideCameraAngle, x1, y1, x2, y2);",
         "context.entity(state, scale, translation, rotation, overrideCameraAngle, x1, y1, x2, y2);"),
    ],

    # ---- Screen render hooks were renamed to extract* in 26.2 ----
    "dev/echo/client/screens/EchoScreen.java": [
        ("public void renderBackground(GuiGraphicsExtractor context",
         "public void extractBackground(GuiGraphicsExtractor context"),
        ("public void renderTransparentBackground(GuiGraphicsExtractor context",
         "public void extractTransparentBackground(GuiGraphicsExtractor context"),
    ],
    "dev/echo/client/screens/DashboardScreen.java": [
        ("public void render(GuiGraphicsExtractor context",
         "public void extractRenderState(GuiGraphicsExtractor context"),
    ],
    "dev/echo/client/screens/HudEditorScreen.java": [
        ("public void render(GuiGraphicsExtractor context",
         "public void extractRenderState(GuiGraphicsExtractor context"),
    ],
    "dev/echo/client/screens/CategoryPanelScreen.java": [
        ("public void render(GuiGraphicsExtractor context",
         "public void extractRenderState(GuiGraphicsExtractor context"),
    ],
    "dev/echo/client/mixin/ScreenMixin.java": [
        ('@Inject(method = "renderWithTooltipAndSubtitles"',
         '@Inject(method = "extractRenderStateWithTooltipAndSubtitles"'),
    ],

    # ---- Hud render hooks: render* -> extract* ----
    "dev/echo/client/mixin/InGameHudMixin.java": [
        ('@Inject(method = "render",', '@Inject(method = "extractRenderState",'),
        ('@Inject(method = "renderItemHotbar",', '@Inject(method = "extractItemHotbar",'),
        ('@Inject(method = "renderVignetteOverlay",', '@Inject(method = "extractVignette",'),
        ('@Inject(method = "renderNauseaOverlay",', '@Inject(method = "extractConfusionOverlay",'),
        ('@Inject(method = "renderSpyglassOverlay",', '@Inject(method = "extractSpyglassOverlay",'),
        ('@Inject(method = "renderPortalOverlay",', '@Inject(method = "extractPortalOverlay",'),
    ],

    # ---- Fluid rendering moved from BlockRenderDispatcher to FluidRenderer ----
    "dev/echo/client/mixin/BlockRenderManagerMixin.java": [
        ("import net.minecraft.client.renderer.block.BlockRenderDispatcher;",
         "import net.minecraft.client.renderer.block.FluidRenderer;"),
        ("@Mixin(BlockRenderDispatcher.class)", "@Mixin(FluidRenderer.class)"),
        ('@Inject(method = "renderLiquid", at = @At("HEAD"), cancellable = true)',
         '@Inject(method = "tesselate", at = @At("HEAD"), cancellable = true)'),
        ("public void onRenderFluidHead(BlockPos position, BlockAndTintGetter world, VertexConsumer vertices,\n            BlockState blockState, FluidState fluidState, CallbackInfo callback) {",
         "public void onRenderFluidHead(BlockAndTintGetter world, BlockPos position, FluidRenderer.Output output,\n            BlockState blockState, FluidState fluidState, CallbackInfo callback) {"),
    ],

    # ---- Fire overlay: MultiBufferSource -> SubmitNodeCollector ----
    "dev/echo/client/mixin/InGameOverlayRendererMixin.java": [
        ("import net.minecraft.client.renderer.MultiBufferSource;\n", ""),
        ('@Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)',
         '@Inject(method = "submitFire", at = @At("HEAD"), cancellable = true)'),
        ("private static void onRenderFireOverlayHead(PoseStack matrices, MultiBufferSource vertices,\n            TextureAtlasSprite fireSprite, CallbackInfo callback) {",
         "private static void onRenderFireOverlayHead(PoseStack matrices, SubmitNodeCollector queue,\n            TextureAtlasSprite fireSprite, CallbackInfo callback) {"),
    ],

    # ---- Picture-in-picture entity preview ----
    "dev/echo/client/render/EntityPreviewRenderer.java": [
        ("import net.minecraft.client.gui.render.pip.GuiEntityRenderer;",
         "import net.minecraft.client.gui.render.pip.GuiEntityRenderer;"),
        ("renderer.prepare(element, new GuiRenderState(), 1);",
         "renderer.prepare(element, new GuiRenderState(),\n                Minecraft.getInstance().gameRenderer.featureRenderDispatcher(), 1);"),
        ("renderer = new GuiEntityRenderer(client.gameRenderer.renderBuffers().bufferSource(),\n                    client.getEntityRenderDispatcher());",
         "renderer = new GuiEntityRenderer(client.getEntityRenderDispatcher());"),
    ],

    # ---- RenderBuffers lost bufferSource()/outlineBufferSource() ----
    "dev/echo/render/HandChamsRenderer.java": [
        ("client.gameRenderer.renderBuffers().outlineBufferSource().endOutlineBatch();",
         "client.gameRenderer.renderBuffers().stagedVertexBuffer().endFrame();"),
    ],
    "dev/echo/client/render/MinecraftItemAtlasSurface.java": [
        ("client.gameRenderer.renderBuffers().bufferSource().endBatch();",
         "client.gameRenderer.renderBuffers().stagedVertexBuffer().endFrame();"),
        ("client.gameRenderer.featureRenderDispatcher().renderAllFeatures();",
         "client.gameRenderer.featureRenderDispatcher().renderAllFeatures(\n                client.gameRenderer.handAndScreenSubmitNodeStorage);"),
        ("RenderSystem.setProjectionMatrix(projection.getBuffer(SIZE, SIZE), ProjectionType.ORTHOGRAPHIC);",
         "RenderSystem.setProjectionMatrix(projection.getBuffer(\n                new org.joml.Matrix4f().setOrtho(0.0F, SIZE, SIZE, 0.0F, 1000.0F, 21000.0F)),\n                ProjectionType.ORTHOGRAPHIC);"),
    ],

    # ---- Framebuffer primitives gained a format / vector clear colour ----
    "dev/echo/render/FramebufferOperations.java": [
        ("return new TextureTarget(name, width, height, useDepth);",
         "return new TextureTarget(name, width, height, useDepth, com.mojang.blaze3d.GpuFormat.RGBA8_UNORM);"),
        ("framebuffer.getColorTexture(), 0,\n                framebuffer.getDepthTexture(), 1.0);",
         "framebuffer.getColorTexture(), new org.joml.Vector4f(0.0F, 0.0F, 0.0F, 0.0F),\n                framebuffer.getDepthTexture(), 1.0);"),
        ("source.blitAndBlendToTexture(destination.getColorTextureView());",
         "source.blitAndBlendToTexture(source.getColorTextureView(), destination.getColorTextureView());"),
    ],

    # ---- Click packets/state ----
    "dev/echo/client/utils/inventory/SlotPackets.java": [
        ("import net.minecraft.world.inventory.ClickAction;",
         "import net.minecraft.world.inventory.ContainerInput;"),
    ],
    "dev/echo/client/utils/ClientWorldUpdates.java": [
        ("world.setTimeFromServer(time, time, false);", "world.setTimeFromServer(time);"),
    ],
    "dev/echo/client/utils/trajectory/CrossbowTrajectories.java": [
        ("charged == null ? List.of() : charged.items()",
         "charged == null ? List.<ItemStack>of() : charged.getItems()"),
    ],
}


def repair(log_path):
    """Restore members the broad regexes renamed where the compiler says they are gone."""
    text = pathlib.Path(log_path).read_text(errors="replace")
    blocks = re.split(r"(?=^\S+\.java:\d+: error:)", text, flags=re.M)
    fixed = 0
    for b in blocks:
        m = re.match(r"^(src/[^\s:]+\.java):(\d+): error: cannot find symbol$", b, re.M)
        if not m:
            continue
        sm = re.search(r"symbol:\s+method (\w+)\(\)", b)
        if not sm or sm.group(1) not in REPAIRS:
            continue
        path = ROOT.parents[2] / m.group(1)
        if not path.exists():
            continue
        lines = path.read_text().splitlines(keepends=True)
        idx = int(m.group(2)) - 1
        if idx >= len(lines):
            continue
        wrong, right = sm.group(1), REPAIRS[sm.group(1)]
        if f".{wrong}()" in lines[idx]:
            lines[idx] = lines[idx].replace(f".{wrong}()", f".{right}()")
            path.write_text("".join(lines))
            fixed += 1
    print(f"repaired {fixed} call sites")


def main():
    if len(sys.argv) > 2 and sys.argv[1] == "--repair":
        repair(sys.argv[2])
        return 0
    changed = 0
    for rel, edits in FILE_EDITS.items():
        path = ROOT / rel
        if not path.exists():
            print("missing:", rel)
            continue
        text = orig = path.read_text()
        for old, new in edits:
            if old in text:
                text = text.replace(old, new)
            else:
                print(f"  no-match in {rel}: {old[:60]!r}")
        if text != orig:
            path.write_text(text)
            changed += 1
    print(f"{changed} files updated")
    return 0


if __name__ == "__main__":
    sys.exit(main())
