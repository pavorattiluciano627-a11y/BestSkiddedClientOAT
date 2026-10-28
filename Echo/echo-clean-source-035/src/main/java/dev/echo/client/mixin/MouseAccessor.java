package dev.echo.client.mixin;

import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Routes a mouse-button input through the client's normal input handler. */
@Mixin(Mouse.class)
public interface MouseAccessor {
    @Invoker("onMouseButton")
    void invokeOnMouseButton(long windowHandle, MouseInput input, int action);
}
