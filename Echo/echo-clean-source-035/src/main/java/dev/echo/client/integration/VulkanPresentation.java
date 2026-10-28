package dev.echo.client.integration;

import dev.echo.client.Echo;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicReference;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkQueue;

/** Coordinates the main and overlay swapchain presentations. */
public final class VulkanPresentation {
    private static VkQueue mainQueue;
    private static VkPresentInfoKHR mainPresentInfo;
    private static int mainPresentResult;
    public static boolean intercepted;
    private static final AtomicReference<long[]> pendingNativePresent = new AtomicReference<>();
    private static volatile int previousNativeResult;
    // The original Java code tests this token for zero; it does not call an address.
    private static long nativeHandoffToken;

    private VulkanPresentation() {
    }

    /**
     * Retains the original device hook, whose complete bytecode is RETURN.
     * It installs no native hook and intentionally accepts an unused device.
     */
    public static void initializeDevice(VkDevice device) {
        return;
    }

    public static boolean hasPendingNativePresent() {
        return pendingNativePresent.get() != null;
    }

    public static synchronized void clearPendingNativePresent() {
        pendingNativePresent.set(null);
    }

    /** Gives the renderer a chance to combine its presentation with the main window. */
    public static int presentMainWindow(VkQueue queue, VkPresentInfoKHR presentInfo) {
        intercepted = true;
        if (Echo.externalRenderer == null) {
            return KHRSwapchain.vkQueuePresentKHR(queue, presentInfo);
        }
        mainQueue = queue;
        mainPresentInfo = presentInfo;
        try {
            Echo.externalRenderer.renderFrame();
            return mainPresentInfo == null
                    ? mainPresentResult
                    : KHRSwapchain.vkQueuePresentKHR(queue, presentInfo);
        } finally {
            mainQueue = null;
            mainPresentInfo = null;
        }
    }

    /** Presents both swapchains together when a main-window presentation is captured. */
    public static int presentOverlay(VkQueue queue, VkPresentInfoKHR presentInfo) {
        VkPresentInfoKHR capturedMain = mainPresentInfo;
        if (capturedMain == null) {
            return presentOrDefer(queue, presentInfo);
        }
        mainPresentInfo = null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer results = stack.ints(VK10.VK_SUCCESS, VK10.VK_SUCCESS);
            VkPresentInfoKHR combined = VkPresentInfoKHR.calloc(stack)
                    .sType$Default()
                    .pWaitSemaphores(stack.longs(
                            capturedMain.pWaitSemaphores().get(0), presentInfo.pWaitSemaphores().get(0)))
                    .swapchainCount(2)
                    .pSwapchains(stack.longs(
                            capturedMain.pSwapchains().get(0), presentInfo.pSwapchains().get(0)))
                    .pImageIndices(stack.ints(
                            capturedMain.pImageIndices().get(0), presentInfo.pImageIndices().get(0)))
                    .pResults(results);
            int result = KHRSwapchain.vkQueuePresentKHR(mainQueue, combined);
            boolean perSwapchainResults = result >= 0
                    || result == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR
                    || result == KHRSurface.VK_ERROR_SURFACE_LOST_KHR;
            mainPresentResult = perSwapchainResults ? results.get(0) : result;
            return perSwapchainResults ? results.get(1) : result;
        }
    }

    private static int presentOrDefer(VkQueue queue, VkPresentInfoKHR presentInfo) {
        if (nativeHandoffToken == 0L) {
            return KHRSwapchain.vkQueuePresentKHR(queue, presentInfo);
        }
        pendingNativePresent.set(new long[] {
                presentInfo.pSwapchains().get(0),
                presentInfo.pImageIndices().get(0),
                presentInfo.pWaitSemaphores().get(0)
        });
        int result = previousNativeResult;
        previousNativeResult = VK10.VK_SUCCESS;
        return result;
    }
}
