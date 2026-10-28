package dev.echo.render.device;

/** Independently detected renderer capabilities, not a single backend-version flag. */
public record DeviceCapabilities(boolean multiDrawIndirect, boolean computeShaders,
        boolean sampledImageArrays, boolean storageImages, boolean reversedDepth) {}
