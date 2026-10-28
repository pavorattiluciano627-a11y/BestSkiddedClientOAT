package dev.echo.render.buffer;

/** A borrowed byte range; creating or discarding a slice does not release its buffer. */
public record BufferSlice(Buffer buffer, long offset, long length) {}
