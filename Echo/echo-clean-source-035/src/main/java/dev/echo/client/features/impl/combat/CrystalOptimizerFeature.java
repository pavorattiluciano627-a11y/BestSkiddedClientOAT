package dev.echo.client.features.impl.combat;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.HandleInputEvent;
import dev.echo.client.events.interaction.AttackAttemptEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.text.SegmentedText;
import dev.echo.client.utils.items.InventoryItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

/** Candidate under the conditional member, input-event, and item-helper contracts. */
public class CrystalOptimizerFeature extends Feature {
    private EndCrystalEntity trackedCrystal;

    public CrystalOptimizerFeature() {
        super(new FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Crystal Optimizer"), dev.echo.client.bootstrap.CatalogText.literal("Optimizes crystal speeds"), dev.echo.client.features.Category.COMBAT));
    }

    @Subscribe
    public void onAttackAttempt(AttackAttemptEvent event) {
        if (isOutsideWorld() || client.crosshairTarget == null
                || client.crosshairTarget.getType() != HitResult.Type.ENTITY) {
            return;
        }
        if (!(client.crosshairTarget instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof EndCrystalEntity crystal)) {
            return;
        }
        StatusEffectInstance weakness = client.player.getStatusEffect(StatusEffects.WEAKNESS);
        StatusEffectInstance strength = client.player.getStatusEffect(StatusEffects.STRENGTH);
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
            trackedCrystal.onRemoved();
        }
        trackedCrystal = null;
    }

    @Override
    public void onDisable() {
        trackedCrystal = null;
        super.onDisable();
    }

}
