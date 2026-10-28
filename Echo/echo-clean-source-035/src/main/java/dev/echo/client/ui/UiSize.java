package dev.echo.client.ui;

import dev.echo.client.hud.HudElementType;

/**
 * HUD dimensions with the original nonnegative constructor normalization.
 * Source-complete candidate: fromDefaults retains the HUD accessor/cache applicability obligation.
 *
 * @param width horizontal extent
 * @param height vertical extent
 */
public record UiSize(float width, float height) {
    /** Normalizes negative dimensions; NaN remains NaN, as in the original. */
    public UiSize {
        width = Math.max(0.0f, width);
        height = Math.max(0.0f, height);
    }

    /** Builds the default size of an original HUD element. */
    public static UiSize fromDefaults(HudElementType type) {
        return new UiSize(type.defaultWidth(), type.defaultHeight());
    }
}
