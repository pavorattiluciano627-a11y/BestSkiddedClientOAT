package dev.echo.client.mixin;

import dev.echo.client.events.EventBus;
import dev.echo.client.events.render.BlockCullingEvent;
import dev.echo.client.events.render.BlockLightingEvent;
import dev.echo.client.events.render.BlockRenderSeedEvent;
import dev.echo.client.events.render.BlockRenderTypeEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class AbstractBlockAbstractBlockStateMixin {
    @Shadow public abstract Block getBlock();
    @Shadow protected abstract BlockState asState();

    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void onGetRenderTypeHead(CallbackInfoReturnable<RenderShape> callback) {
        if (EventBus.hasSubscribers(BlockRenderTypeEvent.class)
                && new BlockRenderTypeEvent(getBlock()).post()) {
            callback.setReturnValue(RenderShape.INVISIBLE);
        }
    }

    @Inject(method = "getSeed", at = @At("HEAD"))
    private void onGetRenderingSeedHead(BlockPos position, CallbackInfoReturnable<Long> callback) {
        if (EventBus.hasSubscribers(BlockRenderSeedEvent.class)) {
            new BlockRenderSeedEvent(asState(), position).post();
        }
    }

    @Inject(method = {"getFaceOcclusionShape", "getOcclusionShape"}, at = @At("HEAD"), cancellable = true)
    private void onGetCullingFaceHead(CallbackInfoReturnable<VoxelShape> callback) {
        if (EventBus.hasSubscribers(BlockCullingEvent.class) && new BlockCullingEvent().post()) {
            callback.setReturnValue(Shapes.empty());
        }
    }

    @Inject(method = "emissiveRendering", at = @At("HEAD"), cancellable = true)
    private void onHasEmissiveLightingHead(CallbackInfoReturnable<Boolean> callback) {
        if (EventBus.hasSubscribers(BlockLightingEvent.class)) {
            BlockLightingEvent event = new BlockLightingEvent();
            event.post();
            if (event.fullBrightness) {
                callback.setReturnValue(true);
            }
        }
    }

    @Inject(method = "getShadeBrightness", at = @At("HEAD"), cancellable = true)
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
