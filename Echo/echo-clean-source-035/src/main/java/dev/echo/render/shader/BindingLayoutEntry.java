package dev.echo.render.shader;

/**
 * Functional record components. Constructor-side guard reads/writes are unresolved;
 * this candidate must not replace the original until those effects are discharged.
 */
public record BindingLayoutEntry(int binding, ResourceBindingKind kind, int arrayCount) {}
