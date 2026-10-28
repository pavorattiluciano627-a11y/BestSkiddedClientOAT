package dev.echo.client.spotify;

import dev.echo.client.media.MediaSession;

/** Maintains revision-consistent metadata from the system media session. */
public class SpotifyManager {
    private MediaTrack currentTrack;
    private long lastRevision;

    /** Polls changed metadata and retains the previous track if the native revision races. */
    public void update() {
        if (!MediaSession.AVAILABLE) {
            return;
        }
        long revision = MediaSession.revision();
        if (revision == lastRevision) {
            return;
        }
        String title = MediaSession.title();
        MediaTrack replacement = title == null
                ? null
                : new MediaTrack(title, MediaSession.artist(), MediaSession.cover());
        if (MediaSession.revision() != revision) {
            if (replacement != null) {
                replacement.close();
            }
            return;
        }
        if (currentTrack != null) {
            currentTrack.close();
        }
        currentTrack = replacement;
        lastRevision = revision;
    }

    public long positionMillis() {
        return MediaSession.AVAILABLE ? MediaSession.positionMillis() : 0L;
    }

    public long durationMillis() {
        return MediaSession.AVAILABLE ? MediaSession.durationMillis() : 0L;
    }

    public MediaTrack currentTrack() {
        return currentTrack;
    }
}
