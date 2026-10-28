package dev.echo.client.spotify;

import dev.echo.client.Echo;
import dev.echo.render.ImageDecoder;
import dev.echo.render.texture.TextureView;

/** Media metadata and its lazily decoded album-cover texture. */
public class MediaTrack implements AutoCloseable {
    public final String title;
    public final String artist;
    private byte[] encodedCover;
    private TextureView coverTexture;

    MediaTrack(String title, String artist, byte[] encodedCover) {
        this.title = title;
        this.artist = artist;
        this.encodedCover = encodedCover;
    }

    /** Decodes the image once a render device exists; failed decoding is not retried. */
    public TextureView coverTexture() {
        if (coverTexture != null || encodedCover == null || Echo.renderDevice == null) {
            return coverTexture;
        }
        try {
            coverTexture = ImageDecoder.decodeTexture(Echo.renderDevice, encodedCover);
        } catch (RuntimeException failure) {
            Echo.LOGGER.warn("Failed to decode album cover", failure);
        }
        encodedCover = null;
        return coverTexture;
    }

    /** Releases the decoded image when its device is available, then drops its data. */
    @Override
    public void close() {
        if (coverTexture != null && Echo.renderDevice != null) {
            Echo.renderDevice.release(coverTexture.texture);
        }
        coverTexture = null;
        encodedCover = null;
    }
}
