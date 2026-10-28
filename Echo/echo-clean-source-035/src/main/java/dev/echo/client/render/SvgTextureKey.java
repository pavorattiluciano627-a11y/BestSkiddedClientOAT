package dev.echo.client.render;

/** Sized SVG cache identity; dimensions are clamped by the requesting cache. */
public record SvgTextureKey(String resourcePath, int width, int height, SvgColorMode colorMode) {
}
