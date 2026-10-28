package dev.echo.client.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Access to item-use operations on the client. */
@Mixin(Minecraft.class)
public interface MinecraftClientAccessor {
    @Invoker("startUseItem")
    void invokeDoItemUse();

    @Accessor("rightClickDelay")
    void setItemUseCooldown(int ticks);
}
