package dev.echo.render.opengl;

import dev.echo.render.shader.ResourceBindingKind;
import dev.echo.render.shader.ShaderResourceLayout;
import dev.echo.render.shader.glsl.GlslReflection;
import dev.echo.render.shader.glsl.PushConstantItem;
import dev.echo.render.shader.glsl.TextureMapping;
import dev.echo.render.shader.reflection.PrimitiveKind;
import java.util.List;
import java.util.Map;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL31C;

/** Shader compilation and Naga-reflection bindings for the current GL program. */
public final class OpenGlShaderCompiler {
    private OpenGlShaderCompiler() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static int compileShader(int type, String source) {
        int shader = GL30C.glCreateShader(type);
        GL30C.glShaderSource(shader, source);
        GL30C.glCompileShader(shader);
        if (GL30C.glGetShaderi(shader, 35713) == 0) {
            String log = GL30C.glGetShaderInfoLog(shader);
            GL30C.glDeleteShader(shader);
            throw new RuntimeException("GL shader compile failed:\n" + log + "\n--- source ---\n" + source);
        }
        return shader;
    }

    public static int[] allocateTextureUnits(ShaderResourceLayout layout) {
        ResourceBindingKind[] kinds = layout.bindingKinds();
        int[] units = new int[kinds.length];
        int nextUnit = 0;
        for (int binding = 0; binding < kinds.length; binding++) {
            if (kinds[binding] == ResourceBindingKind.SAMPLED_IMAGE
                    || kinds[binding] == ResourceBindingKind.SAMPLED_IMAGE_ARRAY) {
                units[binding] = nextUnit;
                nextUnit += layout.bindingCounts()[binding];
            }
        }
        return units;
    }

    public static void bindUniformBlocks(int program, ShaderResourceLayout layout) {
        int blockIndex = 0;
        for (int binding = 0; binding < layout.bindingKinds().length; binding++) {
            if (layout.bindingKinds()[binding] == ResourceBindingKind.UNIFORM) {
                GL31C.glUniformBlockBinding(program, blockIndex++, binding);
            }
        }
    }

    public static void bindTextureUniforms(int program, GlslReflection reflection,
            ShaderResourceLayout layout, int[] allocatedUnits, int[] textureUnits,
            int[] samplerUnits, int[] unitCounts) {
        for (Map.Entry<String, TextureMapping> entry : reflection.textureMappings.entrySet()) {
            TextureMapping mapping = entry.getValue();
            int location = GL30C.glGetUniformLocation(program, entry.getKey());
            int binding = mapping.textureBinding;
            if (layout.bindingKinds()[binding] == ResourceBindingKind.STORAGE_IMAGE) {
                GL30C.glUniform1i(location, binding);
                textureUnits[binding] = binding;
                unitCounts[binding] = 1;
                continue;
            }
            int unit = allocatedUnits[binding];
            int count = layout.bindingCounts()[binding];
            if (count == 1) {
                GL30C.glUniform1i(location, unit);
            } else {
                int[] units = new int[count];
                for (int index = 0; index < count; index++) {
                    units[index] = unit + index;
                }
                GL30C.glUniform1iv(location, units);
            }
            textureUnits[binding] = unit;
            unitCounts[binding] = count;
            if (mapping.samplerBinding != null) {
                samplerUnits[mapping.samplerBinding] = unit;
                unitCounts[mapping.samplerBinding] = count;
            }
        }
    }

    public static void resolvePushConstantUniforms(int program, List<PushConstantUniform> uniforms,
                                                   GlslReflection reflection) {
        for (PushConstantItem item : reflection.pushConstantItems) {
            int location = GL30C.glGetUniformLocation(program, item.accessPath);
            if (location < 0) {
                continue;
            }
            uniforms.add(new PushConstantUniform(location, item.kind, item.scalarKind,
                    item.components, item.columns, item.offset));
        }
    }

    public static void uploadPushConstants(List<PushConstantUniform> uniforms, long address) {
        for (PushConstantUniform uniform : uniforms) {
            long data = address + uniform.offset();
            int location = uniform.location();
            int components = uniform.components();
            if (uniform.kind() == PrimitiveKind.Matrix) {
                // Matrix calls take columns first, rows second, one matrix, no transpose.
                switch (uniform.columns()) {
                    case 2 -> {
                        switch (components) {
                            case 2 -> GL30C.nglUniformMatrix2fv(location, 1, false, data);
                            case 3 -> GL30C.nglUniformMatrix2x3fv(location, 1, false, data);
                            case 4 -> GL30C.nglUniformMatrix2x4fv(location, 1, false, data);
                        }
                    }
                    case 3 -> {
                        switch (components) {
                            case 2 -> GL30C.nglUniformMatrix3x2fv(location, 1, false, data);
                            case 3 -> GL30C.nglUniformMatrix3fv(location, 1, false, data);
                            case 4 -> GL30C.nglUniformMatrix3x4fv(location, 1, false, data);
                        }
                    }
                    case 4 -> {
                        switch (components) {
                            case 2 -> GL30C.nglUniformMatrix4x2fv(location, 1, false, data);
                            case 3 -> GL30C.nglUniformMatrix4x3fv(location, 1, false, data);
                            case 4 -> GL30C.nglUniformMatrix4fv(location, 1, false, data);
                        }
                    }
                }
                continue;
            }
            switch (uniform.scalarKind()) {
                case Float -> {
                    switch (components) {
                        case 1 -> GL30C.nglUniform1fv(location, 1, data);
                        case 2 -> GL30C.nglUniform2fv(location, 1, data);
                        case 3 -> GL30C.nglUniform3fv(location, 1, data);
                        case 4 -> GL30C.nglUniform4fv(location, 1, data);
                    }
                }
                case Sint, Bool -> {
                    switch (components) {
                        case 1 -> GL30C.nglUniform1iv(location, 1, data);
                        case 2 -> GL30C.nglUniform2iv(location, 1, data);
                        case 3 -> GL30C.nglUniform3iv(location, 1, data);
                        case 4 -> GL30C.nglUniform4iv(location, 1, data);
                    }
                }
                case Uint -> {
                    switch (components) {
                        case 1 -> GL30C.nglUniform1uiv(location, 1, data);
                        case 2 -> GL30C.nglUniform2uiv(location, 1, data);
                        case 3 -> GL30C.nglUniform3uiv(location, 1, data);
                        case 4 -> GL30C.nglUniform4uiv(location, 1, data);
                    }
                }
            }
        }
    }
}
