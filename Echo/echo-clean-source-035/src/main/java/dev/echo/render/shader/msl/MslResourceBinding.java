package dev.echo.render.shader.msl;

import com.google.gson.annotations.SerializedName;

/**
 * Resource group/binding and optional Metal slots, in the original component
 * order. Labels follow Naga ResourceBinding/BindTarget; wire keys are unchanged.
 */
public record MslResourceBinding(
        @SerializedName("initializeGroup") int group,
        @SerializedName("setPipeline") int binding,
        @SerializedName("initializeDocument") Integer buffer,
        @SerializedName("loadItem") Integer texture,
        @SerializedName("handleSchema") Integer sampler,
        @SerializedName("findHandler") boolean mutable) {}
