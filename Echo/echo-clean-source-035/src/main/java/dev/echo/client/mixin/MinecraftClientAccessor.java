package dev.echo.client.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Access to item-use operations on the client. */
@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    @Invoker("doItemUse")
    void invokeDoItemUse();

    @Accessor("itemUseCooldown")
    void setItemUseCooldown(int ticks);
}
