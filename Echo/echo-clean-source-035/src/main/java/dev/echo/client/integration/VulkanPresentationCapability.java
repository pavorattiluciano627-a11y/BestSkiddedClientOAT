package dev.echo.client.integration;

/** The supplied artifact's presentation-hook capability query. */
public final class VulkanPresentationCapability {
    private VulkanPresentationCapability() {
    }

    /** The original query unconditionally reports that the presentation hook is disabled. */
    public static boolean isVulkanPresentationHookEnabled() {
        return false;
    }
}
