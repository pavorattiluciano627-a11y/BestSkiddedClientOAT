package dev.echo.render.shader.msl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.echo.render.shader.ShaderStage;
import java.util.ArrayList;
import java.util.List;

/**
 * Readable recovery of the original MSL response decoder. This is only a wire
 * format reader: the original native bridge does not implement MSL emission.
 */
public record MslOutput(String source, List<MslEntryPoint> entryPoints) {
    public static MslOutput read(String json) {
        return read(json, new ArrayList<>());
    }

    /** Appends as each entry is decoded, preserving the original writer's mutation order. */
    public static MslOutput read(String json, List<MslEntryPoint> entryPoints) {
        JsonObject output = JsonParser.parseString(json).getAsJsonObject();
        for (JsonElement entry : output.getAsJsonArray("entryPoints")) {
            JsonObject value = entry.getAsJsonObject();
            JsonArray size = value.getAsJsonArray("workgroupSize");
            entryPoints.add(new MslEntryPoint(
                    value.get("name").getAsString(),
                    value.get("translated").getAsString(),
                    ShaderStage.valueOf(value.get("stage").getAsString()),
                    new int[] {size.get(0).getAsInt(), size.get(1).getAsInt(), size.get(2).getAsInt()}));
        }
        return new MslOutput(output.get("source").getAsString(), entryPoints);
    }
}
