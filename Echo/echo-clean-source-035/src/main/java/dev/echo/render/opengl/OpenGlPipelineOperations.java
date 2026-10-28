package dev.echo.render.opengl;

import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GL43C;

/** Direct operations on an already-created program or graphics pipeline. */
final class OpenGlPipelineOperations {
    private OpenGlPipelineOperations() {
    }

    static void bindComputeProgram(int programId) {
        GL20C.glUseProgram(programId);
    }

    static void dispatchCompute(int x, int y, int z) {
        GL43C.glDispatchCompute(x, y, z);
    }

    static void deleteComputeProgram(int programId) {
        GL20C.glDeleteProgram(programId);
    }

    static void deleteGraphicsPipeline(int vertexArrayId, int programId) {
        GL33C.glDeleteVertexArrays(vertexArrayId);
        GL33C.glDeleteProgram(programId);
    }
}
