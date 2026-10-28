package dev.echo.client.screens.widgets;

import dev.echo.client.input.InputKey;
import dev.echo.render.FontAsset;
import dev.echo.client.render.UiRenderer;
import dev.echo.client.screens.ScreenManager;
import dev.echo.client.setting.IntSetting;
import dev.echo.client.ui.ThemePalette;
import dev.echo.client.ui.TooltipRenderer;
import dev.echo.client.ui.animation.Easing;
import dev.echo.client.ui.animation.Tween;
import dev.echo.client.utils.strings.TextBuffers;

/** Original integer slider with its separately focused numeric text editor. */
public class IntegerSliderWidget implements SettingWidget {
    // Original activePipeline: constructed in <clinit>, interned, and read as a field.
    private static final String EMPTY_EDIT_PLACEHOLDER = new String(new char[]{'.', '.', '.'}).intern();
    private final IntSetting setting;
    private float x;
    private float y;
    private boolean dragging = false;
    private final Tween fill;
    private int lastDragValue = Integer.MIN_VALUE;
    private boolean editing = false;
    private String editText = "";
    private int caret = 0;
    private final Tween caretPulse;
    private float valueTextX;
    private float valueTextWidth;

    /** Retains the original setting and initializes fill from its normalized value. */
    public IntegerSliderWidget(IntSetting setting) {
        fill = new Tween(Easing.EASE_OUT_CUBIC, 150.0);
        caretPulse = new Tween(Easing.EASE_IN_OUT_QUART, 400.0);
        this.setting = setting;
        float initial = sliderFraction();
        fill.start(initial, initial);
    }

    /** Commits lost focus, applies drag updates, then renders the original row and caret. */
    @Override
    public void render(UiRenderer renderer, float x, float y, int mouseX, int mouseY, float opacity) {
        this.x = x;
        this.y = y;
        if (editing && ScreenManager.focusedSetting != setting) {
            commitTextEdit();
        }
        if (dragging) {
            updateFromPointer(mouseX);
        }
        float fraction = sliderFraction();
        fill.setTarget(fraction);
        float progress = (float) fill.value();
        float rowX = x + ThemePalette.ROW_INDENT;
        float rowWidth = ThemePalette.PANEL_WIDTH - ThemePalette.ROW_INDENT - 4.0f;
        boolean hovered = ThemePalette.contains(rowX, y, rowWidth, ThemePalette.SLIDER_ROW_HEIGHT, mouseX, mouseY);
        if (hovered) {
            CharSequence description = setting.getDescription();
            if (description != null && description.length() > 0) {
                TooltipRenderer.offer(setting, setting.getLabel(), description, mouseX, mouseY);
            }
        }
        renderer.drawRoundedRect(rowX, y, rowWidth, ThemePalette.SLIDER_ROW_HEIGHT, ThemePalette.ROW_RADIUS,
                ThemePalette.multiplyAlpha((hovered ? ThemePalette.ROW_HOVER : ThemePalette.ROW_BACKGROUND).getRGB(), opacity));
        renderer.drawRoundedRect(rowX, y + 2.0f, 1.5f, ThemePalette.SLIDER_ROW_HEIGHT - 4.0f, 0.0f,
                ThemePalette.multiplyAlpha(ThemePalette.ROW_ACCENT.getRGB(), opacity));
        CharSequence label = TextBuffers.abbreviate(setting.getLabel(), 16);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, label, rowX + ThemePalette.CONTENT_PADDING,
                y + 3.0f, 6.0f, ThemePalette.multiplyAlpha(ThemePalette.PRIMARY_TEXT, opacity));
        CharSequence valueText = editing ? (editText.isEmpty() ? EMPTY_EDIT_PLACEHOLDER : editText)
                : TextBuffers.integer(setting.getValue());
        CharSequence unit = setting.getUnit();
        float unitWidth = unit == null || unit.length() == 0 ? 0.0f : FontAsset.INTER_SEMI_BOLD.measureText(unit, 5.0f);
        valueTextWidth = FontAsset.INTER_SEMI_BOLD.measureText(valueText, 6.0f);
        valueTextX = rowX + rowWidth - ThemePalette.CONTENT_PADDING - valueTextWidth - unitWidth;
        int valueColor = editing ? ThemePalette.SELECTED_OPTION_TEXT.getRGB()
                : ThemePalette.multiplyAlpha(ThemePalette.SECONDARY_TEXT, opacity);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, valueText, valueTextX, y + 3.0f, 6.0f,
                editing ? ThemePalette.multiplyAlpha(valueColor, opacity) : valueColor);
        if (unitWidth > 0.0f) {
            renderer.drawText(FontAsset.INTER_SEMI_BOLD, unit, valueTextX + valueTextWidth,
                    y + 4.0f, 5.0f, ThemePalette.multiplyAlpha(ThemePalette.SECONDARY_TEXT, opacity * 0.6f));
        }
        if (editing && !editText.isEmpty()) {
            if (caretPulse.isFinished()) {
                caretPulse.start(caretPulse.targetValue(), 1.0 - caretPulse.targetValue());
            }
            int visibleCaret = Math.min(caret, editText.length());
            float caretX = valueTextX + FontAsset.INTER_SEMI_BOLD.measureText(editText.substring(0, visibleCaret), 6.0f);
            int caretAlpha = (int) (caretPulse.value() * 255.0);
            float caretHeight = 6.0f;
            float caretY = y + 3.0f + (FontAsset.INTER_SEMI_BOLD.lineHeight(6.0f) - caretHeight) * 0.5f;
            renderer.drawRoundedRect(caretX, caretY, 1.0f, caretHeight, 0.0f,
                    ThemePalette.multiplyAlpha(ThemePalette.withAlpha(ThemePalette.SLIDER_FILL, caretAlpha).getRGB(), opacity));
        }
        float trackY = y + ThemePalette.ROW_HEIGHT;
        float trackX = rowX + ThemePalette.CONTENT_PADDING;
        float trackRight = rowX + rowWidth - ThemePalette.CONTENT_PADDING;
        float trackWidth = trackRight - trackX;
        renderer.drawRoundedRect(trackX, trackY, trackWidth, 3.0f, 1.5f,
                ThemePalette.multiplyAlpha(ThemePalette.SLOT_BACKGROUND.getRGB(), opacity));
        renderer.drawRoundedRect(trackX, trackY, trackWidth * progress, 3.0f, 1.5f,
                ThemePalette.multiplyAlpha(ThemePalette.SLIDER_FILL.getRGB(), opacity));
        renderer.drawRoundedRect(trackX + trackWidth * progress - 4.0f, trackY - 2.0f, 8.0f, 7.0f, 4.0f,
                ThemePalette.multiplyAlpha(ThemePalette.TOGGLE_KNOB.getRGB(), opacity));
    }

    /** Returns the shared original slider row height. */
    @Override
    public float height() { return ThemePalette.SLIDER_ROW_HEIGHT; }

    /** Begins empty text editing or starts dragging after committing an existing edit. */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!InputKey.MOUSE_LEFT.matches(button)) return false;
        float rowX = x + ThemePalette.ROW_INDENT;
        float rowWidth = ThemePalette.PANEL_WIDTH - ThemePalette.ROW_INDENT - 4.0f;
        float trackY = y + ThemePalette.ROW_HEIGHT;
        float trackX = rowX + ThemePalette.CONTENT_PADDING;
        float trackRight = rowX + rowWidth - ThemePalette.CONTENT_PADDING;
        if (ThemePalette.contains(valueTextX - 3.0f, y + 1.0f, valueTextWidth + 6.0f, 10.0f,
                (float) mouseX, (float) mouseY)) {
            if (!editing) {
                editing = true;
                ScreenManager.focusedSetting = setting;
                editText = "";
                caret = 0;
                caretPulse.start(0.0, 1.0);
            }
            return true;
        }
        if (editing) commitTextEdit();
        if (ThemePalette.contains(trackX - 6.0f, trackY - 5.0f, trackRight - trackX + 12.0f, 12.0f,
                (float) mouseX, (float) mouseY)) {
            dragging = true;
            lastDragValue = setting.getValue();
            updateFromPointer(mouseX);
            return true;
        }
        return false;
    }

    /** Emits the original single release callback, then clears drag state. */
    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) setting.notifyChanged();
        dragging = false;
    }

    /** Preserves the original character-position editor and handled-key set. */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!editing || ScreenManager.focusedSetting != setting) return false;
        if (keyCode == InputKey.BACKSPACE.code()) {
            if (caret > 0) {
                editText = editText.substring(0, caret - 1) + editText.substring(caret);
                caret--;
            }
        } else if (keyCode == InputKey.DELETE.code()) {
            if (caret < editText.length()) {
                editText = editText.substring(0, caret) + editText.substring(caret + 1);
            }
        } else if (keyCode == InputKey.LEFT.code()) {
            caret = Math.max(0, caret - 1);
        } else if (keyCode == InputKey.RIGHT.code()) {
            caret = Math.min(editText.length(), caret + 1);
        } else if (keyCode == InputKey.HOME.code()) {
            caret = 0;
        } else if (keyCode == InputKey.END.code()) {
            caret = editText.length();
        } else if (keyCode == InputKey.ENTER.code() || keyCode == InputKey.KEYPAD_ENTER.code()) {
            commitTextEdit();
            return true;
        } else if (keyCode == InputKey.ESCAPE.code()) {
            cancelTextEdit();
            return true;
        } else {
            return false;
        }
        caretPulse.start(0.0, 1.0);
        return true;
    }

    /** Accepts ASCII digits or a single leading minus; no clipboard or selection behavior is added. */
    @Override
    public void charTyped(char character, int modifiers) {
        if (!editing || ScreenManager.focusedSetting != setting) return;
        boolean accepted = character >= '0' && character <= '9'
                || character == '-' && caret == 0 && editText.indexOf('-') < 0;
        if (!accepted) return;
        editText = editText.substring(0, caret) + character + editText.substring(caret);
        caret++;
        caretPulse.start(0.0, 1.0);
    }

    private void commitTextEdit() {
        try {
            setting.setValue(Integer.parseInt(editText.trim()));
        } catch (NumberFormatException ignored) {
            // Exact original catch also surrounds the setter and its change callback.
        }
        cancelTextEdit();
    }

    private void cancelTextEdit() {
        editing = false;
        if (ScreenManager.focusedSetting == setting) ScreenManager.focusedSetting = null;
    }

    private void updateFromPointer(double mouseX) {
        float rowX = x + ThemePalette.ROW_INDENT;
        float rowWidth = ThemePalette.PANEL_WIDTH - ThemePalette.ROW_INDENT - 4.0f;
        float trackX = rowX + ThemePalette.CONTENT_PADDING;
        float trackWidth = rowWidth - ThemePalette.CONTENT_PADDING * 2.0f;
        float fraction = Math.clamp((float) (mouseX - trackX) / trackWidth, 0.0f, 1.0f);
        int value = (int) ((float) setting.getMinimum()
                + (float) (setting.getMaximum() - setting.getMinimum()) * fraction);
        setting.setValueSilently(value);
        float range = (float) (setting.getMaximum() - setting.getMinimum());
        float threshold = range * 0.03f;
        if (lastDragValue == Integer.MIN_VALUE || (float) Math.abs(value - lastDragValue) >= threshold) {
            lastDragValue = value;
        }
    }

    private float sliderFraction() {
        return Math.clamp((float) (setting.getValue() - setting.getMinimum())
                / (float) (setting.getMaximum() - setting.getMinimum()), 0.0f, 1.0f);
    }

    /** Returns the retained setting object; javac supplies the ordinary covariant bridge. */
    @Override
    public IntSetting setting() { return setting; }
}
