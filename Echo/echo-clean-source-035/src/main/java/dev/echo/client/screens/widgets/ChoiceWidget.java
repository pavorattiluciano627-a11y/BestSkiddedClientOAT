package dev.echo.client.screens.widgets;

import dev.echo.client.input.InputKey;
import dev.echo.render.FontAsset;
import dev.echo.client.render.UiRenderer;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.ui.ThemePalette;
import dev.echo.client.ui.TooltipRenderer;
import dev.echo.client.ui.animation.Easing;
import dev.echo.client.ui.animation.Tween;
import dev.echo.client.utils.strings.TextBuffers;
import java.awt.Color;

/** Original expanding single-choice row, retaining option order and option references. */
public class ChoiceWidget implements SettingWidget {
    private final ChoiceSetting setting;
    private float x;
    private float y;
    private boolean expanded = false;
    private final Tween expansion;

    /** Retains the supplied choice collection through its owning setting. */
    public ChoiceWidget(ChoiceSetting setting) {
        expansion = new Tween(Easing.EASE_OUT_CUBIC, 200.0);
        this.setting = setting;
        expansion.start(0.0, 0.0);
    }

    /** Draws the original header and alpha-revealed option text in collection order. */
    @Override
    public void render(UiRenderer renderer, float x, float y, int mouseX, int mouseY, float opacity) {
        this.x = x;
        this.y = y;
        expansion.setTarget(expanded ? 1.0f : 0.0f);
        float progress = (float) expansion.value();
        float rowX = x + ThemePalette.ROW_INDENT;
        float rowWidth = ThemePalette.PANEL_WIDTH - ThemePalette.ROW_INDENT - 4.0f;
        float height = height();
        boolean hovered = ThemePalette.contains(rowX, y, rowWidth, ThemePalette.ROW_HEIGHT, mouseX, mouseY);
        if (hovered) {
            CharSequence description = setting.getDescription();
            if (description != null && description.length() > 0) {
                TooltipRenderer.offer(setting, setting.getLabel(), description, mouseX, mouseY);
            }
        }
        renderer.drawRoundedRect(rowX, y, rowWidth, height, ThemePalette.ROW_RADIUS,
                ThemePalette.multiplyAlpha((hovered ? ThemePalette.ROW_HOVER : ThemePalette.ROW_BACKGROUND).getRGB(), opacity));
        renderer.drawRoundedRect(rowX, y + 2.0f, 1.5f, height - 4.0f, 0.0f,
                ThemePalette.multiplyAlpha(ThemePalette.ROW_ACCENT.getRGB(), opacity));
        CharSequence label = TextBuffers.abbreviate(setting.getLabel(), 14);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, label, rowX + ThemePalette.CONTENT_PADDING,
                y + ThemePalette.ROW_HEIGHT / 2.0f - 4.0f, 6.0f,
                ThemePalette.multiplyAlpha(ThemePalette.PRIMARY_TEXT, opacity));
        CharSequence selected = setting.getValue();
        float selectedWidth = FontAsset.INTER_SEMI_BOLD.measureText(selected, 6.0f);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, selected,
                rowX + rowWidth - ThemePalette.CONTENT_PADDING - selectedWidth,
                y + ThemePalette.ROW_HEIGHT / 2.0f - 4.0f, 6.0f,
                ThemePalette.multiplyAlpha(ThemePalette.SECONDARY_TEXT, opacity));

        if (progress > ThemePalette.VISIBLE_ANIMATION_THRESHOLD) {
            float optionY = y + ThemePalette.ROW_HEIGHT;
            int alpha = (int) (progress * 255.0f);
            for (CharSequence option : setting.getOptions()) {
                Color color = setting.isSelected(option) ? ThemePalette.SELECTED_OPTION_TEXT : ThemePalette.SECONDARY_TEXT;
                int argb = ThemePalette.multiplyAlpha(ThemePalette.withAlpha(color, alpha), opacity);
                renderer.drawText(FontAsset.INTER_SEMI_BOLD, option,
                        rowX + ThemePalette.CONTENT_PADDING + 4.0f,
                        optionY + (ThemePalette.ROW_HEIGHT - 2.0f) / 2.0f - 4.0f, 6.0f, argb);
                optionY += ThemePalette.ROW_HEIGHT - 2.0f;
            }
        }
    }

    /** Samples expansion independently, as the original layout query does. */
    @Override
    public float height() {
        float progress = (float) expansion.value();
        float height = ThemePalette.ROW_HEIGHT;
        if (progress > ThemePalette.VISIBLE_ANIMATION_THRESHOLD) {
            height += (ThemePalette.ROW_HEIGHT - 2.0f) * setting.getOptions().size() * progress;
        }
        return height;
    }

    /** Toggles the header or selects an option without closing the original popup. */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!InputKey.MOUSE_LEFT.matches(button)) {
            return false;
        }
        float rowX = x + ThemePalette.ROW_INDENT;
        float rowWidth = ThemePalette.PANEL_WIDTH - ThemePalette.ROW_INDENT - 4.0f;
        if (ThemePalette.contains(rowX, y, rowWidth, ThemePalette.ROW_HEIGHT, (float) mouseX, (float) mouseY)) {
            expanded = !expanded;
            return true;
        }
        float progress = (float) expansion.value();
        if (expanded && progress > ThemePalette.INPUT_ANIMATION_THRESHOLD) {
            float optionY = y + ThemePalette.ROW_HEIGHT;
            for (CharSequence option : setting.getOptions()) {
                if (ThemePalette.contains(rowX + ThemePalette.CONTENT_PADDING, optionY,
                        rowWidth - ThemePalette.CONTENT_PADDING * 2.0f, ThemePalette.ROW_HEIGHT - 2.0f,
                        (float) mouseX, (float) mouseY)) {
                    setting.setValue(option);
                    return true;
                }
                optionY += ThemePalette.ROW_HEIGHT - 2.0f;
            }
        }
        return false;
    }

    /** The original choice row does not handle mouse release. */
    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        // Exact original getConnection(DJDI)V body: return.
    }

    /** Returns the retained setting reference. */
    @Override
    public ChoiceSetting setting() {
        return setting;
    }
}
