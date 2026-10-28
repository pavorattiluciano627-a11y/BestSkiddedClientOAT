package dev.echo.render.shader.spirv;

import dev.echo.render.shader.naga.Module;
import dev.echo.render.shader.naga.ModuleInfo;

/** Returns the native bridge's little-endian SPIR-V byte sequence unchanged. */
public final class SpirvWriter {
    public SpirvOptions options;

    public SpirvWriter(SpirvOptions options) {
        this.options = options;
    }

    public byte[] write(Module module, ModuleInfo info) {
        return module.emitSpv(info, options.flags);
    }
}
