package dev.echo.render.shader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.echo.render.shader.naga.Module;
import java.util.ArrayList;
import java.util.List;

/**
 * Conditional reconstruction: the reference's mutable array guard and exception
 * wrapper obligations remain pending. Arrays deliberately retain reference-based
 * record equality and mutable accessors. Naga JSON keys are external wire data.
 */
public record ShaderResourceLayout(ResourceBindingKind[] bindingKinds, int[] bindingCounts) {
    public static ShaderResourceLayout reflect(Module module) {
        List<BindingLayoutEntry> entries = new ArrayList<>();
        int maximumBinding = -1;
        int globals = module.numGlobalVariables();
        for (int index = 0; index < globals; index++) {
            JsonObject global = JsonParser.parseString(module.globalVariableJson(index)).getAsJsonObject();
            JsonElement binding = global.get("binding");
            if (binding == null || binding.isJsonNull()) {
                continue;
            }
            BindingTypeInfo type = classifyBinding(module, global.get("space"), global.get("ty"));
            if (type == null) {
                continue;
            }
            int group = binding.getAsJsonObject().get("group").getAsInt();
            if (group != 0) {
                throw new IllegalArgumentException("Only @group(0) is supported, found @group(" + group + ")");
            }
            int slot = binding.getAsJsonObject().get("binding").getAsInt();
            entries.add(new BindingLayoutEntry(slot, type.kind(), type.arrayCount()));
            if (slot > maximumBinding) {
                maximumBinding = slot;
            }
        }
        ResourceBindingKind[] kinds = new ResourceBindingKind[maximumBinding + 1];
        int[] counts = new int[maximumBinding + 1];
        for (BindingLayoutEntry entry : entries) {
            kinds[entry.binding()] = entry.kind();
            counts[entry.binding()] = entry.arrayCount();
        }
        return new ShaderResourceLayout(kinds, counts);
    }

    private static BindingTypeInfo classifyBinding(Module module, JsonElement space, JsonElement type) {
        String spaceName = space.isJsonPrimitive()
                ? space.getAsString() : space.getAsJsonObject().keySet().iterator().next();
        return switch (spaceName) {
            case "Uniform" -> new BindingTypeInfo(ResourceBindingKind.UNIFORM, 1);
            case "Storage" -> new BindingTypeInfo(ResourceBindingKind.STORAGE_BUFFER, 1);
            case "Handle" -> classifyHandleType(module, type);
            default -> null;
        };
    }

    private static BindingTypeInfo classifyHandleType(Module module, JsonElement type) {
        JsonObject inner = typeInner(module, type.getAsInt());
        if (inner.has("BindingArray")) {
            JsonObject array = inner.getAsJsonObject("BindingArray");
            JsonElement size = array.get("size");
            int count = 1;
            if (size.isJsonObject() && size.getAsJsonObject().has("Constant")) {
                count = size.getAsJsonObject().get("Constant").getAsInt();
            }
            JsonObject base = typeInner(module, array.get("base").getAsInt());
            return base.has("Image") ? new BindingTypeInfo(ResourceBindingKind.SAMPLED_IMAGE_ARRAY, count) : null;
        }
        if (inner.has("Image")) {
            boolean storage = inner.getAsJsonObject("Image").getAsJsonObject("class").has("Storage");
            return new BindingTypeInfo(storage ? ResourceBindingKind.STORAGE_IMAGE : ResourceBindingKind.SAMPLED_IMAGE, 1);
        }
        if (inner.has("Sampler")) {
            return new BindingTypeInfo(ResourceBindingKind.SAMPLER, 1);
        }
        return null;
    }

    private static JsonObject typeInner(Module module, int type) {
        JsonObject description = JsonParser.parseString(module.typeJson(type)).getAsJsonObject();
        return description.getAsJsonObject("inner");
    }
}
