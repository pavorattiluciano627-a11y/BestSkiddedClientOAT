package dev.echo.client.render;

import dev.echo.client.Echo;
import dev.echo.client.integration.VulkanPresentationCapability;
import dev.echo.client.integration.NativeWindow;
import dev.echo.client.integration.NativeWindowBackend;
import dev.echo.client.integration.VulkanPresentation;
import dev.echo.render.command.CommandEncoder;
import dev.echo.render.opengl.OpenGlDevice;
import dev.echo.render.queue.CommandQueue;
import dev.echo.render.queue.QueueInfo;
import dev.echo.render.surface.PresentMode;
import dev.echo.render.surface.Surface;
import dev.echo.render.surface.SurfaceInfo;
import dev.echo.render.texture.Texture;
import dev.echo.render.texture.TextureInfo;
import dev.echo.render.texture.TextureView;
import dev.echo.render.vulkan.VulkanDevice;
import dev.echo.render.vulkan.VulkanSurface;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;

/**
 * Structured backend lifecycle reconstruction. Constructor key/control and shared
 * cache prerequisites remain open; this analysis source is not a bootstrap proof.
 * Required real provider signatures are recorded in backend-integration.json.
 */
public final class RenderBackendManager implements AutoCloseable {
    private static final int GRAPHICS_AND_PRESENT_QUEUE = 9;
    private static final int DEPTH_ATTACHMENT_USAGE = 8;
    private static final int DEPTH_MIP_LEVELS = 1;

    private final NativeWindowBackend window;
    private final Surface surface;
    private final CommandQueue queue;
    private Texture depthTexture;
    private PresentMode presentMode;
    private int framebufferWidth;
    private int framebufferHeight;
    private final boolean presentationIntegrationAvailable;
    private final boolean openGl;
    private final FramebufferBlur framebufferBlur;

    public RenderBackendManager() {
        framebufferBlur = new FramebufferBlur();
        presentationIntegrationAvailable = VulkanPresentationCapability.isVulkanPresentationHookEnabled();
        openGl = Echo.renderDevice instanceof OpenGlDevice;
        if (presentationIntegrationAvailable) {
            VulkanSurface.queuePresentFunction = VulkanPresentation::presentOverlay;
            VulkanPresentation.initializeDevice(((VulkanDevice) Echo.renderDevice).device);
        }
        window = new NativeWindow(openGl);
        surface = Echo.renderDevice.createSurface(SurfaceInfo.builder().windowHandle(window.windowHandle()).build());
        queue = Echo.renderDevice.createQueue(QueueInfo.builder()
                .capabilities(GRAPHICS_AND_PRESENT_QUEUE).surface(surface).build());
    }

    public long windowHandle() {
        return window.windowHandle();
    }

    /** Completes one client frame; an intercepted Vulkan present may already have drawn it. */
    public void endFrame() {
        if (!presentationIntegrationAvailable || !VulkanPresentation.intercepted) {
            renderFrame();
        }
        if (presentationIntegrationAvailable) {
            VulkanPresentation.intercepted = false;
        }
        framebufferBlur.resetFrame();
        Echo.uiRenderer.clearFrameQueues();
    }

    public void renderFrame() {
        boolean visible = Echo.externalRendererActive
                && Minecraft.getInstance().level != null
                && !Minecraft.getInstance().getWindow().isIconified();
        window.setVisible(visible);
        if (!visible) {
            return;
        }
        if (presentationIntegrationAvailable && VulkanPresentation.hasPendingNativePresent()) {
            return;
        }

        window.synchronizeWithMainWindow();
        int width = window.framebufferWidth();
        int height = window.framebufferHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        PresentMode requestedMode = requestedPresentMode();
        if (width != framebufferWidth || height != framebufferHeight
                || requestedMode != presentMode || surface.needsResize()) {
            surface.resize(width, height, requestedMode);
            framebufferWidth = width;
            framebufferHeight = height;
            presentMode = requestedMode;
        }

        TextureView target = surface.acquireTexture();
        if (target == null) {
            return;
        }
        ensureDepthTexture(target.width(0), target.height(0));
        CommandEncoder commands = queue.beginCommands();
        if (framebufferBlur.isActive()) {
            Echo.worldRenderer.compositeToTarget(commands, target, framebufferBlur.colorTexture());
        } else {
            Echo.worldRenderer.compositeToTarget(commands, target, Echo.worldRenderer.mainColorTexture());
            Echo.uiRenderer.beginFrame(commands, target, depthTexture.views[0], OptionalInt.empty());
            Echo.uiRenderer.endFrame();
        }
        surface.present(queue, commands);
    }

    @Override
    public void close() {
        if (presentationIntegrationAvailable) {
            VulkanPresentation.clearPendingNativePresent();
        }
        queue.waitIdle();
        framebufferBlur.close();
        surface.destroy();
        if (depthTexture != null) {
            depthTexture.destroy();
        }
        queue.destroy();
        window.close();
    }

    private PresentMode requestedPresentMode() {
        if (!presentationIntegrationAvailable) {
            return PresentMode.IMMEDIATE;
        }
        return Minecraft.getInstance().options.enableVsync().get()
                ? PresentMode.FIFO : PresentMode.IMMEDIATE;
    }

    private void ensureDepthTexture(int width, int height) {
        if (depthTexture != null && depthTexture.width == width && depthTexture.height == height) {
            return;
        }
        queue.waitIdle();
        if (depthTexture != null) {
            depthTexture.destroy();
        }
        depthTexture = Echo.renderDevice.createTexture(TextureInfo.builder()
                .width(width).height(height).format(Echo.renderDevice.depthFormat())
                .usage(DEPTH_ATTACHMENT_USAGE).mipCount(DEPTH_MIP_LEVELS).build());
    }

    public FramebufferBlur framebufferBlur() {
        return framebufferBlur;
    }
}
