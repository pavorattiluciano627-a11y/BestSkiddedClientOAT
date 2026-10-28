package dev.echo.client.events.world;

import dev.echo.client.events.Event;
import java.lang.invoke.MethodHandles;
import net.minecraft.world.level.ChunkPos;

/** Chunk coordinate payload published when its block entities are unloaded. */
public class ChunkUnloadEvent extends Event {
    private final ChunkPos chunkPos;

    public ChunkUnloadEvent(ChunkPos chunkPos) {
        super();
        this.chunkPos = chunkPos;
    }

    public ChunkPos getChunkPos() {
        initializePayloadClass();
        return chunkPos;
    }

    /** Initialization belonging to the retained ChunkPos-field target candidate. */
    private static void initializePayloadClass() {
        try {
            MethodHandles.lookup().ensureInitialized(ChunkPos.class);
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException("Minecraft ChunkPos class is not accessible", failure);
        }
    }
}
