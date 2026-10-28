package dev.echo.render.shader.glsl;

import dev.echo.render.shader.reflection.PrimitiveKind;
import dev.echo.render.shader.reflection.ScalarKind;

/** GLSL reflection item; matrix dimensions are rows (components) by columns. */
public final class PushConstantItem {
    public String accessPath;
    public int offset;
    public PrimitiveKind kind;
    public ScalarKind scalarKind;
    public int components;
    public int columns;
}
