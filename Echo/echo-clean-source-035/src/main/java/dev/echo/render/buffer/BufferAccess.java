package dev.echo.render.buffer;

/** Buffer access states used by range tracking and backend barriers. */
public enum BufferAccess {
    UNDEFINED,
    VERTEX_READ,
    INDEX_READ,
    UNIFORM_READ,
    INDIRECT_READ,
    STORAGE_READ,
    STORAGE_WRITE,
    TRANSFER_READ,
    TRANSFER_WRITE
}
