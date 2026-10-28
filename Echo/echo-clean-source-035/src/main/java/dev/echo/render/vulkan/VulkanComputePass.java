package dev.echo.render.vulkan;

import dev.echo.render.buffer.BufferSlice;
import dev.echo.render.command.ComputePass;
import dev.echo.render.pipeline.ComputePipeline;
import dev.echo.render.sampler.Sampler;
import dev.echo.render.texture.TextureView;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;

/** Compute commands sharing the device's barrier and deferred push-descriptor batches. */
public class VulkanComputePass extends ComputePass {
    private final VkCommandBuffer commandBuffer;
    private final VulkanDevice device;
    private final PushDescriptorBatch descriptors;
    private VulkanComputePipeline pipeline;

    public VulkanComputePass(VkCommandBuffer commandBuffer, VulkanDevice device) {
        this.commandBuffer = commandBuffer;
        this.device = device;
        descriptors = device.computeDescriptors;
        descriptors.discardPendingWrites();
    }

    @Override public void setPipeline(ComputePipeline pipeline) {
        if (pipeline == this.pipeline) {
            return;
        }
        descriptors.discardPendingWrites();
        VulkanComputePipeline vkPipeline = (VulkanComputePipeline) pipeline;
        vkPipeline.bind(commandBuffer);
        this.pipeline = vkPipeline;
    }

    @Override public void setBuffer(int binding, BufferSlice slice) {
        super.setBuffer(binding, slice);
        VulkanBuffer buffer = (VulkanBuffer) slice.buffer();
        if (buffer.mapped) {
            buffer.flush(slice.offset(), slice.length());
        }
        descriptors.writeBuffer(binding, pipeline.bindingKind(binding), buffer,
                slice.offset(), slice.length());
    }

    @Override public void setTexture(int binding, TextureView view) {
        super.setTexture(binding, view);
        VulkanTextureView vkView = (VulkanTextureView) view;
        descriptors.writeImage(binding, pipeline.bindingKind(binding), vkView.imageView, view.layout);
    }

    @Override public void setSampler(int binding, Sampler sampler) {
        descriptors.writeSampler(binding, ((VulkanSampler) sampler).sampler);
    }

    @Override public void setPushConstants(long address, int bytes) {
        VulkanPipelineOperations.pushComputeConstants(commandBuffer, pipeline.pipelineLayout, address, bytes);
    }

    @Override public void dispatch(int x, int y, int z) {
        device.barriers.flush(commandBuffer);
        descriptors.flush(commandBuffer, pipeline.pipelineLayout);
        pipeline.dispatch(commandBuffer, x, y, z);
    }

    /** Original method is a single return; there is no Vulkan pass-end command. */
    @Override public void end() {}
}
