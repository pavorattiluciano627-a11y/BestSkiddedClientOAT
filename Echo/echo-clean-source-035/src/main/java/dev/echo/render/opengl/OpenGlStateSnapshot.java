package dev.echo.render.opengl;

import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL40.GL_DRAW_INDIRECT_BUFFER;
import static org.lwjgl.opengl.GL40.GL_DRAW_INDIRECT_BUFFER_BINDING;

/**
 * Captures the GL state used by the renderer's host-state handoff.
 *
 * <p>Analysis candidate: the instruction-by-instruction reconstruction requires
 * the guard and caller invariants listed in this shard's needs.json before promotion.
 */
public class OpenGlStateSnapshot {
    private static final int TEXTURE_UNIT_COUNT = 16;

    public final boolean blendEnabled;
    public final boolean depthTestEnabled;
    public final boolean cullFaceEnabled;
    public final boolean scissorTestEnabled;
    private final int blendSourceRgb;
    private final int blendDestinationRgb;
    private final int blendSourceAlpha;
    private final int blendDestinationAlpha;
    private final int blendEquationRgb;
    private final int blendEquationAlpha;
    private final int depthFunction;
    public final boolean depthWriteEnabled;
    private final int cullFaceMode;
    private final int frontFaceWinding;
    private final int program;
    private final int vertexArray;
    private final int arrayBuffer;
    private final int uniformBuffer;
    private final int drawIndirectBuffer;
    public final int drawFramebuffer;
    public final float lineWidth;
    private final boolean[] colorWriteMask = new boolean[4];
    private final int activeTexture;
    private final int[] textures = new int[TEXTURE_UNIT_COUNT];
    private final int[] samplers = new int[TEXTURE_UNIT_COUNT];
    private final int[] textureBaseLevels = new int[TEXTURE_UNIT_COUNT];
    private final int[] textureMaxLevels = new int[TEXTURE_UNIT_COUNT];

    public OpenGlStateSnapshot() {
        blendEnabled = GL30C.glIsEnabledi(GL_BLEND, 0);
        depthTestEnabled = glIsEnabled(GL_DEPTH_TEST);
        cullFaceEnabled = glIsEnabled(GL_CULL_FACE);
        scissorTestEnabled = glIsEnabled(GL_SCISSOR_TEST);
        blendSourceRgb = glGetInteger(GL_BLEND_SRC_RGB);
        blendDestinationRgb = glGetInteger(GL_BLEND_DST_RGB);
        blendSourceAlpha = glGetInteger(GL_BLEND_SRC_ALPHA);
        blendDestinationAlpha = glGetInteger(GL_BLEND_DST_ALPHA);
        blendEquationRgb = glGetInteger(GL_BLEND_EQUATION_RGB);
        blendEquationAlpha = glGetInteger(GL_BLEND_EQUATION_ALPHA);
        depthFunction = glGetInteger(GL_DEPTH_FUNC);
        depthWriteEnabled = glGetInteger(GL_DEPTH_WRITEMASK) != 0;
        cullFaceMode = glGetInteger(GL_CULL_FACE_MODE);
        frontFaceWinding = glGetInteger(GL_FRONT_FACE);
        program = glGetInteger(GL_CURRENT_PROGRAM);
        vertexArray = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        arrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING);
        uniformBuffer = glGetInteger(GL_UNIFORM_BUFFER_BINDING);
        drawIndirectBuffer = glGetInteger(GL_DRAW_INDIRECT_BUFFER_BINDING);
        drawFramebuffer = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        lineWidth = glGetFloat(GL_LINE_WIDTH);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer mask = stack.malloc(4);
            glGetBooleanv(GL_COLOR_WRITEMASK, mask);
            for (int channel = 0; channel < colorWriteMask.length; channel++) {
                colorWriteMask[channel] = mask.get(channel) != 0;
            }
        }
        activeTexture = glGetInteger(GL_ACTIVE_TEXTURE);
        for (int unit = 0; unit < TEXTURE_UNIT_COUNT; unit++) {
            glActiveTexture(GL_TEXTURE0 + unit);
            textures[unit] = glGetInteger(GL_TEXTURE_BINDING_2D);
            samplers[unit] = glGetInteger(GL_SAMPLER_BINDING);
            if (textures[unit] != 0) {
                textureBaseLevels[unit] = glGetTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL);
                textureMaxLevels[unit] = glGetTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL);
            }
        }
        glActiveTexture(activeTexture);
    }

    /** Restores the state restored by the original method, in its original order. */
    public void restore() {
        glUseProgram(program);
        glBindVertexArray(vertexArray);
        glBindBuffer(GL_ARRAY_BUFFER, arrayBuffer);
        glBindBuffer(GL_UNIFORM_BUFFER, uniformBuffer);
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, drawIndirectBuffer);
        glLineWidth(lineWidth);
        setCapability(GL_BLEND, blendEnabled);
        setCapability(GL_DEPTH_TEST, depthTestEnabled);
        setCapability(GL_CULL_FACE, cullFaceEnabled);
        setCapability(GL_SCISSOR_TEST, scissorTestEnabled);
        glBlendFuncSeparate(blendSourceRgb, blendDestinationRgb,
                            blendSourceAlpha, blendDestinationAlpha);
        glBlendEquationSeparate(blendEquationRgb, blendEquationAlpha);
        glDepthFunc(depthFunction);
        GL11C.glDepthMask(depthWriteEnabled);
        glCullFace(cullFaceMode);
        glFrontFace(frontFaceWinding);
        glColorMask(colorWriteMask[0], colorWriteMask[1], colorWriteMask[2], colorWriteMask[3]);
        for (int unit = 0; unit < TEXTURE_UNIT_COUNT; unit++) {
            glActiveTexture(GL_TEXTURE0 + unit);
            glBindTexture(GL_TEXTURE_2D, textures[unit]);
            if (textures[unit] != 0) {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, textureBaseLevels[unit]);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, textureMaxLevels[unit]);
            }
            glBindSampler(unit, samplers[unit]);
        }
        glActiveTexture(activeTexture);
    }

    private static void setCapability(int capability, boolean enabled) {
        if (enabled) {
            glEnable(capability);
        } else {
            glDisable(capability);
        }
    }
}
