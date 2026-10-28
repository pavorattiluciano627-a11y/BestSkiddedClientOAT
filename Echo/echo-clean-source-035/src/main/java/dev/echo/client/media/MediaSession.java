package dev.echo.client.media;

import dev.echo.client.Echo;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/** Windows system-media-session access supplied by the readable SMTC JNI adapter. */
public final class MediaSession {
    private static final String LIBRARY = "/native/windows-x86_64/media_session.dll";
    public static final boolean AVAILABLE = load();

    private MediaSession() {
    }

    private static boolean load() {
        String operatingSystem = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String architecture = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (!operatingSystem.startsWith("windows")
                || !(architecture.equals("amd64") || architecture.equals("x86_64"))) {
            return false;
        }
        try (InputStream library = MediaSession.class.getResourceAsStream(LIBRARY)) {
            if (library == null) {
                return false;
            }
            Path extractedLibrary = Files.createTempFile("media_session", ".dll");
            extractedLibrary.toFile().deleteOnExit();
            Files.copy(library, extractedLibrary, StandardCopyOption.REPLACE_EXISTING);
            System.load(extractedLibrary.toAbsolutePath().toString());
            return true;
        } catch (Throwable failure) {
            Echo.LOGGER.error("Media session library unavailable", failure);
            return false;
        }
    }

    public static native long revision();

    public static native String title();

    public static native String artist();

    public static native byte[] cover();

    public static native long positionMillis();

    public static native long durationMillis();
}
