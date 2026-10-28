package dev.echo.client.features.impl.movement;

import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.network.PacketReceiveEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.setting.MultiChoiceSetting;
import dev.echo.client.utils.interaction.EntityInteractions;
import dev.echo.client.utils.network.VelocityPackets;
import java.util.Optional;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;

/** Source-selective knockback scaling and the original hurt-tick movement adjustment. */
public class VelocityFeature extends Feature {
    private static final CharSequence CANCEL_MODE = CatalogText.literal("Cancel");
    private static final CharSequence EXPLOSION_SOURCE = CatalogText.literal("Explosions");
    private static final CharSequence HIT_SOURCE = CatalogText.literal("Hits");
    private static final CharSequence MINEMEN_CLUB_MODE = CatalogText.literal("MinemenClub");
    private static ChoiceSetting mode;
    private static FloatSetting amount;
    private static MultiChoiceSetting sources;
    private boolean explosionKnockbackPending;

    public VelocityFeature() {
        super(new FeatureInfo(CatalogText.literal("Velocity"), CatalogText.literal("Modifies knockback received"),
                Category.MOVEMENT, false));
    }

    public static void appendBootstrapSettings(VelocityFeature feature) {
        CatalogSettings.append(feature, mode);
        CatalogSettings.append(feature, amount);
        CatalogSettings.append(feature, sources);
    }

    public static void assignBootstrapSettings(VelocityFeature feature, FloatSetting preparedAmount,
            ChoiceSetting preparedMode, MultiChoiceSetting preparedSources) {
        amount = preparedAmount;
        mode = preparedMode;
        sources = preparedSources;
    }

    @Override
    public void postInitialize() {
        explosionKnockbackPending = false;
        amount.setVisibilityPredicate(ignored -> mode.isSelected(CANCEL_MODE));
    }

    @Subscribe
    public void onVelocityPacket(PacketReceiveEvent event) {
        if (isOutsideWorld()) return;
        if (!(event.getPacket() instanceof ClientboundSetEntityMotionPacket velocity)) return;
        if (client.player == null || VelocityPackets.entityId(velocity) != client.player.getId()) return;

        boolean followsExplosion = explosionKnockbackPending && sources.isSelected(EXPLOSION_SOURCE);
        explosionKnockbackPending = false;
        if (followsExplosion && !sources.isSelected(HIT_SOURCE)) {
            if (mode.isSelected(CANCEL_MODE)) event.cancel();
            return;
        }
        if (!sources.isSelected(HIT_SOURCE)) return;
        if (mode.isSelected(CANCEL_MODE)) {
            if (amount.getValue() == 1.0F) return;
            float scale = amount.getValue();
            if (scale == 0.0F) {
                event.cancel();
            } else {
                Vec3 motion = VelocityPackets.velocity(velocity);
                event.cancel();
                client.player.setDeltaMovement(motion.x * scale, motion.y * scale, motion.z * scale);
            }
        } else if (mode.isSelected(MINEMEN_CLUB_MODE) && client.player.hurtTime == 7) {
            EntityInteractions.setVelocityX(client.player.getDeltaMovement().x() * 0.25);
            EntityInteractions.setVelocityY(-0.05350000038743019);
            EntityInteractions.setVelocityZ(client.player.getDeltaMovement().z() * 0.25);
        }
    }

    @Subscribe
    public void onExplosionPacket(PacketReceiveEvent event) {
        if (isOutsideWorld()) return;
        if (!(event.getPacket() instanceof ClientboundExplodePacket explosion)) return;
        if (client.player == null) return;
        if (!sources.isSelected(EXPLOSION_SOURCE)) return;
        if (!mode.isSelected(CANCEL_MODE)) return;
        if (amount.getValue() == 1.0F) return;
        Optional<Vec3> knockback = explosion.playerKnockback();
        if (knockback.isEmpty()) return;
        float scale = amount.getValue();
        event.cancel();
        if (scale == 0.0F) {
            explosionKnockbackPending = true;
            return;
        }
        Vec3 motion = knockback.get();
        Vec3 scaledMotion = new Vec3(motion.x * scale, motion.y * scale, motion.z * scale);
        client.player.addDeltaMovement(scaledMotion);
        explosionKnockbackPending = true;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        explosionKnockbackPending = false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        explosionKnockbackPending = false;
    }
}
