package dev.echo.render.vulkan;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkBufferMemoryBarrier;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkImageMemoryBarrier;

/**
 * Conditional reconstruction, not approved production source. Promotion requires
 * the append/flush guard and key invariants listed in needs.json. Direct native
 * calls, access/stage aggregation and allocation ownership are preserved here.
 */
public final class BarrierBatch {
    private int bufferCapacity = 64;
    private int imageCapacity = 64;
    private VkBufferMemoryBarrier.Buffer bufferBarriers = VkBufferMemoryBarrier.calloc(bufferCapacity);
    private VkImageMemoryBarrier.Buffer imageBarriers = VkImageMemoryBarrier.calloc(imageCapacity);
    private int bufferCount;
    private int imageCount;
    private int sourceStages;
    private int destinationStages;
    private boolean insideRenderPass;

    public void bufferBarrier(long buffer, long offset, long size, int sourceAccess,
            int destinationAccess, int sourceStageMask, int destinationStageMask) {
        if (bufferCount == bufferCapacity) {
            VkBufferMemoryBarrier.Buffer previous = bufferBarriers;
            bufferCapacity *= 2;
            bufferBarriers = VkBufferMemoryBarrier.calloc(bufferCapacity);
            MemoryUtil.memCopy(previous.address(), bufferBarriers.address(),
                    (long) bufferCount * VkBufferMemoryBarrier.SIZEOF);
            previous.free();
        }
        bufferBarriers.get(bufferCount++).sType$Default()
                .srcAccessMask(sourceAccess).dstAccessMask(destinationAccess)
                .srcQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                .buffer(buffer).offset(offset).size(size);
        sourceStages |= sourceStageMask;
        destinationStages |= destinationStageMask;
    }

    public void imageBarrier(long image, int aspectMask, int baseMipLevel, int levelCount,
            int sourceAccess, int destinationAccess, int oldLayout, int newLayout,
            int sourceStageMask, int destinationStageMask) {
        if (imageCount == imageCapacity) {
            VkImageMemoryBarrier.Buffer previous = imageBarriers;
            imageCapacity *= 2;
            imageBarriers = VkImageMemoryBarrier.calloc(imageCapacity);
            MemoryUtil.memCopy(previous.address(), imageBarriers.address(),
                    (long) imageCount * VkImageMemoryBarrier.SIZEOF);
            previous.free();
        }
        imageBarriers.get(imageCount++).sType$Default()
                .srcAccessMask(sourceAccess).dstAccessMask(destinationAccess)
                .oldLayout(oldLayout).newLayout(newLayout)
                .srcQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED).image(image)
                .subresourceRange(range -> range.aspectMask(aspectMask)
                        .baseMipLevel(baseMipLevel).levelCount(levelCount)
                        .baseArrayLayer(0).layerCount(1));
        sourceStages |= sourceStageMask;
        destinationStages |= destinationStageMask;
    }

    /** The original checks pending counts; it does not test the active-pass flag here. */
    public void requireNoPendingBarriers() {
        if (bufferCount != 0 || imageCount != 0) {
            throw new IllegalStateException("pending barriers at a draw inside a render pass");
        }
    }

    public void flush(VkCommandBuffer commands) {
        if (insideRenderPass) {
            throw new IllegalStateException("barrier flush inside an active render pass");
        }
        if (bufferCount == 0 && imageCount == 0) {
            return;
        }
        VK10.vkCmdPipelineBarrier(commands, sourceStages, destinationStages, 0, null,
                bufferBarriers.limit(bufferCount), imageBarriers.limit(imageCount));
        bufferBarriers.limit(bufferCapacity);
        imageBarriers.limit(imageCapacity);
        bufferCount = 0;
        imageCount = 0;
        sourceStages = 0;
        destinationStages = 0;
    }

    /** One-time native disposal; the original does not reset counts or wait for a queue. */
    public void destroy() {
        bufferBarriers.free();
        imageBarriers.free();
    }

    public void insideRenderPass(boolean active) {
        insideRenderPass = active;
    }
}
