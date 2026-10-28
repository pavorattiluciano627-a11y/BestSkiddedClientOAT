package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.events.EventBus;
import dev.echo.client.events.render.FluidRenderEvent;
import dev.echo.client.integration.SodiumCompatibility;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Candidate: SodiumCompatibility key-free query requires callee proof. */
@Mixin(BlockRenderManager.class)
public class BlockRenderManagerMixin {
    @Inject(method = "renderFluid", at = @At("HEAD"), cancellable = true)
    public void onRenderFluidHead(BlockPos position, BlockRenderView world, VertexConsumer vertices,
            BlockState blockState, FluidState fluidState, CallbackInfo callback) {
        if (!Echo.eventsDisabled && !SodiumCompatibility.isLoaded()
                && EventBus.hasSubscribers(FluidRenderEvent.class)) {
            if (new FluidRenderEvent().post()) {
                callback.cancel();
            }
        }
    }
}
