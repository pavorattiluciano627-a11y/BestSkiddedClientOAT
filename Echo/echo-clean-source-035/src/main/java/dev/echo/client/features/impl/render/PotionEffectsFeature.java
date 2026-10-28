package dev.echo.client.features.impl.render;

import dev.echo.client.features.Category;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.features.hud.EchoHudElement;
import dev.echo.client.hud.HudElementLayout;
import dev.echo.client.hud.HudElementType;
import dev.echo.client.hud.HudLayout;
import dev.echo.client.hud.HudRenderer;
import dev.echo.client.hud.StatusEffectRow;
import dev.echo.render.FontAsset;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.ui.UiSize;
import java.util.List;

/** Separately animated status-effect panels with amplifier, timer and remaining bar. */
public class PotionEffectsFeature extends EchoHudElement {
    private static final float ROW_SPACING = 45.7f;
    private static final String ANIMATION_PREFIX = "effect/";

    public PotionEffectsFeature() {
        super(new FeatureInfo(CatalogText.literal("Potion Effects"),
                CatalogText.literal("Active effects, levels and timers"), Category.RENDER), HudElementType.POTION_EFFECTS);
    }

    @Override
    protected void renderContents(HudElementLayout layout) {
        HudRenderer hud = HudRenderer.INSTANCE;
        float x = HudElementType.POTION_EFFECTS.defaultX();
        float y = HudElementType.POTION_EFFECTS.defaultY();
        List<StatusEffectRow> effects = hud.getEffectRows();
        for (int index = 0; index < effects.size(); index++) {
            StatusEffectRow effect = effects.get(index);
            float progress = hud.entryProgress(ANIMATION_PREFIX + index, 210L + index * 34L, false);
            if (progress <= 0.002f) {
                continue;
            }
            float rowY = y + index * ROW_SPACING;
            float nameWidth = layout.textWidth(FontAsset.INTER_SEMI_BOLD, effect.name(), 13.0f);
            float amplifierWidth = effect.amplifier().isEmpty() ? 0.0f
                    : layout.textWidth(FontAsset.INTER, effect.amplifier(), 12.0f);
            // The original performs this measurement even though positioning is sequential.
            layout.textWidth(FontAsset.INTER, "[" + effect.duration() + "]", 12.0f);
            float slide = 24.0f * (1.0f - progress);
            hud.drawPanel(layout, x - slide, rowY, measureEffect(layout, effect), 41.0f, 10.0f, progress, 14.0f);
            layout.drawIcon(effect.icon(), x + 12.0f - slide, rowY + 12.5f, 16.0f, 16.0f, -1, progress);
            float cursor = x + 36.0f - slide;
            layout.drawText(FontAsset.INTER_SEMI_BOLD, effect.name(), cursor, rowY + 13.0f, 13.0f, -1, progress);
            cursor += nameWidth + 8.0f;
            if (!effect.amplifier().isEmpty()) {
                layout.drawText(FontAsset.INTER, effect.amplifier(), cursor, rowY + 13.5f, 12.0f, -1929379841, progress);
                cursor += amplifierWidth + 8.0f;
            }
            layout.drawText(FontAsset.INTER, "[" + effect.duration() + "]", cursor,
                    rowY + 13.5f, 12.0f, -1711276033, progress);
            float pulse = 0.82f + 0.18f * hud.pulse(3.2f + index * 0.07f);
            layout.drawAccentRect(x + 36.0f - slide, rowY + 35.0f,
                    Math.max(6.0f, 74.0f * effect.remainingFraction() * pulse), 2.0f, 1.0f, progress);
        }
    }

    @Override
    public UiSize measure(HudElementLayout layout) {
        List<StatusEffectRow> effects = HudRenderer.INSTANCE.getEffectRows();
        if (effects.isEmpty()) {
            return UiSize.fromDefaults(HudElementType.POTION_EFFECTS);
        }
        float width = 0.0f;
        for (StatusEffectRow effect : effects) {
            width = Math.max(width, measureEffect(layout, effect));
        }
        return new UiSize(width, 41.0f + (effects.size() - 1) * ROW_SPACING);
    }

    private static float measureEffect(HudElementLayout layout, StatusEffectRow effect) {
        float amplifierWidth = effect.amplifier().isEmpty() ? 0.0f
                : 8.0f + layout.textWidth(FontAsset.INTER, effect.amplifier(), 12.0f);
        return HudLayout.roundWidth(36.0f
                + layout.textWidth(FontAsset.INTER_SEMI_BOLD, effect.name(), 13.0f)
                + amplifierWidth + 8.0f
                + layout.textWidth(FontAsset.INTER, "[" + effect.duration() + "]", 12.0f) + 14.0f);
    }
}
