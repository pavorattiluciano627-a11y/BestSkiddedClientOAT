package dev.echo.client.hud;

import dev.echo.client.events.render.HudRenderEvent;
import dev.echo.client.render.UiRenderer;
import dev.echo.render.FontAsset;
import dev.echo.render.texture.TextureView;

/**
 * Script-facing snapshot of a HUD render event and its drawing operations.
 * Both graphics references are borrowed from the frame; this object
 * neither allocates nor releases them. Binding/lifetime applicability is recorded
 * separately in the original-member and provider receipts.
 */
public class HudSurfacePreview {
    private final UiRenderer renderer;
    private final float tickDelta;
    private final TextureView texture;

    /** Captures the original event getters in renderer, tick-delta, texture order. */
    public HudSurfacePreview(HudRenderEvent event) {
        renderer = event.getRenderer();
        tickDelta = event.getTickDelta();
        texture = event.getTexture();
    }

    public float getTickDelta() {
        return tickDelta;
    }

    public TextureView getTexture() {
        return texture;
    }

    public void drawRect(float x, float y, float width, float height, int argb) {
        renderer.drawRoundedRect(x, y, width, height, 0.0f, argb);
    }

    public void drawRoundedRect(float x, float y, float width, float height, float radius, int argb) {
        renderer.drawRoundedRect(x, y, width, height, radius, argb);
    }

    public void drawText(FontAsset font, String text, float x, float y, float size, int argb) {
        renderer.drawText(font, text, x, y, size, argb);
    }

    public void drawTextWithShadow(FontAsset font, String text, float x, float y, float size, int argb) {
        renderer.drawTextWithShadow(font, text, x, y, size, argb);
    }

    public void drawBackdrop(TextureView image, float x, float y, float width, float height,
                             float radius, float opacity) {
        renderer.drawBackdrop(image, x, y, width, height, radius, opacity);
    }

    public void pushScissor(float x, float y, float width, float height) {
        renderer.pushScissor(x, y, width, height);
    }

    public void popScissor() {
        renderer.popScissor();
    }
}
