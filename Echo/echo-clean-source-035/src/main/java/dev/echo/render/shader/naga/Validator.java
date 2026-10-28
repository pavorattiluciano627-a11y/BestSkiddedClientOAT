package dev.echo.render.shader.naga;

import java.util.Objects;

/** Naga validation masks; all bits are passed unchanged to the native API. */
public final class Validator {
    public int flags;
    public long capabilities;

    public Validator(int flags, long capabilities) {
        this.flags = flags;
        this.capabilities = capabilities;
    }

    public ModuleInfo validate(Module module) {
        return Objects.requireNonNull(module, "module").validate(flags, capabilities);
    }
}
