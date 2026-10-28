package dev.echo.render.shader.msl;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Original six-component native request payload, with explicit compatibility keys. */
record MslOptionsPayload(
        @SerializedName("parseView") int languageVersionMajor,
        @SerializedName("createMessage") int languageVersionMinor,
        @SerializedName("findSnapshot") List<MslResourceBinding> resourceBindings,
        @SerializedName("updateSetting") Integer pushConstantBuffer,
        @SerializedName("parseNode") Integer sizesBuffer,
        @SerializedName("saveQueue") boolean allowAndForcePointSize) {

    /** Snapshots scalar values, retaining the original shallow list reference. */
    static MslOptionsPayload from(MslOptions options, MslPipelineOptions pipelineOptions) {
        return new MslOptionsPayload(options.languageVersionMajor, options.languageVersionMinor,
                options.resourceBindings, options.pushConstantBuffer, options.sizesBuffer,
                pipelineOptions.allowAndForcePointSize);
    }
}
