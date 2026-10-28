package dev.echo.client.features.impl.combat;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.interaction.CrosshairTargetEvent;
import dev.echo.client.feature.combat.PiercingRay;
import dev.echo.client.feature.combat.PiercingTarget;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.RegistryListSetting;
import dev.echo.client.utils.targeting.TargetSelection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Replaces crosshair hits with eligible living targets behind intervening entities. */
public final class PiercingFeature extends Feature {
    private static final CharSequence HURT_TIME = dev.echo.client.bootstrap.CatalogText.literal("Hurt Time");
    public static BooleanSetting blocks;
    public static BooleanSetting interact;
    public static ChoiceSetting sortTargetsBy;
    public static RegistryListSetting<Block> holdingBlock =
            new PiercingBlockSelectionSetting(dev.echo.client.bootstrap.CatalogText.literal("Holding Block"), () -> BuiltInRegistries.BLOCK);

    /** Creates the feature; the shared target policy is consulted at each candidate. */
    public PiercingFeature() {
        super(new FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Piercing"), dev.echo.client.bootstrap.CatalogText.literal("Hit through entities and blocks"), dev.echo.client.features.Category.COMBAT));
    }

    /** Applies only the replacement selected by the matching original event path. */
    @Subscribe
    private void onCrosshairTarget(CrosshairTargetEvent event) {
        if (isOutsideWorld()) {
            return;
        }
        if (interact.getValue() && client.options.keyUse.isDown() && interactionHand() != null) {
            BlockHitResult hit = interactionBlockHit();
            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                event.target = hit;
            }
            return;
        }
        if (client.player.isUsingItem() || client.options.keyUse.isDown()) {
            return;
        }
        PiercingRay ray = attackRay();
        if (ray == null) {
            return;
        }
        PiercingTarget target = selectTarget(ray, maximumAttackDistanceSquared(ray));
        if (target != null) {
            event.target = target.toHitResult();
        }
    }

    private InteractionHand interactionHand() {
        Item mainItem = client.player.getMainHandItem().getItem();
        Item offhandItem = client.player.getOffhandItem().getItem();
        if (mainItem instanceof BlockItem blockItem && holdingBlock.isEntrySelected(blockItem.getBlock())) {
            return InteractionHand.MAIN_HAND;
        }
        if (offhandItem instanceof BlockItem blockItem && holdingBlock.isEntrySelected(blockItem.getBlock())) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private PiercingTarget selectTarget(PiercingRay ray, double maximumDistanceSquared) {
        List<PiercingTarget> targets = collectTargets(ray, maximumDistanceSquared);
        return targets.isEmpty() ? null : selectBest(targets);
    }

    private PiercingRay attackRay() {
        if (isOutsideWorld()) {
            return null;
        }
        LocalPlayer player = client.player;
        Vec3 eye = player.getEyePosition(1.0F);
        return new PiercingRay(player, eye, eye.add(player.getViewVector(1.0F).scale(attackRayLength(player))));
    }

    private double attackRayLength(LocalPlayer player) {
        double attackReach = player.getAttackRangeWith(player.getMainHandItem()).effectiveMaxRange(player);
        return Math.max(attackReach, player.blockInteractionRange());
    }

    private double maximumAttackDistanceSquared(PiercingRay ray) {
        LocalPlayer player = ray.player();
        double reach = player.getAttackRangeWith(player.getMainHandItem()).effectiveMaxRange(player);
        double maximum = reach * reach;
        if (blocks.getValue()) {
            return maximum;
        }
        BlockHitResult hit = raycastBlocks(ray);
        if (hit.getType() == HitResult.Type.MISS) {
            return maximum;
        }
        double hitDistance = ray.start().distanceToSqr(hit.getLocation());
        return hitDistance < maximum ? hitDistance : maximum;
    }

    private BlockHitResult interactionBlockHit() {
        PiercingRay ray = interactionRay();
        return ray == null ? null : raycastBlocks(ray);
    }

    private PiercingRay interactionRay() {
        if (isOutsideWorld()) {
            return null;
        }
        LocalPlayer player = client.player;
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));
        return new PiercingRay(player, eye, end);
    }

    private BlockHitResult raycastBlocks(PiercingRay ray) {
        return client.level.clip(new ClipContext(ray.start(), ray.end(), ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, ray.player()));
    }

    private List<PiercingTarget> collectTargets(PiercingRay ray, double maximumDistanceSquared) {
        List<PiercingTarget> targets = new ArrayList<>();
        AABB search = ray.player().getBoundingBox().expandTowards(ray.end().subtract(ray.start())).inflate(1.0, 1.0, 1.0);
        for (Entity entity : client.level.getEntities(ray.player(), search, EntitySelector.CAN_BE_PICKED)) {
            if (!(entity instanceof LivingEntity living) || !TargetSelection.isValidTarget(living)) {
                continue;
            }
            AABB bounds = entity.getBoundingBox().inflate(entity.getPickRadius());
            Optional<Vec3> intersection = bounds.clip(ray.start(), ray.end());
            if (intersection.isEmpty()) {
                continue;
            }
            Vec3 position = intersection.get();
            if (ray.start().distanceToSqr(position) > maximumDistanceSquared) {
                continue;
            }
            if (!ray.player().getAttackRangeWith(ray.player().getMainHandItem()).isInRange(ray.player(), position)) {
                continue;
            }
            targets.add(new PiercingTarget(living, position));
        }
        return targets;
    }

    private PiercingTarget selectBest(List<PiercingTarget> targets) {
        if (targets.size() == 1) {
            return targets.get(0);
        }
        if (sortTargetsBy.isSelected(HURT_TIME)) {
            targets.sort(Comparator.comparingInt(target -> target.entity().hurtTime));
        } else {
            targets.sort(Comparator.comparingDouble(target -> target.entity().getHealth()));
        }
        return targets.get(0);
    }

}
