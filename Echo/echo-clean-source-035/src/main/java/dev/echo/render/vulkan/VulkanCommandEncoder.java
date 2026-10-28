package dev.echo.render.vulkan;

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
import org.lwjgl.vulkan.VkCommandBuffer;
import static dev.echo.render.vulkan.VulkanAccessMasks.*;
import static dev.echo.render.vulkan.VulkanSynchronization.*;

public class VulkanCommandEncoder extends CommandEncoder {
    public final VulkanDevice device;
    private final VkCommandBuffer commandBuffer;

    VulkanCommandEncoder(VulkanDevice device) { this(device, null); }

    VulkanCommandEncoder(VulkanDevice device, VkCommandBuffer commandBuffer) {
        this.device = device;
        this.commandBuffer = commandBuffer;
    }

    public VkCommandBuffer commandBuffer() {
        return commandBuffer != null ? commandBuffer : device.host.currentCommandBuffer();
    }

    /** Native layout conversion shared with transfer passes. Null retains UNDEFINED. */
    public static int vkImageLayout(TextureLayout layout) {
        return VulkanAccessMasks.imageLayout(layout);
    }

    @Override public TransferPass beginTransfer() { return new VulkanTransferPass(device, false); }
    @Override public ComputePass beginComputePass() { return new VulkanComputePass(commandBuffer(), device); }

    @Override
    public RenderPass beginExternalRenderPass(Supplier<RenderTargetLease> target) {
        VkCommandBuffer command = commandBuffer();
        device.barriers.flush(command);
        RenderTargetLease lease = target.get();
        device.barriers.insideRenderPass(true);
        VulkanRenderPass pass = new VulkanRenderPass(command, device, lease);
        int width = lease.width();
        int height = lease.height();
        pass.setViewport(0, 0, width, height);
        pass.setScissor(0, 0, width, height);
        return pass;
    }

    @Override
    public RenderPass beginRenderPass(TextureView color, OptionalInt clearColor,
                                      TextureView depth, OptionalDouble clearDepth) {
        VkCommandBuffer command = commandBuffer();
        device.barriers.flush(command);
        device.beginRendering(command, color, clearColor, depth, clearDepth);
        return new VulkanRenderPass(command, device, color);
    }

    @Override
    public void transitionBuffer(BufferSlice slice, int stages, BufferAccess access) {
        Buffer buffer = slice.buffer();
        VulkanBuffer nativeBuffer = (VulkanBuffer) buffer;
        long begin = slice.offset();
        long end = begin + slice.length();
        for (BufferRangeState previous : buffer.overlappingRanges(begin, slice.length())) {
            if (previous.access == BufferAccess.UNDEFINED) continue;
            if (previous.access == access && !writes(previous.access)) continue;
            long overlapBegin = Math.max(previous.offset, begin);
            long overlapEnd = Math.min(previous.endOffset(), end);
            device.barriers.bufferBarrier(nativeBuffer.bufferHandle, nativeBuffer.frameOffset() + overlapBegin,
                    overlapEnd - overlapBegin, bufferAccessMask(previous.access), bufferAccessMask(access),
                    bufferStages(previous.access, previous.stages), bufferStages(access, stages));
        }
        buffer.setRangeState(begin, slice.length(), access, stages);
    }

    @Override
    public void transitionTexture(TextureView view, int stages, TextureAccess access, TextureLayout layout) {
        int sourceAccess = textureAccessMask(view.access);
        int sourceStages = textureStages(view.access, view.stages);
        if (view.keepGeneralLayout) {
            layout = TextureLayout.GENERAL;
            sourceAccess = 65536;
            sourceStages = 65536;
        } else if (view.access == access && !writes(view.access) && (layout == null || view.layout == layout)) {
            return;
        }
        VulkanTextureView nativeView = (VulkanTextureView) view;
        VulkanTexture texture = (VulkanTexture) view.texture;
        device.barriers.imageBarrier(nativeView.image, VulkanTexture.aspectMask(texture.vkFormat),
                view.baseMipLevel, view.mipCount, sourceAccess, textureAccessMask(access),
                VulkanAccessMasks.imageLayout(view.layout), VulkanAccessMasks.imageLayout(layout == null ? view.layout : layout),
                sourceStages, textureStages(access, stages));
        view.access = access;
        view.stages = stages;
        if (layout != null) view.layout = layout;
    }
}
