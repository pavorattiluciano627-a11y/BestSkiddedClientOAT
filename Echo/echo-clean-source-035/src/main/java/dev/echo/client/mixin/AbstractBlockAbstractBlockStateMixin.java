package dev.echo.client.mixin;

import dev.echo.client.events.EventBus;
import dev.echo.client.events.render.BlockCullingEvent;
import dev.echo.client.events.render.BlockLightingEvent;
import dev.echo.client.events.render.BlockRenderSeedEvent;
import dev.echo.client.events.render.BlockRenderTypeEvent;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class AbstractBlockAbstractBlockStateMixin {
    @Shadow public abstract Block getBlock();
    @Shadow protected abstract BlockState asBlockState();

    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void onGetRenderTypeHead(CallbackInfoReturnable<BlockRenderType> callback) {
        if (EventBus.hasSubscribers(BlockRenderTypeEvent.class)
                && new BlockRenderTypeEvent(getBlock()).post()) {
            callback.setReturnValue(BlockRenderType.INVISIBLE);
        }
    }

    @Inject(method = "getRenderingSeed", at = @At("HEAD"))
    private void onGetRenderingSeedHead(BlockPos position, CallbackInfoReturnable<Long> callback) {
        if (EventBus.hasSubscribers(BlockRenderSeedEvent.class)) {
            new BlockRenderSeedEvent(asBlockState(), position).post();
        }
    }

    @Inject(method = {"getCullingFace", "getCullingShape"}, at = @At("HEAD"), cancellable = true)
    private void onGetCullingFaceHead(CallbackInfoReturnable<VoxelShape> callback) {
        if (EventBus.hasSubscribers(BlockCullingEvent.class) && new BlockCullingEvent().post()) {
            callback.setReturnValue(VoxelShapes.empty());
        }
    }

    @Inject(method = "hasEmissiveLighting", at = @At("HEAD"), cancellable = true)
    private void onHasEmissiveLightingHead(CallbackInfoReturnable<Boolean> callback) {
        if (EventBus.hasSubscribers(BlockLightingEvent.class)) {
            BlockLightingEvent event = new BlockLightingEvent();
            event.post();
            if (event.fullBrightness) {
                callback.setReturnValue(true);
            }
        }
    }

    @Inject(method = "getAmbientOcclusionLightLevel", at = @At("HEAD"), cancellable = true)
    private void onGetAmbientOcclusionLightLevelHead(CallbackInfoReturnable<Float> callback) {
        if (EventBus.hasSubscribers(BlockLightingEvent.class)) {
            BlockLightingEvent event = new BlockLightingEvent();
            event.post();
            if (event.fullBrightness) {
                callback.setReturnValue(1.0F);
            }
        }
    }
}
