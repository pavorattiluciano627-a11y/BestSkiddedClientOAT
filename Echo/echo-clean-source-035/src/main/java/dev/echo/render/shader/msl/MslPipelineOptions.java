package dev.echo.render.shader.msl;

import com.google.gson.annotations.SerializedName;

/** Retained per-pipeline MSL flag; the original Java default is false. */
public final class MslPipelineOptions {
    @SerializedName("activeRequest")
    public boolean allowAndForcePointSize;
}
