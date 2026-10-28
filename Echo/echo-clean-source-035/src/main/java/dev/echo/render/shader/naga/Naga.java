package dev.echo.render.shader.naga;

import dev.echo.render.nativeapi.NativeLibraries;
import java.io.IOException;

/**
 * Low-level JNI boundary for the nine implemented Naga operations.
 * Nonzero handles are native owning pointers, not arena indices. Prefer
 * {@link Module} and {@link ModuleInfo}, which enforce Java-side lifetimes.
 */
public final class Naga {
    static {
        try {
            NativeLibraries.load("naga_jni");
        } catch (IOException error) {
            throw new RuntimeException("Failed to load naga_jni native library", error);
        }
    }

    private Naga() {}

    /** Frees a live module once; zero is a no-op. */
    public static native void moduleFree(long module);

    /** Frees a live validation result once; zero is a no-op. */
    public static native void moduleInfoFree(long info);

    /** Returns an owning module pointer, or throws with the WGSL parse diagnostic. */
    public static native long parseWgsl(String source);

    /** Returns a separately owned result for the supplied live module. */
    public static native long validate(long module, int flags, long capabilities);

    /** Returns the documented source/reflection JSON; info must belong to module. */
    public static native String emitGlsl(long module, long info, int version, int stage, String entryPoint);

    /** Returns little-endian SPIR-V bytes; info must belong to module. */
    public static native byte[] emitSpv(long module, long info, int flags);

    public static native int numGlobalVariables(long module);

    /** The type index is zero-based and must be nonnegative. */
    public static native String typeJson(long module, int typeIndex);

    /** The global index is zero-based and must be nonnegative. */
    public static native String globalVariableJson(long module, int globalIndex);

    /**
     * The original Java API declared MSL emission, but the original native DLL
     * never exported it. This preserves that linkage failure explicitly and
     * must not be counted as a supported shader compiler backend.
     */
    public static String emitMsl(long module, long info, String optionsJson) {
        throw new UnsatisfiedLinkError("Naga emitMsl is unavailable: the original native library has no MSL export");
    }
}
