package dev.echo.render.vulkan;

import dev.echo.render.buffer.BufferSlice;
import dev.echo.render.command.TransferPass;
import dev.echo.render.texture.TextureAccess;
import dev.echo.render.texture.TextureLayout;
import dev.echo.render.texture.TextureView;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkBufferImageCopy;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkImageMemoryBarrier;

/** Texture copies recorded on the current command buffer or on an owned transfer-pool buffer. */
public class VulkanTransferPass extends TransferPass {
    private final VkCommandBuffer commandBuffer;
    private final boolean dedicatedTransfer;
    private final VulkanDevice device;

    public VulkanTransferPass(VulkanDevice device, boolean dedicatedTransfer) {
        this.device = device;
        commandBuffer = dedicatedTransfer ? device.host.transferPool().begin() : device.host.currentCommandBuffer();
        this.dedicatedTransfer = dedicatedTransfer;
    }

    @Override
    public void uploadTexture(TextureView destination, BufferSlice source) {
        VulkanTextureView view = (VulkanTextureView) destination;
        VulkanBuffer buffer = (VulkanBuffer) source.buffer();
        if (buffer.mapped) buffer.flush(source.offset(), source.length());
        int width = view.width(0);
        int height = view.height(0);
        int depth = view.depth(0);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferImageCopy.Buffer copy = VkBufferImageCopy.calloc(1, stack)
                    .bufferOffset(buffer.frameOffset() + source.offset()).bufferRowLength(0).bufferImageHeight(0);
            copy.imageSubresource().aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
                    .mipLevel(view.baseMipLevel).baseArrayLayer(0).layerCount(1);
            copy.imageOffset().set(0, 0, 0);
            copy.imageExtent().set(width, height, depth);
            if (!dedicatedTransfer) device.barriers.flush(commandBuffer);
            int oldLayout = VulkanCommandEncoder.vkImageLayout(destination.layout);
            boolean discardContents = oldLayout == VK10.VK_IMAGE_LAYOUT_UNDEFINED || dedicatedTransfer;
            int copyLayout = destination.keepGeneralLayout ? VK10.VK_IMAGE_LAYOUT_GENERAL : VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
            int finalLayout = destination.keepGeneralLayout ? VK10.VK_IMAGE_LAYOUT_GENERAL : VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
            transition(stack, view, discardContents ? VK10.VK_IMAGE_LAYOUT_UNDEFINED : oldLayout, copyLayout,
                    discardContents ? 0 : VK10.VK_ACCESS_SHADER_READ_BIT, VK10.VK_ACCESS_TRANSFER_WRITE_BIT,
                    discardContents ? VK10.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT : VK10.VK_PIPELINE_STAGE_ALL_COMMANDS_BIT,
                    VK10.VK_PIPELINE_STAGE_TRANSFER_BIT);
            VK10.vkCmdCopyBufferToImage(commandBuffer, buffer.bufferHandle, view.image, copyLayout, copy);
            transition(stack, view, copyLayout, finalLayout, VK10.VK_ACCESS_TRANSFER_WRITE_BIT,
                    dedicatedTransfer ? 0 : VK10.VK_ACCESS_SHADER_READ_BIT, VK10.VK_PIPELINE_STAGE_TRANSFER_BIT,
                    dedicatedTransfer ? VK10.VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT : VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT);
            destination.layout = destination.keepGeneralLayout ? TextureLayout.GENERAL : TextureLayout.SHADER_READ_ONLY;
            destination.access = TextureAccess.SAMPLED;
            destination.stages = 2;
        }
    }

    private void transition(MemoryStack stack, VulkanTextureView view, int oldLayout, int newLayout,
                            int sourceAccess, int destinationAccess, int sourceStages, int destinationStages) {
        VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack).sType$Default()
                .srcAccessMask(sourceAccess).dstAccessMask(destinationAccess).oldLayout(oldLayout).newLayout(newLayout)
                .srcQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED).dstQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                .image(view.image);
        barrier.subresourceRange().aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT).baseMipLevel(view.baseMipLevel)
                .levelCount(view.mipCount).baseArrayLayer(0).layerCount(1);
        VK10.vkCmdPipelineBarrier(commandBuffer, sourceStages, destinationStages, 0, null, null, barrier);
    }

    @Override
    public void end() {
        if (dedicatedTransfer) {
            TransferCommandPool pool = device.host.transferPool();
            pool.submit(commandBuffer);
            pool.waitIdle();
            pool.freeCommandBuffer(commandBuffer);
        }
    }
}
