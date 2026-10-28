package dev.echo.render.shader;

/** A reflected resource classification and its binding-array cardinality. */
public record BindingTypeInfo(ResourceBindingKind kind, int arrayCount) {}
