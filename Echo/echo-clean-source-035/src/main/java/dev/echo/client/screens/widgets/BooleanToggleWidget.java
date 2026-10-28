package dev.echo.client.screens.widgets;

import dev.echo.client.input.InputKey;
import dev.echo.render.FontAsset;
import dev.echo.client.render.UiRenderer;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.ui.ThemePalette;
import dev.echo.client.ui.TooltipRenderer;
import dev.echo.client.ui.animation.Easing;
import dev.echo.client.ui.animation.Tween;
import dev.echo.client.utils.strings.TextBuffers;
import java.awt.Color;

/**
 * Original boolean-setting row and its animated switch.
 * Source-complete candidate; binding/cache and provider obligations are recorded
 * in analysis/completion-v2 rather than implemented as a runtime key selector.
 */
public class BooleanToggleWidget implements SettingWidget {
    private BooleanSetting setting;
    private float x;
    private float y;
    private Tween toggle = new Tween(Easing.EASE_OUT_CUBIC, 150.0);

    /** Starts the animation at the setting's effective, availability-aware value. */
    public BooleanToggleWidget(BooleanSetting setting) {
        this.setting = setting;
        float initial = setting.getValue() ? 1.0f : 0.0f;
        toggle.start(initial, initial);
    }

    /** Draws the original row, accent rule, label, track, knob shadow and knob. */
    @Override
    public void render(UiRenderer renderer, float x, float y, int mouseX, int mouseY, float opacity) {
        this.x = x;
        this.y = y;
        toggle.setTarget(setting.getValue() ? 1.0f : 0.0f);
        float progress = (float) toggle.value();
        float rowX = x + ThemePalette.ROW_INDENT;
        float rowWidth = ThemePalette.PANEL_WIDTH - ThemePalette.ROW_INDENT - 4.0f;
        boolean hovered = ThemePalette.contains(rowX, y, rowWidth, ThemePalette.ROW_HEIGHT, mouseX, mouseY);
        if (hovered) {
            CharSequence description = setting.getDescription();
            if (description != null && description.length() > 0) {
                TooltipRenderer.offer(setting, setting.getLabel(), description, mouseX, mouseY);
            }
        }

        renderer.drawRoundedRect(rowX, y, rowWidth, ThemePalette.ROW_HEIGHT, ThemePalette.ROW_RADIUS,
                ThemePalette.multiplyAlpha((hovered ? ThemePalette.ROW_HOVER : ThemePalette.ROW_BACKGROUND).getRGB(), opacity));
        renderer.drawRoundedRect(rowX, y + 2.0f, 1.5f, ThemePalette.ROW_HEIGHT - 4.0f, 0.0f,
                ThemePalette.multiplyAlpha(ThemePalette.ROW_ACCENT.getRGB(), opacity));
        CharSequence label = TextBuffers.abbreviate(setting.getLabel(), 18);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, label, rowX + ThemePalette.CONTENT_PADDING,
                y + ThemePalette.ROW_HEIGHT / 2.0f - 4.0f, 6.0f,
                ThemePalette.multiplyAlpha(ThemePalette.PRIMARY_TEXT, opacity));

        float trackX = rowX + rowWidth - ThemePalette.TOGGLE_WIDTH - ThemePalette.CONTENT_PADDING;
        float trackY = y + (ThemePalette.ROW_HEIGHT - ThemePalette.TOGGLE_HEIGHT) / 2.0f;
        Color track = ThemePalette.blend(ThemePalette.TOGGLE_OFF, ThemePalette.TOGGLE_ON, progress);
        renderer.drawRoundedRect(trackX, trackY, ThemePalette.TOGGLE_WIDTH, ThemePalette.TOGGLE_HEIGHT,
                ThemePalette.TOGGLE_HEIGHT / 2.3f, ThemePalette.multiplyAlpha(track.getRGB(), opacity));
        float knobX = trackX + 1.5f
                + (ThemePalette.TOGGLE_WIDTH - ThemePalette.TOGGLE_KNOB_SIZE - 3.0f) * progress;
        float knobY = trackY + 1.5f;
        renderer.drawRoundedRect(knobX + 0.5f, knobY + 0.5f,
                ThemePalette.TOGGLE_KNOB_SIZE, ThemePalette.TOGGLE_KNOB_SIZE,
                ThemePalette.TOGGLE_KNOB_SIZE / 2.0f,
                ThemePalette.multiplyAlpha(ThemePalette.KNOB_SHADOW.getRGB(), opacity));
        renderer.drawRoundedRect(knobX, knobY, ThemePalette.TOGGLE_KNOB_SIZE, ThemePalette.TOGGLE_KNOB_SIZE,
                ThemePalette.TOGGLE_KNOB_SIZE / 2.0f,
                ThemePalette.multiplyAlpha(ThemePalette.TOGGLE_KNOB.getRGB(), opacity));
    }

    /** Returns the shared original row height. */
    @Override
    public float height() {
        return ThemePalette.ROW_HEIGHT;
    }

    /** Changes the setting only when its original switch hitbox receives the primary button. */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!InputKey.MOUSE_LEFT.matches(button)) {
            return false;
        }
        // The original hitbox starts two pixels to the right of the drawn track.
        float hitX = x + ThemePalette.PANEL_WIDTH - ThemePalette.TOGGLE_WIDTH
                - ThemePalette.CONTENT_PADDING - 2.0f;
        float hitY = y + (ThemePalette.ROW_HEIGHT - ThemePalette.TOGGLE_HEIGHT) / 2.0f;
        if (!ThemePalette.contains(hitX, hitY, ThemePalette.TOGGLE_WIDTH, ThemePalette.TOGGLE_HEIGHT,
                (float) mouseX, (float) mouseY)) {
            return false;
        }
        setting.setValue(!setting.getValue());
        return true;
    }

    /** The original toggle has no release action or retained drag state. */
    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        // Exact original getConnection(DJDI)V is a single return instruction.
    }

    /** Returns the same setting object supplied at construction. */
    @Override
    public BooleanSetting setting() {
        return setting;
    }
}
