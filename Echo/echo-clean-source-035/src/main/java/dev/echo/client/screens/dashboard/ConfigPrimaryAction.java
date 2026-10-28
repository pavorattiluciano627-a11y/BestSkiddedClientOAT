package dev.echo.client.screens.dashboard;

/** Original configuration primary-button state; accessor target proof is pending. */
public enum ConfigPrimaryAction {
    LOAD("Load", null, 0.0f),
    LOAD_OWN("Load", null, 0.0f),
    LOAD_COPY("Load", null, 0.0f),
    ADD("Load", null, 0.0f),
    SHOW_COPIES("Show copies", null, 0.0f),
    SAVE("Save", null, 72.0f),
    GET_UPDATE("Get update", "Replace?", 100.0f),
    UNPUBLISH("Unpublish", "Unpublish?", 100.0f);

    /** Original visible button label. */
    public final String label;
    /** Confirmation label, or null when this action does not use one. */
    public final String confirmationLabel;
    /** Width reserved for the secondary action on a card. */
    public final float secondaryWidth;

    ConfigPrimaryAction(String label, String confirmationLabel, float secondaryWidth) {
        this.label = label;
        this.confirmationLabel = confirmationLabel;
        this.secondaryWidth = secondaryWidth;
    }

    /** Identifies the save action. */
    public boolean isSave() {
        return this == SAVE;
    }

    /** Identifies the action which fetches a published update. */
    public boolean isGetUpdate() {
        return this == GET_UPDATE;
    }
}
