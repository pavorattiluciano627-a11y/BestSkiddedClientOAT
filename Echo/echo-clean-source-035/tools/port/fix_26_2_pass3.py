#!/usr/bin/env python3
"""Fourth pass: remaining 26.2 adaptations after the access widener rewrite.

Each rewrite below is grounded in a member that tools/port/jar_index.py proved
exists in the 26.2 client jar.
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2] / "src/main/java"

FILE_EDITS = {
    # RenderLayer's abstract hook is now submit(...EntityRenderState...) in 26.2.
    "dev/echo/client/render/PlayerOverlayFeature.java": [
        ("public void render(PoseStack matrices, SubmitNodeCollector commands, int light,\n                       AvatarRenderState state, float limbAngle, float limbDistance) {",
         "public void submit(PoseStack matrices, SubmitNodeCollector commands, int light,\n                       EntityRenderState renderState, float limbAngle, float limbDistance) {\n        if (!(renderState instanceof AvatarRenderState state)) {\n            return;\n        }"),
    ],
    # Generic GUI text colour is now a Vector4fc.
    "dev/echo/client/render/MinecraftItemAtlasSurface.java": [
        ("device.createCommandEncoder().clearColorAndDepthTextures(color, 0, depth, 1.0);",
         "device.createCommandEncoder().clearColorAndDepthTextures(color,\n                new org.joml.Vector4f(0.0F, 0.0F, 0.0F, 0.0F), depth, 1.0);"),
        ("color, 0, depth, 1.0, x, SIZE - bottom, tileSize, tileSize);",
         "color, new org.joml.Vector4f(0.0F, 0.0F, 0.0F, 0.0F), depth, 1.0,\n                    x, SIZE - bottom, tileSize, tileSize);"),
    ],
    "dev/echo/client/mixin/LightmapTextureManagerMixin.java": [
        ("0, 0, 0, 0", "new org.joml.Vector4f(0.0F, 0.0F, 0.0F, 0.0F)"),
    ],
    # Clear colour is a Vector4fc on the command encoder.
    "dev/echo/client/render/MinecraftRenderPass.java": [
        ("public MinecraftRenderPass(GpuTextureView color, OptionalInt clearColor,",
         "public MinecraftRenderPass(GpuTextureView color, java.util.Optional<org.joml.Vector4fc> clearColor,"),
    ],
    # Framebuffer helper gained the explicit colour format.
    "dev/echo/client/render/FramebufferBlur.java": [
        ("new TextureTarget(name, width, height, true)",
         "new TextureTarget(name, width, height, true, com.mojang.blaze3d.GpuFormat.RGBA8_UNORM)"),
    ],
    # Entity preview import moved to the state.gui.pip package.
    "dev/echo/client/render/EntityPreviewRenderer.java": [
        ("import net.minecraft.client.gui.render.state.pip.GuiEntityRenderState;",
         "import net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState;"),
    ],
    # Vertex index type moved out of VertexFormat.
    "dev/echo/client/mixin/GuiRendererMixin.java": [
        ("VertexFormat.IndexType", "IndexType"),
    ],
    # Click action enum type.
    "dev/echo/client/utils/inventory/SlotPackets.java": [
        ("private static ClickAction toMinecraftAction(SlotAction action) {",
         "private static ContainerInput toMinecraftAction(SlotAction action) {"),
    ],
    # Entity attack range now needs the held stack.
    "dev/echo/client/utils/targeting/AttackRangeProperties.java": [
        ("return entity.entityAttackRange();", "return entity.getAttackRangeWith(stack);"),
        ("return player.entityAttackRange().effectiveMaxRange(player);",
         "return player.getAttackRangeWith(player.getMainHandItem()).effectiveMaxRange(player);"),
        ("return player.entityAttackRange().isInRange(player, point);",
         "return player.getAttackRangeWith(player.getMainHandItem()).isInRange(player, point);"),
    ],
    "dev/echo/client/features/impl/combat/PiercingFeature.java": [
        ("double attackReach = player.entityAttackRange().effectiveMaxRange(player);",
         "double attackReach = player.getAttackRangeWith(player.getMainHandItem()).effectiveMaxRange(player);"),
        ("double reach = player.entityAttackRange().effectiveMaxRange(player);",
         "double reach = player.getAttackRangeWith(player.getMainHandItem()).effectiveMaxRange(player);"),
    ],
    # Charged projectiles accessor.
    "dev/echo/client/utils/trajectory/CrossbowTrajectories.java": [
        ("charged.getItems()", "charged.items()"),
    ],
    # Teleport/position packets expose record accessors.
    "dev/echo/client/features/impl/combat/BacktrackFeature.java": [
        ("position.getId()", "position.id()"),
        ("teleport.getId()", "teleport.id()"),
    ],
    # Chat component now hangs off the Hud.
    "dev/echo/client/utils/chat/ChatHudMessages.java": [
        ("getChat()", "hud.getChat()"),
    ],
    "dev/echo/client/scripting/api/ChatApi.java": [
        (".getChat()", ".hud.getChat()"),
    ],
}

# HudRenderEvent kept its own accessor names; only GpuDevice introspection moved.
EVENT_ACCESSOR_FILES = [
    "dev/echo/client/hud/HudRenderer.java",
    "dev/echo/client/screens/HudEditorScreen.java",
    "dev/echo/client/screens/dashboard/DashboardController.java",
    "dev/echo/client/render/hud/HudSurfacePreview.java",
]


def main():
    changed = 0
    for rel, edits in FILE_EDITS.items():
        path = ROOT / rel
        if not path.exists():
            continue
        text = orig = path.read_text()
        for old, new in edits:
            if old in text:
                text = text.replace(old, new)
            else:
                print(f"  no-match in {rel}: {old[:70]!r}")
        if text != orig:
            path.write_text(text)
            changed += 1

    for rel in EVENT_ACCESSOR_FILES:
        path = ROOT / rel
        if not path.exists():
            print("  missing", rel)
            continue
        text = path.read_text()
        new = text.replace("getDeviceInfo().name()", "getRenderer()") \
                  .replace("getDeviceInfo().vendorName()", "getVendor()") \
                  .replace("getDeviceInfo().backendName()", "getBackendName()")
        if new != text:
            path.write_text(new)
            changed += 1
        else:
            print("  no hud accessor edits in", rel)

    # Typed character input lost its modifier mask; read live modifier state instead.
    path = ROOT / "dev/echo/client/input/CharacterInputModifiers.java"
    path.write_text('''package dev.echo.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Extracts the GLFW modifier mask for a character-input event.
 *
 * 26.2 stopped carrying a modifier mask on {@link CharacterEvent}, so the mask
 * is read from live keyboard state at the moment the character arrives. The
 * returned value keeps the GLFW bit layout the screens already test against.
 */
public final class CharacterInputModifiers {
    private CharacterInputModifiers() {
    }

    public static int modifiers(CharacterEvent input) {
        Window window = Minecraft.getInstance().getWindow();
        if (window == null) {
            return 0;
        }
        int mask = 0;
        if (down(window, GLFW.GLFW_KEY_LEFT_SHIFT) || down(window, GLFW.GLFW_KEY_RIGHT_SHIFT)) {
            mask |= GLFW.GLFW_MOD_SHIFT;
        }
        if (down(window, GLFW.GLFW_KEY_LEFT_CONTROL) || down(window, GLFW.GLFW_KEY_RIGHT_CONTROL)) {
            mask |= GLFW.GLFW_MOD_CONTROL;
        }
        if (down(window, GLFW.GLFW_KEY_LEFT_ALT) || down(window, GLFW.GLFW_KEY_RIGHT_ALT)) {
            mask |= GLFW.GLFW_MOD_ALT;
        }
        if (down(window, GLFW.GLFW_KEY_LEFT_SUPER) || down(window, GLFW.GLFW_KEY_RIGHT_SUPER)) {
            mask |= GLFW.GLFW_MOD_SUPER;
        }
        return mask;
    }

    private static boolean down(Window window, int key) {
        return InputConstants.isKeyDown(window, key);
    }
}
''')
    changed += 1

    # Screens consume the shared helper instead of the removed event accessor.
    for rel in ["dev/echo/client/screens/CategoryPanelScreen.java",
                "dev/echo/client/screens/panels/ConfigsPanel.java",
                "dev/echo/client/screens/panels/FriendsPanel.java"]:
        p = ROOT / rel
        if not p.exists():
            continue
        t = p.read_text()
        n = re.sub(r"\binput\.modifiers\(\)", "CharacterInputModifiers.modifiers(input)", t)
        if n != t:
            if "import dev.echo.client.input.CharacterInputModifiers;" not in n:
                n = n.replace("import net.minecraft.client.input.CharacterEvent;",
                              "import dev.echo.client.input.CharacterInputModifiers;\nimport net.minecraft.client.input.CharacterEvent;", 1)
            p.write_text(n)
            changed += 1

    print(f"{changed} files updated")
    return 0


if __name__ == "__main__":
    sys.exit(main())
