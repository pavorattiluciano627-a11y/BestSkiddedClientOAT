package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.events.render.OcclusionGraphEvent;
import net.minecraft.client.render.ChunkRenderingDataPreparer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Allows the occlusion event to disable culling without enabling an already-disabled input. */
@Mixin(ChunkRenderingDataPreparer.class)
public class ChunkRenderingDataPreparerMixin {
    @ModifyVariable(method = "updateSectionOcclusionGraph", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private boolean modifyUpdateSectionOcclusionGraph(boolean cullChunks) {
        if (Echo.eventsDisabled) {
            return cullChunks;
        }
        if (!cullChunks) {
            return false;
        }
        OcclusionGraphEvent event = new OcclusionGraphEvent();
        event.post();
        return event.cancelled ? false : cullChunks;
    }
}
