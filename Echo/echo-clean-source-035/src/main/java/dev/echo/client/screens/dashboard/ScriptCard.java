package dev.echo.client.screens.dashboard;

/** Original script-row snapshot; status text, enablement and editor text remain mutable. */
public final class ScriptCard {
    public final String name;
    public final String author;
    public final String typeLabel;
    public String statusLabel;
    public final boolean publicVisibility;
    public boolean enabled;
    public String editorText;

    public ScriptCard(String name, String author, String typeLabel, String statusLabel,
                      boolean publicVisibility, boolean enabled, String editorText) {
        this.name = name;
        this.author = author;
        this.typeLabel = typeLabel;
        this.statusLabel = statusLabel;
        this.publicVisibility = publicVisibility;
        this.enabled = enabled;
        this.editorText = editorText;
    }
}
