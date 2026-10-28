package dev.echo.client.hud;

import dev.echo.client.render.SvgColorMode;
import dev.echo.client.render.UiRenderer;
import dev.echo.render.FontAsset;
import dev.echo.render.texture.TextureView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

/**
 * Per-element HUD placement and drawing through the live client render context.
 * Source-complete reconstruction of TokenFactory; the original key/cache and
 * initialization applicability conditions remain in the source evidence ledger.
 */
public final class HudElementLayout {
    private final HudRenderer hud;
    private final UiRenderer renderer;
    private final TextureView backdrop;
    private final float globalScale;
    private final float globalOpacity;
    private float layoutX;
    private float layoutY;
    private float screenX;
    private float screenY;
    private float pivotX;
    private float pivotY;
    private float width;
    private float height;
    private float effectScale = 1;
    private float effectOpacity = 1;
    private float shadowStrength;
    private HudElementType element;

    HudElementLayout(HudRenderer hud, UiRenderer renderer, TextureView backdrop, float globalScale, float opacity) {
        this.hud = hud;
        this.renderer = renderer;
        this.backdrop = backdrop;
        this.globalScale = globalScale;
        this.globalOpacity = Math.max(0.0f, Math.min(1.0f, opacity));
    }

    /** Preserves the notifications-only origin adjustment before computing the scale pivot. */
    public void beginElement(HudElementType element, float x, float y, float width, float height) {
        float offset = element == HudElementType.NOTIFICATIONS ? HudRenderer.notificationOffset(height) : 0;
        this.element = element;
        layoutX = element.defaultX();
        layoutY = element.defaultY() + offset;
        this.width = Math.max(0.0f, width);
        this.height = Math.max(0.0f, height);
        screenX = x;
        screenY = y + offset * globalScale;
        pivotX = layoutX + this.width * 0.5f;
        pivotY = layoutY + this.height * 0.5f;
        effectScale = 1;
        effectOpacity = 1;
        shadowStrength = 0;
    }

    public void setAnimation(float scale, float opacity) {
        effectScale = Math.max(0.0f, scale);
        effectOpacity = Math.max(0.0f, Math.min(1.0f, opacity));
    }

    public void endElement() {
        layoutX = 0; layoutY = 0; screenX = 0; screenY = 0; pivotX = 0; pivotY = 0;
        width = 0; height = 0; effectScale = 1; effectOpacity = 1; shadowStrength = 0; element = null;
    }

    public void drawRoundedRect(float x, float y, float width, float height, float radius, int color, float opacity) {
        renderer.drawRoundedRect(toScreenX(x), toScreenY(y), toScreenLength(width), toScreenLength(height),
                toScreenLength(radius), HudRenderer.multiplyAlpha(color,
                        globalOpacity * effectOpacity * Math.max(0.0f, Math.min(1.0f, opacity))));
    }

    /** Retains the original source-backdrop path and its solid-rectangle fallback. */
    public void drawBackdropRoundedRect(float x, float y, float width, float height, float radius, int color, float opacity) {
        if (backdrop == null) {
            drawRoundedRect(x, y, width, height, radius, color, opacity);
            return;
        }
        renderer.drawBackdrop(backdrop, toScreenX(x), toScreenY(y), toScreenLength(width), toScreenLength(height),
                toScreenLength(radius), globalOpacity * effectOpacity * Math.max(0.0f, Math.min(1.0f, opacity)), false, color);
    }

    /** Draws the original diagonal light/dark accent gradient. */
    public void drawAccentRect(float x, float y, float width, float height, float radius, float opacity) {
        int light = HudRenderer.multiplyAlpha(-723723,
                globalOpacity * effectOpacity * HudRenderer.clampFraction(opacity));
        int dark = HudRenderer.multiplyAlpha(-5592402,
                globalOpacity * effectOpacity * HudRenderer.clampFraction(opacity));
        renderer.drawGradientRect(toScreenX(x), toScreenY(y), toScreenLength(width), toScreenLength(height),
                toScreenLength(radius), toScreenLength(radius), toScreenLength(radius), toScreenLength(radius),
                light, dark, dark, light);
    }

    /** Draws the original six-unit glow before its accent gradient. */
    public void drawHeaderHighlight(float x, float y, float width, float height, float radius, float opacity) {
        if (width <= 0.001f || height <= 0.001f) return;
        float alpha = globalOpacity * effectOpacity * HudRenderer.clampFraction(opacity);
        renderer.drawShadow(toScreenX(x), toScreenY(y), toScreenLength(width), toScreenLength(height),
                toScreenLength(radius), toScreenLength(6.0f), HudRenderer.multiplyAlpha(-2134851392, alpha));
        drawAccentRect(x, y, width, height, radius, opacity);
    }

    /** Draws the optional displaced shadow and then the requested glow in original order. */
    public void drawGlow(float x, float y, float width, float height, float radius,
                         float blurRadius, int color, float opacity) {
        if (shadowStrength > 0.001f) {
            renderer.drawShadow(toScreenX(x), toScreenY(y + 4.0f * shadowStrength),
                    toScreenLength(width), toScreenLength(height), toScreenLength(radius),
                    toScreenLength(blurRadius + 11.0f * shadowStrength),
                    HudRenderer.multiplyAlpha(-1291845632,
                            globalOpacity * effectOpacity * HudRenderer.clampFraction(opacity) * shadowStrength));
        }
        renderer.drawShadow(toScreenX(x), toScreenY(y), toScreenLength(width), toScreenLength(height),
                toScreenLength(radius), toScreenLength(blurRadius),
                HudRenderer.multiplyAlpha(color, globalOpacity * effectOpacity * HudRenderer.clampFraction(opacity)));
    }

    public void drawText(FontAsset font, CharSequence text, float x, float y, float size, int color, float opacity) {
        renderer.drawText(font, text, toScreenX(x), toScreenY(y), toScreenLength(size),
                HudRenderer.multiplyAlpha(color, globalOpacity * effectOpacity * Math.max(0.0f, Math.min(1.0f, opacity))));
    }

    public void drawTextRight(FontAsset font, CharSequence text, float right, float y, float size, int color, float opacity) {
        drawText(font, text, right - textWidth(font, text, size), y, size, color, opacity);
    }

    public float textWidth(FontAsset font, CharSequence text, float size) {
        return renderer.measureText(font, text, toScreenLength(size)) / Math.max(globalScale * effectScale, 1.0e-4f);
    }

    public float fontHeight(FontAsset font, float size) {
        return renderer.measureFontHeight(font, toScreenLength(size)) / Math.max(globalScale * effectScale, 1.0e-4f);
    }

    public float getWidth() { return width; }
    public float getHeight() { return height; }

    public void drawSeparator(float x, float y, float opacity) {
        drawRoundedRect(x, y, 1, 9, 0, 0x40ffffff, opacity);
    }

    /** Rasterizes at the original two-pixel density and retains SVG color treatment. */
    public void drawIcon(HudIcon icon, float x, float y, float width, float height, int color, float opacity) {
        float density = Math.max(1.0e-4f, 2.0f);
        int rasterWidth = Math.max(1, (int) Math.ceil(width * globalScale * density));
        int rasterHeight = Math.max(1, (int) Math.ceil(height * globalScale * density));
        TextureView texture = hud.svgTextures().getOrSchedule(icon.path, rasterWidth, rasterHeight, icon.mode);
        if (texture == null) return;
        float drawWidth = rasterWidth / density;
        float drawHeight = rasterHeight / density;
        float drawX = Math.round((toScreenX(x) + (toScreenLength(width) - drawWidth) * 0.5f) * density) / density;
        float drawY = Math.round((toScreenY(y) + (toScreenLength(height) - drawHeight) * 0.5f) * density) / density;
        float alpha = globalOpacity * effectOpacity * HudRenderer.clampFraction(opacity);
        if (icon.mode == SvgColorMode.ALPHA_MASK) {
            renderer.drawMaskedImage(texture, drawX, drawY, drawWidth, drawHeight, 0.0f, alpha, color);
        } else {
            renderer.drawImage(texture, drawX, drawY, drawWidth, drawHeight, 0.0f, alpha);
        }
    }

    /** Draws an existing texture view without changing its sampling coordinates. */
    public void drawTexture(TextureView texture, float x, float y, float width, float height,
                            float radius, float opacity) {
        if (texture == null) return;
        renderer.drawImage(texture, toScreenX(x), toScreenY(y), toScreenLength(width), toScreenLength(height),
                toScreenLength(radius), globalOpacity * effectOpacity * HudRenderer.clampFraction(opacity));
    }

    /** Draws the caller's exact texture region, including its original UV orientation. */
    public void drawTextureRegion(TextureView texture, float x, float y, float width, float height,
                                  float radius, float opacity, float u0, float v0, float u1, float v1) {
        if (texture == null) return;
        renderer.drawImage(texture, toScreenX(x), toScreenY(y), toScreenLength(width), toScreenLength(height),
                toScreenLength(radius), globalOpacity * effectOpacity * HudRenderer.clampFraction(opacity), u0, v0, u1, v1);
    }

    /** Uses the same live item-size snapping operation as the original HUD item drawing. */
    public void drawSnappedTexture(TextureView texture, float x, float y, float size, float opacity) {
        if (texture == null) return;
        ItemRenderPlacement placement = HudRenderer.itemPlacement(toScreenX(x), toScreenY(y), toScreenLength(size), 2.0f);
        renderer.drawImage(texture, placement.x(), placement.y(), placement.size(), placement.size(), 0.0f,
                globalOpacity * effectOpacity * HudRenderer.clampFraction(opacity));
    }

    /** Draws the live stack, durability bar and cooldown, retaining their early-exit/error order. */
    public void drawItem(ItemStack stack, float x, float y, float size, int seed) {
        float alpha = globalOpacity * effectOpacity;
        boolean hotbar = element == HudElementType.HOTBAR;
        if (hud.inventoryPlayer() == null || stack == null || stack.isEmpty()) return;
        if (hotbar && !hud.replacesVanillaHotbar()) return;
        if (!hotbar && alpha <= 0.36f) return;
        ItemRenderPlacement placement = HudRenderer.itemPlacement(toScreenX(x), toScreenY(y), toScreenLength(size), 2.0f);
        float drawX = placement.x();
        float drawY = placement.y();
        float drawSize = placement.size();
        renderer.drawItem(stack, hud.inventoryPlayer(), seed, drawX, drawY, drawSize, alpha);
        float itemScale = drawSize / 16.0f;
        if (stack.isItemBarVisible()) {
            renderer.drawRoundedRect(drawX + 2.0f * itemScale, drawY + 13.0f * itemScale,
                    13.0f * itemScale, 2.0f * itemScale, 0.0f, HudRenderer.multiplyAlpha(0xff000000, alpha));
            renderer.drawRoundedRect(drawX + 2.0f * itemScale, drawY + 13.0f * itemScale,
                    stack.getItemBarStep() * itemScale, itemScale, 0.0f,
                    HudRenderer.multiplyAlpha(0xff000000 | stack.getItemBarColor(), alpha));
        }
        float cooldown = hud.inventoryPlayer().getItemCooldownManager().getCooldownProgress(stack,
                MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(true));
        if (cooldown > 0.0f) {
            renderer.drawRoundedRect(drawX, drawY + (1.0f - cooldown) * drawSize,
                    drawSize, cooldown * drawSize, 0.0f, HudRenderer.multiplyAlpha(Integer.MAX_VALUE, alpha));
        }
    }

    /** Uses Minecraft's baked font and the original 8/9 size conversion for item-count labels. */
    public void drawItemCount(String text, float x, float y, float size, int color) {
        float alpha = globalOpacity * effectOpacity;
        boolean hotbar = element == HudElementType.HOTBAR;
        if (text == null || text.isEmpty()) return;
        if (hotbar && !hud.replacesVanillaHotbar()) return;
        if (!hotbar && alpha <= 0.36f) return;
        renderer.drawMinecraftText(text, toScreenX(x), toScreenY(y), toScreenLength(size) * 8.0f / 9.0f,
                HudRenderer.multiplyAlpha(color, alpha));
    }

    private float toScreenX(float x) {
        return screenX + (pivotX + (x - pivotX) * effectScale - layoutX) * globalScale;
    }

    private float toScreenY(float y) {
        return screenY + (pivotY + (y - pivotY) * effectScale - layoutY) * globalScale;
    }

    private float toScreenLength(float length) {
        return length * globalScale * effectScale;
    }
}
