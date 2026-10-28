package dev.echo.client.features.impl.internals;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.render.SkinTexturesEvent;
import dev.echo.client.events.render.TextTransformEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.screens.ScreenManager;
import dev.echo.client.setting.TextSetting;
import java.util.Optional;
import net.minecraft.component.type.ProfileComponent;

/** Local display-name substitution preserving nonblocking local-player skin selection. */
public class ProtectFeature extends Feature {
    public static TextSetting nickname;
    public static TextSetting skin;

    public ProtectFeature() {
        super(new FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Protect"), dev.echo.client.bootstrap.CatalogText.literal("Replaces your name and skin everywhere they are shown"), dev.echo.client.features.Category.INTERNALS));
    }

    @Subscribe
    private void onTextTransform(TextTransformEvent event) {
        String replacement = nickname.getText();
        if (replacement.isEmpty()) {
            return;
        }
        String username = client.getSession().getUsername();
        if (username.isEmpty() || username.equals(replacement)) {
            return;
        }
        event.text = event.text.replace(username, replacement);
    }

    @Subscribe
    private void onSkinTextures(SkinTexturesEvent event) {
        String profileName = skin.getText();
        if (profileName.isEmpty() || ScreenManager.focusedSetting == skin) {
            return;
        }
        if (!client.uuidEquals(event.playerId)) {
            return;
        }
        client.getPlayerSkinCache().getFuture(ProfileComponent.ofDynamic(profileName))
                .getNow(Optional.empty())
                .ifPresent(entry -> event.textures = entry.getTextures());
    }

}
