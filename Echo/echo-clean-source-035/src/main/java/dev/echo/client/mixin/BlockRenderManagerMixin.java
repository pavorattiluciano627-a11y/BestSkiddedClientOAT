package dev.echo.client.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.echo.client.Echo;
import dev.echo.client.events.EventBus;
import dev.echo.client.events.render.FluidRenderEvent;
import dev.echo.client.integration.SodiumCompatibility;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Candidate: SodiumCompatibility key-free query requires callee proof. */
@Mixin(FluidRenderer.class)
public class BlockRenderManagerMixin {
    @Inject(method = "tesselate", at = @At("HEAD"), cancellable = true)
    public void onRenderFluidHead(BlockAndTintGetter world, BlockPos position, FluidRenderer.Output output,
            BlockState blockState, FluidState fluidState, CallbackInfo callback) {
        if (!Echo.eventsDisabled && !SodiumCompatibility.isLoaded()
                && EventBus.hasSubscribers(FluidRenderEvent.class)) {
            if (new FluidRenderEvent().post()) {
                callback.cancel();
            }
        }
    }
}
