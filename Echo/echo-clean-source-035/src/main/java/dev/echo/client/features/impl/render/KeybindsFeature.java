package dev.echo.client.features.impl.render;

import dev.echo.client.features.Category;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.features.hud.EchoHudElement;
import dev.echo.client.hud.HudElementLayout;
import dev.echo.client.hud.HudElementType;
import dev.echo.client.hud.HudIcon;
import dev.echo.client.hud.HudLayout;
import dev.echo.client.hud.HudRenderer;
import dev.echo.client.hud.KeybindRow;
import dev.echo.render.FontAsset;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.ui.UiSize;
import java.util.List;

/** Keybind list with staggered row opacity and the original enabled-state colors. */
public class KeybindsFeature extends EchoHudElement {
    private static final float ROW_HEIGHT = 23.0f;

    public KeybindsFeature() {
        super(new FeatureInfo(CatalogText.literal("Keybinds"),
                CatalogText.literal("Compact active-key overview"), Category.RENDER), HudElementType.KEYBINDS);
    }

    @Override
    protected void renderContents(HudElementLayout layout) {
        HudRenderer hud = HudRenderer.INSTANCE;
        float x = HudElementType.KEYBINDS.defaultX();
        float y = HudElementType.KEYBINDS.defaultY();
        float width = layout.getWidth();
        hud.drawPanel(layout, x, y, width, layout.getHeight(), 12.0f, 1.0f, 14.0f);
        layout.drawHeaderHighlight(x + (width - 56.0f) * 0.5f, y + 5.0f, 56.0f, 2.0f, 1.0f, 1.0f);
        layout.drawText(FontAsset.INTER_SEMI_BOLD, "Keybinds", x + 16.0f, y + 14.0f, 13.0f, -1, 1.0f);
        layout.drawIcon(HudIcon.KEYBOARD, x + width - 30.0f, y + 14.5f, 14.0f, 14.0f, -1, 1.0f);
        List<KeybindRow> entries = hud.getKeybindRows();
        for (int index = 0; index < entries.size(); index++) {
            KeybindRow entry = entries.get(index);
            float progress = hud.entryProgress("keybind/" + entry.id(), 300L + index * 24L, false);
            if (progress <= 0.002f) {
                continue;
            }
            float rowY = y + 47.0f + index * ROW_HEIGHT;
            float slide = 6.0f * (1.0f - progress);
            int color = entry.enabled() ? -1 : -1711276033;
            layout.drawIcon(entry.icon(), x + 16.0f - slide, rowY + 1.0f, 12.0f, 12.0f, color, progress);
            layout.drawText(FontAsset.INTER, entry.name(), x + 38.0f - slide, rowY, 12.5f, color, progress);
            layout.drawTextRight(FontAsset.INTER, "[" + entry.binding() + "]", x + width - 16.0f,
                    rowY, 12.0f, -1711276033, progress);
        }
    }

    @Override
    public UiSize measure(HudElementLayout layout) {
        List<KeybindRow> entries = HudRenderer.INSTANCE.getKeybindRows();
        float width = HudElementType.KEYBINDS.defaultWidth();
        for (KeybindRow entry : entries) {
            float nameWidth = layout.textWidth(FontAsset.INTER, entry.name(), 12.5f);
            float bindingWidth = layout.textWidth(FontAsset.INTER, "[" + entry.binding() + "]", 12.0f);
            float rowWidth = HudLayout.roundWidth(Math.max(HudElementType.KEYBINDS.defaultWidth(),
                    38.0f + Math.max(0.0f, nameWidth) + 12.0f + Math.max(0.0f, bindingWidth) + 16.0f));
            width = Math.max(width, rowWidth);
        }
        return new UiSize(width, HudLayout.effectsPanelHeight(entries.size()));
    }
}
