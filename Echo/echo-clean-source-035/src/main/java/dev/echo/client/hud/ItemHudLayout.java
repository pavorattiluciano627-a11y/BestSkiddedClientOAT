package dev.echo.client.hud;

import dev.echo.client.render.EntityMaterialSettings;
import dev.echo.client.render.PlayerPreviewRenderer;
import dev.echo.client.render.UiRenderer;
import dev.echo.client.ui.ArgbMath;
import dev.echo.client.ui.UiBounds;
import dev.echo.render.FontAsset;
import dev.echo.render.texture.TextureView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * Coordinate, opacity and drawing state for live item/player setting previews.
 * The renderer and its atlas/preview outputs remain owned by their real providers.
 * Original key, cache and control-state applicability is recorded separately.
 */
public final class ItemHudLayout {
    private static final int ITEM_RENDER_SEED = 71;

    private final UiRenderer renderer;
    private final float originX;
    private final float originY;
    private final float scale;
    private final float opacity;
    private final float mouseX;
    private final float mouseY;
    private final float width;
    private final float height;
    private final int index;
    private final float lineWidth;
    private final float modelHeight;
    private final float modelBaseY;

    public ItemHudLayout(UiRenderer renderer, float originX, float originY, float scale,
                         float width, float height, float opacity, int index,
                         float mouseX, float mouseY) {
        this.renderer = renderer;
        this.originX = originX;
        this.originY = originY;
        this.scale = scale;
        this.width = width;
        this.height = height;
        this.opacity = Math.max(0.0f, Math.min(1.0f, opacity));
        this.index = index;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        modelHeight = Math.min(height * 0.64f, height - 96.0f);
        modelBaseY = height - Math.max(38.0f, height * 0.17f);
        lineWidth = Math.max(0.75f, modelHeight / 32.0f * 0.14f);
    }

    public void drawPlayer() {
        drawPlayerPreview(null, 0);
    }

    public void drawEntityMaterial(EntityMaterialSettings material, int teamColor) {
        drawPlayerPreview(material, teamColor);
    }

    private void drawPlayerPreview(EntityMaterialSettings material, int teamColor) {
        PlayerPreviewRenderer.requestRender(width, height, scale * 2.0f, modelHeight,
                modelBaseY, mouseX, mouseY, material, teamColor);
        drawPlayerGroundShadow();
        drawPlayerTexture(PlayerPreviewRenderer.getBaseTexture());
        if (material != null) {
            drawPlayerTexture(PlayerPreviewRenderer.getMaterialTexture());
        }
    }

    private void drawPlayerTexture(TextureView texture) {
        if (texture == null) {
            return;
        }
        float topV = texture.yFlipped ? 1.0f : 0.0f;
        renderer.drawImage(texture, toX(0.0f), toY(0.0f), scaled(width), scaled(height),
                0.0f, opacity, 0.0f, topV, 1.0f, 1.0f - topV);
    }

    public UiBounds getPlayerBounds() {
        return PlayerPreviewRenderer.getBounds();
    }

    public float getPlayerEyeY() {
        return PlayerPreviewRenderer.getEyeY();
    }

    public float getPlayerTop() {
        return modelBaseY - modelHeight;
    }

    private void drawPlayerGroundShadow() {
        float shadowWidth = modelHeight * 0.46f;
        renderer.drawShadow(toX(width * 0.5f - shadowWidth * 0.5f), toY(modelBaseY - 5.0f),
                scaled(shadowWidth), scaled(10.0f), scaled(5.0f), scaled(10.0f),
                withOpacity(0x8c000000));
    }

    public void drawItem(ItemStack item, UiBounds bounds) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (item.isEmpty() || player == null) {
            drawEmptyItem(bounds);
            return;
        }
        // UiRenderer routes this exact stack/entity/seed through ItemTextureCache.
        // Its published atlas texture and UV entry are used by the real draw call.
        renderer.drawItem(item, player, ITEM_RENDER_SEED, toX(bounds.x()), toY(bounds.y()),
                scaled(bounds.width()), opacity);
    }

    private void drawEmptyItem(UiBounds bounds) {
        renderer.drawRoundedRect(toX(bounds.x()), toY(bounds.y()), scaled(bounds.width()),
                scaled(bounds.height()), scaled(bounds.width() * 0.14f),
                withOpacity(0x14ffffff), scaled(1.0f), withOpacity(0x26ffffff));
    }

    public void drawItemHighlight(UiBounds bounds, int argb, boolean box, boolean twoD, boolean fill) {
        int opaqueColor = argb | 0xff000000;
        if (box) {
            drawRect(bounds, argb);
            drawRectOutline(bounds, lineWidth, opaqueColor);
        }
        drawTwoDimensionalHighlight(bounds, argb, twoD, fill);
    }

    public void drawPlayerEsp(UiBounds bounds, int argb, boolean box, boolean twoD, boolean fill) {
        int opaqueColor = argb | 0xff000000;
        if (box) {
            drawRect(bounds, argb);
            UiBounds[] faces = PlayerPreviewRenderer.getProjectedFaces();
            if (faces != null) {
                drawProjectedFaces(faces, opaqueColor);
            } else {
                drawRectOutline(bounds, lineWidth, opaqueColor);
            }
        }
        drawTwoDimensionalHighlight(bounds, argb, twoD, fill);
    }

    private void drawProjectedFaces(UiBounds[] faces, int argb) {
        UiBounds first = faces[0];
        UiBounds second = faces[1];
        drawRectOutline(first, lineWidth, argb);
        if (Math.abs(second.y() - first.y()) < lineWidth * 0.5f) {
            return;
        }
        drawRectOutline(second, lineWidth, argb);
        float stroke = snappedStroke(lineWidth);
        float left = snappedX(first.x()) - stroke;
        float right = snappedX(first.right());
        drawFaceConnector(left, snappedY(first.y()), snappedY(second.y()), stroke, true, argb);
        drawFaceConnector(right, snappedY(first.y()), snappedY(second.y()), stroke, true, argb);
        drawFaceConnector(left, snappedY(first.bottom()), snappedY(second.bottom()), stroke, false, argb);
        drawFaceConnector(right, snappedY(first.bottom()), snappedY(second.bottom()), stroke, false, argb);
    }

    private void drawFaceConnector(float x, float firstY, float secondY, float stroke,
                                   boolean top, int argb) {
        float start = Math.min(firstY, secondY);
        float length = Math.abs(secondY - firstY) + stroke;
        fillPixels(x, top ? start - stroke : start, stroke, length, argb);
    }

    private void drawTwoDimensionalHighlight(UiBounds bounds, int argb, boolean twoD, boolean fill) {
        if (!twoD) {
            return;
        }
        float stroke = snappedStroke(lineWidth);
        float left = snappedX(bounds.x());
        float top = snappedY(bounds.y());
        float right = snappedX(bounds.right());
        float bottom = snappedY(bounds.bottom());
        if (fill) {
            fillPixels(left, top, right - left, bottom - top, argb);
        }
        drawBorderPixels(left - stroke, top - stroke, right + stroke, bottom + stroke, stroke, 0xff000000);
        drawBorderPixels(left, top, right, bottom, stroke, argb | 0xff000000);
        drawBorderPixels(left + stroke, top + stroke, right - stroke, bottom - stroke, stroke, 0xff000000);
    }

    public void drawRect(UiBounds bounds, int argb) {
        fillRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), argb);
    }

    private void fillRect(float x, float y, float width, float height, int argb) {
        renderer.drawPixelAlignedRect(toX(x), toY(y), scaled(width), scaled(height),
                0.0f, withOpacity(argb), 0.0f, 0);
    }

    public void drawRectOutline(UiBounds bounds, float width, int argb) {
        float stroke = snappedStroke(width);
        drawBorderPixels(snappedX(bounds.x()) - stroke, snappedY(bounds.y()) - stroke,
                snappedX(bounds.right()) + stroke, snappedY(bounds.bottom()) + stroke, stroke, argb);
    }

    private void drawBorderPixels(float left, float top, float right, float bottom, float stroke, int argb) {
        fillPixels(left, top, right - left, stroke, argb);
        fillPixels(left, bottom - stroke, right - left, stroke, argb);
        fillPixels(left, top + stroke, stroke, bottom - top - stroke * 2.0f, argb);
        fillPixels(right - stroke, top + stroke, stroke, bottom - top - stroke * 2.0f, argb);
    }

    private void fillPixels(float x, float y, float width, float height, int argb) {
        renderer.drawRoundedRect(x, y, width, height, 0.0f, withOpacity(argb));
    }

    private float snappedStroke(float width) {
        float quantum = 0.5f;
        return Math.max(quantum, (float) Math.round(scaled(width) / quantum) * quantum);
    }

    public float getAlignedLineWidth() {
        return snappedStroke(lineWidth) / Math.max(scale, 1.0E-4f);
    }

    private float snappedX(float x) {
        return UiRenderer.snapPixel(toX(x));
    }

    private float snappedY(float y) {
        return UiRenderer.snapPixel(toY(y));
    }

    public void drawLine(float x1, float y1, float x2, float y2, float width, int argb) {
        renderer.drawQuadraticStroke(toX(x1), toY(y1), toX((x1 + x2) * 0.5f),
                toY((y1 + y2) * 0.5f), toX(x2), toY(y2), scaled(width), withOpacity(argb));
    }

    public void drawShadow(UiBounds bounds, float radius, float sigma, int argb) {
        renderer.drawShadow(toX(bounds.x()), toY(bounds.y()), scaled(bounds.width()),
                scaled(bounds.height()), scaled(radius), scaled(sigma), withOpacity(argb));
    }

    public void drawCenteredText(FontAsset font, String text, float x, float y, float size, int argb) {
        float localTextWidth = renderer.measureText(font, text, scaled(size)) / Math.max(scale, 1.0E-4f);
        renderer.drawTextWithShadow(font, text, toX(x - localTextWidth * 0.5f), toY(y),
                scaled(size), withOpacity(argb));
    }

    private int withOpacity(int argb) {
        return ArgbMath.multiplyAlpha(argb, opacity);
    }

    private float toX(float x) {
        return originX + x * scale;
    }

    private float toY(float y) {
        return originY + y * scale;
    }

    private float scaled(float value) {
        return value * scale;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public int getIndex() {
        return index;
    }

    public float getLineWidth() {
        return lineWidth;
    }
}
