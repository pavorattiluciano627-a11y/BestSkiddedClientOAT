package dev.echo.render.nativeapi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/** Loads source-built native libraries packaged beneath {@code /native/}. */
public final class NativeLibraries {
    private NativeLibraries() {}

    /** Extracts a library to a temporary file, closes its stream, and loads it. */
    public static void load(String libraryName) throws IOException {
        String os = System.getProperty("os.name", "");
        String arch = System.getProperty("os.arch", "");
        String resource = resourcePath(libraryName, os, arch);
        String prefix = (isWindows(os) ? "" : "lib") + libraryName;
        try (InputStream input = NativeLibraries.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Native library not found in jar: " + resource);
            }
            Path extracted = Files.createTempFile(prefix, libraryExtension(os));
            extracted.toFile().deleteOnExit();
            Files.copy(input, extracted, StandardCopyOption.REPLACE_EXISTING);
            System.load(extracted.toAbsolutePath().toString());
        }
    }

    /** Returns the original platform-specific packaging path without loading it. */
    public static String resourcePath(String libraryName, String osName, String architecture) {
        return "/native/" + platformDirectory(osName, architecture) + "/"
                + (isWindows(osName) ? "" : "lib") + libraryName + libraryExtension(osName);
    }

    public static String platformDirectory(String osName, String architecture) {
        String os = osName.toLowerCase(Locale.ROOT);
        String arch = architecture.toLowerCase(Locale.ROOT);
        String normalizedArch = switch (arch) {
            case "aarch64", "arm64" -> "aarch64";
            case "amd64", "x86_64" -> "x86_64";
            default -> throw new IllegalStateException("Unsupported arch: " + arch);
        };
        String platform;
        if (os.contains("mac") || os.contains("darwin")) {
            platform = "macos";
        } else if (os.contains("linux")) {
            platform = "linux";
        } else if (os.contains("windows")) {
            platform = "windows";
        } else {
            throw new IllegalStateException("Unsupported os: " + os);
        }
        return platform + "-" + normalizedArch;
    }

    public static boolean isWindows(String osName) {
        return osName.toLowerCase(Locale.ROOT).contains("windows");
    }

    public static String libraryExtension(String osName) {
        String os = osName.toLowerCase(Locale.ROOT);
        if (os.contains("mac") || os.contains("darwin")) {
            return ".dylib";
        }
        return isWindows(os) ? ".dll" : ".so";
    }
}
