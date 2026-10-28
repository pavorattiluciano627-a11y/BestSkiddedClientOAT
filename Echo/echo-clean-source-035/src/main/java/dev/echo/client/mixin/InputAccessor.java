package dev.echo.client.mixin;

import net.minecraft.client.input.Input;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Access to the input snapshot and its derived movement vector. */
@Mixin(Input.class)
public interface InputAccessor {
    @Accessor("playerInput")
    PlayerInput getPlayerInput();

    @Accessor("playerInput")
    void setPlayerInput(PlayerInput input);

    @Accessor("movementVector")
    void setMovementVector(Vec2f movement);
}
