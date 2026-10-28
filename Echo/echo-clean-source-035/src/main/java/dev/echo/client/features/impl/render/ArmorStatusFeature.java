package dev.echo.client.features.impl.render;

import dev.echo.client.features.Category;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.features.hud.EchoHudElement;
import dev.echo.client.hud.HudElementLayout;
import dev.echo.client.hud.HudElementType;
import dev.echo.client.hud.HudLayout;
import dev.echo.client.hud.HudRenderer;
import dev.echo.render.FontAsset;
import net.minecraft.world.item.ItemStack;
import dev.echo.client.bootstrap.CatalogText;

/** Four equipped armor cells and their three separators. */
public class ArmorStatusFeature extends EchoHudElement {
    public ArmorStatusFeature() {
        super(new FeatureInfo(CatalogText.literal("Armor Status"),
                CatalogText.literal("Equipped helmet, chest, legs and boots"), Category.RENDER),
                HudElementType.ARMOR_STATUS);
    }

    @Override
    protected void renderContents(HudElementLayout layout) {
        HudRenderer hud = HudRenderer.INSTANCE;
        float x = HudElementType.ARMOR_STATUS.defaultX();
        float y = HudElementType.ARMOR_STATUS.defaultY();
        hud.drawPanel(layout, x, y, HudElementType.ARMOR_STATUS.defaultWidth(),
                HudElementType.ARMOR_STATUS.defaultHeight(), 12.0f, 1.0f, 14.0f);
        for (int separator = 0; separator < 3; separator++) {
            float separatorX = x + 43.0f + 39.0f * HudLayout.slotIndex(separator, 3);
            layout.drawRoundedRect(separatorX, y + 17.0f, 1.0f, 10.0f, 0.0f, 788529151, 1.0f);
        }
        for (int slot = 0; slot < 4; slot++) {
            ItemStack stack = hud.getEquippedArmor(slot);
            float slotX = x + 8.0f + 39.0f * HudLayout.slotIndex(slot, 4);
            if (stack == null || stack.isEmpty()) {
                float glyphWidth = layout.textWidth(FontAsset.INTER_MULTIPLY, "×", 11.0f);
                float glyphHeight = layout.fontHeight(FontAsset.INTER_MULTIPLY, 11.0f);
                layout.drawText(FontAsset.INTER_MULTIPLY, "×", slotX + (32.0f - glyphWidth) * 0.5f,
                        y + (44.0f - glyphHeight) * 0.5f, 11.0f, 1509949439, 1.0f);
            } else {
                layout.drawItem(stack, slotX + 8.0f, HudLayout.armorItemY(y), 16.0f, 20 + slot);
            }
        }
    }
}
