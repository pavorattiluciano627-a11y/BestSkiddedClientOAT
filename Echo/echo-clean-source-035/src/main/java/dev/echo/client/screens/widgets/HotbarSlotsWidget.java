package dev.echo.client.screens.widgets;

import dev.echo.client.input.InputKey;
import dev.echo.render.FontAsset;
import dev.echo.client.render.UiRenderer;
import dev.echo.client.setting.HotbarSlotsSetting;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.utils.strings.Concat;
import dev.echo.client.ui.ThemePalette;
import dev.echo.client.ui.TooltipRenderer;
import dev.echo.client.ui.animation.Easing;
import dev.echo.client.ui.animation.Tween;
import java.awt.Color;

/** Original nine-slot multi-selection row, with per-click highlight decay. */
public class HotbarSlotsWidget implements SettingWidget {
    private static final float SLOT_GAP = 1.5f;
    private static final float SLOT_ROW_PADDING = 1.5f;
    private final HotbarSlotsSetting setting;
    private float x;
    private float y;
    private int lastClickedSlot = -1;
    private final Tween clickHighlight;

    /** Retains the supplied setting and initializes the original zero highlight. */
    public HotbarSlotsWidget(HotbarSlotsSetting setting) {
        clickHighlight = new Tween(Easing.EASE_OUT_CUBIC, 150.0);
        this.setting = setting;
        clickHighlight.start(0.0, 0.0);
    }

    private float innerWidth(float rowWidth) {
        return rowWidth - 2.0f * ThemePalette.CONTENT_PADDING;
    }

    private float slotWidth(float rowWidth) {
        return (innerWidth(rowWidth) - 12.0f) / 9.0f;
    }

    /** Draws the original shared row, count label, and nine slots in index order. */
    @Override
    public void render(UiRenderer renderer, float x, float y, int mouseX, int mouseY, float opacity) {
        this.x = x;
        this.y = y;
        float rowX = x + ThemePalette.ROW_INDENT;
        float rowWidth = ThemePalette.PANEL_WIDTH - ThemePalette.ROW_INDENT - 4.0f;
        float height = height();
        float slotSize = slotWidth(rowWidth);
        float slotY = y + ThemePalette.ROW_HEIGHT + SLOT_ROW_PADDING;
        float firstSlotX = rowX + ThemePalette.CONTENT_PADDING;
        float highlight = (float) clickHighlight.value();
        boolean hovered = ThemePalette.contains(rowX, y, rowWidth, height, mouseX, mouseY);
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
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, setting.getLabel(), rowX + ThemePalette.CONTENT_PADDING,
                y + ThemePalette.ROW_HEIGHT / 2.0f - 4.0f, 6.0f,
                ThemePalette.multiplyAlpha(ThemePalette.PRIMARY_TEXT, opacity));

        int selectedCount = 0;
        for (int slot = 0; slot < 9; slot++) {
            if (setting.isSelected(slot)) {
                selectedCount++;
            }
        }
        // Helper.resolve constructed a fresh code-point-segmented suffix at this call site.
        Concat countLabel = Concat.sequences(Concat.integer(selectedCount), CatalogText.literal("/9"));
        float countWidth = FontAsset.INTER_SEMI_BOLD.measureText(countLabel, 6.0f);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, countLabel,
                rowX + rowWidth - ThemePalette.CONTENT_PADDING - countWidth,
                y + ThemePalette.ROW_HEIGHT / 2.0f - 4.0f, 6.0f,
                ThemePalette.multiplyAlpha(ThemePalette.TOGGLE_ON.getRGB(), opacity));

        for (int slot = 0; slot < 9; slot++) {
            float slotX = firstSlotX + slot * (slotSize + SLOT_GAP);
            boolean slotHovered = ThemePalette.contains(slotX, slotY, slotSize, slotSize, mouseX, mouseY);
            Color color = setting.isSelected(slot) ? ThemePalette.TOGGLE_ON : ThemePalette.SLOT_BACKGROUND;
            if (slotHovered) {
                color = ThemePalette.blend(color, ThemePalette.ROW_HOVER, 0.3f);
            }
            if (slot == lastClickedSlot) {
                color = ThemePalette.blend(color, ThemePalette.TOGGLE_KNOB, highlight * 0.25f);
            }
            renderer.drawRoundedRect(slotX, slotY, slotSize, slotSize, ThemePalette.SLOT_RADIUS,
                    ThemePalette.multiplyAlpha(color.getRGB(), opacity));
        }
    }

    /** Includes both the label row and the padded square slot row. */
    @Override
    public float height() {
        float rowWidth = ThemePalette.PANEL_WIDTH - ThemePalette.ROW_INDENT - 4.0f;
        return ThemePalette.ROW_HEIGHT + SLOT_ROW_PADDING + slotWidth(rowWidth) + SLOT_ROW_PADDING;
    }

    /** Toggles the first hit slot, then restarts its one-to-zero visual highlight. */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!InputKey.MOUSE_LEFT.matches(button)) {
            return false;
        }
        float rowWidth = ThemePalette.PANEL_WIDTH - ThemePalette.ROW_INDENT - 4.0f;
        float rowX = x + ThemePalette.ROW_INDENT;
        float slotSize = slotWidth(rowWidth);
        float slotY = y + ThemePalette.ROW_HEIGHT + SLOT_ROW_PADDING;
        float firstSlotX = rowX + ThemePalette.CONTENT_PADDING;
        for (int slot = 0; slot < 9; slot++) {
            float slotX = firstSlotX + slot * (slotSize + SLOT_GAP);
            if (ThemePalette.contains(slotX, slotY, slotSize, slotSize, (float) mouseX, (float) mouseY)) {
                setting.toggle(slot);
                lastClickedSlot = slot;
                clickHighlight.start(1.0, 0.0);
                return true;
            }
        }
        return false;
    }

    /** The original slot selector does not perform a release operation. */
    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        // Exact original getConnection(DJDI)V body: return.
    }

    /** Returns the retained setting reference. */
    @Override
    public HotbarSlotsSetting setting() {
        return setting;
    }
}
