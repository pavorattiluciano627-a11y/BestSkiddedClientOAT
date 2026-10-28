package dev.echo.client.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("forwardSpeed")
    float getForwardSpeed();

    @Mutable
    @Accessor("forwardSpeed")
    void setForwardSpeed(float speed);

    @Accessor("sidewaysSpeed")
    float getSidewaysSpeed();

    @Mutable
    @Accessor("sidewaysSpeed")
    void setSidewaysSpeed(float speed);

    @Accessor("jumpingCooldown")
    void setJumpingCooldown(int ticks);

    @Accessor("ticksSinceHandEquipping")
    int getTicksSinceHandEquipping();

    @Accessor("maxHurtTime")
    int getMaxHurtTime();

    @Accessor("lastDamageTime")
    long getLastDamageTime();
}
