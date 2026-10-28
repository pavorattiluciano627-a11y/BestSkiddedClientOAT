package dev.echo.render.shader.msl;

import dev.echo.render.shader.ShaderStage;

/** Java metadata shape for the original MSL response; no MSL implementation is supplied. */
public record MslEntryPoint(String name, String translatedName, ShaderStage stage, int[] workgroupSize) {}
