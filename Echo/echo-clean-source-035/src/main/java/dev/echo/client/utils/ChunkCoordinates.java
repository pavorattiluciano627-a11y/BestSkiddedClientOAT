package dev.echo.client.utils;

import net.minecraft.util.math.ChunkPos;

/** Coordinate accessors for chunk positions (coordinates are in chunks, not blocks). */
public final class ChunkCoordinates {
    private ChunkCoordinates() {
    }

    public static int getX(ChunkPos chunk) {
        return chunk.x;
    }

    public static int getZ(ChunkPos chunk) {
        return chunk.z;
    }
}
