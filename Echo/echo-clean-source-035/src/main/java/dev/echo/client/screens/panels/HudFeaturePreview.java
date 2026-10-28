package dev.echo.client.screens.panels;

import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureManager;
import dev.echo.client.features.HudFeature;
import dev.echo.client.input.InputKey;
import dev.echo.client.render.UiRenderer;
import dev.echo.client.ui.ThemePalette;
import dev.echo.render.FontAsset;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Enabled-HUD bounds overlay and drag state. Original identity: hack/TransportProcessor.
 * The adjacent receipt records conditional caller/cache/control applicability.
 */
public class HudFeaturePreview {
    private static final Color IDLE_BACKGROUND = new Color(255, 255, 255, 10);
    private static final Color ACTIVE_BACKGROUND = new Color(0, 128, 255, 25);
    private static final Color IDLE_BORDER = new Color(255, 255, 255, 80);
    private static final Color ACTIVE_BORDER = new Color(0, 128, 255, 200);

    private HudFeature draggingFeature;
    private float dragOffsetX;
    private float dragOffsetY;

    public HudFeaturePreview() {
        draggingFeature = null;
    }

    public void render(UiRenderer renderer, int viewportWidth, int viewportHeight,
                       int mouseX, int mouseY, float opacity) {
        if (FontAsset.INTER_SEMI_BOLD == null) {
            return;
        }
        renderer.flush();
        List<HudFeature> features = previewFeatures();
        for (HudFeature feature : features) {
            float x = feature.getX();
            float y = feature.getY();
            float width = feature.getWidth();
            float height = feature.getHeight();
            boolean active = draggingFeature == feature || draggingFeature == null
                    && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
            Color background = active ? ACTIVE_BACKGROUND : IDLE_BACKGROUND;
            Color border = active ? ACTIVE_BORDER : IDLE_BORDER;
            renderer.drawRoundedRect(x, y, width, height, ThemePalette.ROW_RADIUS,
                    ThemePalette.multiplyAlpha(background.getRGB(), opacity));
            renderer.drawRoundedRect(x, y, width, 1.0f, 0.0f,
                    ThemePalette.multiplyAlpha(border.getRGB(), opacity));
            renderer.drawRoundedRect(x, y + height - 1.0f, width, 1.0f, 0.0f,
                    ThemePalette.multiplyAlpha(border.getRGB(), opacity));
            renderer.drawRoundedRect(x, y, 1.0f, height, 0.0f,
                    ThemePalette.multiplyAlpha(border.getRGB(), opacity));
            renderer.drawRoundedRect(x + width - 1.0f, y, 1.0f, height, 0.0f,
                    ThemePalette.multiplyAlpha(border.getRGB(), opacity));
            CharSequence label = feature.getName();
            float fontSize = 7.0f;
            float fontHeight = FontAsset.INTER_SEMI_BOLD.lineHeight(fontSize);
            int labelColor = ThemePalette.multiplyAlpha(
                    (active ? ACTIVE_BORDER : ThemePalette.SELECTED_OPTION_TEXT).getRGB(), opacity);
            renderer.drawText(FontAsset.INTER_SEMI_BOLD, label, x, y - fontHeight - 2.0f,
                    fontSize, labelColor);
        }
    }

    /** Selects the first hit in original registration order; a miss retains the previous selection. */
    public boolean mouseClicked(double mouseX, double mouseY) {
        for (HudFeature feature : previewFeatures()) {
            float x = feature.getX();
            float y = feature.getY();
            float width = feature.getWidth();
            float height = feature.getHeight();
            if (mouseX >= x && mouseX <= (double) (x + width)
                    && mouseY >= y && mouseY <= (double) (y + height)) {
                draggingFeature = feature;
                dragOffsetX = (float) mouseX - x;
                dragOffsetY = (float) mouseY - y;
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, int viewportWidth, int viewportHeight) {
        if (draggingFeature == null || !InputKey.MOUSE_LEFT.matches(button)) {
            return false;
        }
        float x = (float) mouseX - dragOffsetX;
        float y = (float) mouseY - dragOffsetY;
        // Preserve receiver capture, repeated field reads, clamp order and invalid-range failures.
        draggingFeature.setPosition(
                Math.clamp(x, 0.0f, (float) viewportWidth - draggingFeature.getWidth()),
                Math.clamp(y, 0.0f, (float) viewportHeight - draggingFeature.getHeight()));
        return true;
    }

    public boolean mouseReleased(int button) {
        if (!InputKey.MOUSE_LEFT.matches(button) || draggingFeature == null) {
            return false;
        }
        draggingFeature = null;
        return true;
    }

    public void reset() {
        draggingFeature = null;
    }

    private List<HudFeature> previewFeatures() {
        List<HudFeature> result = new ArrayList<>();
        for (Feature feature : FeatureManager.allFeatures()) {
            if (feature instanceof HudFeature hudFeature && feature.isEnabled()) {
                result.add(hudFeature);
            }
        }
        return result;
    }
}
