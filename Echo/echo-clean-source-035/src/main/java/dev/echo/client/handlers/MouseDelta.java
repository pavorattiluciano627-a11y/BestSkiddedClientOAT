package dev.echo.client.handlers;

/** Integer horizontal and vertical mouse counts after sensitivity quantization. */
record MouseDelta(int horizontalCounts, int verticalCounts) {
    static final MouseDelta ZERO = new MouseDelta(0, 0);
}
