package dev.echo.client.events.world;

import dev.echo.client.events.Event;
import java.lang.invoke.MethodHandles;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Notification emitted by the client-world chunk-color reset hook.
 * Original OperationFactory identity; binding/cache acceptance remains conditional.
 * Phase, cancellation and the unresolved counter are owned by the genuine Event base.
 */
public class ChunkLoadEvent extends Event {
    private final WorldChunk chunk;

    /**
     * Captures the original chunk reference after the no-argument Event constructor.
     * @param chunk chunk supplied by the producer, including null
     */
    public ChunkLoadEvent(WorldChunk chunk) {
        super();
        this.chunk = chunk;
    }

    /**
     * Returns the original reference after the payload-type initialization required
     * by the original field linkage. Initialization errors propagate before the read.
     * @return the captured chunk, possibly null
     */
    public WorldChunk getChunk() {
        try {
            // Ordinary JVM class initialization, not reflective member dispatch.
            MethodHandles.lookup().ensureInitialized(WorldChunk.class);
        } catch (IllegalAccessException inaccessibleType) {
            // Unreachable for the pinned public WorldChunk in the unnamed module.
            throw new IllegalAccessError(inaccessibleType.getMessage());
        }
        return chunk;
    }
}
