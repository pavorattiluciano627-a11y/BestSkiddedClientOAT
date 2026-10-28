package dev.echo.client.screens;

import dev.echo.client.Echo;
import dev.echo.client.events.EventBus;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.render.HudRenderEvent;
import dev.echo.client.events.render.InGameHudEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureManager;
import dev.echo.client.features.HudFeature;
import dev.echo.client.features.hud.EchoHudElement;
import dev.echo.client.hud.HudElementLayout;
import dev.echo.client.hud.HudElementType;
import dev.echo.client.hud.HudRenderer;
import dev.echo.client.input.InputKey;
import dev.echo.client.render.GuiDimensions;
import dev.echo.client.render.UiRenderer;
import dev.echo.render.FontAsset;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/**
 * Live HUD placement editor, rendered by the subscribed HUD event.
 *
 * <p>Complete functional source candidate for ThreadStrategy. The accompanying
 * proof ledger retains the original key/guard/cache applicability conditions.
 */
public final class HudEditorScreen extends EchoScreen {
    private static final int GRID_MAJOR_INTERVAL = 10;
    private static final int GRID_COLOR = 184549375;
    private static final int GRID_MAJOR_COLOR = 419430399;
    private static final int BACKGROUND_COLOR = -1476395008;
    private static final int OUTLINE_COLOR = 1509949439;
    private static final int ACTIVE_OUTLINE_COLOR = -1;
    private static final int GUIDE_COLOR = -11756801;
    private static final float SNAP_DISTANCE = 8.0F;

    // One non-interned value for each original decoder slot, shared by instances.
    private static final String HUD_TITLE = new String("Modify HUD");
    private static final String AUTOSAVE_PROFILE = new String("_autosave");

    private HudFeature hoveredElement;
    private HudFeature draggedElement;
    private HudFeature selectedElement;
    private float snapGuideX;
    private float snapGuideY;
    private float dragOffsetX;
    private float dragOffsetY;
    private float mouseX;
    private float mouseY;

    /** Creates the HUD editor with its original title and inactive alignment guides. */
    public HudEditorScreen() {
        super(Text.literal(HUD_TITLE));
        snapGuideX = Float.NaN;
        snapGuideY = Float.NaN;
    }

    /** Initializes the vanilla screen before subscribing this exact instance. */
    @Override
    protected void init() {
        super.init();
        EventBus.register(this);
    }

    /** Removes subscriptions and interaction references before scheduling autosave. */
    @Override
    public void removed() {
        super.removed();
        EventBus.unregister(this);
        draggedElement = null;
        hoveredElement = null;
        selectedElement = null;
        if (Echo.featureConfig != null) {
            Echo.featureConfig.saveAsync(AUTOSAVE_PROFILE);
        }
    }

    /** Updates logical pointer coordinates; hover remains frozen during a drag. */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        this.mouseX = toUiX(mouseX);
        this.mouseY = toUiY(mouseY);
        if (draggedElement != null) {
            return;
        }
        hoveredElement = elementAt(this.mouseX, this.mouseY);
    }

    /** Suppresses vanilla in-game HUD drawing while this editor is subscribed. */
    @Subscribe
    public void onInGameHudRender(InGameHudEvent event) {
        event.cancel();
    }

    /** Draws the real HUD elements, editor overlays, guides and active anchor in order. */
    @Subscribe
    public void onHudRender(HudRenderEvent event) {
        UiRenderer renderer = event.getRenderer();
        float width = GuiDimensions.width();
        float height = GuiDimensions.height();
        renderer.drawBackdrop(renderer.halfResolutionBackdrop, 0.0F, 0.0F, width, height, 0.0F, 1.0F);
        renderer.drawRoundedRect(0.0F, 0.0F, width, height, 0.0F, BACKGROUND_COLOR);
        drawGrid(renderer, width, height);
        HudRenderer.INSTANCE.prepareHudFrame(renderer);
        for (HudFeature feature : enabledElements()) {
            EchoHudElement element = (EchoHudElement) feature;
            HudElementLayout layout = HudRenderer.INSTANCE.createLayout(renderer,
                    renderer.fullResolutionBackdrop, element.getScale(), element.getOpacity());
            element.renderAt(layout, feature.getX(), feature.getY());
        }
        // Query again: original rendering callbacks may change the enabled set.
        for (HudFeature feature : enabledElements()) {
            drawElementOutline(renderer, feature);
        }
        if (draggedElement != null) {
            drawSnapGuides(renderer, width, height);
        }
        HudFeature active = draggedElement != null ? draggedElement
                : selectedElement != null ? selectedElement : hoveredElement;
        if (active != null) {
            drawAnchor(renderer, active);
        }
    }

    private void drawSnapGuides(UiRenderer renderer, float width, float height) {
        if (!Float.isNaN(snapGuideX)) {
            renderer.drawRoundedRect(MathHelper.clamp(snapGuideX - 0.5F, 0.0F, width - 1.0F),
                    0.0F, 1.0F, height, 0.0F, GUIDE_COLOR);
        }
        if (!Float.isNaN(snapGuideY)) {
            renderer.drawRoundedRect(0.0F,
                    MathHelper.clamp(snapGuideY - 0.5F, 0.0F, height - 1.0F),
                    width, 1.0F, 0.0F, GUIDE_COLOR);
        }
    }

    private void drawAnchor(UiRenderer renderer, HudFeature feature) {
        float radius = Math.max(2.0F, 4.0F * HudRenderer.INSTANCE.getCanvasScale());
        float x = feature.getX() + feature.getWidth() * feature.getHorizontalAnchor();
        float y = feature.getY() + feature.getHeight() * feature.getVerticalAnchor();
        renderer.drawRoundedRect(x - radius, y - radius, radius * 2.0F, radius * 2.0F,
                radius, GUIDE_COLOR, 1.0F, ACTIVE_OUTLINE_COLOR);
    }

    private void drawGrid(UiRenderer renderer, float width, float height) {
        float spacing = 10.0F * HudRenderer.INSTANCE.getCanvasScale();
        if (spacing <= 0.0F) {
            return;
        }
        float centerX = width * 0.5F;
        float centerY = height * 0.5F;
        for (int index = -MathHelper.floor(centerX / spacing);
                centerX + index * spacing <= width; index++) {
            renderer.drawRoundedRect(centerX + index * spacing - 0.5F, 0.0F,
                    1.0F, height, 0.0F,
                    index % GRID_MAJOR_INTERVAL == 0 ? GRID_MAJOR_COLOR : GRID_COLOR);
        }
        for (int index = -MathHelper.floor(centerY / spacing);
                centerY + index * spacing <= height; index++) {
            renderer.drawRoundedRect(0.0F, centerY + index * spacing - 0.5F,
                    width, 1.0F, 0.0F,
                    index % GRID_MAJOR_INTERVAL == 0 ? GRID_MAJOR_COLOR : GRID_COLOR);
        }
    }

    private void drawElementOutline(UiRenderer renderer, HudFeature feature) {
        boolean active = feature == draggedElement || feature == hoveredElement || feature == selectedElement;
        float x = feature.getX();
        float y = feature.getY();
        float width = feature.getWidth();
        float height = feature.getHeight();
        renderer.drawRoundedRect(x, y, width, height, 6.0F, 0,
                active ? 2.0F : 1.0F, active ? ACTIVE_OUTLINE_COLOR : OUTLINE_COLOR);
        if (!active) {
            return;
        }
        CharSequence name = feature.getName();
        float scale = HudRenderer.INSTANCE.getCanvasScale();
        float fontSize = Math.max(5.5F, 11.0F * scale);
        float textWidth = renderer.measureText(FontAsset.INTER_SEMI_BOLD, name, fontSize);
        float labelHeight = 18.0F * scale;
        float labelY = Math.max(3.0F, y - 20.0F * scale);
        renderer.drawRoundedRect(x, labelY, textWidth + 12.0F * scale, labelHeight,
                4.0F * scale, -267119596);
        float fontHeight = renderer.measureFontHeight(FontAsset.INTER_SEMI_BOLD, fontSize);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, name, x + 6.0F * scale,
                labelY + (labelHeight - fontHeight) * 0.5F, fontSize, -1);
    }

    /** Captures only left-button presses; an empty press also clears the selection. */
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (!InputKey.MOUSE_LEFT.matches(click.button())) {
            return super.mouseClicked(click, doubled);
        }
        float x = toUiX((float) click.x());
        float y = toUiY((float) click.y());
        draggedElement = elementAt(x, y);
        selectedElement = draggedElement;
        if (draggedElement == null) {
            return super.mouseClicked(click, doubled);
        }
        dragOffsetX = x - draggedElement.getX();
        dragOffsetY = y - draggedElement.getY();
        return true;
    }

    /** Moves the captured element, with either Alt key bypassing alignment snapping. */
    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (draggedElement == null) {
            return super.mouseDragged(click, deltaX, deltaY);
        }
        float x = toUiX((float) click.x()) - dragOffsetX;
        float y = toUiY((float) click.y()) - dragOffsetY;
        snapGuideX = Float.NaN;
        snapGuideY = Float.NaN;
        if (!InputKey.LEFT_ALT.isPressed() && !InputKey.RIGHT_ALT.isPressed()) {
            float distance = SNAP_DISTANCE * HudRenderer.INSTANCE.getCanvasScale();
            x = snapAxis(x, draggedElement.getWidth(), true, distance);
            y = snapAxis(y, draggedElement.getHeight(), false, distance);
        }
        moveWithinViewport(draggedElement, x, y);
        return true;
    }

    /** Nudges the selected or hovered element by half a pixel, or five with Shift. */
    @Override
    public boolean keyPressed(KeyInput input) {
        HudFeature feature = selectedElement != null ? selectedElement : hoveredElement;
        if (feature == null || draggedElement != null) {
            return super.keyPressed(input);
        }
        float step = (input.hasShift() ? 10.0F : 1.0F) / 2.0F;
        float dx = input.isLeft() ? -step : input.isRight() ? step : 0.0F;
        float dy = input.isUp() ? -step : input.isDown() ? step : 0.0F;
        if (dx == 0.0F && dy == 0.0F) {
            return super.keyPressed(input);
        }
        moveWithinViewport(feature, feature.getX() + dx, feature.getY() + dy);
        return true;
    }

    private static void moveWithinViewport(HudFeature feature, float x, float y) {
        float maxX = Math.max(0.0F, GuiDimensions.width() - feature.getWidth());
        float maxY = Math.max(0.0F, GuiDimensions.height() - feature.getHeight());
        feature.setPosition(MathHelper.clamp(x, 0.0F, maxX), MathHelper.clamp(y, 0.0F, maxY));
    }

    private float snapAxis(float position, float size, boolean horizontal, float threshold) {
        List<HudFeature> elements = enabledElements();
        float nearest = threshold;
        float offset = 0.0F;
        float guide = Float.NaN;
        for (int index = -1; index < elements.size(); index++) {
            float targetPosition = 0.0F;
            float targetSize = horizontal ? GuiDimensions.width() : GuiDimensions.height();
            if (index >= 0) {
                HudFeature target = elements.get(index);
                if (target == draggedElement) {
                    continue;
                }
                targetPosition = horizontal ? target.getX() : target.getY();
                targetSize = horizontal ? target.getWidth() : target.getHeight();
            }
            for (int targetAnchor = 0; targetAnchor <= 2; targetAnchor++) {
                float targetCoordinate = targetPosition + targetSize * targetAnchor * 0.5F;
                for (int movingAnchor = 0; movingAnchor <= 2; movingAnchor++) {
                    float delta = targetCoordinate - (position + size * movingAnchor * 0.5F);
                    if (Math.abs(delta) < nearest) {
                        nearest = Math.abs(delta);
                        offset = delta;
                        guide = targetCoordinate;
                    }
                }
            }
        }
        if (horizontal) {
            snapGuideX = guide;
        } else {
            snapGuideY = guide;
        }
        return position + offset;
    }

    /** Ends an active drag on any release button while retaining keyboard selection. */
    @Override
    public boolean mouseReleased(Click click) {
        if (draggedElement == null) {
            return super.mouseReleased(click);
        }
        draggedElement = null;
        snapGuideX = Float.NaN;
        snapGuideY = Float.NaN;
        return true;
    }

    private HudFeature elementAt(float x, float y) {
        List<HudFeature> elements = enabledElements();
        for (int index = elements.size() - 1; index >= 0; index--) {
            HudFeature element = elements.get(index);
            // Keep original getter order and FCMPL/FCMPG behavior as well as inclusive edges.
            if (x < element.getX()) continue;
            if (!(x <= element.getX() + element.getWidth())) continue;
            if (y < element.getY()) continue;
            if (!(y <= element.getY() + element.getHeight())) continue;
            return element;
        }
        return null;
    }

    private static HudElementType elementType(HudFeature feature) {
        return ((EchoHudElement) feature).getElementType();
    }

    private static List<HudFeature> enabledElements() {
        List<HudFeature> result = new ArrayList<>();
        for (Feature feature : FeatureManager.allFeatures()) {
            if (feature instanceof EchoHudElement element && element.isEnabled()) {
                result.add(element);
            }
        }
        return result;
    }

    /** The original editor leaves the integrated game running. */
    @Override
    public boolean shouldPause() {
        return false;
    }
}
