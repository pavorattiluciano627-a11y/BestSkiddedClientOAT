package dev.echo.render.opengl;

import dev.echo.render.shader.reflection.PrimitiveKind;
import dev.echo.render.shader.reflection.ScalarKind;

/**
 * A GLSL uniform backed by a byte range in the emulated push-constant block.
 * Candidate only: the original public record has a protected canonical
 * constructor, which ordinary Java record syntax cannot express unchanged.
 */
public record PushConstantUniform(int location, PrimitiveKind kind, ScalarKind scalarKind,
                                  int components, int columns, int offset) {}
