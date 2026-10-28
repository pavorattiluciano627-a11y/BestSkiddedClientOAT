package dev.echo.client.features.impl.render;

import dev.echo.client.Echo;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.lifecycle.ClientTickEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.features.hud.EchoHudElement;
import dev.echo.client.hud.HudElementLayout;
import dev.echo.client.hud.HudElementType;
import dev.echo.client.hud.HudRenderer;
import dev.echo.client.media.MediaSession;
import dev.echo.render.FontAsset;
import dev.echo.client.spotify.MediaTrack;
import dev.echo.client.spotify.SpotifyManager;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.render.texture.TextureView;
import java.util.Locale;

/** Current system-media track, cropped cover and millisecond playback progress. */
public class MusicFeature extends EchoHudElement {
    private static final float MAXIMUM_TEXT_WIDTH = 144.0f;
    private static final float MINIMUM_ARTIST_WIDTH = 24.0f;
    private static final String ELLIPSIS = "…";

    public MusicFeature() {
        super(new FeatureInfo(CatalogText.literal("Music"),
                CatalogText.literal("Current track and playback time"), Category.RENDER), HudElementType.MUSIC);
    }

    @Override
    public boolean canEnable() {
        return MediaSession.AVAILABLE;
    }

    @Subscribe
    public void onClientTick(ClientTickEvent event) {
        if (Echo.spotifyManager != null) {
            Echo.spotifyManager.update();
        }
    }

    @Override
    protected void renderContents(HudElementLayout layout) {
        HudRenderer hud = HudRenderer.INSTANCE;
        float x = HudElementType.MUSIC.defaultX();
        float y = HudElementType.MUSIC.defaultY();
        SpotifyManager manager = Echo.spotifyManager;
        MediaTrack track = manager == null ? null : manager.currentTrack();
        String title = track == null ? "Nothing playing" : track.title;
        String artist = track == null ? "" : track.artist;
        long position = track == null ? 0L : manager.positionMillis();
        long duration = track == null ? 0L : manager.durationMillis();
        hud.drawPanel(layout, x, y, HudElementType.MUSIC.defaultWidth(), 41.0f, 10.0f, 1.0f, 14.0f);
        TextureView cover = track == null ? null : track.coverTexture();
        if (cover == null) {
            layout.drawTexture(hud.getMusicPlaceholder(), x + 5.0f, y + 5.0f, 31.0f, 31.0f, 8.0f, 1.0f);
        } else {
            drawArtwork(layout, cover, x + 5.0f, y + 5.0f, 31.0f);
        }
        title = ellipsize(layout, FontAsset.INTER_SEMI_BOLD, title, 13.0f, MAXIMUM_TEXT_WIDTH);
        float titleWidth = layout.textWidth(FontAsset.INTER_SEMI_BOLD, title, 13.0f);
        layout.drawText(FontAsset.INTER_SEMI_BOLD, title, x + 46.0f, y + 13.0f, 13.0f, -1, 1.0f);
        float artistWidth = MAXIMUM_TEXT_WIDTH - titleWidth - 10.0f;
        if (!artist.isEmpty() && artistWidth >= MINIMUM_ARTIST_WIDTH) {
            layout.drawText(FontAsset.INTER, ellipsize(layout, FontAsset.INTER, artist, 12.0f, artistWidth),
                    x + 46.0f + titleWidth + 10.0f, y + 13.5f, 12.0f, -1929379841, 1.0f);
        }
        layout.drawRoundedRect(x + 199.0f, y, 1.0f, 41.0f, 0.0f, 0x0dffffff, 1.0f);
        layout.drawText(FontAsset.INTER_SEMI_BOLD, formatPlaybackTime(position),
                x + 213.0f, y + 13.5f, 12.5f, -1, 1.0f);
        layout.drawSeparator(x + 248.0f, y + 16.0f, 1.0f);
        layout.drawText(FontAsset.INTER, formatPlaybackTime(duration),
                x + 259.0f, y + 13.5f, 12.0f, -1929379841, 1.0f);
        float fraction = duration <= 0L ? 0.0f : Math.min(1.0f, (float) position / (float) duration);
        layout.drawAccentRect(x + 213.0f, y + 33.0f, 70.0f * fraction, 2.0f, 1.0f, 1.0f);
    }

    private static String ellipsize(HudElementLayout layout, FontAsset font, String text,
                                    float fontSize, float maximumWidth) {
        if (layout.textWidth(font, text, fontSize) <= maximumWidth) {
            return text;
        }
        int low = 0;
        int high = text.length();
        while (low < high) {
            int middle = (low + high + 1) / 2;
            String candidate = text.substring(0, middle).stripTrailing() + ELLIPSIS;
            if (layout.textWidth(font, candidate, fontSize) <= maximumWidth) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return text.substring(0, low).stripTrailing() + ELLIPSIS;
    }

    private static void drawArtwork(HudElementLayout layout, TextureView texture,
                                     float x, float y, float size) {
        float width = texture.width(0);
        float height = texture.height(0);
        float horizontalInset = width > height ? (1.0f - height / width) / 2.0f : 0.0f;
        float verticalInset = height > width ? (1.0f - width / height) / 2.0f : 0.0f;
        layout.drawTextureRegion(texture, x, y, size, size, 8.0f, 1.0f,
                horizontalInset, verticalInset, 1.0f - horizontalInset, 1.0f - verticalInset);
    }

    private static String formatPlaybackTime(long milliseconds) {
        long seconds = Math.max(0L, milliseconds) / 1000L;
        return seconds / 60L + ":" + String.format(Locale.ROOT, "%02d", seconds % 60L);
    }
}
