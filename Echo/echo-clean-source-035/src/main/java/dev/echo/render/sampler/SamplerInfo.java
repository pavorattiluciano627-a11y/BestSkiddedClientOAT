package dev.echo.render.sampler;

/** Immutable, identity-equal sampler description. Explicit null is not an unset default. */
public final class SamplerInfo {
    public final Filter filter;
    public final MipmapMode mipMode;
    public final WrapMode wrap;

    SamplerInfo(Filter filter, MipmapMode mipMode, WrapMode wrap) {
        this.filter = filter;
        this.mipMode = mipMode;
        this.wrap = wrap;
    }

    private static Filter defaultFilter() { return Filter.LINEAR; }
    private static MipmapMode defaultMipMode() { return MipmapMode.NONE; }
    private static WrapMode defaultWrap() { return WrapMode.CLAMP_TO_EDGE; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private boolean filterSet;
        private Filter filter;
        private boolean mipModeSet;
        private MipmapMode mipMode;
        private boolean wrapSet;
        private WrapMode wrap;

        Builder() {}

        public Builder filter(Filter value) {
            filter = value;
            filterSet = true;
            return this;
        }

        public Builder mipMode(MipmapMode value) {
            mipMode = value;
            mipModeSet = true;
            return this;
        }

        public Builder wrap(WrapMode value) {
            wrap = value;
            wrapSet = true;
            return this;
        }

        public SamplerInfo build() {
            return new SamplerInfo(filterSet ? filter : defaultFilter(),
                    mipModeSet ? mipMode : defaultMipMode(), wrapSet ? wrap : defaultWrap());
        }

        @Override
        public String toString() {
            return "SfSamplerInfo.SfSamplerInfoBuilder(filter$value=" + filter
                    + ", mipMode$value=" + mipMode + ", wrap$value=" + wrap + ")";
        }
    }
}
