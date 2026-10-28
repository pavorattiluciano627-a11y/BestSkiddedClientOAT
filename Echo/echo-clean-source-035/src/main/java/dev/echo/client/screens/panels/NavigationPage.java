package dev.echo.client.screens.panels;

/** Sidebar destinations and their original Lucide glyphs and display labels. */
public enum NavigationPage {
    FEATURES("\ue2ad", "Features"),
    CONFIGS("\ue0cf", "Configs"),
    FRIENDS("\ue19f", "Friends"),
    HUD_EDITOR("\ue1d8", "HUD Editor");

    /** Glyph in the retained upstream Lucide font. */
    public final String icon;
    /** Original, case-sensitive display label. */
    public final String label;

    NavigationPage(String icon, String label) {
        this.icon = icon;
        this.label = label;
    }
}
