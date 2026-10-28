package dev.echo.render.shader.msl;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/**
 * Retained MSL language/resource options. Semantic labels follow Naga's MSL
 * model; annotations preserve the original Java wire identities explicitly.
 */
public final class MslOptions {
    @SerializedName("visibleClient")
    public int languageVersionMajor = 3;
    @SerializedName("previousChannel")
    public int languageVersionMinor = 0;
    @SerializedName("temporaryResult")
    public List<MslResourceBinding> resourceBindings = new ArrayList<>();
    @SerializedName("lastLayer")
    public Integer pushConstantBuffer;
    @SerializedName("latestAccount")
    public Integer sizesBuffer;
}
