package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.events.input.SelectedSlotEvent;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Posts an old/new slot event before an actual selection change. */
@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {
    @Inject(method = "setSelectedSlot", at = @At("HEAD"), cancellable = true)
    private void onSetSelectedSlotHead(int selectedSlot, CallbackInfo callback) {
        if (!Echo.eventsDisabled) {
            PlayerInventory inventory = (PlayerInventory) (Object) this;
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
