package dev.echo.render.shader.wgsl;

import dev.echo.render.shader.naga.Module;
import dev.echo.render.shader.naga.Naga;

/** WGSL parsing into a module whose ownership passes to the caller. */
public final class WgslParser {
    public Module parse(String source) {
        return parseSource(source);
    }

    public static Module parseSource(String source) {
        long handle = Naga.parseWgsl(source);
        try {
            return new Module(handle);
        } catch (RuntimeException | Error error) {
            if (handle != 0) {
                Naga.moduleFree(handle);
            }
            throw error;
        }
    }
}
