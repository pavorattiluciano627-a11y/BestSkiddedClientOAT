package dev.echo.client.screens.panels;

import dev.echo.client.Echo;
import dev.echo.client.config.ConfigMetadata;
import dev.echo.client.features.internals.ClickGUIFeature;
import dev.echo.client.input.InputKey;
import dev.echo.client.render.UiRenderer;
import dev.echo.client.screens.CategoryPanelScreen;
import dev.echo.client.ui.ThemePalette;
import dev.echo.client.utils.chat.ChatHudMessages;
import dev.echo.client.utils.strings.Concat;
import dev.echo.render.FontAsset;
import dev.echo.render.ImageDecoder;
import dev.echo.render.texture.TextureView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Original ContextStore local-profile panel, with conditional bindings recorded in the receipt. */
public class ConfigsPanel {
    static final float WIDTH = 344.0F;
    static final float HEIGHT = 232.0F;
    static final float PADDING = 10.0F;
    static final float RADIUS = 10.0F;
    static final float HEADER_HEIGHT = 24.0F;
    static final float HEADER_BUTTON_SIZE = 18.0F;
    static final float CARD_HEIGHT = 72.0F;
    static final float CARD_RADIUS = 8.0F;
    static final float CARD_GAP = 6.0F;
    static final float ACTION_HEIGHT = 14.0F;
    static final float ACTION_GAP = 6.0F;
    static final float ACTION_RADIUS = 4.0F;
    static final float SCROLLBAR_WIDTH = 4.0F;
    static final float SCROLLBAR_RADIUS = 2.0F;
    static final int COLUMNS = 3;
    static final float POPUP_WIDTH = 230.0F;
    static final float POPUP_HEIGHT = 76.0F;
    static final float POPUP_RADIUS = 12.0F;
    static final float POPUP_ICON_SIZE = 28.0F;
    static final float POPUP_FIELD_HEIGHT = 16.0F;
    static final float POPUP_BUTTON_SIZE = 18.0F;
    static final float POPUP_DRAG_HEIGHT = 32.0F;
    private static final String TITLE = new String("Configs");
    private static final String NEW_TITLE = new String("New Config");
    private static final String NAME_HINT = new String("Config name");
    private static final String SAVE = new String("Save");
    private static final String LOAD = new String("Load");
    private static final String LOCAL = new String("local");
    private static final String EMPTY_TITLE = new String("No saved configs yet");
    private static final String EMPTY_HINT = new String("Use the top-right button later.");
    private static final String SAVED_PREFIX = new String("saved ");
    private static final String SAVED_NEW_PREFIX = new String("saved ");
    private static final String LOADED_PREFIX = new String("loaded ");
    private static final String FAILED_PREFIX = new String("failed to ");
    private static final String SAVE_PREFIX = new String("save ");
    private static final String LOAD_PREFIX = new String("load ");
    private static final String FAILED_SAVE_PREFIX = new String("failed to save ");
    private static final String NAME_REQUIRED = new String("enter a config name");
    private static final String SINGLEPLAYER = new String("singleplayer");
    private static final String ICON_ERROR = new String("Failed to decode glass config icon for profile '{}': {}");
    private static final String ELLIPSIS = new String("...");
    private static final String AUTOSAVE = new String("_autosave");

    private final CategoryPanelScreen screen;
    private float x;
    private float y;
    private float scroll;
    private float maxScroll;
    private final Map<String, TextureView> iconTextures;
    private final List<ConfigActionButton> actionButtons;
    private TextureView serverIcon;
    private byte[] serverIconBytes;
    private boolean positionInitialized;
    private boolean dragging;
    private float dragOffsetX;
    private float dragOffsetY;
    private boolean createPopupOpen;
    private boolean popupPositionInitialized;
    private float popupX;
    private float popupY;
    private boolean popupDragging;
    private float popupDragOffsetX;
    private float popupDragOffsetY;
    private boolean nameFocused;
    private String configName;
    private int nameCursor;

    public ConfigsPanel(CategoryPanelScreen screen) {
        iconTextures = new HashMap<>();
        actionButtons = new ArrayList<>();
        configName = "";
        nameCursor = 0;
        this.screen = screen;
    }

    public void render(UiRenderer renderer, float viewportWidth, int mouseX, int mouseY,
                       float opacity, TextureView backdrop, float baselineY) {
        initializePosition(viewportWidth, baselineY);
        initializePopupPosition();
        int contentMouseX = mouseX;
        int contentMouseY = mouseY;
        if (createPopupOpen && ThemePalette.contains(popupX, popupY, POPUP_WIDTH, POPUP_HEIGHT, mouseX, mouseY)) {
            contentMouseX = Integer.MIN_VALUE;
            contentMouseY = Integer.MIN_VALUE;
        }
        drawBackground(renderer, backdrop, opacity);
        drawHeader(renderer, contentMouseX, contentMouseY, opacity);
        drawCards(renderer, contentMouseX, contentMouseY, opacity);
        drawScrollbar(renderer, opacity);
        if (createPopupOpen) drawCreatePopup(renderer, backdrop, mouseX, mouseY, opacity);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double vertical) {
        if (createPopupOpen && ThemePalette.contains(popupX, popupY, POPUP_WIDTH, POPUP_HEIGHT, (float) mouseX, (float) mouseY)) return true;
        if (!ThemePalette.contains(x, y, WIDTH, HEIGHT, (float) mouseX, (float) mouseY)) return false;
        if (maxScroll <= 0.0F || vertical == 0.0) return true;
        float speed = scrollSpeed();
        float nextScroll = Math.clamp(scroll - (float) vertical * speed, 0.0F, maxScroll);
        scroll = nextScroll;
        return true;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!InputKey.MOUSE_LEFT.matches(button)) return false;
        if (createPopupOpen && clickPopup((float) mouseX, (float) mouseY)) return true;
        if (clickCreateButton((float) mouseX, (float) mouseY)) return true;
        if (ThemePalette.contains(x, y, WIDTH, HEADER_HEIGHT, (float) mouseX, (float) mouseY)) {
            dragging = true;
            dragOffsetX = x - (float) mouseX;
            dragOffsetY = y - (float) mouseY;
            nameFocused = false;
            return true;
        }
        for (ConfigActionButton action : actionButtons) {
            if (ThemePalette.contains(action.x, action.y, action.width, action.height, (float) mouseX, (float) mouseY)) return performAction(action);
        }
        return ThemePalette.contains(x, y, WIDTH, HEIGHT, (float) mouseX, (float) mouseY);
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

    public boolean charTyped(CharInput input) {
        if (!createPopupOpen || !nameFocused) return false;
        char character = (char) input.codepoint();
        if (Character.isISOControl(character)) return false;
        int cursor = Math.clamp(nameCursor, 0, configName.length());
        configName = PanelTextEditing.insertCharacter(configName, cursor, character);
        nameCursor = cursor + 1;
        return true;
    }

    public boolean keyPressed(KeyInput input) {
        if (!createPopupOpen) return false;
        if (input.key() == InputKey.ESCAPE.code()) {
            createPopupOpen = false;
            nameFocused = false;
            return true;
        }
        if (!nameFocused) return false;
        if (input.key() == InputKey.BACKSPACE.code()) {
            if ((input.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
                int start = previousWordStart(configName, nameCursor);
                configName = configName.substring(0, start) + configName.substring(nameCursor);
                nameCursor = start;
            } else if (nameCursor > 0 && !configName.isEmpty()) {
                configName = configName.substring(0, nameCursor - 1) + configName.substring(nameCursor);
                nameCursor--;
            }
            return true;
        }
        if (input.key() == InputKey.DELETE.code()) {
            if (nameCursor < configName.length()) configName = configName.substring(0, nameCursor) + configName.substring(nameCursor + 1);
            return true;
        }
        if (input.key() == InputKey.LEFT.code()) { nameCursor = Math.max(0, nameCursor - 1); return true; }
        if (input.key() == InputKey.RIGHT.code()) { nameCursor = Math.min(configName.length(), nameCursor + 1); return true; }
        if (input.key() == InputKey.HOME.code()) { nameCursor = 0; return true; }
        if (input.key() == InputKey.END.code()) { nameCursor = configName.length(); return true; }
        if (input.key() == InputKey.ENTER.code() || input.key() == InputKey.KEYPAD_ENTER.code()) {
            submitConfig();
            return true;
        }
        return false;
    }

    private void drawBackground(UiRenderer renderer, TextureView backdrop, float opacity) {
        if (backdrop != null) renderer.drawBackdrop(backdrop, x, y, WIDTH, HEIGHT, RADIUS, opacity);
        renderer.drawRoundedRect(x, y, WIDTH, HEIGHT, RADIUS, ThemePalette.multiplyAlpha(ThemePalette.PANEL_FOREGROUND.getRGB(), opacity));
    }

    private void drawHeader(UiRenderer renderer, int mouseX, int mouseY, float opacity) {
        if (FontAsset.INTER_SEMI_BOLD != null) renderer.drawText(FontAsset.INTER_SEMI_BOLD, TITLE, x + PADDING, y + 7.0F, 10.0F, ThemePalette.multiplyAlpha(ThemePalette.SELECTED_OPTION_TEXT, opacity));
        float buttonX = x + WIDTH - PADDING - HEADER_BUTTON_SIZE;
        float buttonY = y + 3.0F;
        boolean hovered = ThemePalette.contains(buttonX, buttonY, HEADER_BUTTON_SIZE, HEADER_BUTTON_SIZE, mouseX, mouseY);
        renderer.drawRoundedRect(buttonX, buttonY, HEADER_BUTTON_SIZE, HEADER_BUTTON_SIZE, 5.0F, ThemePalette.multiplyAlpha((hovered ? ThemePalette.ROW_HOVER : ThemePalette.ROW_BACKGROUND).getRGB(), opacity));
        if (FontAsset.LUCIDE != null) {
            float size = 8.0F;
            String glyph = "\ue13d";
            float width = FontAsset.LUCIDE.measureText(glyph, size);
            float textX = buttonX + (HEADER_BUTTON_SIZE - width) / 2.0F;
            float textY = buttonY + 9.0F - size / 2.0F;
            renderer.drawText(FontAsset.LUCIDE, glyph, textX, textY, size, ThemePalette.multiplyAlpha(ThemePalette.SELECTED_OPTION_TEXT, opacity));
        }
    }

    private void drawCreatePopup(UiRenderer renderer, TextureView backdrop, int mouseX, int mouseY, float opacity) {
        if (backdrop != null) renderer.drawBackdrop(backdrop, popupX, popupY, POPUP_WIDTH, POPUP_HEIGHT, POPUP_RADIUS, opacity);
        renderer.drawRoundedRect(popupX, popupY, POPUP_WIDTH, POPUP_HEIGHT, POPUP_RADIUS, ThemePalette.multiplyAlpha(ThemePalette.PANEL_FOREGROUND.getRGB(), opacity));
        float iconX = popupX + PADDING;
        float iconY = popupY + PADDING;
        renderer.drawRoundedRect(iconX, iconY, POPUP_ICON_SIZE, POPUP_ICON_SIZE, 8.0F, ThemePalette.multiplyAlpha(ThemePalette.ROW_HOVER.getRGB(), opacity));
        TextureView icon = currentServerIcon();
        if (icon != null) renderer.drawImage(icon, iconX, iconY, POPUP_ICON_SIZE, POPUP_ICON_SIZE, 8.0F, opacity);
        else if (FontAsset.LUCIDE != null) {
            float size = 10.0F;
            String glyph = "\ue0cf";
            float width = FontAsset.LUCIDE.measureText(glyph, size);
            float textX = iconX + (POPUP_ICON_SIZE - width) / 2.0F;
            float textY = iconY + 14.0F - size / 2.0F;
            renderer.drawText(FontAsset.LUCIDE, glyph, textX, textY, size, ThemePalette.multiplyAlpha(ThemePalette.SELECTED_OPTION_TEXT, opacity));
        }
        if (FontAsset.INTER_SEMI_BOLD != null) {
            String title = configName.isBlank() ? NEW_TITLE : configName;
            renderer.drawText(FontAsset.INTER_SEMI_BOLD, ellipsize(title, 130.0F, 8.0F), popupX + 48.0F, popupY + 18.0F, 8.0F, ThemePalette.multiplyAlpha(ThemePalette.SELECTED_OPTION_TEXT, opacity));
        }
        float fieldX = popupX + PADDING;
        float fieldY = popupY + POPUP_HEIGHT - POPUP_FIELD_HEIGHT - PADDING;
        float fieldWidth = 184.0F;
        boolean hovered = ThemePalette.contains(fieldX, fieldY, fieldWidth, POPUP_FIELD_HEIGHT, mouseX, mouseY);
        renderer.drawRoundedRect(fieldX, fieldY, fieldWidth, POPUP_FIELD_HEIGHT, 6.0F, ThemePalette.multiplyAlpha(ThemePalette.withAlpha(ThemePalette.SLOT_BACKGROUND, nameFocused ? 220 : hovered ? 190 : 160), opacity));
        if (FontAsset.INTER_SEMI_BOLD != null) {
            String label = configName.isBlank() ? NAME_HINT : configName;
            int color = ThemePalette.multiplyAlpha(configName.isBlank() ? ThemePalette.SECONDARY_TEXT : ThemePalette.SELECTED_OPTION_TEXT, opacity);
            renderer.drawText(FontAsset.INTER_SEMI_BOLD, ellipsize(label, fieldWidth - 12.0F, 6.0F), fieldX + 6.0F, fieldY + 4.0F, 6.0F, color);
            if (nameFocused && caretVisible()) {
                int cursor = Math.clamp(nameCursor, 0, configName.length());
                float caret = fieldX + 6.0F + FontAsset.INTER_SEMI_BOLD.measureText(configName.substring(0, cursor), 6.0F);
                float right = fieldX + fieldWidth - 6.0F;
                caret = Math.min(caret, right);
                renderer.drawRoundedRect(caret, fieldY + 3.0F, 1.0F, 10.0F, 0.0F, ThemePalette.multiplyAlpha(ThemePalette.SLIDER_FILL, opacity));
            }
        }
        float buttonX = fieldX + fieldWidth + 8.0F;
        float buttonY = fieldY - 1.0F;
        boolean buttonHovered = ThemePalette.contains(buttonX, buttonY, POPUP_BUTTON_SIZE, POPUP_BUTTON_SIZE, mouseX, mouseY);
        renderer.drawRoundedRect(buttonX, buttonY, POPUP_BUTTON_SIZE, POPUP_BUTTON_SIZE, 6.0F, ThemePalette.multiplyAlpha((buttonHovered ? ThemePalette.ROW_HOVER : ThemePalette.ROW_BACKGROUND).getRGB(), opacity));
        if (FontAsset.LUCIDE != null) {
            float size = 9.0F;
            String glyph = "\ue06c";
            float width = FontAsset.LUCIDE.measureText(glyph, size);
            float textX = buttonX + (POPUP_BUTTON_SIZE - width) / 2.0F;
            float textY = buttonY + 9.0F - size / 2.0F;
            renderer.drawText(FontAsset.LUCIDE, glyph, textX, textY, size, ThemePalette.multiplyAlpha(ThemePalette.SELECTED_OPTION_TEXT, opacity));
        }
    }

    private void drawCards(UiRenderer renderer, int mouseX, int mouseY, float opacity) {
        actionButtons.clear();
        List<ConfigMetadata> profiles = getProfiles();
        if (profiles.isEmpty()) { drawEmptyCards(renderer, opacity); return; }
        float listX = x + PADDING;
        float listY = y + HEADER_HEIGHT + PADDING;
        float listWidth = 314.0F;
        float listHeight = 188.0F;
        float cardWidth = (listWidth - 12.0F) / 3.0F;
        float contentHeight = PanelGeometry.configGridHeight(profiles.size());
        maxScroll = Math.max(0.0F, contentHeight - listHeight);
        scroll = Math.clamp(scroll, 0.0F, maxScroll);
        renderer.pushScissor(listX, listY, listWidth, listHeight);
        for (int index = 0; index < profiles.size(); index++) {
            int column = index % COLUMNS;
            int row = index / COLUMNS;
            float cardX = listX + column * (cardWidth + CARD_GAP);
            float cardY = listY + row * 78.0F - scroll;
            if (cardY + CARD_HEIGHT < listY || cardY > listY + listHeight) continue;
            ConfigMetadata profile = profiles.get(index);
            boolean hovered = ThemePalette.contains(cardX, cardY, cardWidth, CARD_HEIGHT, mouseX, mouseY);
            drawCard(renderer, cardX, cardY, cardWidth, opacity, hovered, profile, mouseX, mouseY);
        }
        renderer.popScissor();
    }

    private void drawCard(UiRenderer renderer, float cardX, float cardY, float width, float opacity,
                          boolean hovered, ConfigMetadata profile, int mouseX, int mouseY) {
        renderer.drawRoundedRect(cardX, cardY, width, CARD_HEIGHT, CARD_RADIUS, ThemePalette.multiplyAlpha((hovered ? ThemePalette.ROW_HOVER : ThemePalette.ROW_BACKGROUND).getRGB(), opacity));
        float iconSize = 22.0F;
        float iconX = cardX + 8.0F;
        float iconY = cardY + 8.0F;
        renderer.drawRoundedRect(iconX, iconY, iconSize, iconSize, 5.0F, ThemePalette.multiplyAlpha(ThemePalette.ROW_HOVER.getRGB(), opacity));
        TextureView icon = profileIcon(profile);
        if (icon != null) renderer.drawImage(icon, iconX, iconY, iconSize, iconSize, 5.0F, opacity);
        else if (FontAsset.LUCIDE != null) {
            float size = 8.0F;
            String glyph = "\ue1f3";
            float glyphWidth = FontAsset.LUCIDE.measureText(glyph, size);
            float textX = iconX + (iconSize - glyphWidth) / 2.0F;
            float textY = iconY + iconSize / 2.0F - size / 2.0F;
            renderer.drawText(FontAsset.LUCIDE, glyph, textX, textY, size, ThemePalette.multiplyAlpha(ThemePalette.TAB_ACTIVE_TEXT, opacity));
        }
        float titleX = iconX + iconSize + 6.0F;
        float textWidth = width - (titleX - cardX) - 8.0F;
        if (FontAsset.INTER_SEMI_BOLD != null) {
            renderer.drawText(FontAsset.INTER_SEMI_BOLD, ellipsize(profile.name(), textWidth, 8.0F), titleX, cardY + 9.0F, 8.0F, ThemePalette.multiplyAlpha(ThemePalette.SELECTED_OPTION_TEXT, opacity));
            renderer.drawText(FontAsset.INTER_SEMI_BOLD, ellipsize(serverLabel(profile), textWidth, 6.0F), cardX + 8.0F, cardY + 31.0F, 6.0F, ThemePalette.multiplyAlpha(ThemePalette.PRIMARY_TEXT, opacity));
            renderer.drawText(FontAsset.INTER_SEMI_BOLD, LOCAL, cardX + 8.0F, cardY + 40.0F, 6.0F, ThemePalette.multiplyAlpha(ThemePalette.SECONDARY_TEXT, opacity));
        }
        float buttonX = cardX + 8.0F;
        float buttonY = cardY + CARD_HEIGHT - ACTION_HEIGHT - 8.0F;
        float buttonWidth = (width - 16.0F - ACTION_GAP) / 2.0F;
        boolean saveHovered = ThemePalette.contains(buttonX, buttonY, buttonWidth, ACTION_HEIGHT, mouseX, mouseY);
        boolean loadHovered = ThemePalette.contains(buttonX + buttonWidth + ACTION_GAP, buttonY, buttonWidth, ACTION_HEIGHT, mouseX, mouseY);
        drawActionButton(renderer, buttonX, buttonY, buttonWidth, opacity, SAVE, saveHovered);
        drawActionButton(renderer, buttonX + buttonWidth + ACTION_GAP, buttonY, buttonWidth, opacity, LOAD, loadHovered);
        actionButtons.add(new ConfigActionButton(profile.name(), ConfigAction.SAVE, buttonX, buttonY, buttonWidth, ACTION_HEIGHT));
        actionButtons.add(new ConfigActionButton(profile.name(), ConfigAction.LOAD, buttonX + buttonWidth + ACTION_GAP, buttonY, buttonWidth, ACTION_HEIGHT));
    }

    private void drawActionButton(UiRenderer renderer, float buttonX, float buttonY, float width, float opacity, String label, boolean hovered) {
        renderer.drawRoundedRect(buttonX, buttonY, width, ACTION_HEIGHT, ACTION_RADIUS, ThemePalette.multiplyAlpha((hovered ? ThemePalette.ROW_HOVER : ThemePalette.REGISTRY_SURFACE).getRGB(), opacity));
        if (FontAsset.INTER_SEMI_BOLD == null) return;
        float textWidth = FontAsset.INTER_SEMI_BOLD.measureText(label, 6.0F);
        float textX = buttonX + (width - textWidth) / 2.0F;
        float textY = buttonY + 7.0F - 3.0F;
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, label, textX, textY, 6.0F, ThemePalette.multiplyAlpha(ThemePalette.SELECTED_OPTION_TEXT, opacity));
    }

    private void drawScrollbar(UiRenderer renderer, float opacity) {
        if (maxScroll <= 0.0F) return;
        float barX = x + WIDTH - PADDING - SCROLLBAR_WIDTH;
        float barY = y + HEADER_HEIGHT + PADDING;
        float height = 188.0F;
        renderer.drawRoundedRect(barX, barY, SCROLLBAR_WIDTH, height, SCROLLBAR_RADIUS, ThemePalette.multiplyAlpha(ThemePalette.ROW_BACKGROUND.getRGB(), opacity));
        float fraction = height / (height + maxScroll);
        float thumbHeight = Math.max(18.0F, height * fraction);
        float travel = height - thumbHeight;
        float offset = maxScroll <= 0.0F ? 0.0F : travel * (scroll / maxScroll);
        renderer.drawRoundedRect(barX, barY + offset, SCROLLBAR_WIDTH, thumbHeight, SCROLLBAR_RADIUS, ThemePalette.multiplyAlpha(ThemePalette.SLIDER_FILL.getRGB(), opacity));
    }

    private void drawEmptyCards(UiRenderer renderer, float opacity) {
        maxScroll = 0.0F;
        scroll = 0.0F;
        float textX = x + PADDING;
        float textY = y + HEADER_HEIGHT + PADDING + 10.0F;
        if (FontAsset.INTER_SEMI_BOLD == null) return;
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, EMPTY_TITLE, textX, textY, 8.0F, ThemePalette.multiplyAlpha(ThemePalette.SELECTED_OPTION_TEXT, opacity));
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, EMPTY_HINT, textX, textY + 12.0F, 6.0F, ThemePalette.multiplyAlpha(ThemePalette.PRIMARY_TEXT, opacity));
    }

    private boolean performAction(ConfigActionButton action) {
        if (Echo.featureConfig == null) return true;
        boolean success = switch (action.action) {
            case SAVE -> Echo.featureConfig.save(action.configName);
            case LOAD -> Echo.featureConfig.load(action.configName);
        };
        if (success) {
            ChatHudMessages.addMessage(Text.literal(Concat.strings(action.action == ConfigAction.SAVE ? SAVED_PREFIX : LOADED_PREFIX, action.configName).toString()));
        } else {
            ChatHudMessages.addMessage(Text.literal(Concat.strings(FAILED_PREFIX, action.action == ConfigAction.SAVE ? SAVE_PREFIX : LOAD_PREFIX, action.configName).toString()));
        }
        return true;
    }

    private boolean clickCreateButton(float mouseX, float mouseY) {
        if (!ThemePalette.contains(createButtonX(), createButtonY(), HEADER_BUTTON_SIZE, HEADER_BUTTON_SIZE, mouseX, mouseY)) return false;
        createPopupOpen = true;
        nameFocused = true;
        nameCursor = configName.length();
        initializePopupPosition();
        return true;
    }

    private boolean clickPopup(float mouseX, float mouseY) {
        if (!ThemePalette.contains(popupX, popupY, POPUP_WIDTH, POPUP_HEIGHT, mouseX, mouseY)) {
            nameFocused = false;
            return false;
        }
        float buttonX = popupSubmitX();
        if (ThemePalette.contains(buttonX, popupSubmitY(), POPUP_BUTTON_SIZE, POPUP_BUTTON_SIZE, mouseX, mouseY)) {
            submitConfig();
            return true;
        }
        float fieldX = popupFieldX();
        if (ThemePalette.contains(fieldX, popupFieldY(), popupFieldWidth(), POPUP_FIELD_HEIGHT, mouseX, mouseY)) {
            nameFocused = true;
            nameCursor = cursorAt(mouseX, fieldX + 6.0F);
            return true;
        }
        if (ThemePalette.contains(popupX, popupY, POPUP_WIDTH, POPUP_DRAG_HEIGHT, mouseX, mouseY)) {
            popupDragging = true;
            popupDragOffsetX = popupX - mouseX;
            popupDragOffsetY = popupY - mouseY;
            nameFocused = false;
            return true;
        }
        nameFocused = false;
        return true;
    }

    private void submitConfig() {
        String name = configName.trim();
        if (name.isEmpty()) {
            ChatHudMessages.addMessage(Text.literal(NAME_REQUIRED));
            return;
        }
        if (Echo.featureConfig == null) return;
        boolean saved = Echo.featureConfig.save(name);
        if (!saved) {
            ChatHudMessages.addMessage(Text.literal(Concat.strings(FAILED_SAVE_PREFIX, name).toString()));
            return;
        }
        createPopupOpen = false;
        nameFocused = false;
        configName = "";
        nameCursor = 0;
        ChatHudMessages.addMessage(Text.literal(Concat.strings(SAVED_NEW_PREFIX, name).toString()));
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

    private float createButtonX() { return x + WIDTH - PADDING - HEADER_BUTTON_SIZE; }
    private float createButtonY() { return y + 3.0F; }
    private float popupFieldX() { return popupX + PADDING; }
    private float popupFieldY() { return popupY + POPUP_HEIGHT - POPUP_FIELD_HEIGHT - PADDING; }
    private float popupFieldWidth() { return 184.0F; }
    private float popupSubmitX() { return popupFieldX() + popupFieldWidth() + 8.0F; }
    private float popupSubmitY() { return popupFieldY() - 1.0F; }

    private String serverLabel(ConfigMetadata profile) {
        if (profile.serverIp() != null && !profile.serverIp().isBlank()) return profile.serverIp();
        if (profile.serverName() != null && !profile.serverName().isBlank()) return profile.serverName();
        return SINGLEPLAYER;
    }

    private TextureView profileIcon(ConfigMetadata profile) {
        byte[] bytes = profile.serverIcon();
        if (bytes == null || bytes.length == 0) return null;
        try {
            return iconTextures.computeIfAbsent(profile.name(), ignored -> ImageDecoder.decodeTexture(Echo.renderDevice, bytes));
        } catch (Exception failure) {
            Echo.LOGGER.warn(ICON_ERROR, profile.name(), failure.getMessage());
            return null;
        }
    }

    private TextureView currentServerIcon() {
        ServerInfo server = MinecraftClient.getInstance().getCurrentServerEntry();
        if (server == null) return null;
        byte[] bytes = server.getFavicon();
        if (bytes == null || bytes.length == 0) return null;
        if (serverIcon != null && Arrays.equals(serverIconBytes, bytes)) return serverIcon;
        serverIconBytes = bytes.clone();
        serverIcon = ImageDecoder.decodeTexture(Echo.renderDevice, serverIconBytes);
        return serverIcon;
    }

    private boolean caretVisible() { return System.currentTimeMillis() / 500L % 2L == 0L; }

    private int cursorAt(float mouseX, float textX) {
        if (FontAsset.INTER_SEMI_BOLD == null) return configName.length();
        float offset = mouseX - textX;
        if (offset <= 0.0F) return 0;
        for (int index = 0; index <= configName.length(); index++) {
            float width = FontAsset.INTER_SEMI_BOLD.measureText(configName.substring(0, index), 6.0F);
            if (offset <= width) return index;
        }
        return configName.length();
    }

    private int previousWordStart(String text, int cursor) {
        int index = Math.clamp(cursor, 0, text.length());
        while (index > 0 && text.charAt(index - 1) == ' ') index--;
        while (index > 0 && text.charAt(index - 1) != ' ') index--;
        return index;
    }

    private CharSequence ellipsize(String text, float width, float size) {
        if (FontAsset.INTER_SEMI_BOLD == null) return text;
        if (FontAsset.INTER_SEMI_BOLD.measureText(text, size) <= width) return text;
        for (int index = text.length() - 1; index > 0; index--) {
            String shortened = text.substring(0, index) + ELLIPSIS;
            if (FontAsset.INTER_SEMI_BOLD.measureText(shortened, size) <= width) return shortened;
        }
        return ELLIPSIS;
    }

    private List<ConfigMetadata> getProfiles() {
        if (Echo.featureConfig == null) return List.of();
        ArrayList<ConfigMetadata> result = new ArrayList<>(Echo.featureConfig.list());
        result.removeIf(profile -> AUTOSAVE.equals(profile.name()));
        result.sort(Comparator.comparing(ConfigMetadata::name));
        return result;
    }
}
