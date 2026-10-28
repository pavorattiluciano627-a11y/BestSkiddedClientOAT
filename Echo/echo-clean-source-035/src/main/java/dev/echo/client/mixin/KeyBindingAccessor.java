package dev.echo.client.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyBindingAccessor {
    @Accessor("key")
    InputConstants.Key getBoundKey();

    @Accessor("clickCount")
    int getTimesPressed();

    @Accessor("clickCount")
    void setTimesPressed(int count);
}
