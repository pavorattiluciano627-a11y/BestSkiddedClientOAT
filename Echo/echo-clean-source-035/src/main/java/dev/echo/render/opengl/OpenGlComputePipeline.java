package dev.echo.render.opengl;

import dev.echo.render.pipeline.ComputePipeline;
import dev.echo.render.pipeline.ComputePipelineInfo;
import dev.echo.render.shader.ResourceBindingKind;
import dev.echo.render.shader.ShaderResourceLayout;
import dev.echo.render.shader.ShaderStage;
import dev.echo.render.shader.glsl.GlslOptions;
import dev.echo.render.shader.glsl.GlslPipelineOptions;
import dev.echo.render.shader.glsl.GlslWriter;
import dev.echo.render.shader.naga.ModuleInfo;
import dev.echo.render.shader.naga.Validator;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL43C;

/** Native OpenGL compute program with explicit reflection-derived binding tables. */
class OpenGlComputePipeline extends ComputePipeline {
    private final int programId;
    final int[] textureUnits;
    final int[] samplerUnits;
    final int[] unitCounts;
    final ResourceBindingKind[] bindingKinds;
    final List<PushConstantUniform> pushConstantUniforms = new ArrayList<>();

    public OpenGlComputePipeline(OpenGlDevice device, ComputePipelineInfo info) {
        super(info);
        String source;
        GlslWriter writer;
        try (ModuleInfo validated = new Validator(63, 68619L).validate(info.module)) {
            GlslOptions options = new GlslOptions();
            options.version = device.glslVersion;
            GlslPipelineOptions pipelineOptions = new GlslPipelineOptions();
            pipelineOptions.stage = ShaderStage.Compute;
            pipelineOptions.entryPoint = "cs_main";
            writer = new GlslWriter(options, pipelineOptions);
            source = writer.write(info.module, validated);
        }
        int shader = OpenGlShaderCompiler.compileShader(37305, source);
        programId = GL20C.glCreateProgram();
        GL20C.glAttachShader(programId, shader);
        GL20C.glLinkProgram(programId);
        if (GL20C.glGetProgrami(programId, 35714) == 0) {
            String log = GL20C.glGetProgramInfoLog(programId);
            GL20C.glDeleteProgram(programId);
            GL20C.glDeleteShader(shader);
            throw new RuntimeException("GL compute program link failed: " + log);
        }
        GL20C.glDetachShader(programId, shader);
        GL20C.glDeleteShader(shader);

        ShaderResourceLayout layout = ShaderResourceLayout.reflect(info.module);
        int count = layout.bindingKinds().length;
        textureUnits = new int[count];
        samplerUnits = new int[count];
        unitCounts = new int[count];
        bindingKinds = layout.bindingKinds();
        int[] allocatedUnits = OpenGlShaderCompiler.allocateTextureUnits(layout);
        GL20C.glUseProgram(programId);
        OpenGlShaderCompiler.bindUniformBlocks(programId, layout);
        OpenGlShaderCompiler.bindTextureUniforms(programId, writer.reflection, layout,
                allocatedUnits, textureUnits, samplerUnits, unitCounts);
        OpenGlShaderCompiler.resolvePushConstantUniforms(programId, pushConstantUniforms,
                writer.reflection);
        GL20C.glUseProgram(0);
    }

    public void bind() { OpenGlPipelineOperations.bindComputeProgram(programId); }

    public void dispatch(int x, int y, int z) { OpenGlPipelineOperations.dispatchCompute(x, y, z); }

    @Override public void destroy() { OpenGlPipelineOperations.deleteComputeProgram(programId); }
}
