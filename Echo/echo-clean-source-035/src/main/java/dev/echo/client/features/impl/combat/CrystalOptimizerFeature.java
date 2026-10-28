package dev.echo.client.features.impl.combat;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.HandleInputEvent;
import dev.echo.client.events.interaction.AttackAttemptEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.text.SegmentedText;
import dev.echo.client.utils.items.InventoryItems;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Candidate under the conditional member, input-event, and item-helper contracts. */
public class CrystalOptimizerFeature extends Feature {
    private EndCrystal trackedCrystal;

    public CrystalOptimizerFeature() {
        super(new FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Crystal Optimizer"), dev.echo.client.bootstrap.CatalogText.literal("Optimizes crystal speeds"), dev.echo.client.features.Category.COMBAT));
    }

    @Subscribe
    public void onAttackAttempt(AttackAttemptEvent event) {
        if (isOutsideWorld() || client.hitResult == null
                || client.hitResult.getType() != HitResult.Type.ENTITY) {
            return;
        }
        if (!(client.hitResult instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof EndCrystal crystal)) {
            return;
        }
        MobEffectInstance weakness = client.player.getEffect(MobEffects.WEAKNESS);
        MobEffectInstance strength = client.player.getEffect(MobEffects.STRENGTH);
        boolean canBreakCrystal = weakness == null
                || strength != null && strength.getAmplifier() > weakness.getAmplifier()
                || InventoryItems.holdsPickaxe() || InventoryItems.holdsSword();
        if (canBreakCrystal) {
            trackedCrystal = crystal;
        }
    }

    @Subscribe
    private void onInputHandled(HandleInputEvent.Post event) {
        if (trackedCrystal == null) {
            return;
        }
        if (!trackedCrystal.isRemoved()) {
            trackedCrystal.setRemoved(Entity.RemovalReason.KILLED);
            trackedCrystal.onClientRemoval();
        }
        trackedCrystal = null;
    }

    @Override
    public void onDisable() {
        trackedCrystal = null;
        super.onDisable();
    }

}
