package dev.echo.client.config;

/**
 * Profile metadata in the order carried by the configuration protocol.
 *
 * <p>The icon retains the record's original array-reference equality and accessor
 * semantics; it is neither cloned nor compared by content.
 *
 * @param id the persistent profile identifier
 * @param name the display name
 * @param description the profile description
 * @param author the author name
 * @param serverName the associated server name
 * @param serverIp the associated server address
 * @param serverIcon decoded icon bytes, or null
 * @param type the profile type
 * @param publicVisibility whether visibility is public
 * @param createdAt the creation timestamp
 * @param updatedAt the update timestamp
 * @param active whether this is the selected profile
 * @param owned whether the profile is owned locally
 * @param sourceId the source profile identifier
 * @param publishedId the published profile identifier
 * @param publishedChanges whether local changes differ from the published profile
 * @param updateAvailable whether the source has an available update
 */
public record ConfigMetadata(
        String id, String name, String description, String author,
        String serverName, String serverIp, byte[] serverIcon, String type,
        boolean publicVisibility, long createdAt, long updatedAt,
        boolean active, boolean owned, String sourceId, String publishedId,
        boolean publishedChanges, boolean updateAvailable) {

    /**
     * Reports whether the profile has a nonblank published identifier.
     *
     * @return whether a published identifier is present
     */
    public boolean isPublished() {
        return !publishedId.isBlank();
    }
}
