package dev.echo.client.features.impl.render;

import dev.echo.client.features.Category;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.features.hud.EchoHudElement;
import dev.echo.client.hud.HudElementLayout;
import dev.echo.client.hud.HudElementType;
import dev.echo.client.hud.HudLayout;
import dev.echo.client.hud.HudRenderer;
import dev.echo.render.FontAsset;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.ui.UiSize;
import dev.echo.render.texture.TextureView;
import net.minecraft.item.ItemStack;

/** Animated target portrait, health, armor and main-hand item. */
public class TargetFeature extends EchoHudElement {
    public TargetFeature() {
        super(new FeatureInfo(CatalogText.literal("Target"),
                CatalogText.literal("Target health and armor"), Category.RENDER), HudElementType.TARGET);
    }

    @Override
    protected boolean shouldRender() {
        return HudRenderer.INSTANCE.getTargetVisibility() > 0.002f;
    }

    @Override
    protected void renderContents(HudElementLayout layout) {
        HudRenderer hud = HudRenderer.INSTANCE;
        boolean inGame = client.currentScreen == null;
        float visibility = inGame ? hud.getTargetVisibility() : 1.0f;
        float textOpacity = inGame ? hud.getTargetTextOpacity() : 1.0f;
        layout.setAnimation(0.92f + 0.08f * visibility, visibility);
        String name = hud.getTargetName();
        String healthText = HudRenderer.formatTargetHealth(hud.getTargetHealth());
        float panelWidth = layout.getWidth();
        float contentWidth = Math.max(96.0f, panelWidth - 62.0f - 10.0f);
        float right = 932.0f + contentWidth;
        hud.drawPanel(layout, 870.0f, 511.0f, panelWidth, 58.0f, 12.0f, 1.0f, 14.0f);
        TextureView skin = hud.getTargetSkin();
        if (skin == null) {
            layout.drawTexture(hud.getFallbackTargetPortrait(), 877.0f, 518.0f, 44.0f, 44.0f, 8.0f, 1.0f);
        } else {
            layout.drawTextureRegion(skin, 877.0f, 518.0f, 44.0f, 44.0f, 8.0f, 1.0f,
                    0.125f, 0.125f, 0.25f, 0.25f);
            layout.drawTextureRegion(skin, 877.0f, 518.0f, 44.0f, 44.0f, 8.0f, 1.0f,
                    0.625f, 0.125f, 0.75f, 0.25f);
        }
        float nameY = HudLayout.centeredLeft(530.0f, layout.fontHeight(FontAsset.INTER_SEMI_BOLD, 13.0f));
        float healthY = HudLayout.centeredLeft(530.0f, layout.fontHeight(FontAsset.INTER, 12.0f));
        layout.drawText(FontAsset.INTER_SEMI_BOLD, name, 932.0f, nameY, 13.0f, -1, textOpacity);
        layout.drawText(FontAsset.INTER, healthText,
                HudLayout.rightAlignedLeft(right, layout.textWidth(FontAsset.INTER, healthText, 12.0f)),
                healthY, 12.0f, -1929379841, textOpacity);
        for (int slot = 0; slot < 4; slot++) {
            drawArmorSlot(layout, slot, slotX(slot), 539.0f);
        }
        ItemStack mainHand = hud.getTargetMainHand();
        if (mainHand.isEmpty()) {
            drawEmptySlot(layout, slotX(4), 539.0f);
        } else {
            layout.drawItem(mainHand, slotX(4), 539.0f, 16.0f, 44);
        }
        layout.drawRoundedRect(932.0f, 555.5f, contentWidth, 2.0f, 1.0f, 0x26ffffff, 1.0f);
        float fraction = HudLayout.clampFraction(hud.getTargetHealth() / Math.max(1.0f, hud.getTargetMaximumHealth()));
        layout.drawAccentRect(932.0f, 555.5f, contentWidth * fraction, 2.0f, 1.0f, 1.0f);
    }

    private static float slotX(int slot) {
        return 932.0f + 18.0f * HudLayout.slotIndex(slot, 5);
    }

    private static void drawArmorSlot(HudElementLayout layout, int slot, float x, float y) {
        ItemStack stack = HudRenderer.INSTANCE.getTargetArmor(slot);
        if (stack.isEmpty()) {
            drawEmptySlot(layout, x, y);
            return;
        }
        layout.drawItem(stack, x, y, 16.0f, 40 + slot);
    }

    private static void drawEmptySlot(HudElementLayout layout, float x, float y) {
        float width = layout.textWidth(FontAsset.INTER, "×", 9.0f);
        float height = layout.fontHeight(FontAsset.INTER, 9.0f);
        layout.drawText(FontAsset.INTER, "×", x + (16.0f - width) * 0.5f,
                y + (16.0f - height) * 0.5f, 9.0f, 0x59ffffff, 1.0f);
    }

    @Override
    public UiSize measure(HudElementLayout layout) {
        HudRenderer hud = HudRenderer.INSTANCE;
        String health = HudRenderer.formatTargetHealth(hud.getTargetHealth());
        float nameWidth = layout.textWidth(FontAsset.INTER_SEMI_BOLD, hud.getTargetName(), 13.0f);
        float healthWidth = layout.textWidth(FontAsset.INTER, health, 12.0f);
        return new UiSize(HudLayout.targetPanelWidth(HudLayout.targetContentWidth(nameWidth, healthWidth)), 58.0f);
    }
}
