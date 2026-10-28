package dev.echo.client.screens.widgets;

import dev.echo.client.input.InputKey;
import dev.echo.client.input.KeyModifier;
import dev.echo.render.FontAsset;
import dev.echo.client.render.UiRenderer;
import dev.echo.client.screens.ScreenManager;
import dev.echo.client.setting.MultiChoiceSetting;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.utils.strings.Concat;
import dev.echo.client.ui.ThemePalette;
import dev.echo.client.ui.TooltipRenderer;
import dev.echo.client.ui.animation.Easing;
import dev.echo.client.ui.animation.Tween;
import dev.echo.client.utils.strings.TextBuffers;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Original ordered boolean-option group, optional search field and eight-row paging. */
public class BooleanGroupWidget implements SettingWidget {
    private final MultiChoiceSetting setting;
    private float x;
    private float y;
    private boolean expanded = false;
    private final Tween expansion;
    private static final int VISIBLE_ROWS = 8;
    private int scrollOffset = 0;
    private String search = "";
    private int caret = 0;
    private final Tween caretPulse;

    public BooleanGroupWidget(MultiChoiceSetting setting) {
        expansion = new Tween(Easing.EASE_OUT_CUBIC, 200.0);
        caretPulse = new Tween(Easing.EASE_IN_OUT_QUART, 400.0);
        this.setting = setting;
        expansion.start(0.0, 0.0);
    }

    @Override
    public void render(UiRenderer renderer, float x, float y, int mouseX, int mouseY, float opacity) {
        this.x = x; this.y = y;
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
        renderer.drawRoundedRect(rowX, y + 2, 1.5f, height - 4, 0,
                ThemePalette.multiplyAlpha(ThemePalette.ROW_ACCENT.getRGB(), opacity));
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, TextBuffers.abbreviate(setting.getLabel(), 14),
                rowX + ThemePalette.CONTENT_PADDING, y + ThemePalette.ROW_HEIGHT / 2 - 4, 6,
                ThemePalette.multiplyAlpha(ThemePalette.PRIMARY_TEXT, opacity));
        int selected = 0;
        for (Boolean value : setting.getOptions().values()) if (Boolean.TRUE.equals(value)) selected++;
        CharSequence count = selected == 0 ? CatalogText.literal("None")
                : Concat.sequences(Concat.integer(selected), CatalogText.literal(" sel."));
        float countWidth = FontAsset.INTER_SEMI_BOLD.measureText(count, 6);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, count,
                rowX + rowWidth - ThemePalette.CONTENT_PADDING - countWidth,
                y + ThemePalette.ROW_HEIGHT / 2 - 4, 6, ThemePalette.multiplyAlpha(ThemePalette.SECONDARY_TEXT, opacity));
        if (!(progress > ThemePalette.VISIBLE_ANIMATION_THRESHOLD)) return;
        int alpha = (int) (progress * 255.0f);
        float optionY = y + ThemePalette.ROW_HEIGHT;
        if (setting.isSelectionMode()) {
            float searchY = optionY;
            renderer.drawRoundedRect(rowX + ThemePalette.CONTENT_PADDING, searchY,
                    rowWidth - ThemePalette.CONTENT_PADDING * 2, ThemePalette.ROW_HEIGHT, ThemePalette.SLOT_RADIUS,
                    ThemePalette.multiplyAlpha(ThemePalette.withAlpha(ThemePalette.SLOT_BACKGROUND, alpha), opacity));
            if (FontAsset.INTER_SEMI_BOLD != null) {
                CharSequence text = search.isEmpty() ? CatalogText.literal("Search...") : search;
                Color color = search.isEmpty() ? ThemePalette.SECONDARY_TEXT : ThemePalette.SELECTED_OPTION_TEXT;
                renderer.drawText(FontAsset.INTER_SEMI_BOLD, text, rowX + ThemePalette.CONTENT_PADDING + 4,
                        searchY + ThemePalette.ROW_HEIGHT / 2 - 3, 6,
                        ThemePalette.multiplyAlpha(ThemePalette.withAlpha(color, alpha), opacity));
                if (ScreenManager.focusedSetting == setting) {
                    if (caretPulse.isFinished()) caretPulse.start(caretPulse.targetValue(), 1 - caretPulse.targetValue());
                    int position = Math.min(caret, search.length());
                    float caretX = rowX + ThemePalette.CONTENT_PADDING + 4
                            + FontAsset.INTER_SEMI_BOLD.measureText(search.substring(0, position), 6);
                    int caretAlpha = (int) (caretPulse.value() * alpha);
                    renderer.drawRoundedRect(caretX, searchY + 2, 1, ThemePalette.ROW_HEIGHT - 4, 0,
                            ThemePalette.multiplyAlpha(ThemePalette.withAlpha(ThemePalette.SLIDER_FILL, caretAlpha), opacity));
                }
            }
            optionY += ThemePalette.ROW_HEIGHT + ThemePalette.SEARCH_GAP;
        }
        List<Map.Entry<CharSequence, Boolean>> entries = filteredEntries();
        int size = entries.size();
        int first = size > VISIBLE_ROWS ? scrollOffset : 0;
        int end = size > VISIBLE_ROWS ? Math.min(scrollOffset + VISIBLE_ROWS, size) : size;
        for (int index = first; index < end; index++) {
            Map.Entry<CharSequence, Boolean> entry = entries.get(index);
            Color color = Boolean.TRUE.equals(entry.getValue()) ? ThemePalette.SELECTED_OPTION_TEXT : ThemePalette.SECONDARY_TEXT;
            renderer.drawText(FontAsset.INTER_SEMI_BOLD, entry.getKey(), rowX + ThemePalette.CONTENT_PADDING + 4,
                    optionY + (ThemePalette.ROW_HEIGHT - 2) / 2 - 4, 6,
                    ThemePalette.multiplyAlpha(ThemePalette.withAlpha(color, alpha), opacity));
            optionY += ThemePalette.ROW_HEIGHT - 2;
        }
    }

    /** Copies entry references in insertion order; search uses the original default-locale lowering. */
    private List<Map.Entry<CharSequence, Boolean>> filteredEntries() {
        List<Map.Entry<CharSequence, Boolean>> entries = new ArrayList<>(setting.getOptions().entrySet());
        if (!setting.isSelectionMode() || search.isEmpty()) return entries;
        String query = search.toLowerCase();
        List<Map.Entry<CharSequence, Boolean>> filtered = new ArrayList<>();
        for (Map.Entry<CharSequence, Boolean> entry : entries) {
            if (Concat.containsLowercase(entry.getKey(), query)) filtered.add(entry);
        }
        return filtered;
    }

    private float searchHeight() {
        return setting.isSelectionMode() ? ThemePalette.ROW_HEIGHT + ThemePalette.SEARCH_GAP : 0;
    }

    /** The original height intentionally counts all options, even after filtering. */
    @Override
    public float height() {
        float progress = (float) expansion.value();
        float height = ThemePalette.ROW_HEIGHT;
        if (progress > ThemePalette.VISIBLE_ANIMATION_THRESHOLD) {
            float rows = Math.min(setting.getOptions().size(), VISIBLE_ROWS) * (ThemePalette.ROW_HEIGHT - 2);
            height += (searchHeight() + rows) * progress;
        }
        return height;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!InputKey.MOUSE_LEFT.matches(button)) return false;
        float rowX = x + ThemePalette.ROW_INDENT;
        float width = ThemePalette.PANEL_WIDTH - ThemePalette.ROW_INDENT - 4;
        if (ThemePalette.contains(rowX, y, width, ThemePalette.ROW_HEIGHT, (float) mouseX, (float) mouseY)) {
            expanded = !expanded;
            if (expanded) { scrollOffset = 0; ScreenManager.focusedSetting = null; }
            return true;
        }
        float progress = (float) expansion.value();
        if (!expanded || !(progress > ThemePalette.INPUT_ANIMATION_THRESHOLD)) return false;
        if (setting.isSelectionMode() && ThemePalette.contains(rowX + ThemePalette.CONTENT_PADDING,
                y + ThemePalette.ROW_HEIGHT, width - ThemePalette.CONTENT_PADDING * 2,
                ThemePalette.ROW_HEIGHT, (float) mouseX, (float) mouseY)) {
            ScreenManager.focusedSetting = setting;
            caretPulse.start(0, 1);
            float relativeX = (float) mouseX - rowX - ThemePalette.CONTENT_PADDING - 4;
            caret = 0;
            for (int index = 0; index <= search.length(); index++) {
                if (relativeX <= FontAsset.INTER_SEMI_BOLD.measureText(search.substring(0, index), 6)) break;
                caret = index;
            }
            caret = Math.min(caret, search.length());
            return true;
        }
        List<Map.Entry<CharSequence, Boolean>> entries = filteredEntries();
        int size = entries.size();
        float visibleHeight = Math.min(size * (ThemePalette.ROW_HEIGHT - 2), VISIBLE_ROWS * (ThemePalette.ROW_HEIGHT - 2));
        float top = y + ThemePalette.ROW_HEIGHT + searchHeight();
        int first = size > VISIBLE_ROWS ? scrollOffset : 0;
        int end = size > VISIBLE_ROWS ? Math.min(scrollOffset + VISIBLE_ROWS, size) : size;
        float optionY = top;
        if (mouseY >= top && mouseY <= top + visibleHeight) {
            for (int index = first; index < end; index++) {
                Map.Entry<CharSequence, Boolean> entry = entries.get(index);
                if (ThemePalette.contains(rowX + ThemePalette.CONTENT_PADDING, optionY,
                        width - ThemePalette.CONTENT_PADDING * 2, ThemePalette.ROW_HEIGHT - 2,
                        (float) mouseX, (float) mouseY)) {
                    setting.toggle(entry.getKey());
                    return true;
                }
                optionY += ThemePalette.ROW_HEIGHT - 2;
            }
        }
        if (setting.isSelectionMode()) ScreenManager.focusedSetting = null;
        return false;
    }

    @Override public void mouseReleased(double mouseX, double mouseY, int button) {
        // Original body has no release action.
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!setting.isSelectionMode() || ScreenManager.focusedSetting != setting) return false;
        if (keyCode == InputKey.BACKSPACE.code()) {
            if (KeyModifier.CONTROL.isSet(modifiers)) {
                int previous = previousWordStart(search, caret);
                search = search.substring(0, previous) + search.substring(caret);
                caret = previous; scrollOffset = 0;
            } else if (caret > 0) {
                search = search.substring(0, caret - 1) + search.substring(caret);
                caret--; scrollOffset = 0;
            }
        } else if (keyCode == InputKey.DELETE.code()) {
            if (caret < search.length()) { search = search.substring(0, caret) + search.substring(caret + 1); scrollOffset = 0; }
        } else if (keyCode == InputKey.LEFT.code()) caret = Math.max(0, caret - 1);
        else if (keyCode == InputKey.RIGHT.code()) caret = Math.min(search.length(), caret + 1);
        else if (keyCode == InputKey.HOME.code()) caret = 0;
        else if (keyCode == InputKey.END.code()) caret = search.length();
        else if (keyCode == InputKey.ESCAPE.code()) { ScreenManager.focusedSetting = null; return true; }
        else return false;
        caretPulse.start(0, 1);
        return true;
    }

    @Override
    public void charTyped(char character, int modifiers) {
        if (!setting.isSelectionMode() || ScreenManager.focusedSetting != setting || character < 32 || character == 127) return;
        int position = Math.min(caret, search.length());
        search = search.substring(0, position) + character + search.substring(position);
        caret = position + 1; scrollOffset = 0; caretPulse.start(0, 1);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        float progress = (float) expansion.value();
        if (!expanded || !(progress > ThemePalette.INPUT_ANIMATION_THRESHOLD)) return false;
        float rowX = x + ThemePalette.ROW_INDENT;
        float width = ThemePalette.PANEL_WIDTH - ThemePalette.ROW_INDENT - 4;
        int size = filteredEntries().size();
        if (size <= VISIBLE_ROWS || !ThemePalette.contains(rowX, y, width, height(), (float) mouseX, (float) mouseY)) return false;
        float top = y + ThemePalette.ROW_HEIGHT + searchHeight();
        float visibleHeight = Math.min(size * (ThemePalette.ROW_HEIGHT - 2), VISIBLE_ROWS * (ThemePalette.ROW_HEIGHT - 2));
        if (mouseY < top || mouseY > top + visibleHeight) return false;
        if (amount > 0) scrollOffset = Math.max(0, scrollOffset - 1);
        else if (amount < 0) scrollOffset = Math.min(size - VISIBLE_ROWS, scrollOffset + 1);
        return true;
    }

    private static int previousWordStart(String text, int caret) {
        int index = caret;
        while (index > 0 && text.charAt(index - 1) == ' ') index--;
        while (index > 0 && text.charAt(index - 1) != ' ') index--;
        return index;
    }

    @Override public MultiChoiceSetting setting() { return setting; }
}
