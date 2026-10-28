package dev.echo.client.screens.dashboard;

import dev.echo.render.FontAsset;

/** Original logical-coordinate drawing helpers on the existing renderer. */
public interface DashboardPainter {
    void backdropPanel(float x, float y, float width, float height, float radius,
                       int fill, float borderWidth, int border);
    void panel(float x, float y, float width, float height, float radius,
               int fill, float borderWidth, int border);
    void roundedRect(float x, float y, float width, float height, float radius, int color);
    float textWidth(FontAsset font, CharSequence text, float size);
    void text(FontAsset font, CharSequence text, float x, float y, float size, int color);
    void icon(DashboardIcon icon, float x, float y, float width, float height, float opacity, int tint);
}
