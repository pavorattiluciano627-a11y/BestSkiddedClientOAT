package dev.echo.render.vulkan;

import dev.echo.render.buffer.Buffer;
import java.nio.LongBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkBufferCreateInfo;

/** VMA buffer with 256-byte frame strides and a single owned mapped allocation. */
public class VulkanBuffer extends Buffer {
    public final long bufferHandle;
    private final long allocation;
    private final long mappedBaseAddress;
    private final VulkanDevice device;
    private final long frameStride;

    public VulkanBuffer(VulkanDevice device, int usage, long size, boolean dynamic, boolean mapped) {
        super(usage, size, dynamic, mapped, dynamic ? device.framesInFlight : 1);
        this.device = device;
        frameStride = (size + 255L) & ~255L;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long allocationSize = dynamic ? frameStride * device.framesInFlight : size;
            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack).sType$Default()
                    .size(allocationSize).usage(vkBufferUsage(usage)).sharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE);
            VmaAllocationCreateInfo allocationInfo = VmaAllocationCreateInfo.calloc(stack)
                    .usage(mapped ? 9 : 8).flags(mapped ? 1028 : 0);
            LongBuffer bufferResult = stack.mallocLong(1);
            PointerBuffer allocationResult = stack.mallocPointer(1);
            VulkanChecks.checkSuccess(Vma.vmaCreateBuffer(device.allocator, bufferInfo, allocationInfo,
                    bufferResult, allocationResult, null), "Failed to create buffer");
            bufferHandle = bufferResult.get(0);
            allocation = allocationResult.get(0);
            if (mapped) {
                PointerBuffer addressResult = stack.mallocPointer(1);
                VulkanChecks.checkSuccess(Vma.vmaMapMemory(device.allocator, allocation, addressResult),
                        "Failed to map buffer");
                mappedBaseAddress = addressResult.get(0);
            } else {
                mappedBaseAddress = 0;
            }
        }
    }

    private static int vkBufferUsage(int usage) {
        int flags = 0;
        if ((usage & 1) != 0) flags |= VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
        if ((usage & 32) != 0) flags |= VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT;
        if ((usage & 2) != 0) flags |= VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT;
        if ((usage & 4) != 0) flags |= VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
        if ((usage & 8) != 0) flags |= VK10.VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT;
        if ((usage & 16) != 0) flags |= VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
        return flags;
    }

    public long frameOffset() {
        return dynamic ? (long) device.frameIndex() * frameStride : 0;
    }

    @Override
    protected int frameIndex() {
        return dynamic ? device.frameIndex() : 0;
    }

    @Override
    public long mappedAddress() {
        return mappedBaseAddress + frameOffset();
    }

    @Override
    public void flush(long offset, long length) {
        Vma.vmaFlushAllocation(device.allocator, allocation, frameOffset() + offset, length);
    }

    @Override
    public void destroy() {
        if (mappedBaseAddress != 0) Vma.vmaUnmapMemory(device.allocator, allocation);
        Vma.vmaDestroyBuffer(device.allocator, bufferHandle, allocation);
    }
}
