package dev.echo.client.mixin;

import com.mojang.authlib.GameProfile;
import dev.echo.client.events.render.SkinTexturesEvent;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryMixin {
    @Shadow @Final private GameProfile profile;

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void onGetSkinTexturesReturn(CallbackInfoReturnable<SkinTextures> callback) {
        callback.setReturnValue(SkinTexturesEvent.resolve(profile.id(), callback.getReturnValue()));
    }
}
