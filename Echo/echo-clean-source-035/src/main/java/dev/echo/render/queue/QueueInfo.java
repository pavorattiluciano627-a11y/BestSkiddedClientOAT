package dev.echo.render.queue;

import dev.echo.render.surface.Surface;

/** Queue requirements; equality remains object identity, as in the original descriptor. */
public final class QueueInfo {
    public final int capabilities;
    public final Surface surface;

    QueueInfo(int capabilities, Surface surface) {
        this.capabilities = capabilities;
        this.surface = surface;
    }

    private static Surface defaultSurface() {
        return null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int capabilities;
        private boolean surfaceSet;
        private Surface surface;

        public Builder capabilities(int capabilities) {
            this.capabilities = capabilities;
            return this;
        }

        public Builder surface(Surface surface) {
            this.surface = surface;
            surfaceSet = true;
            return this;
        }

        public QueueInfo build() {
            return new QueueInfo(capabilities, surfaceSet ? surface : defaultSurface());
        }

        @Override
        public String toString() {
            return "SfQueueInfo.SfQueueInfoBuilder(capabilities=" + capabilities
                    + ", surface$value=" + String.valueOf(surface) + ")";
        }
    }
}
