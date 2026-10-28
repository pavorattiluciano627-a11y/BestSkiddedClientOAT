package dev.echo.client.mixin;

import com.mojang.authlib.GameProfile;
import dev.echo.client.events.render.SkinTexturesEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInfo.class)
public abstract class PlayerListEntryMixin {
    @Shadow @Final private GameProfile profile;

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void onGetSkinTexturesReturn(CallbackInfoReturnable<PlayerSkin> callback) {
        callback.setReturnValue(SkinTexturesEvent.resolve(profile.id(), callback.getReturnValue()));
    }
}
