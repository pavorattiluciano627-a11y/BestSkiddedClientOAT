package dev.echo.render.shader.msl;

import com.google.gson.Gson;
import dev.echo.render.shader.naga.Module;
import dev.echo.render.shader.naga.ModuleInfo;
import dev.echo.render.shader.naga.Naga;
import java.util.ArrayList;
import java.util.List;

/**
 * Retained Java MSL request/response API. The original native library has no
 * emitMsl export: write reaches the explicit unavailable boundary, not a fallback.
 */
public final class MslWriter {
    private static final Gson JSON = new Gson();
    public MslOptions options;
    public MslPipelineOptions pipelineOptions;
    public List<MslEntryPoint> entryPoints = new ArrayList<>();

    public MslWriter(MslOptions options, MslPipelineOptions pipelineOptions) {
        this.options = options;
        this.pipelineOptions = pipelineOptions;
    }

    /** Packs fields before borrowing handles; Gson runs after both handle reads. */
    public String write(Module module, ModuleInfo info) {
        MslOptionsPayload payload = MslOptionsPayload.from(options, pipelineOptions);
        synchronized (module) {
            synchronized (info) {
                return readOutput(Naga.emitMsl(module.handle(), info.handleFor(module), JSON.toJson(payload)));
            }
        }
    }

    /** Serializes the original payload order with default Gson null omission. */
    public String optionsJson() {
        return JSON.toJson(MslOptionsPayload.from(options, pipelineOptions));
    }

    /** Appends entries in response order, including entries preceding a later parse failure. */
    public String readOutput(String json) {
        return MslOutput.read(json, entryPoints).source();
    }
}
