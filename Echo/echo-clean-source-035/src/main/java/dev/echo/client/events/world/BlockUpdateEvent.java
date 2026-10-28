package dev.echo.client.events.world;

import dev.echo.client.events.Event;
import java.lang.invoke.MethodHandles;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Position and state are retained by reference, including mutable BlockPos values. */
public class BlockUpdateEvent extends Event {
    private final BlockPos position;
    private final BlockState state;

    public BlockUpdateEvent(BlockPos position, BlockState state) {
        super();
        this.position = position;
        this.state = state;
    }

    public BlockPos getPosition() {
        initializePayloadClass(BlockPos.class);
        return position;
    }

    public BlockState getState() {
        initializePayloadClass(BlockState.class);
        return state;
    }

    /** Retains the original field bootstrap's payload-type initialization. */
    private static void initializePayloadClass(Class<?> type) {
        try {
            MethodHandles.lookup().ensureInitialized(type);
        } catch (IllegalAccessException failure) {
            // Both call sites use public Minecraft classes in the client module.
            throw new IllegalStateException("Minecraft payload class is not accessible", failure);
        }
    }
}
