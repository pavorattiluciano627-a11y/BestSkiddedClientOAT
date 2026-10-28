package dev.echo.render.vulkan;

import dev.echo.render.buffer.BufferSlice;
import dev.echo.render.command.RenderPass;
import dev.echo.render.pipeline.GraphicsPipeline;
import dev.echo.render.sampler.Sampler;
import dev.echo.render.surface.RenderTargetLease;
import dev.echo.render.texture.TextureView;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkViewport;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.KHRDynamicRendering.vkCmdEndRenderingKHR;

public class VulkanRenderPass extends RenderPass {
    private final VkCommandBuffer commandBuffer;
    private final VulkanDevice device;
    private final PushDescriptorBatch descriptors;
    private RenderTargetLease renderTargetLease;
    private VulkanGraphicsPipeline pipeline;
    private boolean yFlipped;
    private int targetHeight;
    private TextureView colorAttachment;
    private TextureView depthAttachment;

    public VulkanRenderPass(VkCommandBuffer commandBuffer, VulkanDevice device, RenderTargetLease lease) {
        this.commandBuffer = commandBuffer;
        this.device = device;
        renderTargetLease = lease;
        descriptors = device.graphicsDescriptors;
        descriptors.discardPendingWrites();
        if (lease != null) {
            yFlipped = lease.isYFlipped();
            targetHeight = lease.height();
        }
    }

    public VulkanRenderPass(VkCommandBuffer commandBuffer, VulkanDevice device, TextureView color) {
        this(commandBuffer, device, (RenderTargetLease) null);
        colorAttachment = color;
        yFlipped = color.yFlipped;
        targetHeight = color.height(0);
    }

    @Override
    public void setAttachments(TextureView color, TextureView depth) {
        endRendering();
        renderTargetLease = null;
        device.barriers.flush(commandBuffer);
        device.beginRendering(commandBuffer, color, OptionalInt.empty(), depth, OptionalDouble.empty());
        colorAttachment = color;
        depthAttachment = depth;
        yFlipped = color.yFlipped;
        targetHeight = color.height(0);
    }

    @Override public boolean isYFlipped() { return yFlipped; }

    @Override
    public void setPipeline(GraphicsPipeline pipeline) {
        if (pipeline == this.pipeline) return;
        descriptors.discardPendingWrites();
        VulkanGraphicsPipeline nativePipeline = (VulkanGraphicsPipeline) pipeline;
        nativePipeline.bind(commandBuffer, yFlipped);
        this.pipeline = nativePipeline;
    }

    @Override
    public void setScissor(int x, int y, int width, int height) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
            scissor.offset().set(x, yFlipped ? targetHeight - (y + height) : y);
            scissor.extent().set(width, height);
            vkCmdSetScissor(commandBuffer, 0, scissor);
        }
    }

    @Override
    public void setVertexBuffer(BufferSlice slice) {
        super.setVertexBuffer(slice);
        if ((slice.buffer().usage & 1) == 0) throw new IllegalStateException("Buffer must be a vertex buffer");
        VulkanBuffer buffer = (VulkanBuffer) slice.buffer();
        if (buffer.mapped) buffer.flush(slice.offset(), slice.length());
        try (MemoryStack stack = MemoryStack.stackPush()) {
            vkCmdBindVertexBuffers(commandBuffer, 0, stack.longs(buffer.bufferHandle),
                    stack.longs(buffer.frameOffset() + slice.offset()));
        }
    }

    @Override
    public void setIndexBuffer(BufferSlice slice) {
        super.setIndexBuffer(slice);
        VulkanBuffer buffer = (VulkanBuffer) slice.buffer();
        if (buffer.mapped) buffer.flush(slice.offset(), slice.length());
        vkCmdBindIndexBuffer(commandBuffer, buffer.bufferHandle, buffer.frameOffset() + slice.offset(), VK_INDEX_TYPE_UINT32);
    }

    @Override
    public void setBuffer(int binding, BufferSlice slice) {
        VulkanBuffer buffer = (VulkanBuffer) slice.buffer();
        if (buffer.mapped) buffer.flush(slice.offset(), slice.length());
        descriptors.writeBuffer(binding, pipeline.bindingKind(binding), buffer, slice.offset(), slice.length());
    }

    @Override
    public void setTexture(int binding, TextureView view) {
        super.setTexture(binding, view);
        descriptors.writeImage(binding, pipeline.bindingKind(binding), ((VulkanTextureView) view).imageView, view.layout);
    }

    @Override
    public void setTextures(int binding, TextureView[] views) {
        super.setTextures(binding, views);
        long[] handles = new long[views.length];
        for (int index = 0; index < views.length; index++) handles[index] = ((VulkanTextureView) views[index]).imageView;
        descriptors.writeImageArray(binding, handles);
    }

    @Override public void setSampler(int binding, Sampler sampler) {
        descriptors.writeSampler(binding, ((VulkanSampler) sampler).sampler);
    }

    @Override public void setPushConstants(long address, int bytes) {
        nvkCmdPushConstants(commandBuffer, pipeline.pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
                0, bytes, address);
    }

    @Override
    public void drawInstanced(int vertices, int instances, int firstVertex, int firstInstance) {
        device.barriers.requireNoPendingBarriers();
        descriptors.flush(commandBuffer, pipeline.pipelineLayout);
        pipeline.draw(commandBuffer, vertices, instances, firstVertex, firstInstance);
    }

    @Override
    public void drawIndexed(int indices, int instances, int firstIndex) {
        device.barriers.requireNoPendingBarriers();
        descriptors.flush(commandBuffer, pipeline.pipelineLayout);
        pipeline.drawIndexed(commandBuffer, indices, instances, firstIndex);
    }

    @Override
    public void drawIndirect(BufferSlice commands, int drawCount, int stride) {
        super.drawIndirect(commands, drawCount, stride);
        if ((commands.buffer().usage & 8) == 0) throw new IllegalStateException("Buffer must be an indirect buffer");
        VulkanBuffer buffer = (VulkanBuffer) commands.buffer();
        device.barriers.requireNoPendingBarriers();
        descriptors.flush(commandBuffer, pipeline.pipelineLayout);
        pipeline.drawIndirect(commandBuffer, buffer.bufferHandle, buffer.frameOffset() + commands.offset(), drawCount, stride);
    }

    @Override
    public void setViewport(int x, int y, int width, int height) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkViewport.Buffer viewport = VkViewport.calloc(1, stack).x(x)
                    .y(yFlipped ? targetHeight - (y + height) : y + height).width(width)
                    .height(yFlipped ? height : -height).minDepth(0).maxDepth(1);
            vkCmdSetViewport(commandBuffer, 0, viewport);
        }
    }

    @Override public void end() {
        endRendering();
        device.barriers.flush(commandBuffer);
    }

    private void endRendering() {
        if (renderTargetLease == null) vkCmdEndRenderingKHR(commandBuffer);
        else renderTargetLease.close();
        device.barriers.insideRenderPass(false);
        releaseHostAttachment(colorAttachment, 256, 1024);
        releaseHostAttachment(depthAttachment, 1024, 768);
    }

    private void releaseHostAttachment(TextureView view, int sourceAccess, int sourceStages) {
        if (view == null || !view.keepGeneralLayout) return;
        VulkanTextureView nativeView = (VulkanTextureView) view;
        VulkanTexture texture = (VulkanTexture) view.texture;
        device.barriers.imageBarrier(nativeView.image, VulkanTexture.aspectMask(texture.vkFormat), view.baseMipLevel,
                view.mipCount, sourceAccess, 98304, 1, 1, sourceStages, 65536);
    }
}
