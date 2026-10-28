package dev.echo.render;

import dev.echo.client.Echo;

/**
 * Font-file and code-point ranges used by the outline font cache.
 * Measurement uses the live UI renderer's loaded font metrics, not guessed values.
 */
public enum FontAsset {
    INTER("fonts/suisse_intl_regular.otf", 32, 95, true),
    INTER_SEMI_BOLD("fonts/suisse_intl_semibold.otf", 32, 95, true),
    JETBRAINS_MONO("fonts/jetbrains_mono.ttf", 32, 95, true),
    JETBRAINS_MONO_MEDIUM("fonts/jetbrains_mono_medium.ttf", 32, 95, true),
    JETBRAINS_MONO_ITALIC("fonts/jetbrains_mono_italic.ttf", 32, 95, true),
    JETBRAINS_MONO_EM_DASH("fonts/jetbrains_mono.ttf", 8212, 1, true),
    JETBRAINS_MONO_ITALIC_EM_DASH("fonts/jetbrains_mono_italic.ttf", 8212, 1, true),
    INTER_MIDDLE_DOT("fonts/suisse_intl_regular.otf", 183, 1, true),
    INTER_MULTIPLY("fonts/suisse_intl_regular.otf", 215, 1, true),
    LUCIDE("fonts/lucide.ttf", 57400, 1640, false);

    public final String path;
    public final int firstCodePoint;
    public final int glyphCount;
    public final boolean kerning;

    FontAsset(String path, int firstCodePoint, int glyphCount, boolean kerning) {
        this.path = path;
        this.firstCodePoint = firstCodePoint;
        this.glyphCount = glyphCount;
        this.kerning = kerning;
    }

    public float measureText(CharSequence text, float size) {
        return Echo.uiRenderer.measureText(this, text, size);
    }

    public float lineHeight(float size) {
        return Echo.uiRenderer.measureFontHeight(this, size);
    }
}
