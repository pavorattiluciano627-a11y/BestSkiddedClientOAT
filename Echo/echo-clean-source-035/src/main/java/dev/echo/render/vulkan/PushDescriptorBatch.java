package dev.echo.render.vulkan;

import dev.echo.render.texture.TextureLayout;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.KHRPushDescriptor;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

/** Group-zero push descriptors with relocatable native arenas and deferred arena reclamation. */
public class PushDescriptorBatch {
    private VkWriteDescriptorSet.Buffer writes;
    private VkDescriptorBufferInfo.Buffer bufferInfos;
    private VkDescriptorImageInfo.Buffer imageInfos;
    private final int pipelineBindPoint;
    private int writeCount;
    private int writeCapacity = 8;
    private int bufferInfoCount;
    private int bufferInfoCapacity = 8;
    private int imageInfoCount;
    private int imageInfoCapacity = 32;
    private int submittedWriteCount;
    private final List<Runnable> retiredArenaReleases = new ArrayList<>();

    public PushDescriptorBatch(int pipelineBindPoint) {
        this.pipelineBindPoint = pipelineBindPoint;
        writes = VkWriteDescriptorSet.calloc(writeCapacity);
        bufferInfos = VkDescriptorBufferInfo.calloc(bufferInfoCapacity);
        imageInfos = VkDescriptorImageInfo.calloc(imageInfoCapacity);
    }

    private void growWrites() {
        VkWriteDescriptorSet.Buffer previous = writes;
        writeCapacity *= 2;
        writes = VkWriteDescriptorSet.calloc(writeCapacity);
        MemoryUtil.memCopy(previous.address(), writes.address(), (long) writeCount * VkWriteDescriptorSet.SIZEOF);
        retiredArenaReleases.add(previous::free);
    }

    private void growBufferInfos() {
        VkDescriptorBufferInfo.Buffer previous = bufferInfos;
        long previousAddress = previous.address();
        bufferInfoCapacity *= 2;
        bufferInfos = VkDescriptorBufferInfo.calloc(bufferInfoCapacity);
        long nextAddress = bufferInfos.address();
        MemoryUtil.memCopy(previousAddress, nextAddress, (long) bufferInfoCount * VkDescriptorBufferInfo.SIZEOF);
        for (int index = submittedWriteCount; index < writeCount; index++) {
            VkWriteDescriptorSet write = writes.get(index);
            VkDescriptorBufferInfo.Buffer info = write.pBufferInfo();
            if (info != null) {
                long relocatedAddress = nextAddress + (info.address() - previousAddress);
                write.pBufferInfo(VkDescriptorBufferInfo.create(relocatedAddress, write.descriptorCount()));
            }
        }
        retiredArenaReleases.add(previous::free);
    }

    private void growImageInfos() {
        VkDescriptorImageInfo.Buffer previous = imageInfos;
        long previousAddress = previous.address();
        imageInfoCapacity *= 2;
        imageInfos = VkDescriptorImageInfo.calloc(imageInfoCapacity);
        long nextAddress = imageInfos.address();
        MemoryUtil.memCopy(previousAddress, nextAddress, (long) imageInfoCount * VkDescriptorImageInfo.SIZEOF);
        for (int index = submittedWriteCount; index < writeCount; index++) {
            VkWriteDescriptorSet write = writes.get(index);
            VkDescriptorImageInfo.Buffer info = write.pImageInfo();
            if (info != null) {
                long relocatedAddress = nextAddress + (info.address() - previousAddress);
                write.pImageInfo(VkDescriptorImageInfo.create(relocatedAddress, write.descriptorCount()));
            }
        }
        retiredArenaReleases.add(previous::free);
    }

    public void writeBuffer(int binding, int descriptorType, VulkanBuffer buffer, long offset, long length) {
        if (bufferInfoCount == bufferInfoCapacity) growBufferInfos();
        if (writeCount == writeCapacity) growWrites();
        int index = bufferInfoCount++;
        bufferInfos.get(index).buffer(buffer.bufferHandle).offset(buffer.frameOffset() + offset).range(length);
        writes.get(writeCount++).sType$Default().dstSet(0).dstBinding(binding).dstArrayElement(0)
                .descriptorType(descriptorType).descriptorCount(1).pImageInfo(null).pTexelBufferView(null)
                .pBufferInfo(VkDescriptorBufferInfo.create(bufferInfos.address(index), 1));
    }

    public void writeImage(int binding, int descriptorType, long imageView, TextureLayout layout) {
        if (imageInfoCount == imageInfoCapacity) growImageInfos();
        if (writeCount == writeCapacity) growWrites();
        int vkLayout = descriptorType == VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE || layout == TextureLayout.GENERAL
                ? VK10.VK_IMAGE_LAYOUT_GENERAL : VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
        int index = imageInfoCount++;
        imageInfos.get(index).imageView(imageView).imageLayout(vkLayout);
        writes.get(writeCount++).sType$Default().dstSet(0).dstBinding(binding).dstArrayElement(0)
                .descriptorType(descriptorType).descriptorCount(1).pBufferInfo(null).pTexelBufferView(null)
                .pImageInfo(VkDescriptorImageInfo.create(imageInfos.address(index), 1));
    }

    public void writeImageArray(int binding, long[] imageViews) {
        while (imageInfoCount + imageViews.length > imageInfoCapacity) growImageInfos();
        if (writeCount == writeCapacity) growWrites();
        int first = imageInfoCount;
        for (long imageView : imageViews) {
            imageInfos.get(imageInfoCount++).imageView(imageView).imageLayout(VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        }
        writes.get(writeCount++).sType$Default().dstSet(0).dstBinding(binding).dstArrayElement(0)
                .descriptorType(VK10.VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE).descriptorCount(imageViews.length)
                .pBufferInfo(null).pTexelBufferView(null)
                .pImageInfo(VkDescriptorImageInfo.create(imageInfos.address(first), imageViews.length));
    }

    public void writeSampler(int binding, long sampler) {
        if (imageInfoCount == imageInfoCapacity) growImageInfos();
        if (writeCount == writeCapacity) growWrites();
        int index = imageInfoCount++;
        imageInfos.get(index).sampler(sampler);
        writes.get(writeCount++).sType$Default().dstSet(0).dstBinding(binding).dstArrayElement(0)
                .descriptorType(VK10.VK_DESCRIPTOR_TYPE_SAMPLER).descriptorCount(1)
                .pBufferInfo(null).pTexelBufferView(null)
                .pImageInfo(VkDescriptorImageInfo.create(imageInfos.address(index), 1));
    }

    public void flush(VkCommandBuffer commandBuffer, long pipelineLayout) {
        if (writeCount == submittedWriteCount) return;
        VkWriteDescriptorSet.Buffer pending = VkWriteDescriptorSet.create(writes.address(submittedWriteCount),
                writeCount - submittedWriteCount);
        KHRPushDescriptor.vkCmdPushDescriptorSetKHR(commandBuffer, pipelineBindPoint, pipelineLayout, 0, pending);
        submittedWriteCount = writeCount;
    }

    /** Drops the unsubmitted writes on a pipeline change; arena space is reclaimed by reset. */
    public void discardPendingWrites() {
        writeCount = submittedWriteCount;
    }

    public void reset() {
        retiredArenaReleases.forEach(Runnable::run);
        retiredArenaReleases.clear();
        writeCount = 0;
        bufferInfoCount = 0;
        imageInfoCount = 0;
        submittedWriteCount = 0;
    }

    /** Matches the original lifetime contract: reset releases retired arenas before destroy. */
    public void destroy() {
        writes.free();
        bufferInfos.free();
        imageInfos.free();
    }
}
