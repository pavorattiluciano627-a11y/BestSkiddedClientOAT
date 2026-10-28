package dev.echo.client.mixin;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Access to the input snapshot and its derived movement vector. */
@Mixin(ClientInput.class)
public interface InputAccessor {
    @Accessor("keyPresses")
    Input getPlayerInput();

    @Accessor("keyPresses")
    void setPlayerInput(Input input);

    @Accessor("moveVector")
    void setMovementVector(Vec2 movement);
}
