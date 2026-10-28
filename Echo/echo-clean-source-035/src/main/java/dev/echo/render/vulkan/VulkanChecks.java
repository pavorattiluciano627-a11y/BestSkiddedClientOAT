package dev.echo.render.vulkan;

/** Pending direct identity-catch proof and VulkanResultNames guard proof. */
public final class VulkanChecks {
    public static void checkSuccess(int result, String message) {
        if (result != 0) {
            throw new RuntimeException(String.format("%s: %s", message, VulkanResultNames.describe(result)));
        }
    }

    private VulkanChecks() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
