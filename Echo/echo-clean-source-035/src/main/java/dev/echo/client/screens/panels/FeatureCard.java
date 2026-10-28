package dev.echo.client.screens.panels;

import dev.echo.client.features.Feature;
import dev.echo.client.input.InputKey;
import dev.echo.client.render.UiRenderer;
import dev.echo.client.screens.CategoryPanelScreen;
import dev.echo.client.screens.widgets.*;
import dev.echo.client.setting.*;
import dev.echo.client.ui.ThemePalette;
import dev.echo.client.ui.TooltipRenderer;
import dev.echo.client.ui.animation.Easing;
import dev.echo.client.ui.animation.Tween;
import dev.echo.render.FontAsset;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.Click;

/**
 * One feature row and its original ordered setting widgets (hack/TableModel).
 * Complete conditional source; caller, sentinel and cache obligations are in the handoff.
 */
public class FeatureCard {
    private float height;
    private float width;
    private float x;
    private float y;
    private final Feature feature;
    private final CategoryPanelScreen screen;
    private final List<SettingWidget> widgets;
    private final Tween expansion;
    public boolean expanded;
    private static final float VISIBILITY_ICON_SIZE = 9.0f;
    private static final float ICON_GAP = 4.0f;
    private float keyLeft;
    private float keyRight;
    // The original class initializer interns this decoded field.
    private static final String CAPTURE_LABEL = "...";

    public FeatureCard(CategoryPanelScreen screen, Feature feature) {
        height = ThemePalette.FEATURE_ROW_HEIGHT;
        widgets = new ObjectArrayList<>();
        expansion = new Tween(Easing.EASE_OUT_CUBIC, 300.0);
        expanded = false;
        this.feature = feature;
        this.screen = screen;
        for (Setting setting : feature.settings) {
            // The original type switch rejects null but appends null for an unknown type.
            widgets.add(switch (Objects.requireNonNull(setting)) {
                case BooleanSetting value -> new BooleanToggleWidget(value);
                case IntSetting value -> new IntegerSliderWidget(value);
                case FloatSetting value -> new FloatSliderWidget(value);
                case ChoiceSetting value -> new ChoiceWidget(value);
                case KeybindSetting value -> new KeybindWidget(value);
                case ColorSetting value -> new ColorPickerWidget(value);
                case MultiChoiceSetting value -> new BooleanGroupWidget(value);
                case FloatRangeSetting value -> new RangeSliderWidget(value);
                case TextSetting value -> new TextSettingWidget(value);
                case RegistryListSetting<?> value -> new RegistryMultiSelectWidget<>(value);
                case HotbarSlotsSetting value -> new HotbarSlotsWidget(value);
                default -> null;
            });
        }
        expansion.start(ThemePalette.FEATURE_ROW_HEIGHT, ThemePalette.FEATURE_ROW_HEIGHT);
    }

    private float preferredHeight() {
        if (!expanded) {
            return ThemePalette.FEATURE_ROW_HEIGHT;
        }
        float result = ThemePalette.FEATURE_ROW_HEIGHT;
        for (SettingWidget widget : widgets) {
            if (widget == null || !widget.setting().isVisible()) {
                continue;
            }
            result += widget.height() + ThemePalette.SEARCH_GAP;
        }
        return result;
    }

    public void updateExpansion() {
        float target = preferredHeight();
        if (target != expansion.targetValue()) {
            expansion.setTarget(target);
        }
    }

    public void layout(float x, float y) {
        this.x = x;
        this.y = y;
        width = ThemePalette.PANEL_WIDTH;
        updateExpansion();
        height = (float) expansion.value();
    }

    public void render(UiRenderer renderer, int mouseX, int mouseY, float opacity, boolean layoutLocked) {
        boolean hovered = ThemePalette.contains(x, y, width, ThemePalette.FEATURE_ROW_HEIGHT, mouseX, mouseY);
        if (hovered && !layoutLocked) {
            CharSequence description = feature.getDescription();
            if (description != null && description.length() > 0) {
                TooltipRenderer.offer(feature, feature.getName(), description, mouseX, mouseY);
            }
        }
        if (layoutLocked) {
            renderVisibilityRow(renderer, opacity, hovered);
            return;
        }
        if (feature.isEnabled()) {
            renderer.drawRoundedRect(x + 2.0f, y, width - 4.0f, ThemePalette.FEATURE_ROW_HEIGHT,
                    ThemePalette.ROW_RADIUS, ThemePalette.multiplyAlpha(ThemePalette.ROW_ACCENT.getRGB(), opacity));
        }
        if (hovered) {
            renderer.drawRoundedRect(x + 2.0f, y, width - 4.0f, ThemePalette.FEATURE_ROW_HEIGHT,
                    ThemePalette.ROW_RADIUS, ThemePalette.multiplyAlpha(ThemePalette.FEATURE_CARD_BACKGROUND.getRGB(), opacity));
        }
        int labelColor = ThemePalette.multiplyAlpha(
                ThemePalette.toArgb(feature.isEnabled() ? ThemePalette.TAB_ACTIVE_TEXT : ThemePalette.PRIMARY_TEXT), opacity);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, feature.getName(), x + 2.0f + ThemePalette.CONTENT_PADDING,
                y + ThemePalette.FEATURE_ROW_HEIGHT / 2.0f - 5.0f, 8.0f, labelColor);

        boolean capturing = screen.bindingCard == this;
        boolean unbound = !capturing && feature.getKey() == InputKey.NONE;
        String keyLabel = "[" + (capturing ? CAPTURE_LABEL : unbound ? " " : feature.getKey().displayLabel()) + "]";
        float keyWidth = FontAsset.INTER_SEMI_BOLD.measureText(keyLabel, 7.0f);
        float keyX = x + width - keyWidth - 10.0f;
        keyLeft = keyX - 1.0f;
        keyRight = keyX + keyWidth + 1.0f;
        boolean keyHovered = !capturing && ThemePalette.contains(keyLeft, y, keyWidth + 2.0f,
                ThemePalette.FEATURE_ROW_HEIGHT, mouseX, mouseY);
        if (!unbound || keyHovered) {
            int keyColor = capturing || keyHovered
                    ? ThemePalette.multiplyAlpha(ThemePalette.SELECTED_OPTION_TEXT, opacity) : labelColor;
            renderer.drawText(FontAsset.INTER_SEMI_BOLD, keyLabel, keyX,
                    y + ThemePalette.FEATURE_ROW_HEIGHT / 2.0f - 4.0f, 7.0f, keyColor);
            if (!capturing && !unbound && FontAsset.LUCIDE != null) {
                String glyph = feature.isHoldMode() ? "\ue1e8" : "\ue18b";
                renderer.drawText(FontAsset.LUCIDE, glyph,
                        keyX - FontAsset.LUCIDE.measureText(glyph, 8.0f) - ICON_GAP,
                        y + ThemePalette.FEATURE_ROW_HEIGHT / 2.0f - 4.0f, 8.0f, labelColor);
            }
        }
        if (!feature.settings.isEmpty()) {
            String glyph = expanded ? "\u25bc" : "\u25b6";
            float glyphWidth = FontAsset.INTER_SEMI_BOLD.measureText(glyph, 8.0f);
            renderer.drawText(FontAsset.INTER_SEMI_BOLD, glyph, x + width - glyphWidth - ICON_GAP,
                    y + ThemePalette.FEATURE_ROW_HEIGHT / 2.0f - 5.0f, 8.0f, labelColor);
        }
        if (height > ThemePalette.FEATURE_ROW_HEIGHT) {
            float widgetY = y + ThemePalette.FEATURE_ROW_HEIGHT + ThemePalette.SEARCH_GAP;
            renderer.pushScissor(x, widgetY, ThemePalette.PANEL_WIDTH, height - ThemePalette.FEATURE_ROW_HEIGHT);
            for (SettingWidget widget : widgets) {
                if (widget == null || !widget.setting().isVisible()) {
                    continue;
                }
                widget.render(renderer, x, widgetY, mouseX, mouseY, opacity);
                widgetY += widget.height() + ThemePalette.SEARCH_GAP;
            }
            renderer.popScissor();
        }
        // Original post-render geometry mutation; layout() restores it on the next pass.
        x -= 2.0f;
        width -= 4.0f;
    }

    private void renderVisibilityRow(UiRenderer renderer, float opacity, boolean hovered) {
        if (hovered) {
            renderer.drawRoundedRect(x + 2.0f, y, width - 4.0f, ThemePalette.FEATURE_ROW_HEIGHT,
                    ThemePalette.ROW_RADIUS, ThemePalette.multiplyAlpha(ThemePalette.FEATURE_CARD_BACKGROUND.getRGB(), opacity));
        }
        boolean visible = feature.isVisible();
        int labelColor = visible ? ThemePalette.multiplyAlpha(ThemePalette.SELECTED_OPTION_TEXT, opacity)
                : ThemePalette.multiplyAlpha(ThemePalette.PRIMARY_TEXT, opacity);
        float labelX = x + 2.0f + ThemePalette.CONTENT_PADDING;
        if (FontAsset.LUCIDE != null) {
            String glyph = visible ? "\ue11c" : "\ue13d";
            float glyphWidth = FontAsset.LUCIDE.measureText(glyph, VISIBILITY_ICON_SIZE);
            float glyphY = y + ThemePalette.FEATURE_ROW_HEIGHT / 2.0f - 4.5f;
            renderer.drawText(FontAsset.LUCIDE, glyph, labelX, glyphY, VISIBILITY_ICON_SIZE, labelColor);
            labelX += glyphWidth + ICON_GAP;
        }
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, feature.getName(), labelX,
                y + ThemePalette.FEATURE_ROW_HEIGHT / 2.0f - 5.0f, 8.0f, labelColor);
    }

    public boolean isKeyHovered(float mouseX) {
        return mouseX >= keyLeft && mouseX <= keyRight;
    }

    public boolean mouseClicked(Click click) {
        if (!expanded) {
            return false;
        }
        float widgetY = y + ThemePalette.FEATURE_ROW_HEIGHT + ThemePalette.SEARCH_GAP;
        for (SettingWidget widget : widgets) {
            if (widget == null || !widget.setting().isVisible()) {
                continue;
            }
            float widgetHeight = widget.height();
            if (click.y() >= widgetY && click.y() <= widgetY + widgetHeight
                    && widget.mouseClicked(click.x(), click.y(), click.button())) {
                return true;
            }
            widgetY += widgetHeight + ThemePalette.SEARCH_GAP;
        }
        return false;
    }

    /** Releases every non-null widget, including widgets that have since become hidden. */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!expanded) {
            return false;
        }
        for (SettingWidget widget : widgets) {
            if (widget != null) {
                widget.mouseReleased(mouseX, mouseY, button);
            }
        }
        return false; // The original reports unconsumed after delivering the releases.
    }

    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (!expanded) {
            return false;
        }
        for (SettingWidget widget : widgets) {
            if (widget != null && widget.setting().isVisible() && widget.keyPressed(key, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    public boolean charTyped(char character, int modifiers) {
        if (!expanded) {
            return false;
        }
        for (SettingWidget widget : widgets) {
            if (widget != null && widget.setting().isVisible()) {
                widget.charTyped(character, modifiers);
            }
        }
        return false; // Character forwarding does not consume the parent stream.
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double vertical) {
        if (!expanded) {
            return false;
        }
        float widgetY = y + ThemePalette.FEATURE_ROW_HEIGHT + ThemePalette.SEARCH_GAP;
        for (SettingWidget widget : widgets) {
            if (widget == null || !widget.setting().isVisible()) {
                continue;
            }
            float widgetHeight = widget.height();
            if (mouseY >= widgetY && mouseY <= widgetY + widgetHeight
                    && mouseX >= x && mouseX <= x + ThemePalette.PANEL_WIDTH
                    && widget.mouseScrolled(mouseX, mouseY, vertical)) {
                return true;
            }
            widgetY += widgetHeight + ThemePalette.SEARCH_GAP;
        }
        return false;
    }

    public float getHeight() { return height; }
    public float getWidth() { return width; }
    public float getX() { return x; }
    public float getY() { return y; }
    public Feature getFeature() { return feature; }
}
