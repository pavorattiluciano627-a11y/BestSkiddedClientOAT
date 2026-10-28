package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.events.world.BlockUpdateEvent;
import dev.echo.client.events.world.ChunkLoadEvent;
import dev.echo.client.events.world.ChunkUnloadEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Publishes chunk lifecycle and block updates at their original client-world boundaries. */
@Mixin(ClientLevel.class)
public abstract class ClientWorldMixin extends Level {
    protected ClientWorldMixin(WritableLevelData properties, ResourceKey<Level> worldKey,
            RegistryAccess registries, Holder<DimensionType> dimensionType,
            boolean client, boolean debugWorld, long biomeAccessSeed, int maxChainedNeighborUpdates) {
        super(properties, worldKey, registries, dimensionType, client, debugWorld,
                biomeAccessSeed, maxChainedNeighborUpdates);
    }

    @Inject(method = "onChunkLoaded", at = @At("RETURN"))
    void onResetChunkColorReturn(ChunkPos position, CallbackInfo callback) {
        if (!Echo.eventsDisabled) {
            LevelChunk chunk = getChunk(position.x, position.z);
            new ChunkLoadEvent(chunk).post();
        }
    }

    @Inject(method = "unload", at = @At("HEAD"))
    void onUnloadBlockEntitiesHead(LevelChunk chunk, CallbackInfo callback) {
        if (!Echo.eventsDisabled) {
            new ChunkUnloadEvent(chunk.getPos()).post();
        }
    }

    @Inject(method = "setBlock", at = @At("RETURN"))
    void onSetBlockStateReturn(BlockPos position, BlockState state, int flags,
            int maxUpdateDepth, CallbackInfoReturnable<Boolean> callback) {
        if (!Echo.eventsDisabled && callback.getReturnValue()) {
            new BlockUpdateEvent(position, state).post();
        }
    }

    @Inject(method = "setServerVerifiedBlockState", at = @At("TAIL"))
    void onHandleBlockUpdateTail(BlockPos position, BlockState state, int flags, CallbackInfo callback) {
        if (!Echo.eventsDisabled) {
            new BlockUpdateEvent(position, state).post();
        }
    }
}
