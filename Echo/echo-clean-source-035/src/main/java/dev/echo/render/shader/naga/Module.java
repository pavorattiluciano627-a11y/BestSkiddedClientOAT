package dev.echo.render.shader.naga;

import java.util.Objects;

/**
 * Owns one parsed Naga module. Close after its validation results and writers
 * are finished. Operations and close serialize on this instance, preventing
 * native reads from racing destruction. Closing twice is harmless.
 */
public final class Module implements AutoCloseable {
    private long handle;

    /**
     * Transfers ownership of a live pointer returned by {@link Naga#parseWgsl}.
     * The caller must not free it or give it to a second owner afterwards.
     */
    public Module(long handle) {
        if (handle == 0) {
            throw new IllegalArgumentException("Cannot own a null Naga module");
        }
        this.handle = handle;
    }

    /** Borrowed pointer; callers using the raw ABI must hold this object's monitor. */
    public synchronized long handle() {
        if (handle == 0) {
            throw new IllegalStateException("Naga module is closed");
        }
        return handle;
    }

    public synchronized int numGlobalVariables() {
        return Naga.numGlobalVariables(handle());
    }

    public synchronized String typeJson(int typeIndex) {
        requireIndex(typeIndex);
        return Naga.typeJson(handle(), typeIndex);
    }

    public synchronized String globalVariableJson(int globalIndex) {
        requireIndex(globalIndex);
        return Naga.globalVariableJson(handle(), globalIndex);
    }

    synchronized ModuleInfo validate(int flags, long capabilities) {
        long info = Naga.validate(handle(), flags, capabilities);
        try {
            return new ModuleInfo(this, info);
        } catch (RuntimeException | Error error) {
            if (info != 0) {
                Naga.moduleInfoFree(info);
            }
            throw error;
        }
    }

    /** Holds both owners through the JNI call; lock order is module then info. */
    public synchronized String emitGlsl(ModuleInfo info, int version, int stage, String entryPoint) {
        Objects.requireNonNull(info, "info");
        synchronized (info) {
            return Naga.emitGlsl(handle(), info.handleFor(this), version, stage, entryPoint);
        }
    }

    /** Holds both owners through the JNI call; lock order is module then info. */
    public synchronized byte[] emitSpv(ModuleInfo info, int flags) {
        Objects.requireNonNull(info, "info");
        synchronized (info) {
            return Naga.emitSpv(handle(), info.handleFor(this), flags);
        }
    }

    @Override
    public synchronized void close() {
        if (handle != 0) {
            Naga.moduleFree(handle);
            handle = 0;
        }
    }

    private static void requireIndex(int index) {
        if (index < 0) {
            // The native serde handle conversion panics for negative indices.
            throw new IllegalArgumentException("Naga arena index must be nonnegative: " + index);
        }
    }
}
