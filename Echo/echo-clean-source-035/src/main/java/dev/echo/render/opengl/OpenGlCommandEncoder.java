package dev.echo.render.opengl;

import dev.echo.render.buffer.Buffer;
import dev.echo.render.buffer.BufferAccess;
import dev.echo.render.buffer.BufferRangeState;
import dev.echo.render.buffer.BufferSlice;
import dev.echo.render.command.CommandEncoder;
import dev.echo.render.command.ComputePass;
import dev.echo.render.command.RenderPass;
import dev.echo.render.command.TransferPass;
import dev.echo.render.surface.RenderTargetLease;
import dev.echo.render.texture.TextureAccess;
import dev.echo.render.texture.TextureLayout;
import dev.echo.render.texture.TextureView;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;
import static org.lwjgl.opengl.GL42C.glMemoryBarrier;

public class OpenGlCommandEncoder extends CommandEncoder {
    private final OpenGlDevice device;

    OpenGlCommandEncoder(OpenGlDevice device) { this.device = device; }
    @Override public TransferPass beginTransfer() { return new OpenGlTransferPass(); }
    @Override public ComputePass beginComputePass() { return new OpenGlComputePass(); }

    @Override
    public RenderPass beginExternalRenderPass(Supplier<RenderTargetLease> target) {
        return new OpenGlRenderPass(device, target.get());
    }

    @Override
    public RenderPass beginRenderPass(TextureView color, OptionalInt clearColor,
                                      TextureView depth, OptionalDouble clearDepth) {
        OpenGlRenderPass pass = new OpenGlRenderPass(device, null);
        pass.setAttachments(color, depth);
        pass.clearAttachments(clearColor, depth == null ? OptionalDouble.empty() : clearDepth);
        return pass;
    }

    @Override
    public void transitionBuffer(BufferSlice slice, int stages, BufferAccess access) {
        Buffer buffer = slice.buffer();
        for (BufferRangeState previous : buffer.overlappingRanges(slice.offset(), slice.length())) {
            if (previous.access == BufferAccess.STORAGE_WRITE) {
                glMemoryBarrier(OpenGlMemoryBarriers.bufferConsumerBits(access));
                break;
            }
        }
        buffer.setRangeState(slice.offset(), slice.length(), access, stages);
    }

    @Override
    public void transitionTexture(TextureView view, int stages, TextureAccess access, TextureLayout layout) {
        if (view.access == TextureAccess.STORAGE_WRITE) {
            glMemoryBarrier(OpenGlMemoryBarriers.textureConsumerBits(access));
        }
        view.access = access;
        if (layout != null) view.layout = layout;
        // OpenGL's original implementation does not update the view's Vulkan stage mask.
    }
}
