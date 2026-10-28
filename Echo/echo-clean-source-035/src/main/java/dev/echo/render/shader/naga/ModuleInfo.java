package dev.echo.render.shader.naga;

import java.util.Objects;

/**
 * Owns one validation result and remembers the module it validates. Native
 * ModuleInfo is separately allocated, so closing this never closes the module.
 */
public final class ModuleInfo implements AutoCloseable {
    private final Module module;
    private long handle;

    /** Transfers a native validation pointer to this owner exactly once. */
    public ModuleInfo(Module module, long handle) {
        this.module = Objects.requireNonNull(module, "module");
        if (handle == 0) {
            throw new IllegalArgumentException("Cannot own a null Naga validation result");
        }
        this.handle = handle;
    }

    /** Borrowed pointer; callers using the raw ABI must hold this object's monitor. */
    public synchronized long handle() {
        if (handle == 0) {
            throw new IllegalStateException("Naga validation result is closed");
        }
        return handle;
    }

    /** Borrowed pointer checked against its module; raw JNI callers must hold both owners' monitors. */
    public synchronized long handleFor(Module expectedModule) {
        if (module != expectedModule) {
            throw new IllegalArgumentException("Naga validation result belongs to a different module");
        }
        return handle();
    }

    @Override
    public synchronized void close() {
        if (handle != 0) {
            Naga.moduleInfoFree(handle);
            handle = 0;
        }
    }
}
