package dev.echo.render.opengl;

import dev.echo.render.pipeline.GraphicsPipeline;
import dev.echo.render.pipeline.GraphicsPipelineInfo;
import dev.echo.render.pipeline.PrimitiveTopology;
import dev.echo.render.shader.ShaderResourceLayout;
import dev.echo.render.shader.ShaderStage;
import dev.echo.render.shader.glsl.GlslOptions;
import dev.echo.render.shader.glsl.GlslPipelineOptions;
import dev.echo.render.shader.glsl.GlslWriter;
import dev.echo.render.shader.naga.ModuleInfo;
import dev.echo.render.shader.naga.Validator;
import dev.echo.render.vertex.VertexAttribute;
import dev.echo.render.vertex.VertexFormat;
import dev.echo.render.vertex.VertexScalarKind;
import dev.echo.render.vertex.VertexStepMode;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GL42C;
import org.lwjgl.opengl.GL43C;

/** GL shader program and VAO, including host-coordinate Y inversion. */
public class OpenGlGraphicsPipeline extends GraphicsPipeline {
    private final int programId;
    private final int vertexArrayId;
    public final int[] textureUnits;
    public final int[] samplerUnits;
    public final int[] unitCounts;
    public final List<PushConstantUniform> pushConstantUniforms = new ArrayList<>();
    private int flipUniformLocation;

    public OpenGlGraphicsPipeline(OpenGlDevice device, GraphicsPipelineInfo info) {
        super(info);
        GlslWriter vertexWriter;
        GlslWriter fragmentWriter;
        String vertexSource;
        String fragmentSource;
        try (ModuleInfo validated = new Validator(63, 68619L).validate(info.module)) {
            GlslOptions options = new GlslOptions();
            options.version = device.glslVersion;
            GlslPipelineOptions vertexOptions = new GlslPipelineOptions();
            vertexOptions.stage = ShaderStage.Vertex;
            vertexOptions.entryPoint = "vs_main";
            vertexWriter = new GlslWriter(options, vertexOptions);
            vertexSource = vertexWriter.write(info.module, validated);
            GlslPipelineOptions fragmentOptions = new GlslPipelineOptions();
            fragmentOptions.stage = ShaderStage.Fragment;
            fragmentOptions.entryPoint = "fs_main";
            fragmentWriter = new GlslWriter(options, fragmentOptions);
            fragmentSource = fragmentWriter.write(info.module, validated);
        }
        vertexSource = patchVertexShader(vertexSource);
        int vertexShader = OpenGlShaderCompiler.compileShader(35633, vertexSource);
        int fragmentShader = OpenGlShaderCompiler.compileShader(35632, fragmentSource);
        programId = GL33C.glCreateProgram();
        GL33C.glAttachShader(programId, vertexShader);
        GL33C.glAttachShader(programId, fragmentShader);
        GL33C.glLinkProgram(programId);
        if (GL33C.glGetProgrami(programId, 35714) == 0) {
            String log = GL33C.glGetProgramInfoLog(programId);
            GL33C.glDeleteProgram(programId);
            GL33C.glDeleteShader(vertexShader);
            GL33C.glDeleteShader(fragmentShader);
            throw new RuntimeException("GL program link failed: " + log);
        }
        GL33C.glDetachShader(programId, vertexShader);
        GL33C.glDetachShader(programId, fragmentShader);
        GL33C.glDeleteShader(vertexShader);
        GL33C.glDeleteShader(fragmentShader);
        vertexArrayId = GL33C.glGenVertexArrays();
        GL33C.glBindVertexArray(vertexArrayId);
        GL33C.glBindVertexArray(0);

        ShaderResourceLayout layout = ShaderResourceLayout.reflect(info.module);
        int count = layout.bindingKinds().length;
        textureUnits = new int[count];
        samplerUnits = new int[count];
        unitCounts = new int[count];
        int[] allocatedUnits = OpenGlShaderCompiler.allocateTextureUnits(layout);
        GL33C.glUseProgram(programId);
        OpenGlShaderCompiler.bindUniformBlocks(programId, layout);
        OpenGlShaderCompiler.bindTextureUniforms(programId, vertexWriter.reflection, layout,
                allocatedUnits, textureUnits, samplerUnits, unitCounts);
        OpenGlShaderCompiler.bindTextureUniforms(programId, fragmentWriter.reflection, layout,
                allocatedUnits, textureUnits, samplerUnits, unitCounts);
        OpenGlShaderCompiler.resolvePushConstantUniforms(programId, pushConstantUniforms,
                vertexWriter.reflection);
        OpenGlShaderCompiler.resolvePushConstantUniforms(programId, pushConstantUniforms,
                fragmentWriter.reflection);
        flipUniformLocation = GL33C.glGetUniformLocation(programId, "_chimera_flip");
        GL33C.glUseProgram(0);
    }

    private static String patchVertexShader(String source) {
        int directivesEnd = 0;
        while (source.startsWith("#", directivesEnd)) {
            directivesEnd = source.indexOf('\n', directivesEnd) + 1;
        }
        int insertion = source.lastIndexOf("return;");
        if (insertion < 0) {
            insertion = source.lastIndexOf('}');
        }
        return source.substring(0, directivesEnd) + "uniform float _chimera_flip;\n"
                + source.substring(directivesEnd, insertion)
                + "gl_Position.y *= _chimera_flip;\n" + source.substring(insertion);
    }

    public void bind(OpenGlRenderPass pass) {
        GL33C.glUseProgram(programId);
        GL33C.glBindVertexArray(vertexArrayId);
        if (flipUniformLocation >= 0) {
            GL33C.glUniform1f(flipUniformLocation, pass.isYFlipped() ? 1.0f : -1.0f);
        }
        pass.setBlendEnabled(info.blendEnabled);
        pass.setCullEnabled(info.cullEnabled);
        pass.setDepthTestEnabled(info.depthTestEnabled);
        pass.setDepthWriteEnabled(info.depthTestEnabled && info.depthWriteEnabled);
        pass.setBlendMode(info.blendMode);
        pass.setCullFace(info.cullFace);
        pass.setDepthCompare(info.depthCompare);
        pass.setFrontFace(info.frontFace);
        pass.setLineWidth(info.lineWidth);
    }

    public void setVertexBufferPointer(long offset) {
        for (VertexAttribute attribute : info.vertexAttributes) {
            VertexFormat format = attribute.format();
            int type = switch (format.kind) {
                case Float -> 5126;
                case Int -> 5124;
            };
            GL33C.glEnableVertexAttribArray(attribute.location());
            GL33C.glVertexAttribDivisor(attribute.location(),
                    info.vertexStepMode == VertexStepMode.INSTANCE ? 1 : 0);
            if (format.kind == VertexScalarKind.Float) {
                GL33C.glVertexAttribPointer(attribute.location(), format.components, type, false,
                        info.vertexStride, offset + attribute.offset());
            } else {
                GL33C.glVertexAttribIPointer(attribute.location(), format.components, type,
                        info.vertexStride, offset + attribute.offset());
            }
        }
    }

    public boolean isInstanced() { return info.vertexStepMode == VertexStepMode.INSTANCE; }
    public int vertexStride() { return info.vertexStride; }

    public void draw(int count, int instances, int first, int baseInstance) {
        int topology = glTopology(info.topology);
        if (info.vertexStepMode == VertexStepMode.INSTANCE) {
            if (baseInstance != 0) {
                GL42C.glDrawArraysInstancedBaseInstance(topology, first, count, instances, baseInstance);
                return;
            }
            GL33C.glDrawArraysInstanced(topology, first, count, instances);
        } else {
            GL33C.glDrawArrays(topology, first, count);
        }
    }

    public void drawIndexed(int count, int instances, long offset) {
        int topology = glTopology(info.topology);
        if (info.vertexStepMode == VertexStepMode.INSTANCE) {
            GL33C.glDrawElementsInstanced(topology, count, 5125, offset, instances);
        } else {
            GL33C.glDrawElements(topology, count, 5125, offset);
        }
    }

    public void drawIndirect(long offset, int count, int stride) {
        GL43C.glMultiDrawArraysIndirect(glTopology(info.topology), offset, count, stride);
    }

    private static int glTopology(PrimitiveTopology topology) {
        return switch (topology) {
            case POINT_LIST -> 0;
            case LINE_LIST -> 1;
            case LINE_STRIP -> 3;
            case TRIANGLE_LIST -> 4;
            case TRIANGLE_STRIP -> 5;
            case TRIANGLE_FAN -> 6;
        };
    }

    @Override public void destroy() {
        OpenGlPipelineOperations.deleteGraphicsPipeline(vertexArrayId, programId);
    }
}
