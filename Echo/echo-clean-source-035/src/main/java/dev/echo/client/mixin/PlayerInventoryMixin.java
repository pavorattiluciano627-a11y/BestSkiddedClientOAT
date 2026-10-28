package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.events.input.SelectedSlotEvent;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Posts an old/new slot event before an actual selection change. */
@Mixin(Inventory.class)
public class PlayerInventoryMixin {
    @Inject(method = "setSelectedSlot", at = @At("HEAD"), cancellable = true)
    private void onSetSelectedSlotHead(int selectedSlot, CallbackInfo callback) {
        if (!Echo.eventsDisabled) {
            Inventory inventory = (Inventory) (Object) this;
            int previousSlot = inventory.getSelectedSlot();
            if (previousSlot != selectedSlot) {
                SelectedSlotEvent event = new SelectedSlotEvent(previousSlot, selectedSlot);
                event.post();
                if (event.cancelled) {
                    callback.cancel();
                }
            }
        }
    }
}
