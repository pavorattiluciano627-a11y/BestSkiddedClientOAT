package dev.echo.client.screens.dashboard;

import dev.echo.render.FontAsset;

/** Original common dashboard drawing sequences, with the genuine font provider. */
public final class DashboardWidgetRenderer {
    private final DashboardPainter painter;

    public DashboardWidgetRenderer(DashboardPainter painter) { this.painter = painter; }

    public void section(float x, float y, float width, float height, float headerHeight, CharSequence title) {
        painter.backdropPanel(x, y, width, height, 8.0f, -234091508, 0.0f, 0);
        painter.panel(x, y, width, headerHeight, 6.0f, -15000805, 1.0f, -14079703);
        painter.text(FontAsset.INTER, title, x + 15.0f, y + 10.0f, 13.0f, -4144960);
    }

    public void borderedPanel(float x, float y, float width, float height, float radius, int fill, int border) {
        painter.panel(x, y, width, height, radius, fill, 1.0f, border);
    }

    public void centeredText(FontAsset font, CharSequence text, float centerX, float y, float size, int color) {
        float width = painter.textWidth(font, text, size);
        painter.text(font, text, centerX - width * 0.5f, y, size, color);
    }

    public void rightAlignedText(FontAsset font, CharSequence text, float right, float y, float size, int color) {
        float width = painter.textWidth(font, text, size);
        painter.text(font, text, right - width, y, size, color);
    }

    public void editorScrollbar(int lineCount, float displayedFirstLine) {
        int visibleLines = 35;
        if (lineCount <= visibleLines) return;
        float top = 104.0f;
        float trackHeight = 828.0f;
        float thumbHeight = Math.max(40.0f, trackHeight * visibleLines / lineCount);
        float scrollRange = Math.max(1, lineCount - visibleLines);
        float thumbY = top + (trackHeight - thumbHeight) * displayedFirstLine / scrollRange;
        painter.roundedRect(1883.0f, top, 3.0f, trackHeight, 1.5f, 0x0affffff);
        painter.roundedRect(1882.0f, thumbY, 5.0f, thumbHeight, 2.5f, 0x40ffffff);
    }

    public void smallChevron(DashboardIcon icon, float centerX, float centerY, boolean enabled) {
        int tint = enabled ? -6710887 : 0x40ffffff;
        painter.icon(icon, centerX - 3.75f, centerY - 2.25f, 7.5f, 4.5f, 1.0f, tint);
    }
}
