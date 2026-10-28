package dev.echo.client.ui;

import dev.echo.client.render.UiRenderer;
import dev.echo.render.FontAsset;
import dev.echo.render.texture.TextureView;
import java.util.ArrayList;
import java.util.List;

/** Shared hover-offer state and delayed tooltip rendering, with original slice/metric ordering. */
public final class TooltipRenderer {
    private static final long HOVER_DELAY_MILLIS = 250L;
    private static final float FONT_SIZE = 7.0f;
    private static final float MAX_LINE_WIDTH = 220.0f;
    private static final float HORIZONTAL_PADDING = 5.0f;
    private static final float VERTICAL_PADDING = 4.0f;
    private static final float CURSOR_OFFSET = 0.0f;
    private static final float SCREEN_MARGIN = 4.0f;
    private static final float LINE_HEIGHT = 9.0f;

    private static Object hoveredOwner;
    private static long hoverStartedAtMillis;
    private static Object pendingOwner;
    private static CharSequence pendingDescription;
    private static int pendingMouseX;
    private static int pendingMouseY;

    private TooltipRenderer() {
    }

    /** The label argument is retained but, as in the original, is not inspected or rendered. */
    public static void offer(Object owner, CharSequence label, CharSequence description, int mouseX, int mouseY) {
        if (owner == null || description == null || description.length() == 0) {
            return;
        }
        pendingOwner = owner;
        pendingDescription = description;
        pendingMouseX = mouseX;
        pendingMouseY = mouseY;
    }

    public static void render(UiRenderer renderer, int viewportWidth, int viewportHeight, TextureView backdrop) {
        Object owner = pendingOwner;
        CharSequence description = pendingDescription;
        int mouseX = pendingMouseX;
        int mouseY = pendingMouseY;
        pendingOwner = null;
        pendingDescription = null;
        if (owner == null) {
            hoveredOwner = null;
            return;
        }
        long now = System.currentTimeMillis();
        if (owner != hoveredOwner) {
            hoveredOwner = owner;
            hoverStartedAtMillis = now;
            return;
        }
        if (now - hoverStartedAtMillis < HOVER_DELAY_MILLIS) {
            return;
        }
        FontAsset font = FontAsset.INTER_SEMI_BOLD;
        if (font == null) {
            return;
        }
        List<CharSequence> lines = wrapLines(description, font, FONT_SIZE, MAX_LINE_WIDTH);
        float contentWidth = 0.0f;
        for (CharSequence line : lines) {
            contentWidth = Math.max(contentWidth, font.measureText(line, FONT_SIZE));
        }
        if (contentWidth <= 0.0f) {
            return;
        }
        float width = contentWidth + HORIZONTAL_PADDING * 2.0f;
        float contentHeight = (float) lines.size() * LINE_HEIGHT;
        float height = VERTICAL_PADDING * 2.0f + contentHeight;
        float x = mouseX + CURSOR_OFFSET;
        float y = mouseY - height - CURSOR_OFFSET;
        if (x + width > viewportWidth - SCREEN_MARGIN) {
            x = mouseX - width - CURSOR_OFFSET;
        }
        if (y < SCREEN_MARGIN) {
            y = mouseY + CURSOR_OFFSET;
        }
        if (x < SCREEN_MARGIN) {
            x = SCREEN_MARGIN;
        }
        if (y + height > viewportHeight - SCREEN_MARGIN) {
            y = viewportHeight - height - SCREEN_MARGIN;
        }
        int borderColor = ThemePalette.TOOLTIP_BORDER.getRGB();
        int backgroundColor = ThemePalette.TOOLTIP_BACKGROUND.getRGB();
        int textColor = ThemePalette.SELECTED_OPTION_TEXT.getRGB();
        float radius = ThemePalette.SLOT_RADIUS;
        if (backdrop != null) {
            renderer.drawBackdrop(backdrop, x, y, width, height, radius, 1.0f);
        }
        renderer.drawRoundedRect(x, y, width, height, radius, borderColor);
        renderer.drawRoundedRect(x + 1.0f, y + 1.0f, width - 2.0f, height - 2.0f,
                Math.max(0.0f, radius - 1.0f), backgroundColor);
        float textX = x + HORIZONTAL_PADDING;
        float textY = y + VERTICAL_PADDING;
        for (CharSequence line : lines) {
            renderer.drawText(font, line, textX, textY, FONT_SIZE, textColor);
            textY += LINE_HEIGHT;
        }
    }

    public static void reset() {
        hoveredOwner = null;
        hoverStartedAtMillis = 0L;
        pendingOwner = null;
        pendingDescription = null;
    }

    private static List<CharSequence> wrapLines(CharSequence text, FontAsset font, float size, float maximumWidth) {
        List<CharSequence> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return lines;
        }
        int cursor = 0;
        int lineStart = -1;
        int lineEnd = -1;
        while (cursor < text.length()) {
            while (cursor < text.length() && text.charAt(cursor) == ' ') {
                cursor++;
            }
            if (cursor >= text.length()) {
                break;
            }
            int wordStart = cursor;
            while (cursor < text.length() && text.charAt(cursor) != ' ') {
                cursor++;
            }
            int wordEnd = cursor;
            if (lineStart < 0) {
                lineStart = wordStart;
                lineEnd = wordEnd;
                continue;
            }
            CharSequence candidate = text.subSequence(lineStart, wordEnd);
            if (font.measureText(candidate, size) <= maximumWidth) {
                lineEnd = wordEnd;
                continue;
            }
            lines.add(text.subSequence(lineStart, lineEnd));
            lineStart = wordStart;
            lineEnd = wordEnd;
        }
        if (lineStart >= 0) {
            lines.add(text.subSequence(lineStart, lineEnd));
        }
        return lines;
    }
}
