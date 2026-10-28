package dev.echo.render.vulkan;

import dev.echo.render.buffer.Buffer;
import dev.echo.render.buffer.BufferInfo;
import dev.echo.render.command.CommandEncoder;
import dev.echo.render.command.TransferPass;
import dev.echo.render.device.DeviceCapabilities;
import dev.echo.render.device.RenderDevice;
import dev.echo.render.pipeline.ComputePipeline;
import dev.echo.render.pipeline.ComputePipelineInfo;
import dev.echo.render.pipeline.GraphicsPipeline;
import dev.echo.render.pipeline.GraphicsPipelineInfo;
import dev.echo.render.queue.CommandQueue;
import dev.echo.render.queue.QueueInfo;
import dev.echo.render.resource.GpuResource;
import dev.echo.render.resource.ResourceLoader;
import dev.echo.render.sampler.Sampler;
import dev.echo.render.sampler.SamplerInfo;
import dev.echo.render.surface.Surface;
import dev.echo.render.surface.SurfaceInfo;
import dev.echo.render.texture.Texture;
import dev.echo.render.texture.TextureFormat;
import dev.echo.render.texture.TextureInfo;
import dev.echo.render.texture.TextureView;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR;
import static org.lwjgl.vulkan.KHRDynamicRendering.vkCmdBeginRenderingKHR;

/** Uses the host's VkDevice and allocator; owns descriptors and frame-retirement queues. */
public class VulkanDevice extends RenderDevice {
    public final VulkanHostContext host;
    public final VkDevice device;
    public final int framesInFlight;
    public final long allocator;
    private final List<ArrayDeque<GpuResource>> deferredResources;
    public final PushDescriptorBatch graphicsDescriptors;
    public final PushDescriptorBatch computeDescriptors;
    public final BarrierBatch barriers;

    public VulkanDevice(VulkanHostContext host, ResourceLoader resources, DeviceCapabilities capabilities) {
        super(resources, capabilities);
        this.host = host;
        device = host.device();
        framesInFlight = host.framesInFlight();
        allocator = host.allocator();
        deferredResources = new ArrayList<>(framesInFlight);
        for (int frame = 0; frame < framesInFlight; frame++) deferredResources.add(new ArrayDeque<>());
        graphicsDescriptors = new PushDescriptorBatch(VK_PIPELINE_BIND_POINT_GRAPHICS);
        computeDescriptors = new PushDescriptorBatch(VK_PIPELINE_BIND_POINT_COMPUTE);
        barriers = new BarrierBatch();
    }

    @Override
    public void beginFrame() {
        flushDeferredResources();
        graphicsDescriptors.reset();
        computeDescriptors.reset();
    }

    @Override public CommandEncoder commandEncoder() { return new VulkanCommandEncoder(this); }
    @Override public TransferPass beginTransfer() { return new VulkanTransferPass(this, true); }
    public int frameIndex() { return host.frameIndex(); }
    @Override public GraphicsPipeline createGraphicsPipeline(GraphicsPipelineInfo info) { return new VulkanGraphicsPipeline(this, info); }
    @Override public ComputePipeline createComputePipeline(ComputePipelineInfo info) { return new VulkanComputePipeline(this, info); }

    public static DeviceCapabilities queryCapabilities(VkDevice device, boolean reversedDepth) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDevice physical = device.getPhysicalDevice();
            VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.calloc(stack);
            vkGetPhysicalDeviceFeatures(physical, features);
            VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.calloc(stack);
            vkGetPhysicalDeviceProperties(physical, properties);
            return new DeviceCapabilities(features.multiDrawIndirect(), true,
                    properties.limits().maxDescriptorSetSampledImages() > 1, true, reversedDepth);
        }
    }

    @Override
    public CommandQueue createQueue(QueueInfo info) {
        VkPhysicalDevice physical = device.getPhysicalDevice();
        long surface = info.surface == null ? 0 : ((VulkanSurface) info.surface).surface;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer count = stack.mallocInt(1);
            vkGetPhysicalDeviceQueueFamilyProperties(physical, count, null);
            VkQueueFamilyProperties.Buffer properties = VkQueueFamilyProperties.calloc(count.get(0), stack);
            vkGetPhysicalDeviceQueueFamilyProperties(physical, count, properties);
            VulkanQueueFamily best = null;
            int bestCapabilities = 0;
            for (VulkanQueueFamily candidate : host.queueFamilies()) {
                int flags = properties.get(candidate.familyIndex()).queueFlags();
                int available = queueCapabilities(stack, physical, flags, candidate.familyIndex(), surface);
                if ((available & info.capabilities) != info.capabilities) continue;
                if (best == null || Integer.bitCount(available) < Integer.bitCount(bestCapabilities)) {
                    best = candidate;
                    bestCapabilities = available;
                }
            }
            if (best != null) return new VulkanQueue(this, best.queue(), best.familyIndex(), bestCapabilities);
        }
        throw new IllegalStateException("No queue family satisfies the requested capabilities: " + info.capabilities);
    }

    private static int queueCapabilities(MemoryStack stack, VkPhysicalDevice physical, int flags,
                                         int family, long surface) {
        int result = 0;
        if ((flags & VK_QUEUE_GRAPHICS_BIT) != 0) result |= 1;
        if ((flags & VK_QUEUE_COMPUTE_BIT) != 0) result |= 2;
        if ((flags & VK_QUEUE_TRANSFER_BIT) != 0) result |= 4;
        if (surface != 0) {
            IntBuffer supported = stack.mallocInt(1);
            vkGetPhysicalDeviceSurfaceSupportKHR(physical, family, surface, supported);
            if (supported.get(0) == VK_TRUE) result |= 8;
        }
        return result;
    }

    @Override public Surface createSurface(SurfaceInfo info) { return new VulkanSurface(this, info); }
    @Override public boolean isVulkan() { return true; }
    @Override public TextureFormat depthFormat() { return VulkanTextureFormats.fromVkFormat(host.depthFormat()); }

    public void flushDeferredResources() {
        ArrayDeque<GpuResource> retired = deferredResources.get(host.frameIndex());
        while (!retired.isEmpty()) retired.poll().destroy();
    }

    public void beginRendering(VkCommandBuffer command, TextureView color, OptionalInt clearColor,
                               TextureView depth, OptionalDouble clearDepth) {
        VulkanTextureView colorView = (VulkanTextureView) color;
        VulkanTextureView depthView = (VulkanTextureView) depth;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int width = colorView.width(0);
            int height = colorView.height(0);
            VkRect2D area = VkRect2D.calloc(stack);
            area.offset().set(0, 0);
            area.extent().set(width, height);
            VkRenderingAttachmentInfo.Buffer colors = VkRenderingAttachmentInfo.calloc(1, stack)
                    .sType$Default().imageView(colorView.imageView)
                    .imageLayout(VulkanSynchronization.imageLayout(colorView.layout)).storeOp(VK_ATTACHMENT_STORE_OP_STORE);
            if (clearColor.isPresent()) {
                VkClearValue value = VkClearValue.calloc(stack);
                setClearColor(value.color(), clearColor.getAsInt());
                colors.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR).clearValue(value);
            } else {
                colors.loadOp(VK_ATTACHMENT_LOAD_OP_LOAD);
            }
            VkRenderingInfo rendering = VkRenderingInfo.calloc(stack).sType$Default()
                    .renderArea(area).layerCount(1).viewMask(0).pColorAttachments(colors);
            if (depthView != null) {
                VkRenderingAttachmentInfo depthInfo = VkRenderingAttachmentInfo.calloc(stack).sType$Default()
                        .imageView(depthView.imageView).imageLayout(VulkanSynchronization.imageLayout(depthView.layout))
                        .storeOp(VK_ATTACHMENT_STORE_OP_STORE);
                if (clearDepth.isPresent()) {
                    VkClearValue value = VkClearValue.calloc(stack);
                    value.depthStencil().depth((float) clearDepth.getAsDouble());
                    depthInfo.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR).clearValue(value);
                } else {
                    depthInfo.loadOp(VK_ATTACHMENT_LOAD_OP_LOAD);
                }
                rendering.pDepthAttachment(depthInfo);
            }
            vkCmdBeginRenderingKHR(command, rendering);
            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
            scissor.offset().set(0, 0);
            scissor.extent().set(width, height);
            vkCmdSetScissor(command, 0, scissor);
        }
        barriers.insideRenderPass(true);
    }

    private static VkClearColorValue setClearColor(VkClearColorValue value, int argb) {
        value.float32(0, (argb >> 16 & 255) / 255.0f);
        value.float32(1, (argb >> 8 & 255) / 255.0f);
        value.float32(2, (argb & 255) / 255.0f);
        value.float32(3, (argb >>> 24 & 255) / 255.0f);
        return value;
    }

    public TextureView importTextureView(long image, long imageView, int width, int height,
                                         TextureFormat format, int vkFormat) {
        return new VulkanTexture(this, image, imageView, width, height, format, vkFormat).views[0];
    }

    @Override public Buffer createBuffer(BufferInfo info) {
        return new VulkanBuffer(this, info.usage, info.size, info.dynamic, info.mapped);
    }

    @Override public Texture createTexture(TextureInfo info) {
        return new VulkanTexture(this, info.width, info.height, info.depth, info.format, info.usage, info.mipCount);
    }

    @Override public Sampler createSampler(SamplerInfo info) {
        return new VulkanSampler(this, info.filter, info.mipMode, info.wrap);
    }

    @Override public void release(GpuResource resource) { deferredResources.get(host.frameIndex()).add(resource); }
    @Override public void destroyImmediately(GpuResource resource) { resource.destroy(); }
}
