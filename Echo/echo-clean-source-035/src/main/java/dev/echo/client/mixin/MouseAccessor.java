package dev.echo.client.mixin;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Routes a mouse-button input through the client's normal input handler. */
@Mixin(MouseHandler.class)
public interface MouseAccessor {
    @Invoker("onButton")
    void invokeOnMouseButton(long windowHandle, MouseButtonInfo input, int action);
}
