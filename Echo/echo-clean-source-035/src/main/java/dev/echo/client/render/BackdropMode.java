package dev.echo.client.render;

/** Original mode policy; member target and enum initialization proofs are pending. */
public enum BackdropMode {
    FULL,
    HUD_GLASS;

    public int downsamplePasses(int requestedPasses) {
        int nonnegativePasses = Math.max(0, requestedPasses);
        return this == FULL ? nonnegativePasses : Math.min(2, nonnegativePasses);
    }

    public int upsamplePasses(int requestedPasses) {
        return Math.max(0, downsamplePasses(requestedPasses) - retainedMipLevel(requestedPasses));
    }

    public int retainedMipLevel(int requestedPasses) {
        return this == FULL ? 0 : Math.min(1, downsamplePasses(requestedPasses));
    }

    public float radiusMultiplier() {
        return this == FULL ? 1.0F : 1.5F;
    }
}
