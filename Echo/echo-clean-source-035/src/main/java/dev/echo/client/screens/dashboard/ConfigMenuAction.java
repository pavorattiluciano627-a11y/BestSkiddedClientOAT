package dev.echo.client.screens.dashboard;

/** Original configuration menu order, labels and destructive-action highlighting. */
public enum ConfigMenuAction {
    EDIT(new String("Edit details"), false),
    ADD_COPY(new String("Add another copy"), false),
    PUBLISH(new String("Publish"), false),
    UPDATE_PUBLIC(new String("Update public"), false),
    GET_UPDATE(new String("Get update"), false),
    UNPUBLISH(new String("Unpublish"), false),
    DELETE(new String("Delete"), true);

    private static final String CONFIRM_UNPUBLISH = new String("Click to unpublish");
    private static final String CONFIRM_REPLACE = new String("Click to replace");
    private static final String CONFIRM_PUBLISHED_DELETE = new String("Delete and unpublish");
    private static final String CONFIRM_DELETE = new String("Click to delete");

    public final String label;
    public final boolean destructive;

    ConfigMenuAction(String label, boolean destructive) {
        this.label = label;
        this.destructive = destructive;
    }

    public String confirmationLabel(ConfigCard card) {
        if (this == UNPUBLISH) return CONFIRM_UNPUBLISH;
        if (this == GET_UPDATE) return CONFIRM_REPLACE;
        return card.isPublished() ? CONFIRM_PUBLISHED_DELETE : CONFIRM_DELETE;
    }
}
