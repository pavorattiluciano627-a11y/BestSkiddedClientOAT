package dev.echo.client.screens.dashboard;

import dev.echo.render.FontAsset;
import dev.echo.client.render.SvgColorMode;
import dev.echo.client.render.UiRenderer;
import dev.echo.render.svg.SvgTextureCache;
import dev.echo.render.texture.TextureView;

/** V3's concrete drawing helpers, reconciled to the delivered FontAsset identity. */
public final class DashboardCanvasPainter implements DashboardPainter {
    private final UiRenderer renderer;
    private final SvgTextureCache svgTextures;
    private final DashboardTransform transform;
    private final TextureView backdrop;
    private final float opacity;
    private final float iconRasterScale;

    public DashboardCanvasPainter(UiRenderer renderer, SvgTextureCache svgTextures,
                                  DashboardTransform transform, TextureView backdrop,
                                  float opacity, float iconRasterScale) {
        this.renderer = renderer;
        this.svgTextures = svgTextures;
        this.transform = transform;
        this.backdrop = backdrop;
        this.opacity = opacity;
        this.iconRasterScale = iconRasterScale;
    }

    @Override
    public void backdropPanel(float x, float y, float width, float height, float radius,
                              int fill, float borderWidth, int border) {
        if (backdrop == null) {
            panel(x, y, width, height, radius, fill, borderWidth, border);
            return;
        }
        renderer.drawBackdrop(backdrop, transform.renderX(x), transform.renderY(y),
                transform.renderSize(width), transform.renderSize(height), transform.renderSize(radius), opacity, true, fill);
        if (borderWidth > 0.0f) panel(x, y, width, height, radius, 0, borderWidth, border);
    }

    @Override
    public void panel(float x, float y, float width, float height, float radius, int fill, float borderWidth, int border) {
        renderer.drawRoundedRect(transform.renderX(x), transform.renderY(y), transform.renderSize(width),
                transform.renderSize(height), transform.renderSize(radius), DashboardColors.multiplyAlpha(fill, opacity),
                transform.renderSize(borderWidth), DashboardColors.multiplyAlpha(border, opacity));
    }

    @Override
    public void roundedRect(float x, float y, float width, float height, float radius, int color) {
        renderer.drawRoundedRect(transform.renderX(x), transform.renderY(y), transform.renderSize(width),
                transform.renderSize(height), transform.renderSize(radius), DashboardColors.multiplyAlpha(color, opacity));
    }

    public static FontAsset specialGlyphFont(FontAsset font, char character) {
        if (character == '\u00b7') return FontAsset.INTER_MIDDLE_DOT;
        if (character == '\u2014') return font == FontAsset.JETBRAINS_MONO_ITALIC
                ? FontAsset.JETBRAINS_MONO_ITALIC_EM_DASH : FontAsset.JETBRAINS_MONO_EM_DASH;
        return null;
    }

    @Override
    public float textWidth(FontAsset font, CharSequence text, float size) {
        float width = 0.0f;
        int runStart = 0;
        for (int index = 0; index < text.length(); index++) {
            FontAsset special = specialGlyphFont(font, text.charAt(index));
            if (special == null) continue;
            if (runStart < index) width += renderer.measureText(font, text.subSequence(runStart, index), transform.renderSize(size));
            width += renderer.measureText(special, text.subSequence(index, index + 1), transform.renderSize(size));
            runStart = index + 1;
        }
        if (runStart < text.length()) {
            CharSequence tail = runStart == 0 ? text : text.subSequence(runStart, text.length());
            width += renderer.measureText(font, tail, transform.renderSize(size));
        }
        return width / Math.max(transform.viewportScale() * transform.zoom() * transform.animationScale(), 1.0e-4f);
    }

    @Override
    public void text(FontAsset font, CharSequence text, float x, float y, float size, int color) {
        float cursor = transform.renderX(x);
        int effectiveColor = DashboardColors.multiplyAlpha(color, opacity);
        int runStart = 0;
        for (int index = 0; index < text.length(); index++) {
            FontAsset special = specialGlyphFont(font, text.charAt(index));
            if (special == null) continue;
            if (runStart < index) {
                CharSequence run = text.subSequence(runStart, index);
                renderer.drawText(font, run, cursor, transform.renderY(y), transform.renderSize(size), effectiveColor);
                cursor += renderer.measureText(font, run, transform.renderSize(size));
            }
            CharSequence glyph = text.subSequence(index, index + 1);
            renderer.drawText(special, glyph, cursor, transform.renderY(y), transform.renderSize(size), effectiveColor);
            cursor += renderer.measureText(special, glyph, transform.renderSize(size));
            runStart = index + 1;
        }
        if (runStart < text.length()) {
            CharSequence tail = runStart == 0 ? text : text.subSequence(runStart, text.length());
            renderer.drawText(font, tail, cursor, transform.renderY(y), transform.renderSize(size), effectiveColor);
        }
    }

    @Override
    public void icon(DashboardIcon icon, float x, float y, float width, float height, float alpha, int tint) {
        roundedIcon(icon, x, y, width, height, 0.0f, alpha, tint);
    }

    public void roundedIcon(DashboardIcon icon, float x, float y, float width, float height, float radius, float alpha, int tint) {
        if (svgTextures == null) return;
        int pixelWidth = Math.max(1, Math.round(width * iconRasterScale));
        int pixelHeight = Math.max(1, Math.round(height * iconRasterScale));
        TextureView texture = svgTextures.getOrSchedule(icon.path, pixelWidth, pixelHeight, icon.colorMode);
        if (texture == null) return;
        float effectiveOpacity = alpha * opacity;
        float drawWidth = UiRenderer.snapPixel(transform.renderSize(width));
        float drawHeight = UiRenderer.snapPixel(transform.renderSize(height));
        float drawX = UiRenderer.snapPixel(transform.renderX(x) + (transform.renderSize(width) - drawWidth) * 0.5f);
        float drawY = UiRenderer.snapPixel(transform.renderY(y) + (transform.renderSize(height) - drawHeight) * 0.5f);
        if (icon.colorMode == SvgColorMode.ALPHA_MASK) {
            renderer.drawMaskedImage(texture, drawX, drawY, drawWidth, drawHeight, transform.renderSize(radius), effectiveOpacity, tint);
        } else renderer.drawImage(texture, drawX, drawY, drawWidth, drawHeight, transform.renderSize(radius), effectiveOpacity);
    }
}
