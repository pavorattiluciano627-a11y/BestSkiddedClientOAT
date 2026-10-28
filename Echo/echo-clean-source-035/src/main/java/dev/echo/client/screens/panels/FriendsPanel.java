package dev.echo.client.screens.panels;

import dev.echo.client.Echo;
import dev.echo.client.features.internals.ClickGUIFeature;
import dev.echo.client.input.InputKey;
import dev.echo.client.render.UiRenderer;
import dev.echo.client.screens.CategoryPanelScreen;
import dev.echo.client.ui.ThemePalette;
import dev.echo.client.utils.interaction.EntityInteractions;
import dev.echo.render.FontAsset;
import dev.echo.render.TextureResources;
import dev.echo.render.texture.TextureView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import dev.echo.client.input.CharacterInputModifiers;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.lwjgl.glfw.GLFW;

/** Original TaskController panel; binding/cache/guard conditions are in the delivery receipt. */
public class FriendsPanel {
    private static final float WIDTH = 238.0F;
    private static final float HEIGHT = 250.0F;
    private static final float PADDING = 10.0F;
    private static final float RADIUS = 14.0F;
    private static final float HEADER_HEIGHT = 24.0F;
    private static final float HEADER_BUTTON_SIZE = 18.0F;
    private static final float SEARCH_HEIGHT = 16.0F;
    private static final float ROW_HEIGHT = 18.0F;
    private static final float ROW_RADIUS = 5.0F;
    private static final float ROW_GAP = 4.0F;
    private static final float HEAD_SIZE = 12.0F;
    private static final float ROW_BUTTON_SIZE = 14.0F;
    private static final float SCROLLBAR_WIDTH = 4.0F;
    private static final float SCROLLBAR_RADIUS = 2.0F;
    private static final float POPUP_WIDTH = 202.0F;
    private static final float POPUP_HEIGHT = 74.0F;
    private static final float POPUP_RADIUS = 12.0F;
    private static final float POPUP_DRAG_HEIGHT = 32.0F;
    private static final float POPUP_HEAD_SIZE = 28.0F;
    private static final float POPUP_FIELD_HEIGHT = 16.0F;
    private static final float POPUP_BUTTON_SIZE = 18.0F;
    private static final String TITLE = new String("Friends");
    private static final String SEARCH_HINT = new String("Search");
    private static final String ADD_TITLE = new String("Add friend");
    private static final String NAME_HINT = new String("Player name");
    private static final String EMPTY_TITLE = new String("No players found");
    private static final String EMPTY_HINT = new String("Try search or add one manually.");
    private static final String ELLIPSIS = new String("...");

    private final CategoryPanelScreen screen;
    private final List<FriendActionButton> actionButtons;
    private float x;
    private float y;
    private float scroll;
    private float maxScroll;
    private boolean positionInitialized;
    private boolean dragging;
    private float dragOffsetX;
    private float dragOffsetY;
    private boolean searchFocused;
    private String searchQuery;
    private int searchCursor;
    private boolean addFriendPopupOpen;
    private boolean popupPositionInitialized;
    private float popupX;
    private float popupY;
    private boolean popupDragging;
    private float popupDragOffsetX;
    private float popupDragOffsetY;
    private boolean addFriendFieldFocused;
    private String addFriendName;
    private int addFriendCursor;

    public FriendsPanel(CategoryPanelScreen screen) {
        actionButtons = new ArrayList<>();
        searchQuery = "";
        searchCursor = 0;
        addFriendName = "";
        addFriendCursor = 0;
        this.screen = screen;
    }

    public void render(UiRenderer renderer, float viewportWidth, int mouseX, int mouseY,
                       float opacity, TextureView backdrop, float baselineY) {
        initializePosition(viewportWidth, baselineY);
        initializePopupPosition();
        int contentMouseX = mouseX;
        int contentMouseY = mouseY;
        if (addFriendPopupOpen && ThemePalette.contains(popupX, popupY, POPUP_WIDTH, POPUP_HEIGHT, mouseX, mouseY)) {
            contentMouseX = Integer.MIN_VALUE;
            contentMouseY = Integer.MIN_VALUE;
        }
        drawBackground(renderer, backdrop, opacity);
        drawHeader(renderer, contentMouseX, contentMouseY, opacity);
        drawRows(renderer, contentMouseX, contentMouseY, opacity);
        drawScrollbar(renderer, opacity);
        if (addFriendPopupOpen) drawAddFriendPopup(renderer, backdrop, mouseX, mouseY, opacity);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double vertical) {
        if (addFriendPopupOpen && ThemePalette.contains(popupX, popupY, POPUP_WIDTH, POPUP_HEIGHT, (float) mouseX, (float) mouseY)) return true;
        if (!ThemePalette.contains(x, y, WIDTH, HEIGHT, (float) mouseX, (float) mouseY)) return false;
        if (vertical == 0.0 || maxScroll <= 0.0F) return true;
        scroll = Math.clamp(scroll - (float) vertical * scrollSpeed(), 0.0F, maxScroll);
        return true;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!InputKey.MOUSE_LEFT.matches(button)) return false;
        float pointerX = (float) mouseX;
        float pointerY = (float) mouseY;
        if (addFriendPopupOpen && clickPopup(pointerX, pointerY)) return true;
        if (clickAddButton(pointerX, pointerY)) return true;
        if (clickSearch(pointerX, pointerY)) return true;
        for (FriendActionButton action : actionButtons) {
            if (ThemePalette.contains(action.x, action.y, action.width, action.height, pointerX, pointerY)) {
                toggleFriend(action.playerName);
                return true;
            }
        }
        if (ThemePalette.contains(x, y, WIDTH, HEADER_HEIGHT, pointerX, pointerY)) {
            dragging = true;
            dragOffsetX = x - pointerX;
            dragOffsetY = y - pointerY;
            searchFocused = false;
            addFriendFieldFocused = false;
            return true;
        }
        if (ThemePalette.contains(x, y, WIDTH, HEIGHT, pointerX, pointerY)) {
            searchFocused = false;
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, int viewportWidth, int viewportHeight) {
        if (!InputKey.MOUSE_LEFT.matches(button)) return false;
        if (popupDragging) {
            popupX = PanelGeometry.clampPosition((float) mouseX + popupDragOffsetX, POPUP_WIDTH, viewportWidth);
            popupY = PanelGeometry.clampPosition((float) mouseY + popupDragOffsetY, POPUP_HEIGHT, viewportHeight);
            return true;
        }
        if (dragging) {
            x = PanelGeometry.clampPosition((float) mouseX + dragOffsetX, WIDTH, viewportWidth);
            y = PanelGeometry.clampPosition((float) mouseY + dragOffsetY, HEIGHT, viewportHeight);
            return true;
        }
        return false;
    }

    public boolean mouseReleased(int button) {
        if (!InputKey.MOUSE_LEFT.matches(button)) return false;
        boolean captured = dragging || popupDragging;
        dragging = false;
        popupDragging = false;
        return captured;
    }

    public boolean charTyped(CharacterEvent input) {
        char character = (char) input.codepoint();
        if (Character.isISOControl(character)) return false;
        if (addFriendPopupOpen && addFriendFieldFocused) {
            addFriendName = PanelTextEditing.insertCharacter(addFriendName, addFriendCursor, character);
            addFriendCursor = Math.min(addFriendName.length(), addFriendCursor + 1);
            return true;
        }
        if (!searchFocused) return false;
        searchQuery = PanelTextEditing.insertCharacter(searchQuery, searchCursor, character);
        searchCursor = Math.min(searchQuery.length(), searchCursor + 1);
        return true;
    }

    public boolean keyPressed(KeyEvent input) {
        if (addFriendPopupOpen) {
            if (input.key() == InputKey.ESCAPE.code()) {
                addFriendPopupOpen = false;
                addFriendFieldFocused = false;
                return true;
            }
            if (addFriendFieldFocused && handleTextKey(input, true)) return true;
        }
        if (!searchFocused) return false;
        if (input.key() == InputKey.ESCAPE.code()) {
            searchFocused = false;
            return true;
        }
        return handleTextKey(input, false);
    }

    private void drawBackground(UiRenderer renderer, TextureView backdrop, float opacity) {
        if (backdrop != null) renderer.drawBackdrop(backdrop, x, y, WIDTH, HEIGHT, RADIUS, opacity);
        renderer.drawRoundedRect(x, y, WIDTH, HEIGHT, RADIUS, ThemePalette.multiplyAlpha(ThemePalette.PANEL_FOREGROUND.getRGB(), opacity));
    }

    private void drawHeader(UiRenderer renderer, int mouseX, int mouseY, float opacity) {
        if (FontAsset.INTER_SEMI_BOLD != null) renderer.drawText(FontAsset.INTER_SEMI_BOLD, TITLE, x + PADDING, y + 7.0F, 10.0F, ThemePalette.multiplyAlpha(ThemePalette.SELECTED_OPTION_TEXT, opacity));
        float buttonX = addButtonX();
        float buttonY = addButtonY();
        boolean hovered = ThemePalette.contains(buttonX, buttonY, HEADER_BUTTON_SIZE, HEADER_BUTTON_SIZE, mouseX, mouseY);
        renderer.drawRoundedRect(buttonX, buttonY, HEADER_BUTTON_SIZE, HEADER_BUTTON_SIZE, 5.0F,
                ThemePalette.multiplyAlpha((hovered ? ThemePalette.ROW_HOVER : ThemePalette.ROW_BACKGROUND).getRGB(), opacity));
        drawIcon(renderer, "\ue13d", buttonX, buttonY, HEADER_BUTTON_SIZE, 8.0F, opacity);
        float fieldX = searchX();
        float fieldY = searchY();
        float fieldWidth = searchWidth();
        boolean fieldHovered = ThemePalette.contains(fieldX, fieldY, fieldWidth, SEARCH_HEIGHT, mouseX, mouseY);
        int fill = ThemePalette.multiplyAlpha(ThemePalette.withAlpha(ThemePalette.SLOT_BACKGROUND, searchFocused ? 220 : fieldHovered ? 190 : 160), opacity);
        renderer.drawRoundedRect(fieldX, fieldY, fieldWidth, SEARCH_HEIGHT, 6.0F, fill);
        if (FontAsset.INTER_SEMI_BOLD != null) {
            String label = searchQuery.isBlank() ? SEARCH_HINT : searchQuery;
            int color = ThemePalette.multiplyAlpha(searchQuery.isBlank() ? ThemePalette.SECONDARY_TEXT : ThemePalette.SELECTED_OPTION_TEXT, opacity);
            renderer.drawText(FontAsset.INTER_SEMI_BOLD, ellipsize(label, fieldWidth - 12.0F, 6.0F), fieldX + 6.0F, fieldY + 4.0F, 6.0F, color);
            if (searchFocused && caretVisible()) {
                float caret = caretX(searchQuery, searchCursor, fieldX + 6.0F, fieldWidth - 12.0F, 6.0F);
                renderer.drawRoundedRect(caret, fieldY + 3.0F, 1.0F, 10.0F, 0.0F, ThemePalette.multiplyAlpha(ThemePalette.SLIDER_FILL, opacity));
            }
        }
    }

    private void drawRows(UiRenderer renderer, int mouseX, int mouseY, float opacity) {
        actionButtons.clear();
        List<FriendRow> rows = getRows();
        float listX = x + PADDING;
        float listY = y + HEADER_HEIGHT + PADDING;
        float listWidth = 208.0F;
        float listHeight = 206.0F;
        float contentHeight = rows.isEmpty() ? 0.0F : rows.size() * ROW_HEIGHT + Math.max(0, rows.size() - 1) * ROW_GAP;
        maxScroll = Math.max(0.0F, contentHeight - listHeight);
        scroll = Math.clamp(scroll, 0.0F, maxScroll);
        renderer.pushScissor(listX, listY, listWidth, listHeight);
        if (rows.isEmpty()) {
            drawEmptyRows(renderer, listX, listY, opacity);
            renderer.popScissor();
            return;
        }
        for (int index = 0; index < rows.size(); index++) {
            FriendRow row = rows.get(index);
            float rowY = listY + index * 22.0F - scroll;
            if (rowY + ROW_HEIGHT < listY || rowY > listY + listHeight) continue;
            drawRow(renderer, listX, rowY, listWidth, row, mouseX, mouseY, opacity);
        }
        renderer.popScissor();
    }

    private void drawRow(UiRenderer renderer, float rowX, float rowY, float width, FriendRow row, int mouseX, int mouseY, float opacity) {
        boolean hovered = ThemePalette.contains(rowX, rowY, width, ROW_HEIGHT, mouseX, mouseY);
        renderer.drawRoundedRect(rowX, rowY, width, ROW_HEIGHT, ROW_RADIUS, ThemePalette.multiplyAlpha((hovered ? ThemePalette.ROW_HOVER : ThemePalette.ROW_BACKGROUND).getRGB(), opacity));
        float headX = rowX + 4.0F;
        float headY = rowY + 3.0F;
        renderer.drawRoundedRect(headX, headY, HEAD_SIZE, HEAD_SIZE, 4.0F, ThemePalette.multiplyAlpha(ThemePalette.ROW_HOVER.getRGB(), opacity));
        TextureView head = row.playerEntry == null ? null : TextureResources.playerHead(row.playerEntry);
        if (head != null) renderer.drawImage(head, headX, headY, HEAD_SIZE, HEAD_SIZE, 4.0F, opacity);
        else drawIcon(renderer, "\ue19f", headX, headY, HEAD_SIZE, 6.0F, opacity);
        float buttonX = rowX + width - 4.0F - ROW_BUTTON_SIZE;
        float buttonY = rowY + 2.0F;
        boolean buttonHovered = ThemePalette.contains(buttonX, buttonY, ROW_BUTTON_SIZE, ROW_BUTTON_SIZE, mouseX, mouseY);
        renderer.drawRoundedRect(buttonX, buttonY, ROW_BUTTON_SIZE, ROW_BUTTON_SIZE, 4.0F, ThemePalette.multiplyAlpha((buttonHovered ? ThemePalette.ROW_HOVER : ThemePalette.REGISTRY_SURFACE).getRGB(), opacity));
        drawIcon(renderer, row.friend ? "\ue11c" : "\ue13d", buttonX, buttonY, ROW_BUTTON_SIZE, 8.0F, opacity);
        if (FontAsset.INTER_SEMI_BOLD != null) {
            float textX = headX + HEAD_SIZE + 5.0F;
            float textWidth = buttonX - textX - 5.0F;
            renderer.drawText(FontAsset.INTER_SEMI_BOLD, ellipsize(row.playerName, textWidth, 7.0F), textX, rowY + 5.0F, 7.0F, ThemePalette.multiplyAlpha(ThemePalette.SELECTED_OPTION_TEXT, opacity));
        }
        actionButtons.add(new FriendActionButton(row.playerName, buttonX, buttonY, ROW_BUTTON_SIZE, ROW_BUTTON_SIZE));
    }

    private void drawScrollbar(UiRenderer renderer, float opacity) {
        if (maxScroll <= 0.0F) return;
        float barX = x + WIDTH - PADDING - SCROLLBAR_WIDTH;
        float barY = y + HEADER_HEIGHT + PADDING;
        float height = 206.0F;
        renderer.drawRoundedRect(barX, barY, SCROLLBAR_WIDTH, height, SCROLLBAR_RADIUS, ThemePalette.multiplyAlpha(ThemePalette.ROW_BACKGROUND.getRGB(), opacity));
        float fraction = height / (height + maxScroll);
        float thumbHeight = Math.max(18.0F, height * fraction);
        float travel = height - thumbHeight;
        float offset = maxScroll <= 0.0F ? 0.0F : travel * (scroll / maxScroll);
        renderer.drawRoundedRect(barX, barY + offset, SCROLLBAR_WIDTH, thumbHeight, SCROLLBAR_RADIUS, ThemePalette.multiplyAlpha(ThemePalette.SLIDER_FILL.getRGB(), opacity));
    }

    private void drawAddFriendPopup(UiRenderer renderer, TextureView backdrop, int mouseX, int mouseY, float opacity) {
        if (backdrop != null) renderer.drawBackdrop(backdrop, popupX, popupY, POPUP_WIDTH, POPUP_HEIGHT, POPUP_RADIUS, opacity);
        renderer.drawRoundedRect(popupX, popupY, POPUP_WIDTH, POPUP_HEIGHT, POPUP_RADIUS, ThemePalette.multiplyAlpha(ThemePalette.PANEL_FOREGROUND.getRGB(), opacity));
        float headX = popupX + PADDING;
        float headY = popupY + PADDING;
        renderer.drawRoundedRect(headX, headY, POPUP_HEAD_SIZE, POPUP_HEAD_SIZE, 8.0F, ThemePalette.multiplyAlpha(ThemePalette.ROW_HOVER.getRGB(), opacity));
        TextureView head = addFriendHead();
        if (head != null) renderer.drawImage(head, headX, headY, POPUP_HEAD_SIZE, POPUP_HEAD_SIZE, 8.0F, opacity);
        else drawIcon(renderer, "\ue19f", headX, headY, POPUP_HEAD_SIZE, 10.0F, opacity);
        if (FontAsset.INTER_SEMI_BOLD != null) {
            String title = addFriendName.isBlank() ? ADD_TITLE : addFriendName;
            renderer.drawText(FontAsset.INTER_SEMI_BOLD, ellipsize(title, 120.0F, 8.0F), popupX + 48.0F, popupY + 18.0F, 8.0F, ThemePalette.multiplyAlpha(ThemePalette.SELECTED_OPTION_TEXT, opacity));
        }
        float fieldX = popupFieldX();
        float fieldY = popupFieldY();
        float fieldWidth = popupFieldWidth();
        boolean hovered = ThemePalette.contains(fieldX, fieldY, fieldWidth, POPUP_FIELD_HEIGHT, mouseX, mouseY);
        renderer.drawRoundedRect(fieldX, fieldY, fieldWidth, POPUP_FIELD_HEIGHT, 6.0F, ThemePalette.multiplyAlpha(ThemePalette.withAlpha(ThemePalette.SLOT_BACKGROUND, addFriendFieldFocused ? 220 : hovered ? 190 : 160), opacity));
        if (FontAsset.INTER_SEMI_BOLD != null) {
            String label = addFriendName.isBlank() ? NAME_HINT : addFriendName;
            int color = ThemePalette.multiplyAlpha(addFriendName.isBlank() ? ThemePalette.SECONDARY_TEXT : ThemePalette.SELECTED_OPTION_TEXT, opacity);
            renderer.drawText(FontAsset.INTER_SEMI_BOLD, ellipsize(label, fieldWidth - 12.0F, 6.0F), fieldX + 6.0F, fieldY + 4.0F, 6.0F, color);
            if (addFriendFieldFocused && caretVisible()) {
                float caret = caretX(addFriendName, addFriendCursor, fieldX + 6.0F, fieldWidth - 12.0F, 6.0F);
                renderer.drawRoundedRect(caret, fieldY + 3.0F, 1.0F, 10.0F, 0.0F, ThemePalette.multiplyAlpha(ThemePalette.SLIDER_FILL, opacity));
            }
        }
        float buttonX = popupSubmitX();
        float buttonY = popupSubmitY();
        boolean submitHovered = ThemePalette.contains(buttonX, buttonY, POPUP_BUTTON_SIZE, POPUP_BUTTON_SIZE, mouseX, mouseY);
        renderer.drawRoundedRect(buttonX, buttonY, POPUP_BUTTON_SIZE, POPUP_BUTTON_SIZE, 6.0F, ThemePalette.multiplyAlpha((submitHovered ? ThemePalette.ROW_HOVER : ThemePalette.ROW_BACKGROUND).getRGB(), opacity));
        drawIcon(renderer, "\ue13d", buttonX, buttonY, POPUP_BUTTON_SIZE, 9.0F, opacity);
    }

    private void drawEmptyRows(UiRenderer renderer, float listX, float listY, float opacity) {
        maxScroll = 0.0F;
        scroll = 0.0F;
        if (FontAsset.INTER_SEMI_BOLD == null) return;
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, EMPTY_TITLE, listX, listY + 10.0F, 8.0F, ThemePalette.multiplyAlpha(ThemePalette.SELECTED_OPTION_TEXT, opacity));
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, EMPTY_HINT, listX, listY + 22.0F, 6.0F, ThemePalette.multiplyAlpha(ThemePalette.PRIMARY_TEXT, opacity));
    }

    private boolean clickAddButton(float mouseX, float mouseY) {
        if (!ThemePalette.contains(addButtonX(), addButtonY(), HEADER_BUTTON_SIZE, HEADER_BUTTON_SIZE, mouseX, mouseY)) return false;
        addFriendPopupOpen = true;
        addFriendFieldFocused = true;
        addFriendCursor = addFriendName.length();
        initializePopupPosition();
        return true;
    }

    private boolean clickSearch(float mouseX, float mouseY) {
        if (!ThemePalette.contains(searchX(), searchY(), searchWidth(), SEARCH_HEIGHT, mouseX, mouseY)) return false;
        searchFocused = true;
        addFriendFieldFocused = false;
        searchCursor = cursorAt(searchQuery, mouseX, searchX() + 6.0F, 6.0F);
        return true;
    }

    private boolean clickPopup(float mouseX, float mouseY) {
        if (!ThemePalette.contains(popupX, popupY, POPUP_WIDTH, POPUP_HEIGHT, mouseX, mouseY)) {
            addFriendFieldFocused = false;
            return false;
        }
        if (ThemePalette.contains(popupSubmitX(), popupSubmitY(), POPUP_BUTTON_SIZE, POPUP_BUTTON_SIZE, mouseX, mouseY)) {
            submitFriend();
            return true;
        }
        if (ThemePalette.contains(popupFieldX(), popupFieldY(), popupFieldWidth(), POPUP_FIELD_HEIGHT, mouseX, mouseY)) {
            addFriendFieldFocused = true;
            addFriendCursor = cursorAt(addFriendName, mouseX, popupFieldX() + 6.0F, 6.0F);
            return true;
        }
        if (ThemePalette.contains(popupX, popupY, POPUP_WIDTH, POPUP_DRAG_HEIGHT, mouseX, mouseY)) {
            popupDragging = true;
            popupDragOffsetX = popupX - mouseX;
            popupDragOffsetY = popupY - mouseY;
            addFriendFieldFocused = false;
            return true;
        }
        addFriendFieldFocused = false;
        return true;
    }

    // Reused UI02 friend-editing fragment, reconciled with the actual input provider.
    private boolean handleTextKey(KeyEvent input, boolean addFriendField) {
        String text = addFriendField ? addFriendName : searchQuery;
        int cursor = addFriendField ? addFriendCursor : searchCursor;
        switch (InputKey.fromCode(input.key())) {
            case BACKSPACE -> {
                if ((input.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
                    int start = previousWordStart(text, cursor);
                    text = text.substring(0, start) + text.substring(cursor);
                    cursor = start;
                } else if (cursor > 0 && !text.isEmpty()) {
                    text = text.substring(0, cursor - 1) + text.substring(cursor);
                    cursor--;
                }
                updateTextField(addFriendField, text, cursor);
                return true;
            }
            case DELETE -> {
                if (cursor < text.length()) text = text.substring(0, cursor) + text.substring(cursor + 1);
                updateTextField(addFriendField, text, cursor);
                return true;
            }
            case LEFT -> { updateTextField(addFriendField, text, Math.max(0, cursor - 1)); return true; }
            case RIGHT -> { updateTextField(addFriendField, text, Math.min(text.length(), cursor + 1)); return true; }
            case HOME -> { updateTextField(addFriendField, text, 0); return true; }
            case END -> { updateTextField(addFriendField, text, text.length()); return true; }
            case ENTER, KEYPAD_ENTER -> {
                if (addFriendField) { submitFriend(); return true; }
                return false;
            }
            default -> { return false; }
        }
    }

    private void updateTextField(boolean addFriendField, String text, int cursor) {
        int boundedCursor = Math.clamp(cursor, 0, text.length());
        if (addFriendField) { addFriendName = text; addFriendCursor = boundedCursor; return; }
        searchQuery = text;
        searchCursor = boundedCursor;
    }

    private void submitFriend() {
        String name = addFriendName.trim();
        if (name.isEmpty() || Echo.friendManager == null) return;
        Echo.friendManager.add(name);
        addFriendName = "";
        addFriendCursor = 0;
        addFriendPopupOpen = false;
        addFriendFieldFocused = false;
    }

    private void toggleFriend(String name) {
        if (Echo.friendManager == null) return;
        if (Echo.friendManager.contains(name)) Echo.friendManager.remove(name);
        else Echo.friendManager.add(name);
    }

    private List<FriendRow> getRows() {
        LinkedHashMap<String, FriendRow> rows = new LinkedHashMap<>();
        if (Minecraft.getInstance().getConnection() != null) {
            for (PlayerInfo entry : Minecraft.getInstance().getConnection().getOnlinePlayers()) {
                String name = entry.getProfile().name();
                if (name == null || name.isBlank() || Minecraft.getInstance().player != null
                        && name.equalsIgnoreCase(Minecraft.getInstance().player.getName().getString())) continue;
                rows.put(normalizedPlayerName(name), new FriendRow(name, entry, EntityInteractions.isFriend(name)));
            }
        }
        if (Echo.friendManager != null) {
            for (String name : Echo.friendManager.names()) {
                String normalized = normalizedPlayerName(name);
                if (normalized == null) continue;
                FriendRow existing = rows.get(normalized);
                if (existing != null) { existing.friend = true; continue; }
                rows.put(normalized, new FriendRow(name, findPlayerEntry(name), true));
            }
        }
        ArrayList<FriendRow> result = new ArrayList<>(rows.values());
        result.removeIf(row -> !matchesSearch(row.playerName));
        result.sort(Comparator.comparing((FriendRow row) -> !row.friend)
                .thenComparing(row -> row.playerName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private PlayerInfo findPlayerEntry(String name) {
        if (Minecraft.getInstance().getConnection() == null) return null;
        return Minecraft.getInstance().getConnection().getPlayerInfo(name);
    }

    private TextureView addFriendHead() {
        if (addFriendName.isBlank()) return null;
        PlayerInfo entry = findPlayerEntry(addFriendName.trim());
        return entry == null ? null : TextureResources.playerHead(entry);
    }

    private boolean matchesSearch(String name) {
        String query = searchQuery.trim();
        return query.isEmpty() || name.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    private float scrollSpeed() {
        ClickGUIFeature feature = screen.clickGuiFeature;
        return feature == null ? ThemePalette.SCROLL_SPEED : feature.getScrollSpeed();
    }

    private void initializePosition(float viewportWidth, float baselineY) {
        if (positionInitialized) return;
        x = (viewportWidth - WIDTH) / 2.0F;
        y = baselineY + 8.0F;
        positionInitialized = true;
    }

    private void initializePopupPosition() {
        if (popupPositionInitialized) return;
        popupX = x + WIDTH + 12.0F;
        popupY = y + 18.0F;
        popupPositionInitialized = true;
    }

    private float searchX() { return x + WIDTH - PADDING - HEADER_BUTTON_SIZE - 6.0F - searchWidth(); }
    private float searchY() { return y + 4.0F; }
    private float searchWidth() { return 88.0F; }
    private float addButtonX() { return x + WIDTH - PADDING - HEADER_BUTTON_SIZE; }
    private float addButtonY() { return y + 3.0F; }
    private float popupFieldX() { return popupX + PADDING; }
    private float popupFieldY() { return popupY + POPUP_HEIGHT - POPUP_FIELD_HEIGHT - PADDING; }
    private float popupFieldWidth() { return 156.0F; }
    private float popupSubmitX() { return popupFieldX() + popupFieldWidth() + 8.0F; }
    private float popupSubmitY() { return popupFieldY() - 1.0F; }

    private int previousWordStart(String text, int cursor) {
        int index = Math.clamp(cursor, 0, text.length());
        while (index > 0 && text.charAt(index - 1) == ' ') index--;
        while (index > 0 && text.charAt(index - 1) != ' ') index--;
        return index;
    }

    private int cursorAt(String text, float mouseX, float textX, float size) {
        if (FontAsset.INTER_SEMI_BOLD == null) return text.length();
        float offset = mouseX - textX;
        if (offset <= 0.0F) return 0;
        for (int index = 0; index <= text.length(); index++) {
            float width = FontAsset.INTER_SEMI_BOLD.measureText(text.substring(0, index), size);
            if (offset <= width) return index;
        }
        return text.length();
    }

    private float caretX(String text, int cursor, float textX, float width, float size) {
        if (FontAsset.INTER_SEMI_BOLD == null) return textX;
        int bounded = Math.clamp(cursor, 0, text.length());
        float caret = textX + FontAsset.INTER_SEMI_BOLD.measureText(text.substring(0, bounded), size);
        return Math.min(caret, textX + width);
    }

    private boolean caretVisible() { return System.currentTimeMillis() / 500L % 2L == 0L; }

    private CharSequence ellipsize(String text, float width, float size) {
        if (FontAsset.INTER_SEMI_BOLD == null) return text;
        if (FontAsset.INTER_SEMI_BOLD.measureText(text, size) <= width) return text;
        for (int index = text.length() - 1; index > 0; index--) {
            String shortened = text.substring(0, index) + ELLIPSIS;
            if (FontAsset.INTER_SEMI_BOLD.measureText(shortened, size) <= width) return shortened;
        }
        return ELLIPSIS;
    }

    private void drawIcon(UiRenderer renderer, String glyph, float iconX, float iconY, float extent, float size, float opacity) {
        if (FontAsset.LUCIDE == null) return;
        float width = FontAsset.LUCIDE.measureText(glyph, size);
        float textX = iconX + (extent - width) / 2.0F;
        float textY = iconY + extent / 2.0F - size / 2.0F;
        renderer.drawText(FontAsset.LUCIDE, glyph, textX, textY, size, ThemePalette.multiplyAlpha(ThemePalette.SELECTED_OPTION_TEXT, opacity));
    }

    private String normalizedPlayerName(String name) {
        if (name == null) return null;
        String trimmed = name.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }
}
