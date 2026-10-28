package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.events.world.BlockUpdateEvent;
import dev.echo.client.events.world.ChunkLoadEvent;
import dev.echo.client.events.world.ChunkUnloadEvent;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Publishes chunk lifecycle and block updates at their original client-world boundaries. */
@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin extends World {
    protected ClientWorldMixin(MutableWorldProperties properties, RegistryKey<World> worldKey,
            DynamicRegistryManager registries, RegistryEntry<DimensionType> dimensionType,
            boolean client, boolean debugWorld, long biomeAccessSeed, int maxChainedNeighborUpdates) {
        super(properties, worldKey, registries, dimensionType, client, debugWorld,
                biomeAccessSeed, maxChainedNeighborUpdates);
    }

    @Inject(method = "resetChunkColor", at = @At("RETURN"))
    void onResetChunkColorReturn(ChunkPos position, CallbackInfo callback) {
        if (!Echo.eventsDisabled) {
            WorldChunk chunk = getChunk(position.x, position.z);
            new ChunkLoadEvent(chunk).post();
        }
    }

    @Inject(method = "unloadBlockEntities", at = @At("HEAD"))
    void onUnloadBlockEntitiesHead(WorldChunk chunk, CallbackInfo callback) {
        if (!Echo.eventsDisabled) {
            new ChunkUnloadEvent(chunk.getPos()).post();
        }
    }

    @Inject(method = "setBlockState", at = @At("RETURN"))
    void onSetBlockStateReturn(BlockPos position, BlockState state, int flags,
            int maxUpdateDepth, CallbackInfoReturnable<Boolean> callback) {
        if (!Echo.eventsDisabled && callback.getReturnValue()) {
            new BlockUpdateEvent(position, state).post();
        }
    }

    @Inject(method = "handleBlockUpdate", at = @At("TAIL"))
    void onHandleBlockUpdateTail(BlockPos position, BlockState state, int flags, CallbackInfo callback) {
        if (!Echo.eventsDisabled) {
            new BlockUpdateEvent(position, state).post();
        }
    }
}
