package dev.echo.render.shader.glsl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.echo.render.shader.naga.Module;
import dev.echo.render.shader.naga.ModuleInfo;
import dev.echo.render.shader.reflection.PrimitiveKind;
import dev.echo.render.shader.reflection.ScalarKind;
import java.util.Map;

/** Emits desktop GLSL and decodes the native bridge's explicit JSON contract. */
public final class GlslWriter {
    public GlslOptions options;
    public GlslPipelineOptions pipelineOptions;
    public GlslReflection reflection = new GlslReflection();

    public GlslWriter(GlslOptions options, GlslPipelineOptions pipelineOptions) {
        this.options = options;
        this.pipelineOptions = pipelineOptions;
    }

    public String write(Module module, ModuleInfo info) {
        return readOutput(module.emitGlsl(info, options.version,
                pipelineOptions.stage.ordinal(), pipelineOptions.entryPoint));
    }

    /**
     * Decodes a native emitGlsl response. Repeated calls preserve the original
     * behavior: texture entries are put by name and push-constant items append.
     * Native wire names are mapped explicitly rather than inferred from fields.
     */
    public String readOutput(String json) {
        JsonObject output = JsonParser.parseString(json).getAsJsonObject();
        JsonObject reflected = output.getAsJsonObject("reflection");
        for (Map.Entry<String, JsonElement> entry
                : reflected.getAsJsonObject("texture_mapping").entrySet()) {
            JsonObject value = entry.getValue().getAsJsonObject();
            TextureMapping mapping = new TextureMapping();
            mapping.textureBinding = value.get("texture_binding").getAsInt();
            JsonElement sampler = value.get("sampler_binding");
            mapping.samplerBinding = sampler.isJsonNull() ? null : sampler.getAsInt();
            reflection.textureMappings.put(entry.getKey(), mapping);
        }
        for (JsonElement entry : reflected.getAsJsonArray("push_constant_items")) {
            JsonObject value = entry.getAsJsonObject();
            PushConstantItem item = new PushConstantItem();
            item.accessPath = value.get("access_path").getAsString();
            item.offset = value.get("offset").getAsInt();
            item.kind = PrimitiveKind.valueOf(value.get("kind").getAsString());
            item.scalarKind = ScalarKind.valueOf(value.get("scalar_kind").getAsString());
            item.components = value.get("components").getAsInt();
            item.columns = value.get("columns").getAsInt();
            reflection.pushConstantItems.add(item);
        }
        return output.get("source").getAsString();
    }
}
