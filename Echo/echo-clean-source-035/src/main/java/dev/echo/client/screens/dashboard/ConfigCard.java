package dev.echo.client.screens.dashboard;

import dev.echo.client.config.ConfigMetadata;
import dev.echo.render.texture.TextureView;

/** Original configuration-card snapshot; cachedRegistry is the copied profile's source author. */
public final class ConfigCard {
    public final String id;
    public final String name;
    public final String serverLabel;
    public final String author;
    public final TextureView serverIcon;
    public final String updatedLabel;
    public boolean selected;
    public final boolean publicVisibility;
    public final String type;
    public final String description;
    public final boolean owned;
    public final String sourceId;
    public final String publishedId;
    public final boolean publishedChanges;
    public final boolean updateAvailable;
    public String sourceAuthor = "";

    public ConfigCard(ConfigMetadata metadata, String serverLabel, TextureView serverIcon,
                      String updatedLabel, boolean selected) {
        id = metadata.id();
        name = metadata.name();
        this.serverLabel = serverLabel;
        author = metadata.author() == null ? "" : metadata.author();
        this.serverIcon = serverIcon;
        this.updatedLabel = updatedLabel;
        this.selected = selected;
        publicVisibility = metadata.publicVisibility();
        type = metadata.type();
        description = metadata.description();
        owned = metadata.owned();
        sourceId = metadata.sourceId();
        publishedId = metadata.publishedId();
        publishedChanges = metadata.publishedChanges();
        updateAvailable = metadata.updateAvailable();
    }

    public boolean isPublished() { return !publishedId.isBlank(); }
}
