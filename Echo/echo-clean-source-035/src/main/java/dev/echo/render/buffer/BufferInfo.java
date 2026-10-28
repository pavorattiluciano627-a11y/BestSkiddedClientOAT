package dev.echo.render.buffer;

/** Buffer allocation descriptor. Equality remains object identity. */
public final class BufferInfo {
    public final int usage;
    public final long size;
    public final boolean dynamic;
    public final boolean mapped;
    public final boolean explicitFlush;

    BufferInfo(int usage, long size, boolean dynamic, boolean mapped, boolean explicitFlush) {
        this.usage = usage;
        this.size = size;
        this.dynamic = dynamic;
        this.mapped = mapped;
        this.explicitFlush = explicitFlush;
    }

    private static boolean defaultDynamic() { return false; }
    private static boolean defaultMapped() { return false; }
    private static boolean defaultExplicitFlush() { return false; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private int usage;
        private long size;
        private boolean dynamicSet;
        private boolean dynamic;
        private boolean mappedSet;
        private boolean mapped;
        private boolean explicitFlushSet;
        private boolean explicitFlush;

        Builder() {}

        public Builder usage(int usage) { this.usage = usage; return this; }
        public Builder size(long size) { this.size = size; return this; }
        public Builder dynamic(boolean dynamic) {
            this.dynamic = dynamic;
            this.dynamicSet = true;
            return this;
        }
        public Builder mapped(boolean mapped) {
            this.mapped = mapped;
            this.mappedSet = true;
            return this;
        }
        public Builder explicitFlush(boolean explicitFlush) {
            this.explicitFlush = explicitFlush;
            this.explicitFlushSet = true;
            return this;
        }
        public BufferInfo build() {
            boolean dynamic = dynamicSet ? this.dynamic : defaultDynamic();
            boolean mapped = mappedSet ? this.mapped : defaultMapped();
            boolean explicitFlush = explicitFlushSet ? this.explicitFlush : defaultExplicitFlush();
            return new BufferInfo(usage, size, dynamic, mapped, explicitFlush);
        }
        @Override public String toString() {
            boolean explicitFlushValue = explicitFlush;
            boolean mappedValue = mapped;
            boolean dynamicValue = dynamic;
            long sizeValue = size;
            int usageValue = usage;
            return "SfBufferInfo.SfBufferInfoBuilder(usage=" + usageValue + ", size=" + sizeValue
                    + ", dynamic$value=" + dynamicValue + ", map$value=" + mappedValue
                    + ", explicitFlush$value=" + explicitFlushValue + ")";
        }
    }
}
