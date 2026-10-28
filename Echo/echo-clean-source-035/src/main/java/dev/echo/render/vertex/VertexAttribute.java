package dev.echo.render.vertex;

/** A shader input location, its stored format, and its byte offset in a vertex. */
public record VertexAttribute(int location, VertexFormat format, int offset) {}
