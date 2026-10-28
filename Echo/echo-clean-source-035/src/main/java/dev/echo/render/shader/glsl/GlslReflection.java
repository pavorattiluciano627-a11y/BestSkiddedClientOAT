package dev.echo.render.shader.glsl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Mutable reflection containers retain insertion order from parsed native output. */
public final class GlslReflection {
    public Map<String, TextureMapping> textureMappings = new LinkedHashMap<>();
    public List<PushConstantItem> pushConstantItems = new ArrayList<>();
}
