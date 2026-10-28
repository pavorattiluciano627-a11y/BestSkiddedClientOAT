package dev.echo.client.screens.widgets;

import dev.echo.client.input.InputKey;
import dev.echo.render.FontAsset;
import dev.echo.client.render.UiRenderer;
import dev.echo.client.setting.ColorSetting;
import dev.echo.client.ui.ThemePalette;
import dev.echo.client.ui.TooltipRenderer;
import dev.echo.client.ui.animation.Easing;
import dev.echo.client.ui.animation.Tween;
import dev.echo.client.utils.strings.TextBuffers;
import java.awt.Color;

/** Original RGB/HSB picker, optional alpha strip, and release-time change callback. */
public class ColorPickerWidget implements SettingWidget {
    private final ColorSetting setting;
    private float x;
    private float y;
    private boolean expanded = false;
    private final Tween expansion;
    private boolean draggingSaturationBrightness = false;
    private boolean draggingHue = false;
    private boolean draggingAlpha = false;

    /** Retains the color setting and starts with the picker collapsed. */
    public ColorPickerWidget(ColorSetting setting) {
        expansion = new Tween(Easing.EASE_OUT_CUBIC, 200.0);
        this.setting = setting;
        expansion.start(0.0, 0.0);
    }

    /** Updates an active drag before drawing the original picker gradients and cursors. */
    @Override
    public void render(UiRenderer renderer, float x, float y, int mouseX, int mouseY, float opacity) {
        this.x = x;
        this.y = y;
        expansion.setTarget(expanded ? 1.0f : 0.0f);
        float progress = (float) expansion.value();
        if (draggingSaturationBrightness) {
            updateSaturationBrightness(mouseX, mouseY);
        } else if (draggingHue) {
            updateHue(mouseX);
        } else if (draggingAlpha) {
            updateAlpha(mouseX);
        }

        float rowX = x + ThemePalette.ROW_INDENT;
        float rowWidth = ThemePalette.PANEL_WIDTH - ThemePalette.ROW_INDENT - 4.0f;
        boolean hovered = ThemePalette.contains(rowX, y, rowWidth, height(), mouseX, mouseY);
        if (hovered) {
            CharSequence description = setting.getDescription();
            if (description != null && description.length() > 0) {
                TooltipRenderer.offer(setting, setting.getLabel(), description, mouseX, mouseY);
            }
        }
        renderer.drawRoundedRect(rowX, y, rowWidth, height(), ThemePalette.ROW_RADIUS,
                ThemePalette.multiplyAlpha((hovered ? ThemePalette.ROW_HOVER : ThemePalette.ROW_BACKGROUND).getRGB(), opacity));
        renderer.drawRoundedRect(rowX, y + 2.0f, 1.5f, height() - 4.0f, 0.0f,
                ThemePalette.multiplyAlpha(ThemePalette.ROW_ACCENT.getRGB(), opacity));
        CharSequence label = TextBuffers.abbreviate(setting.getLabel(), 14);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, label, rowX + ThemePalette.CONTENT_PADDING,
                y + ThemePalette.ROW_HEIGHT / 2.0f - 4.0f, 6.0f,
                ThemePalette.multiplyAlpha(ThemePalette.PRIMARY_TEXT, opacity));

        float previewX = rowX + rowWidth - ThemePalette.TOGGLE_WIDTH - ThemePalette.CONTENT_PADDING;
        float previewY = y + (ThemePalette.ROW_HEIGHT - ThemePalette.TOGGLE_HEIGHT) / 2.0f;
        renderer.drawRoundedRect(previewX, previewY, ThemePalette.TOGGLE_WIDTH, ThemePalette.TOGGLE_HEIGHT,
                ThemePalette.TOGGLE_HEIGHT / 2.3f, ThemePalette.multiplyAlpha(setting.getArgb(), opacity));
        if (progress > 0.01f) {
            float[] hsb = Color.RGBtoHSB(setting.getRed(), setting.getGreen(), setting.getBlue(), null);
            float pickerY = y + ThemePalette.ROW_HEIGHT;
            float pickerHeight = 60.0f * progress;
            float pickerX = rowX + ThemePalette.CONTENT_PADDING;
            float pickerWidth = rowWidth - ThemePalette.CONTENT_PADDING * 2.0f;
            renderer.drawSaturationBrightness(pickerX, pickerY, pickerWidth, pickerHeight, hsb[0], opacity);
            float saturationX = pickerX + pickerWidth * hsb[1];
            float brightnessY = pickerY + pickerHeight * (1.0f - hsb[2]);
            renderer.drawRoundedRect(saturationX - 2.0f, brightnessY - 2.0f, 4.0f, 4.0f, 2.0f,
                    ThemePalette.multiplyAlpha(Color.WHITE.getRGB(), opacity * progress));

            float hueY = pickerY + pickerHeight + 5.0f * progress;
            renderer.drawHueStrip(pickerX, hueY, pickerWidth, 5.0f * progress, opacity);
            renderer.drawRoundedRect(pickerX + pickerWidth * hsb[0] - 2.0f,
                    hueY - 2.0f * progress, 4.0f, 9.0f * progress, 2.0f * progress,
                    ThemePalette.multiplyAlpha(Color.WHITE.getRGB(), opacity * progress));

            if (setting.isAlphaMode()) {
                float alphaY = hueY + 10.0f * progress;
                float red = setting.getRed() / 255.0f;
                float green = setting.getGreen() / 255.0f;
                float blue = setting.getBlue() / 255.0f;
                renderer.drawAlphaStrip(pickerX, alphaY, pickerWidth, 5.0f * progress,
                        red, green, blue, opacity);
                renderer.drawRoundedRect(pickerX + pickerWidth * setting.getAlphaFraction() - 2.0f,
                        alphaY - 2.0f * progress, 4.0f, 9.0f * progress, 2.0f * progress,
                        ThemePalette.multiplyAlpha(Color.WHITE.getRGB(), opacity * progress));
            }
        }
    }

    /** Includes the optional alpha strip in the original animated height. */
    @Override
    public float height() {
        float progress = (float) expansion.value();
        float height = ThemePalette.ROW_HEIGHT;
        if (progress > ThemePalette.VISIBLE_ANIMATION_THRESHOLD) {
            float pickerHeight = setting.isAlphaMode()
                    ? ThemePalette.COLOR_PICKER_HEIGHT : ThemePalette.COLOR_PICKER_HEIGHT - 10.0f;
            height += pickerHeight * progress;
        }
        return height;
    }

    /** Starts one exclusive drag or toggles the preview; picking does not notify until release. */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!InputKey.MOUSE_LEFT.matches(button)) {
            return false;
        }
        float rowX = x + ThemePalette.ROW_INDENT;
        float rowWidth = ThemePalette.PANEL_WIDTH - ThemePalette.ROW_INDENT - 4.0f;
        float previewX = rowX + rowWidth - ThemePalette.TOGGLE_WIDTH - ThemePalette.CONTENT_PADDING;
        float previewY = y + (ThemePalette.ROW_HEIGHT - ThemePalette.TOGGLE_HEIGHT) / 2.0f;
        if (ThemePalette.contains(previewX, previewY, ThemePalette.TOGGLE_WIDTH, ThemePalette.TOGGLE_HEIGHT,
                (float) mouseX, (float) mouseY)) {
            expanded = !expanded;
            return true;
        }
        float progress = (float) expansion.value();
        if (expanded && progress > ThemePalette.INPUT_ANIMATION_THRESHOLD) {
            float pickerY = y + ThemePalette.ROW_HEIGHT;
            float pickerX = rowX + ThemePalette.CONTENT_PADDING;
            float pickerWidth = rowWidth - ThemePalette.CONTENT_PADDING * 2.0f;
            float hueY = pickerY + 65.0f;
            float alphaY = hueY + 10.0f;
            if (ThemePalette.contains(pickerX, pickerY, pickerWidth, 60.0f, (float) mouseX, (float) mouseY)) {
                stopDragging();
                draggingSaturationBrightness = true;
                updateSaturationBrightness(mouseX, mouseY);
                return true;
            }
            if (ThemePalette.contains(pickerX, hueY, pickerWidth, 5.0f, (float) mouseX, (float) mouseY)) {
                stopDragging();
                draggingHue = true;
                updateHue(mouseX);
                return true;
            }
            if (setting.isAlphaMode()
                    && ThemePalette.contains(pickerX, alphaY, pickerWidth, 5.0f, (float) mouseX, (float) mouseY)) {
                stopDragging();
                draggingAlpha = true;
                updateAlpha(mouseX);
                return true;
            }
        }
        return false;
    }

    /** Notifies once for any active drag before clearing its state, regardless of released button. */
    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingSaturationBrightness || draggingHue || draggingAlpha) {
            setting.notifyChanged();
        }
        // Deliberately not a finally block: a thrown callback retains the original drag state.
        stopDragging();
    }

    private void stopDragging() {
        draggingSaturationBrightness = false;
        draggingHue = false;
        draggingAlpha = false;
    }

    private void updateSaturationBrightness(double mouseX, double mouseY) {
        float rowX = x + ThemePalette.ROW_INDENT;
        float pickerX = rowX + ThemePalette.CONTENT_PADDING;
        float pickerWidth = ThemePalette.PANEL_WIDTH - ThemePalette.ROW_INDENT - 4.0f
                - ThemePalette.CONTENT_PADDING * 2.0f;
        float[] hsb = Color.RGBtoHSB(setting.getRed(), setting.getGreen(), setting.getBlue(), null);
        float pickerY = y + ThemePalette.ROW_HEIGHT;
        float saturation = Math.clamp((float) (mouseX - pickerX) / pickerWidth, 0.0f, 1.0f);
        float brightness = Math.clamp(1.0f - (float) (mouseY - pickerY) / 60.0f, 0.0f, 1.0f);
        Color color = Color.getHSBColor(hsb[0], saturation, brightness);
        setting.setRedSilently(color.getRed());
        setting.setGreenSilently(color.getGreen());
        setting.setBlueSilently(color.getBlue());
    }

    private void updateHue(double mouseX) {
        float rowX = x + ThemePalette.ROW_INDENT;
        float pickerX = rowX + ThemePalette.CONTENT_PADDING;
        float pickerWidth = ThemePalette.PANEL_WIDTH - ThemePalette.ROW_INDENT - 4.0f
                - ThemePalette.CONTENT_PADDING * 2.0f;
        float[] hsb = Color.RGBtoHSB(setting.getRed(), setting.getGreen(), setting.getBlue(), null);
        float hue = Math.clamp((float) (mouseX - pickerX) / pickerWidth, 0.0f, 1.0f);
        Color color = Color.getHSBColor(hue, hsb[1], hsb[2]);
        setting.setRedSilently(color.getRed());
        setting.setGreenSilently(color.getGreen());
        setting.setBlueSilently(color.getBlue());
    }

    private void updateAlpha(double mouseX) {
        float rowX = x + ThemePalette.ROW_INDENT;
        float pickerX = rowX + ThemePalette.CONTENT_PADDING;
        float pickerWidth = ThemePalette.PANEL_WIDTH - ThemePalette.ROW_INDENT - 4.0f
                - ThemePalette.CONTENT_PADDING * 2.0f;
        setting.setAlphaSilently((int) (Math.clamp((float) (mouseX - pickerX) / pickerWidth, 0.0f, 1.0f) * 255.0f));
    }

    /** Returns the retained color setting reference. */
    @Override
    public ColorSetting setting() {
        return setting;
    }
}
