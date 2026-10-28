package dev.echo.render.vulkan;

import dev.echo.render.sampler.Filter;
import dev.echo.render.sampler.MipmapMode;
import dev.echo.render.sampler.Sampler;
import dev.echo.render.sampler.WrapMode;
import java.nio.LongBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSamplerCreateInfo;
import static org.lwjgl.vulkan.VK10.*;

/** An owning sampler handle; device.release defers destruction until its frame is retired. */
public class VulkanSampler extends Sampler {
    private final VulkanDevice device;
    public final long sampler;

    public VulkanSampler(VulkanDevice device, Filter filter, MipmapMode mipMode, WrapMode wrap) {
        super(filter, mipMode, wrap);
        this.device = device;
        int sampling = switch (filter) {
            case NEAREST -> VK_FILTER_NEAREST;
            case LINEAR -> VK_FILTER_LINEAR;
        };
        int mipSampling = switch (mipMode) {
            case NONE, NEAREST -> VK_SAMPLER_MIPMAP_MODE_NEAREST;
            case LINEAR -> VK_SAMPLER_MIPMAP_MODE_LINEAR;
        };
        int addressing = switch (wrap) {
            case REPEAT -> VK_SAMPLER_ADDRESS_MODE_REPEAT;
            case CLAMP_TO_EDGE -> VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
        };
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSamplerCreateInfo info = VkSamplerCreateInfo.calloc(stack).sType$Default()
                    .magFilter(sampling).minFilter(sampling).mipmapMode(mipSampling)
                    .addressModeU(addressing).addressModeV(addressing).addressModeW(addressing)
                    .mipLodBias(0).anisotropyEnable(false).compareEnable(false).minLod(0)
                    .maxLod(mipMode == MipmapMode.NONE ? 0 : 1000).unnormalizedCoordinates(false);
            LongBuffer result = stack.mallocLong(1);
            VulkanChecks.checkSuccess(vkCreateSampler(device.device, info, null, result), "Failed to create sampler");
            sampler = result.get(0);
        }
    }

    @Override
    public void destroy() {
        vkDestroySampler(device.device, sampler, null);
    }
}
