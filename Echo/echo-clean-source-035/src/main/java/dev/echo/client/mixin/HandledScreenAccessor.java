package dev.echo.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The slot currently focused by a container screen, if any. */
@Mixin(AbstractContainerScreen.class)
public interface HandledScreenAccessor {
    @Accessor("hoveredSlot")
    @Nullable Slot getFocusedSlot();
}
