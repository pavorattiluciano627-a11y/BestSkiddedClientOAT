package dev.echo.client.screens;

import dev.echo.client.Echo;
import dev.echo.client.MinecraftAccess;
import dev.echo.client.bridge.GameRenderControl;
import dev.echo.client.events.EventBus;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.scripting.ScriptLoadedEvent;
import dev.echo.client.events.scripting.ScriptUnloadedEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.internals.ClickGUIFeature;
import dev.echo.client.input.InputKey;
import dev.echo.client.render.GuiDimensions;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.render.UiRenderer;
import dev.echo.client.screens.panels.CategoryPanel;
import dev.echo.client.screens.panels.ConfigsPanel;
import dev.echo.client.screens.panels.FeatureCard;
import dev.echo.client.screens.panels.FriendsPanel;
import dev.echo.client.screens.panels.NavigationPage;
import dev.echo.client.screens.panels.NavigationSidebar;
import dev.echo.client.screens.panels.PanelGeometry;
import dev.echo.client.screens.panels.SidebarPalette;
import dev.echo.client.ui.ThemePalette;
import dev.echo.client.ui.TooltipRenderer;
import dev.echo.client.ui.animation.Easing;
import dev.echo.client.ui.animation.Tween;
import dev.echo.render.texture.TextureView;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

/**
 * Original alternate category-panel screen, including draggable category order,
 * feature binding, configuration/friend pages, and script-driven card updates.
 * Dispatch, key-domain and synthetic-guard applicability remain explicit in the
 * accompanying recovery receipt; this is a full conditional source candidate.
 */
public class CategoryPanelScreen extends EchoScreen implements MinecraftAccess {
    private final Tween visibility;
    private final NavigationSidebar navigation;
    private final ConfigsPanel configs;
    private final FriendsPanel friends;
    private final List<CategoryPanel> panels;
    public int draggedPanelIndex;
    public FeatureCard bindingCard;
    private boolean initialized;
    private boolean layoutLocked;
    private float dragOffsetX;
    private float dragOffsetY;
    private final float[] slotX;
    private final float[] slotY;
    private float scrollOffset;
    private float maximumScroll;
    private static final float PAGE_SCROLL_STEP = 100.0F;
    public ClickGUIFeature clickGuiFeature;
    // The original class initializer decrypts and interns this exact string.
    private static final String AUTOSAVE_PROFILE = "_autosave";

    /** Preserves the original allocation/publication sequence and zero-filled slots. */
    public CategoryPanelScreen() {
        super(Text.literal(""));
        visibility = new Tween(Easing.EASE_OUT_BACK, 50.0);
        navigation = new NavigationSidebar();
        configs = new ConfigsPanel(this);
        friends = new FriendsPanel(this);
        panels = new ObjectArrayList<>();
        draggedPanelIndex = -1;
        bindingCard = null;
        initialized = false;
        layoutLocked = false;
        slotX = new float[Category.values().length];
        slotY = new float[Category.values().length];
        scrollOffset = 0.0F;
        maximumScroll = 0.0F;
    }

    @Override
    protected void init() {
        super.init();
        if (!initialized) {
            clickGuiFeature = (ClickGUIFeature) Echo.featureManager.findFeature(ClickGUIFeature.class);
            Category[] categories = Category.values();
            for (int index = 0; index < categories.length; index++) {
                panels.add(new CategoryPanel(this, categories[index], panelSlotX(index),
                        ThemePalette.SCREEN_PADDING, index));
            }
            initialized = true;
        }
        visibility.start(0.0, 1.0);
        EventBus.register(this);
    }

    public float panelSlotX(int index) {
        return slotX[index];
    }

    public float panelSlotY(int index) {
        return slotY[index];
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        if (visibility.startValue() == 1.0 && visibility.isFinished()) {
            close();
            return;
        }
        int logicalX = (int) toUiX(mouseX);
        int logicalY = (int) toUiY(mouseY);
        layoutPanels();
        updateDraggedPanel(logicalX, logicalY);
        updatePanelPositions();
        renderPanels(logicalX, logicalY, (float) visibility.value());
    }

    /** Row heights are calculated before publishing slots and before clamping scroll. */
    private void layoutPanels() {
        int columns = Math.max(1, (int) ((GuiDimensions.width() - 24)
                / (ThemePalette.PANEL_WIDTH + ThemePalette.PANEL_SPACING)));
        float rowWidth = Math.min(panels.size(), columns) * ThemePalette.PANEL_WIDTH
                + (Math.min(panels.size(), columns) - 1) * ThemePalette.PANEL_SPACING;
        float rowStartX = (GuiDimensions.width() - rowWidth) / 2.0F;
        List<Float> rowHeights = new ArrayList<>();
        int column = 0;
        float rowHeight = 0.0F;
        for (CategoryPanel panel : panels) {
            rowHeight = Math.max(rowHeight, panel.getHeight());
            column++;
            if (column >= columns) {
                rowHeights.add(rowHeight);
                rowHeight = 0.0F;
                column = 0;
            }
        }
        if (column > 0) {
            rowHeights.add(rowHeight);
        }
        float x = rowStartX;
        float y = navigation.contentTop() - scrollOffset;
        int row = 0;
        column = 0;
        for (int index = 0; index < panels.size(); index++) {
            slotX[index] = x;
            slotY[index] = y;
            column++;
            if (column >= columns) {
                column = 0;
                x = rowStartX;
                y += rowHeights.get(row) + ThemePalette.PANEL_SPACING;
                row++;
            } else {
                x += ThemePalette.PANEL_WIDTH + ThemePalette.PANEL_SPACING;
            }
        }
        float contentHeight = navigation.contentTop();
        for (Float height : rowHeights) {
            contentHeight += height + ThemePalette.PANEL_SPACING;
        }
        contentHeight += 20.0F;
        maximumScroll = Math.max(0.0F, contentHeight - GuiDimensions.height());
        scrollOffset = Math.max(0.0F, Math.min(scrollOffset, maximumScroll));
    }

    private void updatePanelPositions() {
        for (int index = 0; index < panels.size(); index++) {
            if (draggedPanelIndex == index) {
                continue;
            }
            CategoryPanel panel = panels.get(index);
            float x = panelSlotX(index);
            float y = panelSlotY(index);
            if (x != panel.getX() || y != panel.getY()) {
                panel.getPositionTween().scrollTo(x, y);
            }
        }
    }

    @Override
    public void removed() {
        super.removed();
        EventBus.unregister(this);
        ScreenManager.focusedSetting = null;
        TooltipRenderer.reset();
        if (Echo.featureConfig != null) {
            Echo.featureConfig.saveAsync(AUTOSAVE_PROFILE);
        }
    }

    @Subscribe
    private void onScriptLoad(ScriptLoadedEvent event) {
        panels.stream().filter(panel -> panel.getCategory() == event.getFeature().getCategory())
                .findFirst().ifPresent(panel -> panel.addFeature(event.getFeature()));
    }

    @Subscribe
    private void onScriptUnload(ScriptUnloadedEvent event) {
        panels.stream().filter(panel -> panel.getCategory() == event.getFeature().getCategory())
                .findFirst().ifPresent(panel -> panel.removeFeature(event.getFeature()));
    }

    private void renderPanels(int mouseX, int mouseY, float opacity) {
        ((GameRenderControl) CLIENT.gameRenderer).renderGuiWithoutFog();
        UiRenderer renderer = Echo.uiRenderer;
        if (renderer == null) {
            MinecraftRenderState.setScreen(null);
            return;
        }
        TextureView backdrop = renderer.backdropTexture;
        if (navigation.isSelected(NavigationPage.FEATURES)) {
            for (CategoryPanel panel : panels) {
                panel.render(renderer, mouseX, mouseY, opacity, backdrop);
            }
        }
        if (navigation.isSelected(NavigationPage.CONFIGS)) {
            configs.render(renderer, GuiDimensions.width(), mouseX, mouseY, opacity,
                    backdrop, navigation.contentTop());
        }
        if (navigation.isSelected(NavigationPage.FRIENDS)) {
            friends.render(renderer, GuiDimensions.width(), mouseX, mouseY, opacity,
                    backdrop, navigation.contentTop());
        }
        navigation.renderPage(renderer, GuiDimensions.width(), GuiDimensions.height(), mouseX, mouseY, opacity);
        navigation.render(renderer, GuiDimensions.width(), mouseX, mouseY, sidebarPalette(), opacity, backdrop);
        TooltipRenderer.render(renderer, GuiDimensions.width(), GuiDimensions.height(), backdrop);
    }

    private void updateDraggedPanel(float mouseX, float mouseY) {
        if (layoutLocked) {
            draggedPanelIndex = -1;
            return;
        }
        if (draggedPanelIndex == -1) {
            return;
        }
        CategoryPanel dragged = panels.get(draggedPanelIndex);
        dragged.setX(mouseX + dragOffsetX);
        dragged.setY(mouseY + dragOffsetY);
        float centerX = mouseX + dragOffsetX + ThemePalette.PANEL_WIDTH / 2.0F;
        float centerY = mouseY + dragOffsetY + dragged.getHeight() / 2.0F;
        swapWithCloserSlot(draggedPanelIndex - 1, centerX, centerY, dragged);
        swapWithCloserSlot(draggedPanelIndex + 1, centerX, centerY, dragged);
    }

    private void swapWithCloserSlot(int index, float centerX, float centerY, CategoryPanel dragged) {
        if (index < 0 || index >= panels.size()) {
            return;
        }
        float currentDistance = PanelGeometry.squaredDistance(centerX, centerY,
                slotX[draggedPanelIndex] + ThemePalette.PANEL_WIDTH / 2.0F, slotY[draggedPanelIndex]);
        float otherDistance = PanelGeometry.squaredDistance(centerX, centerY,
                slotX[index] + ThemePalette.PANEL_WIDTH / 2.0F, slotY[index]);
        if (otherDistance < currentDistance) {
            CategoryPanel other = panels.get(index);
            other.animatePosition(panelSlotX(draggedPanelIndex), panelSlotY(draggedPanelIndex));
            other.setIndex(draggedPanelIndex);
            dragged.setIndex(index);
            Collections.swap(panels, draggedPanelIndex, index);
            draggedPanelIndex = index;
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (navigation.isSelected(NavigationPage.CONFIGS) && configs.keyPressed(input)) {
            return true;
        }
        if (navigation.isSelected(NavigationPage.FRIENDS) && friends.keyPressed(input)) {
            return true;
        }
        for (CategoryPanel panel : panels) {
            if (panel.keyPressed(input.key(), input.scancode(), input.modifiers())) {
                return true;
            }
        }
        if (input.key() == InputKey.ESCAPE.code()) {
            if (bindingCard != null) {
                if (!(bindingCard.getFeature() instanceof ClickGUIFeature)) {
                    bindingCard.getFeature().setKey(InputKey.NONE);
                }
                bindingCard = null;
                return true;
            }
            if (layoutLocked) {
                layoutLocked = false;
                return true;
            }
            visibility.start(1.0, 0.0);
            if (draggedPanelIndex != -1) {
                panels.get(draggedPanelIndex).animatePosition(
                        panelSlotX(draggedPanelIndex), panelSlotY(draggedPanelIndex));
                draggedPanelIndex = -1;
            }
            return true;
        }
        if (bindingCard != null) {
            bindingCard.getFeature().setKey(InputKey.fromCode(input.key()));
            bindingCard = null;
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        float mouseX = toUiX((float) click.x());
        float mouseY = toUiY((float) click.y());
        if (bindingCard != null) {
            bindingCard.getFeature().setKey(InputKey.fromMouseCode(click.button()));
            bindingCard = null;
            return true;
        }
        if (navigation.mouseClicked(mouseX, mouseY, click.button())) {
            return true;
        }
        if (navigation.isSelected(NavigationPage.CONFIGS)) {
            return configs.mouseClicked(mouseX, mouseY, click.button()) || super.mouseClicked(click, doubled);
        }
        if (navigation.isSelected(NavigationPage.FRIENDS)) {
            return friends.mouseClicked(mouseX, mouseY, click.button()) || super.mouseClicked(click, doubled);
        }
        if (!navigation.isSelected(NavigationPage.FEATURES)) {
            return super.mouseClicked(click, doubled);
        }
        for (int index = panels.size() - 1; index >= 0; index--) {
            CategoryPanel panel = panels.get(index);
            if (!ThemePalette.contains(panel.getX(), panel.getY(), panel.getWidth(), panel.getHeight(), mouseX, mouseY)) {
                continue;
            }
            if (panel.isLockHovered(mouseX, mouseY)) {
                if (InputKey.MOUSE_LEFT.matches(click.button())) {
                    toggleLayoutLock();
                }
                return true;
            }
            if (mouseY <= panel.getY() + ThemePalette.PANEL_HEADER_HEIGHT) {
                if (InputKey.MOUSE_LEFT.matches(click.button())) {
                    if (layoutLocked) {
                        return true;
                    }
                    dragOffsetX = panel.getX() - mouseX;
                    dragOffsetY = panel.getY() - mouseY;
                    draggedPanelIndex = index;
                    return true;
                }
                if (InputKey.MOUSE_RIGHT.matches(click.button())) {
                    panel.expanded = !panel.expanded;
                    panel.updateExpansion();
                    return true;
                }
            } else {
                return panel.mouseClicked(new Click(mouseX, mouseY, click.buttonInfo()));
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        float mouseX = toUiX((float) click.x());
        float mouseY = toUiY((float) click.y());
        if (navigation.mouseDragged(mouseX, mouseY, click.button(), width, height)) {
            return true;
        }
        if (navigation.isSelected(NavigationPage.CONFIGS)
                && configs.mouseDragged(mouseX, mouseY, click.button(), width, height)) {
            return true;
        }
        if (navigation.isSelected(NavigationPage.FRIENDS)
                && friends.mouseDragged(mouseX, mouseY, click.button(), width, height)) {
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        float mouseX = toUiX((float) click.x());
        float mouseY = toUiY((float) click.y());
        if (navigation.mouseReleased(click.button())) {
            return true;
        }
        if (navigation.isSelected(NavigationPage.CONFIGS) && configs.mouseReleased(click.button())) {
            return true;
        }
        if (navigation.isSelected(NavigationPage.FRIENDS) && friends.mouseReleased(click.button())) {
            return true;
        }
        if (navigation.isSelected(NavigationPage.FEATURES)) {
            if (draggedPanelIndex != -1) {
                panels.get(draggedPanelIndex).animatePosition(
                        panelSlotX(draggedPanelIndex), panelSlotY(draggedPanelIndex));
                draggedPanelIndex = -1;
            }
            for (CategoryPanel panel : panels) {
                panel.mouseReleased(mouseX, mouseY, click.button());
            }
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        float logicalX = toUiX((float) mouseX);
        float logicalY = toUiY((float) mouseY);
        if (navigation.isSelected(NavigationPage.CONFIGS) && configs.mouseScrolled(logicalX, logicalY, vertical)) {
            return true;
        }
        if (navigation.isSelected(NavigationPage.FRIENDS) && friends.mouseScrolled(logicalX, logicalY, vertical)) {
            return true;
        }
        if (navigation.isSelected(NavigationPage.FEATURES)) {
            for (CategoryPanel panel : panels) {
                if (panel.expanded && ThemePalette.contains(panel.getX(), panel.getY(),
                        panel.getWidth(), panel.getHeight(), logicalX, logicalY)
                        && panel.mouseScrolled(logicalX, logicalY, vertical)) {
                    return true;
                }
            }
            if (vertical != 0.0 && maximumScroll > 0.0F) {
                scrollOffset = Math.max(0.0F, Math.min(maximumScroll, scrollOffset - (float) vertical * PAGE_SCROLL_STEP));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (navigation.isSelected(NavigationPage.CONFIGS) && configs.charTyped(input)) {
            return true;
        }
        if (navigation.isSelected(NavigationPage.FRIENDS) && friends.charTyped(input)) {
            return true;
        }
        if (ScreenManager.focusedSetting != null) {
            for (CategoryPanel panel : panels) {
                panel.charTyped((char) input.codepoint(), input.modifiers());
            }
            return true;
        }
        return super.charTyped(input);
    }

    private static SidebarPalette sidebarPalette() {
        return new SidebarPalette(ThemePalette.TOGGLE_ON, ThemePalette.PANEL_FOREGROUND,
                ThemePalette.ROW_BACKGROUND, ThemePalette.ROW_HOVER, ThemePalette.PRIMARY_TEXT,
                ThemePalette.TAB_ACTIVE_TEXT);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /** The original argument is intentionally unused: this is a screen-wide lock. */
    public boolean isLayoutLocked(CategoryPanel panel) {
        return layoutLocked;
    }

    private void toggleLayoutLock() {
        layoutLocked = !layoutLocked;
    }
}
