package dev.echo.client.screens.widgets;

import dev.echo.client.input.InputKey;
import dev.echo.client.input.KeyModifier;
import dev.echo.render.FontAsset;
import dev.echo.client.render.UiRenderer;
import dev.echo.client.screens.ScreenManager;
import dev.echo.client.setting.RegistryListSetting;
import dev.echo.client.setting.list.RegistryEntryGroup;
import dev.echo.client.setting.list.SelectionState;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.utils.strings.Concat;
import dev.echo.client.ui.ThemePalette;
import dev.echo.client.ui.TooltipRenderer;
import dev.echo.client.ui.animation.Easing;
import dev.echo.client.ui.animation.Tween;
import dev.echo.client.utils.strings.TextBuffers;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Original registry group/entry selector with state-prioritized sorting and independent search paging. */
public class RegistryMultiSelectWidget<T> implements SettingWidget {
    private final RegistryListSetting<T> setting;
    private final List<RegistryEntryGroup<T>> groups;
    private final Function<T, String> entryLabel;
    private float x;
    private float y;
    private boolean expanded = false;
    private final Tween expansion;
    private String search = "";
    private int caret = 0;
    private final Tween caretPulse;
    private boolean showGroups = true;
    private static final int VISIBLE_ROWS = 8;
    private int scrollOffset = 0;
    // Original activePipeline is an interned <clinit> result, not a ConstantValue field.
    private static final String SELECTION_SUFFIX = new String(new char[]{' ', 's', 'e', 'l', '.'}).intern();

    public RegistryMultiSelectWidget(RegistryListSetting<T> setting) {
        expansion = new Tween(Easing.EASE_OUT_CUBIC, 200);
        caretPulse = new Tween(Easing.EASE_IN_OUT_QUART, 400);
        this.setting = setting;
        groups = setting.getGroups();
        entryLabel = setting.getEntryLabel();
        showGroups = !groups.isEmpty();
        expansion.start(0, 0);
    }

    private float popupHeight() {
        if (!expanded) return 0;
        float rows = VISIBLE_ROWS * (ThemePalette.ROW_HEIGHT - 2);
        if (groups.isEmpty()) return ThemePalette.ROW_HEIGHT + ThemePalette.SEARCH_GAP + rows + ThemePalette.CONTENT_PADDING;
        float tabHeight = ThemePalette.ROW_HEIGHT * 0.7f;
        return ThemePalette.ROW_HEIGHT + ThemePalette.SEARCH_GAP + tabHeight + ThemePalette.SEARCH_GAP + rows + ThemePalette.CONTENT_PADDING;
    }

    @Override
    public void render(UiRenderer renderer, float x, float y, int mouseX, int mouseY, float opacity) {
        this.x = x; this.y = y;
        expansion.setTarget(expanded ? 1.0f : 0.0f);
        float progress = (float) expansion.value();
        float rowX = x + ThemePalette.ROW_INDENT;
        float width = ThemePalette.PANEL_WIDTH - ThemePalette.ROW_INDENT - 4;
        float height = height();
        boolean hovered = ThemePalette.contains(rowX, y, width, ThemePalette.ROW_HEIGHT, mouseX, mouseY);
        if (hovered) {
            CharSequence description = setting.getDescription();
            if (description != null && description.length() > 0) TooltipRenderer.offer(setting, setting.getLabel(), description, mouseX, mouseY);
        }
        renderer.drawRoundedRect(rowX, y, width, height, ThemePalette.ROW_RADIUS,
                ThemePalette.multiplyAlpha((hovered ? ThemePalette.REGISTRY_SURFACE : ThemePalette.REGISTRY_BACKGROUND).getRGB(), opacity));
        renderer.drawRoundedRect(rowX, y + 2, 1.5f, height - 4, 0, ThemePalette.multiplyAlpha(ThemePalette.ROW_ACCENT.getRGB(), opacity));
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, TextBuffers.abbreviate(setting.getLabel(), 14),
                rowX + ThemePalette.CONTENT_PADDING, y + ThemePalette.ROW_HEIGHT / 2 - 4, 6,
                ThemePalette.multiplyAlpha(ThemePalette.PRIMARY_TEXT, opacity));
        int count = setting.getSelectedCount();
        CharSequence countText = count == 0 ? CatalogText.literal("None") : Concat.strings(String.valueOf(count), SELECTION_SUFFIX);
        float countWidth = FontAsset.INTER_SEMI_BOLD.measureText(countText, 6);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, countText, rowX + width - ThemePalette.CONTENT_PADDING - countWidth,
                y + ThemePalette.ROW_HEIGHT / 2 - 4, 6, ThemePalette.multiplyAlpha(ThemePalette.SECONDARY_TEXT, opacity));
        if (!(progress > ThemePalette.VISIBLE_ANIMATION_THRESHOLD)) return;
        int alpha = (int) (progress * 255);
        float rowY = y + ThemePalette.ROW_HEIGHT;
        renderer.drawRoundedRect(rowX + ThemePalette.CONTENT_PADDING, rowY, width - ThemePalette.CONTENT_PADDING * 2,
                ThemePalette.ROW_HEIGHT, ThemePalette.SLOT_RADIUS,
                ThemePalette.multiplyAlpha(ThemePalette.withAlpha(ThemePalette.SLOT_BACKGROUND, alpha), opacity));
        if (FontAsset.INTER_SEMI_BOLD != null) {
            CharSequence text = search.isEmpty() ? CatalogText.literal("Search...") : search;
            Color color = search.isEmpty() ? ThemePalette.SECONDARY_TEXT : ThemePalette.SELECTED_OPTION_TEXT;
            renderer.drawText(FontAsset.INTER_SEMI_BOLD, text, rowX + ThemePalette.CONTENT_PADDING + 4,
                    rowY + ThemePalette.ROW_HEIGHT / 2 - 3, 6, ThemePalette.multiplyAlpha(ThemePalette.withAlpha(color, alpha), opacity));
            if (ScreenManager.focusedSetting == setting) {
                if (caretPulse.isFinished()) caretPulse.start(caretPulse.targetValue(), 1 - caretPulse.targetValue());
                int position = Math.min(caret, search.length());
                float caretX = rowX + ThemePalette.CONTENT_PADDING + 4 + FontAsset.INTER_SEMI_BOLD.measureText(search.substring(0, position), 6);
                int caretAlpha = (int) (caretPulse.value() * alpha);
                renderer.drawRoundedRect(caretX, rowY + 2, 1, ThemePalette.ROW_HEIGHT - 4, 0,
                        ThemePalette.multiplyAlpha(ThemePalette.withAlpha(ThemePalette.SLIDER_FILL, caretAlpha), opacity));
            }
        }
        rowY += ThemePalette.ROW_HEIGHT + ThemePalette.SEARCH_GAP;
        if (!groups.isEmpty()) {
            float tabHeight = ThemePalette.ROW_HEIGHT * 0.7f;
            float tabWidth = (width - ThemePalette.CONTENT_PADDING * 2 - ThemePalette.SEARCH_GAP) / 2;
            int groupColor = ThemePalette.multiplyAlpha(ThemePalette.withAlpha(showGroups ? ThemePalette.ROW_ACCENT : ThemePalette.REGISTRY_SURFACE, alpha), opacity);
            int entryColor = ThemePalette.multiplyAlpha(ThemePalette.withAlpha(!showGroups ? ThemePalette.ROW_ACCENT : ThemePalette.REGISTRY_SURFACE, alpha), opacity);
            renderer.drawRoundedRect(rowX + ThemePalette.CONTENT_PADDING, rowY, tabWidth, tabHeight, ThemePalette.SLOT_RADIUS, groupColor);
            renderer.drawRoundedRect(rowX + ThemePalette.CONTENT_PADDING + tabWidth + ThemePalette.SEARCH_GAP, rowY,
                    tabWidth, tabHeight, ThemePalette.SLOT_RADIUS, entryColor);
            if (FontAsset.INTER_SEMI_BOLD != null) {
                CharSequence groupsTab = CatalogText.literal("Groups");
                CharSequence entriesTab = CatalogText.literal("Entries");
                renderer.drawText(FontAsset.INTER_SEMI_BOLD, groupsTab,
                        rowX + ThemePalette.CONTENT_PADDING + tabWidth / 2 - FontAsset.INTER_SEMI_BOLD.measureText(groupsTab, 5) / 2,
                        rowY + tabHeight / 2 - 2, 5,
                        ThemePalette.multiplyAlpha(ThemePalette.withAlpha(showGroups ? ThemePalette.TAB_ACTIVE_TEXT : ThemePalette.SELECTED_OPTION_TEXT, alpha), opacity));
                renderer.drawText(FontAsset.INTER_SEMI_BOLD, entriesTab,
                        rowX + ThemePalette.CONTENT_PADDING + tabWidth + ThemePalette.SEARCH_GAP + tabWidth / 2
                                - FontAsset.INTER_SEMI_BOLD.measureText(entriesTab, 5) / 2,
                        rowY + tabHeight / 2 - 2, 5,
                        ThemePalette.multiplyAlpha(ThemePalette.withAlpha(!showGroups ? ThemePalette.TAB_ACTIVE_TEXT : ThemePalette.SELECTED_OPTION_TEXT, alpha), opacity));
            }
            rowY += tabHeight + ThemePalette.SEARCH_GAP;
        }
        if (showGroups) {
            List<RegistryEntryGroup<T>> visible = filteredGroups(groups);
            int first = visible.size() > VISIBLE_ROWS ? scrollOffset : 0;
            int end = visible.size() > VISIBLE_ROWS ? Math.min(scrollOffset + VISIBLE_ROWS, visible.size()) : visible.size();
            for (int index = first; index < end; index++) {
                RegistryEntryGroup<T> group = visible.get(index);
                SelectionState state = setting.getGroupState(group);
                if (FontAsset.INTER_SEMI_BOLD != null) {
                    CharSequence marker = state == SelectionState.ALL ? CatalogText.literal("[x]")
                            : state == SelectionState.PARTIAL ? CatalogText.literal("[~]") : CatalogText.literal("[ ]");
                    Color color = state == SelectionState.ALL ? ThemePalette.ROW_ACCENT : ThemePalette.SECONDARY_TEXT;
                    renderer.drawText(FontAsset.INTER_SEMI_BOLD, marker, rowX + ThemePalette.CONTENT_PADDING + 4,
                            rowY + (ThemePalette.ROW_HEIGHT - 2) / 2 - 3, 6, ThemePalette.multiplyAlpha(ThemePalette.withAlpha(color, alpha), opacity));
                    renderer.drawText(FontAsset.INTER_SEMI_BOLD, TextBuffers.abbreviate(group.label(), 18),
                            rowX + ThemePalette.CONTENT_PADDING + 20, rowY + (ThemePalette.ROW_HEIGHT - 2) / 2 - 3, 6,
                            ThemePalette.multiplyAlpha(ThemePalette.withAlpha(ThemePalette.SELECTED_OPTION_TEXT, alpha), opacity));
                }
                rowY += ThemePalette.ROW_HEIGHT - 2;
            }
        } else {
            List<T> visible = filteredEntries();
            int first = visible.size() > VISIBLE_ROWS ? scrollOffset : 0;
            int end = visible.size() > VISIBLE_ROWS ? Math.min(scrollOffset + VISIBLE_ROWS, visible.size()) : visible.size();
            for (int index = first; index < end; index++) {
                T entry = visible.get(index);
                boolean selected = setting.isEntrySelected(entry);
                if (FontAsset.INTER_SEMI_BOLD != null) {
                    CharSequence marker = selected ? CatalogText.literal("[x]") : CatalogText.literal("[ ]");
                    Color color = selected ? ThemePalette.ROW_ACCENT : ThemePalette.SECONDARY_TEXT;
                    renderer.drawText(FontAsset.INTER_SEMI_BOLD, marker, rowX + ThemePalette.CONTENT_PADDING + 4,
                            rowY + (ThemePalette.ROW_HEIGHT - 2) / 2 - 3, 6, ThemePalette.multiplyAlpha(ThemePalette.withAlpha(color, alpha), opacity));
                    renderer.drawText(FontAsset.INTER_SEMI_BOLD, TextBuffers.abbreviateString(entryLabel.apply(entry), 18),
                            rowX + ThemePalette.CONTENT_PADDING + 20, rowY + (ThemePalette.ROW_HEIGHT - 2) / 2 - 3, 6,
                            ThemePalette.multiplyAlpha(ThemePalette.withAlpha(ThemePalette.SELECTED_OPTION_TEXT, alpha), opacity));
                }
                rowY += ThemePalette.ROW_HEIGHT - 2;
            }
        }
    }

    private List<RegistryEntryGroup<T>> filteredGroups(List<RegistryEntryGroup<T>> source) {
        List<RegistryEntryGroup<T>> result = new ArrayList<>(source);
        result.sort(Comparator.<RegistryEntryGroup<T>, Integer>comparing(group -> {
            SelectionState state = setting.getGroupState(group);
            return state == SelectionState.ALL ? 0 : state == SelectionState.PARTIAL ? 1 : 2;
        }).thenComparing((first, second) -> Concat.compareLowercase(first.label(), second.label())));
        if (search.isEmpty()) return result;
        String query = search.toLowerCase();
        return result.stream().filter(group -> Concat.containsLowercase(group.label(), query)).collect(Collectors.toList());
    }

    private List<T> filteredEntries() {
        List<T> result = new ArrayList<>();
        Predicate<T> filter = setting.getEntryFilter();
        for (T entry : setting.getRegistry()) if (filter.test(entry)) result.add(entry);
        result.sort(Comparator.<T, Boolean>comparing(entry -> !setting.isEntrySelected(entry))
                .thenComparing(entry -> entryLabel.apply(entry).toLowerCase()));
        if (search.isEmpty()) return result;
        String query = search.toLowerCase();
        return result.stream().filter(entry -> entryLabel.apply(entry).toLowerCase().contains(query)).collect(Collectors.toList());
    }

    @Override public float height() {
        float progress = (float) expansion.value();
        float height = ThemePalette.ROW_HEIGHT;
        if (progress > ThemePalette.VISIBLE_ANIMATION_THRESHOLD) height += popupHeight() * progress;
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
        float searchY = y + ThemePalette.ROW_HEIGHT;
        float tabHeight = ThemePalette.ROW_HEIGHT * 0.7f;
        float listY = groups.isEmpty() ? searchY + ThemePalette.ROW_HEIGHT + ThemePalette.SEARCH_GAP
                : searchY + ThemePalette.ROW_HEIGHT + ThemePalette.SEARCH_GAP + tabHeight + ThemePalette.SEARCH_GAP;
        if (ThemePalette.contains(rowX + ThemePalette.CONTENT_PADDING, searchY, width - ThemePalette.CONTENT_PADDING * 2,
                ThemePalette.ROW_HEIGHT, (float) mouseX, (float) mouseY)) {
            ScreenManager.focusedSetting = setting; caretPulse.start(0, 1);
            float relativeX = (float) mouseX - rowX - ThemePalette.CONTENT_PADDING - 4;
            caret = 0;
            for (int index = 0; index <= search.length(); index++) {
                if (relativeX <= FontAsset.INTER_SEMI_BOLD.measureText(search.substring(0, index), 6)) break;
                caret = index;
            }
            caret = Math.min(caret, search.length());
            return true;
        }
        if (!groups.isEmpty()) {
            float tabY = searchY + ThemePalette.ROW_HEIGHT + ThemePalette.SEARCH_GAP;
            float tabWidth = (width - ThemePalette.CONTENT_PADDING * 2 - ThemePalette.SEARCH_GAP) / 2;
            if (ThemePalette.contains(rowX + ThemePalette.CONTENT_PADDING, tabY, tabWidth, tabHeight, (float) mouseX, (float) mouseY)) {
                showGroups = true; scrollOffset = 0; return true;
            }
            if (ThemePalette.contains(rowX + ThemePalette.CONTENT_PADDING + tabWidth + ThemePalette.SEARCH_GAP,
                    tabY, tabWidth, tabHeight, (float) mouseX, (float) mouseY)) {
                showGroups = false; scrollOffset = 0; return true;
            }
        }
        if (mouseY >= listY) {
            if (showGroups) {
                List<RegistryEntryGroup<T>> visible = filteredGroups(groups);
                int first = visible.size() > VISIBLE_ROWS ? scrollOffset : 0;
                int end = visible.size() > VISIBLE_ROWS ? Math.min(scrollOffset + VISIBLE_ROWS, visible.size()) : visible.size();
                float rowY = listY;
                for (int index = first; index < end; index++) {
                    if (ThemePalette.contains(rowX + ThemePalette.CONTENT_PADDING, rowY,
                            width - ThemePalette.CONTENT_PADDING * 2, ThemePalette.ROW_HEIGHT - 2, (float) mouseX, (float) mouseY)) {
                        RegistryEntryGroup<T> group = visible.get(index);
                        SelectionState state = setting.getGroupState(group);
                        setting.setGroupSelected(group, state != SelectionState.ALL);
                        return true;
                    }
                    rowY += ThemePalette.ROW_HEIGHT - 2;
                }
            } else {
                List<T> visible = filteredEntries();
                int first = visible.size() > VISIBLE_ROWS ? scrollOffset : 0;
                int end = visible.size() > VISIBLE_ROWS ? Math.min(scrollOffset + VISIBLE_ROWS, visible.size()) : visible.size();
                float rowY = listY;
                for (int index = first; index < end; index++) {
                    if (ThemePalette.contains(rowX + ThemePalette.CONTENT_PADDING, rowY,
                            width - ThemePalette.CONTENT_PADDING * 2, ThemePalette.ROW_HEIGHT - 2, (float) mouseX, (float) mouseY)) {
                        T entry = visible.get(index);
                        setting.setEntrySelected(entry, !setting.isEntrySelected(entry));
                        return true;
                    }
                    rowY += ThemePalette.ROW_HEIGHT - 2;
                }
            }
        }
        ScreenManager.focusedSetting = null;
        return false;
    }

    @Override public void mouseReleased(double mouseX, double mouseY, int button) { /* Original return-only body. */ }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ScreenManager.focusedSetting != setting) return false;
        if (keyCode == InputKey.BACKSPACE.code()) {
            if (KeyModifier.CONTROL.isSet(modifiers)) {
                int previous = previousWordStart(search, caret);
                search = search.substring(0, previous) + search.substring(caret); caret = previous; scrollOffset = 0;
            } else if (caret > 0) { search = search.substring(0, caret - 1) + search.substring(caret); caret--; scrollOffset = 0; }
        } else if (keyCode == InputKey.DELETE.code()) {
            if (caret < search.length()) { search = search.substring(0, caret) + search.substring(caret + 1); scrollOffset = 0; }
        } else if (keyCode == InputKey.LEFT.code()) caret = Math.max(0, caret - 1);
        else if (keyCode == InputKey.RIGHT.code()) caret = Math.min(search.length(), caret + 1);
        else if (keyCode == InputKey.HOME.code()) caret = 0;
        else if (keyCode == InputKey.END.code()) caret = search.length();
        else if (keyCode == InputKey.ESCAPE.code()) { ScreenManager.focusedSetting = null; return true; }
        else return false;
        caretPulse.start(0, 1); return true;
    }

    @Override public void charTyped(char character, int modifiers) {
        if (ScreenManager.focusedSetting != setting || character < 32 || character == 127) return;
        int position = Math.min(caret, search.length());
        search = search.substring(0, position) + character + search.substring(position);
        caret = position + 1; scrollOffset = 0; caretPulse.start(0, 1);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        float progress = (float) expansion.value();
        if (!expanded || !(progress > ThemePalette.INPUT_ANIMATION_THRESHOLD)) return false;
        float rowX = x + ThemePalette.ROW_INDENT;
        float width = ThemePalette.PANEL_WIDTH - ThemePalette.ROW_INDENT - 4;
        // Original scroll band includes this extra gap and tab allowance even when groups are absent.
        float top = y + ThemePalette.ROW_HEIGHT + ThemePalette.SEARCH_GAP + ThemePalette.ROW_HEIGHT
                + ThemePalette.SEARCH_GAP + ThemePalette.ROW_HEIGHT * 0.7f + ThemePalette.SEARCH_GAP;
        if (!ThemePalette.contains(rowX, top, width, VISIBLE_ROWS * (ThemePalette.ROW_HEIGHT - 2), (float) mouseX, (float) mouseY)) return false;
        int count = showGroups ? filteredGroups(groups).size() : filteredEntries().size();
        if (count <= VISIBLE_ROWS) return false;
        scrollOffset = amount > 0 ? Math.max(0, scrollOffset - 1) : Math.min(count - VISIBLE_ROWS, scrollOffset + 1);
        return true;
    }

    private static int previousWordStart(String text, int caret) {
        int index = caret;
        while (index > 0 && text.charAt(index - 1) == ' ') index--;
        while (index > 0 && text.charAt(index - 1) != ' ') index--;
        return index;
    }

    @Override public RegistryListSetting<T> setting() { return setting; }
}
