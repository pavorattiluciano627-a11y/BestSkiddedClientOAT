package dev.echo.client.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("zza")
    float getForwardSpeed();

    @Mutable
    @Accessor("zza")
    void setForwardSpeed(float speed);

    @Accessor("xxa")
    float getSidewaysSpeed();

    @Mutable
    @Accessor("xxa")
    void setSidewaysSpeed(float speed);

    @Accessor("noJumpDelay")
    void setJumpingCooldown(int ticks);

    @Accessor("itemSwapTicker")
    int getTicksSinceHandEquipping();

    @Accessor("hurtDuration")
    int getMaxHurtTime();

    @Accessor("lastDamageStamp")
    long getLastDamageTime();
}
