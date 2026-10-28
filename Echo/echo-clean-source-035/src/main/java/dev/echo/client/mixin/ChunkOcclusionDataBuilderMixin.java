package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.events.render.OcclusionGraphEvent;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkOcclusionDataBuilder.class)
public abstract class ChunkOcclusionDataBuilderMixin {
    @Inject(method = "markClosed", at = @At("HEAD"), cancellable = true)
    private void onMarkClosedHead(BlockPos position, CallbackInfo callback) {
        if (!Echo.eventsDisabled) {
            OcclusionGraphEvent event = new OcclusionGraphEvent();
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }
}
