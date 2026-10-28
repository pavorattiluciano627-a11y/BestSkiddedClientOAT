package dev.echo.client.features;

import dev.echo.client.text.SegmentedText;

/** Feature category declaration order, display text, and Lucide icon characters. */
public enum Category {
    COMBAT(SegmentedText.literal("Combat"), "\ue2b4"),
    MOVEMENT(SegmentedText.literal("Movement"), "\ue1bf"),
    RENDER(SegmentedText.literal("Visuals"), "\ue53a"),
    WORLD(SegmentedText.literal("World"), "\ue1f3"),
    UTILITY(SegmentedText.literal("Utility"), "\ue19f"),
    INTERNALS(SegmentedText.literal("Internals"), "\ue30b");

    public final CharSequence name;
    public final CharSequence icon;

    Category(CharSequence name, CharSequence icon) {
        this.name = name;
        this.icon = icon;
    }
}
