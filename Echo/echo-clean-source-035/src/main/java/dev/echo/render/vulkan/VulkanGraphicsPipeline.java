package dev.echo.render.vulkan;

import dev.echo.render.pipeline.GraphicsPipeline;
import dev.echo.render.pipeline.GraphicsPipelineInfo;
import dev.echo.render.shader.ShaderResourceLayout;
import dev.echo.render.vertex.VertexAttribute;
import dev.echo.render.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDepthStencilStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRenderingCreateInfoKHR;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

/** Dynamic-rendering pipeline, with a lazily allocated reversed-winding variant. */
public class VulkanGraphicsPipeline extends GraphicsPipeline {
    private final long shaderModule;
    private final long descriptorSetLayout;
    public final long pipelineLayout;
    private final long pipeline;
    private long flippedPipeline;
    private final ShaderResourceLayout resourceLayout;
    private final GraphicsPipelineInfo descriptor;
    private final VulkanDevice device;

    public VulkanGraphicsPipeline(VulkanDevice device, GraphicsPipelineInfo info) {
        super(info);
        this.device = device;
        descriptor = info;
        resourceLayout = ShaderResourceLayout.reflect(info.module);
        int pushConstantBytes = info.pushConstantBytes;
        ByteBuffer spirv = VulkanShaderCompiler.compileSpirv(info.module);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            shaderModule = VulkanShaderCompiler.createShaderModule(device.device, stack, spirv);
            descriptorSetLayout = VulkanShaderCompiler.createDescriptorSetLayout(
                    device.device, stack, resourceLayout, 17);
            pipelineLayout = VulkanShaderCompiler.createPipelineLayout(device.device, stack,
                    descriptorSetLayout, pushConstantBytes, 17);
            pipeline = createPipeline(stack, false);
        }
        MemoryUtil.memFree(spirv);
    }

    public int bindingKind(int binding) {
        return VulkanShaderCompiler.vkDescriptorType(resourceLayout.bindingKinds()[binding]);
    }

    public void bind(VkCommandBuffer commands, boolean flipWinding) {
        VK10.vkCmdBindPipeline(commands, 0,
                flipWinding && descriptor.cullEnabled ? flippedPipeline() : pipeline);
    }

    private long flippedPipeline() {
        if (flippedPipeline == 0L) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                flippedPipeline = createPipeline(stack, true);
            }
        }
        return flippedPipeline;
    }

    public void draw(VkCommandBuffer commands, int count, int instances, int first, int baseInstance) {
        VulkanPipelineOperations.draw(commands, count, instances, first, baseInstance);
    }

    public void drawIndexed(VkCommandBuffer commands, int count, int instances, int firstIndex) {
        VulkanPipelineOperations.drawIndexed(commands, count, instances, firstIndex);
    }

    public void drawIndirect(VkCommandBuffer commands, long buffer, long offset, int count, int stride) {
        VulkanPipelineOperations.drawIndirect(commands, buffer, offset, count, stride);
    }

    @Override public void destroy() {
        VK10.vkDestroyPipeline(device.host.device(), pipeline, null);
        if (flippedPipeline != 0L) {
            VK10.vkDestroyPipeline(device.host.device(), flippedPipeline, null);
        }
        VK10.vkDestroyPipelineLayout(device.host.device(), pipelineLayout, null);
        VK10.vkDestroyDescriptorSetLayout(device.host.device(), descriptorSetLayout, null);
        VK10.vkDestroyShaderModule(device.host.device(), shaderModule, null);
    }

    private long createPipeline(MemoryStack stack, boolean flipWinding) {
        int colorFormat = VulkanTextureFormats.vkFormat(descriptor.colorFormat);
        boolean hasDepth = descriptor.depthFormat != null;
        int depthFormat = hasDepth ? VulkanTextureFormats.vkFormat(descriptor.depthFormat) : 0;
        ByteBuffer vertexEntryPoint = stack.UTF8("vs_main");
        ByteBuffer fragmentEntryPoint = stack.UTF8("fs_main");
        VkPipelineShaderStageCreateInfo.Buffer stages = VkPipelineShaderStageCreateInfo.calloc(2, stack);
        stages.get(0).sType$Default().stage(1).module(shaderModule).pName(vertexEntryPoint);
        stages.get(1).sType$Default().stage(16).module(shaderModule).pName(fragmentEntryPoint);

        VkVertexInputBindingDescription.Buffer bindings = VkVertexInputBindingDescription.calloc(1, stack);
        bindings.get(0).binding(0).stride(descriptor.vertexStride).inputRate(switch (descriptor.vertexStepMode) {
            case VERTEX -> 0;
            case INSTANCE -> 1;
        });
        VkVertexInputAttributeDescription.Buffer attributes =
                VkVertexInputAttributeDescription.calloc(descriptor.vertexAttributes.size(), stack);
        for (int index = 0; index < descriptor.vertexAttributes.size(); index++) {
            VertexAttribute attribute = descriptor.vertexAttributes.get(index);
            attributes.get(index).binding(0).location(attribute.location())
                    .format(vkVertexFormat(attribute.format())).offset(attribute.offset());
        }
        VkPipelineVertexInputStateCreateInfo vertexInput = VkPipelineVertexInputStateCreateInfo.calloc(stack)
                .sType$Default().pVertexBindingDescriptions(bindings).pVertexAttributeDescriptions(attributes);
        VkPipelineInputAssemblyStateCreateInfo assembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                .sType$Default().topology(switch (descriptor.topology) {
                    case POINT_LIST -> 0;
                    case LINE_LIST -> 1;
                    case LINE_STRIP -> 2;
                    case TRIANGLE_LIST -> 3;
                    case TRIANGLE_STRIP -> 4;
                    case TRIANGLE_FAN -> 5;
                }).primitiveRestartEnable(false);
        VkPipelineViewportStateCreateInfo viewport = VkPipelineViewportStateCreateInfo.calloc(stack)
                .sType$Default().viewportCount(1).scissorCount(1);
        int cullMode = descriptor.cullEnabled ? switch (descriptor.cullFace) {
            case FRONT -> 1;
            case BACK -> 2;
        } : 0;
        VkPipelineRasterizationStateCreateInfo rasterization = VkPipelineRasterizationStateCreateInfo.calloc(stack)
                .sType$Default().polygonMode(0).cullMode(cullMode).frontFace(switch (descriptor.frontFace) {
                    case CLOCKWISE -> flipWinding ? 0 : 1;
                    case COUNTER_CLOCKWISE -> flipWinding ? 1 : 0;
                }).lineWidth(descriptor.lineWidth);
        VkPipelineMultisampleStateCreateInfo multisample = VkPipelineMultisampleStateCreateInfo.calloc(stack)
                .sType$Default().rasterizationSamples(1);
        VkPipelineDepthStencilStateCreateInfo depth = VkPipelineDepthStencilStateCreateInfo.calloc(stack)
                .sType$Default().depthTestEnable(hasDepth && descriptor.depthTestEnabled)
                .depthWriteEnable(hasDepth && descriptor.depthTestEnabled && descriptor.depthWriteEnabled)
                .depthCompareOp(switch (descriptor.depthCompare) {
                    case LESS_OR_EQUAL -> 3;
                    case GREATER_OR_EQUAL -> 6;
                }).minDepthBounds(0.0f).maxDepthBounds(1.0f);
        VkPipelineColorBlendAttachmentState.Buffer attachments = VkPipelineColorBlendAttachmentState.calloc(1, stack);
        attachments.get(0).blendEnable(descriptor.blendEnabled).colorWriteMask(15);
        if (descriptor.blendEnabled) {
            switch (descriptor.blendMode) {
                case SRC_ALPHA -> attachments.get(0).srcColorBlendFactor(6).dstColorBlendFactor(7)
                        .colorBlendOp(0).srcAlphaBlendFactor(1).dstAlphaBlendFactor(7).alphaBlendOp(0);
                case PREMULTIPLIED_ALPHA -> attachments.get(0).srcColorBlendFactor(1).dstColorBlendFactor(7)
                        .colorBlendOp(0).srcAlphaBlendFactor(1).dstAlphaBlendFactor(7).alphaBlendOp(0);
                case ADDITIVE -> attachments.get(0).srcColorBlendFactor(1).dstColorBlendFactor(1)
                        .colorBlendOp(0).srcAlphaBlendFactor(1).dstAlphaBlendFactor(1).alphaBlendOp(0);
            }
        }
        VkPipelineColorBlendStateCreateInfo blend = VkPipelineColorBlendStateCreateInfo.calloc(stack)
                .sType$Default().pAttachments(attachments);
        VkPipelineDynamicStateCreateInfo dynamic = VkPipelineDynamicStateCreateInfo.calloc(stack)
                .sType$Default().pDynamicStates(stack.ints(0, 1, 3, 2));
        VkPipelineRenderingCreateInfoKHR rendering = VkPipelineRenderingCreateInfoKHR.calloc(stack)
                .sType(1000044002).pColorAttachmentFormats(stack.ints(colorFormat)).depthAttachmentFormat(depthFormat);
        VkGraphicsPipelineCreateInfo.Buffer createInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                .sType$Default().pStages(stages).pVertexInputState(vertexInput).pInputAssemblyState(assembly)
                .pViewportState(viewport).pRasterizationState(rasterization).pMultisampleState(multisample)
                .pDepthStencilState(depth).pColorBlendState(blend).pDynamicState(dynamic).layout(pipelineLayout)
                .basePipelineHandle(0L).basePipelineIndex(-1).pNext(rendering);
        LongBuffer result = stack.mallocLong(1);
        VulkanChecks.checkSuccess(VK10.vkCreateGraphicsPipelines(device.host.device(), 0L,
                createInfo, null, result), "Failed to create graphics pipeline");
        return result.get(0);
    }

    private static int vkVertexFormat(VertexFormat format) {
        return switch (format) {
            case Float -> 100;
            case Float2 -> 103;
            case Float3 -> 106;
            case Float4 -> 109;
            case Int -> 99;
            case Int2 -> 102;
            case Int3 -> 105;
            case Int4 -> 108;
            case Mat2, Mat3, Mat4 -> throw new RuntimeException("unreachable: matrices are expanded in descriptor");
        };
    }
}
