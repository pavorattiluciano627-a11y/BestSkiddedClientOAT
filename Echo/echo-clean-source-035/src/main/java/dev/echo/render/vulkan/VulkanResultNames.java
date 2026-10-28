package dev.echo.render.vulkan;

/** Conditional switch reconstruction; input-key and boolean guard proof is pending. */
public class VulkanResultNames {
    public static String describe(int result) {
        return switch (result) {
            case -13 -> "VK_ERROR_UNKNOWN";
            case -12 -> "VK_ERROR_FRAGMENTED_POOL";
            case -11 -> "VK_ERROR_FORMAT_NOT_SUPPORTED";
            case -10 -> "VK_ERROR_TOO_MANY_OBJECTS";
            case -9 -> "VK_ERROR_INCOMPATIBLE_DRIVER";
            case -8 -> "VK_ERROR_FEATURE_NOT_PRESENT";
            case -7 -> "VK_ERROR_EXTENSION_NOT_PRESENT";
            case -6 -> "VK_ERROR_LAYER_NOT_PRESENT";
            case -5 -> "VK_ERROR_MEMORY_MAP_FAILED";
            case -4 -> "VK_ERROR_DEVICE_LOST";
            case -3 -> "VK_ERROR_INITIALIZATION_FAILED";
            case -2 -> "VK_ERROR_OUT_OF_DEVICE_MEMORY";
            case -1 -> "VK_ERROR_OUT_OF_HOST_MEMORY";
            case 0 -> "VK_SUCCESS";
            case 1 -> "VK_NOT_READY";
            case 2 -> "VK_TIMEOUT";
            case 3 -> "VK_EVENT_SET";
            case 4 -> "VK_EVENT_RESET";
            case 5 -> "VK_INCOMPLETE";
            default -> Integer.toString(result);
        };
    }
}
