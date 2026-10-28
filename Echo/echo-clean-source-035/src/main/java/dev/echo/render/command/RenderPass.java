package dev.echo.render.command;

import dev.echo.render.buffer.BufferAccess;
import dev.echo.render.buffer.BufferSlice;
import dev.echo.render.pipeline.GraphicsPipeline;
import dev.echo.render.sampler.Sampler;
import dev.echo.render.texture.TextureView;
import static dev.echo.render.util.RenderChecks.require;

public abstract class RenderPass {
    public abstract void setAttachments(TextureView color, TextureView depth);
    public abstract void setPipeline(GraphicsPipeline pipeline);
    public abstract void setScissor(int x, int y, int width, int height);

    public void setVertexBuffer(BufferSlice slice) {
        require(slice.buffer().accessAt(slice.offset()) == BufferAccess.VERTEX_READ,
                "vertex buffer must be in VERTEX_READ state");
        require((slice.buffer().usage & 1) != 0, "buffer must be a vertex buffer");
    }

    public void setIndexBuffer(BufferSlice slice) {
        require(slice.buffer().accessAt(slice.offset()) == BufferAccess.INDEX_READ,
                "index buffer must be in INDEX_READ state");
        require((slice.buffer().usage & 32) != 0, "buffer must be an index buffer");
    }

    public abstract void setBuffer(int binding, BufferSlice slice);

    public void setTexture(int binding, TextureView view) {
        // Original base hook has a return-only body. Both backends override it.
    }

    public void setTextures(int binding, TextureView[] views) {
        // Original base hook has a return-only body. Both backends override it.
    }

    public abstract void setSampler(int binding, Sampler sampler);
    public abstract void setPushConstants(long address, int bytes);

    public void draw(int vertices, int instances, int firstVertex) {
        drawInstanced(vertices, instances, firstVertex, 0);
    }

    public abstract void drawInstanced(int vertices, int instances, int firstVertex, int firstInstance);
    public abstract void drawIndexed(int indices, int instances, int firstIndex);

    public void drawIndirect(BufferSlice commands, int drawCount, int stride) {
        require(commands.buffer().accessAt(commands.offset()) == BufferAccess.INDIRECT_READ,
                "indirect buffer must be in INDIRECT_READ state");
        require((commands.buffer().usage & 8) != 0, "buffer must be an indirect buffer");
    }

    public abstract void setViewport(int x, int y, int width, int height);
    public abstract boolean isYFlipped();
    public abstract void end();
}
