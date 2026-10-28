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
import net.minecraft.block.Block;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/** Replaces crosshair hits with eligible living targets behind intervening entities. */
public final class PiercingFeature extends Feature {
    private static final CharSequence HURT_TIME = dev.echo.client.bootstrap.CatalogText.literal("Hurt Time");
    public static BooleanSetting blocks;
    public static BooleanSetting interact;
    public static ChoiceSetting sortTargetsBy;
    public static RegistryListSetting<Block> holdingBlock =
            new PiercingBlockSelectionSetting(dev.echo.client.bootstrap.CatalogText.literal("Holding Block"), () -> Registries.BLOCK);

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
        if (interact.getValue() && client.options.useKey.isPressed() && interactionHand() != null) {
            BlockHitResult hit = interactionBlockHit();
            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                event.target = hit;
            }
            return;
        }
        if (client.player.isUsingItem() || client.options.useKey.isPressed()) {
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

    private Hand interactionHand() {
        Item mainItem = client.player.getMainHandStack().getItem();
        Item offhandItem = client.player.getOffHandStack().getItem();
        if (mainItem instanceof BlockItem blockItem && holdingBlock.isEntrySelected(blockItem.getBlock())) {
            return Hand.MAIN_HAND;
        }
        if (offhandItem instanceof BlockItem blockItem && holdingBlock.isEntrySelected(blockItem.getBlock())) {
            return Hand.OFF_HAND;
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
        ClientPlayerEntity player = client.player;
        Vec3d eye = player.getCameraPosVec(1.0F);
        return new PiercingRay(player, eye, eye.add(player.getRotationVec(1.0F).multiply(attackRayLength(player))));
    }

    private double attackRayLength(ClientPlayerEntity player) {
        double attackReach = player.getAttackRange().getEffectiveMaxRange(player);
        return Math.max(attackReach, player.getBlockInteractionRange());
    }

    private double maximumAttackDistanceSquared(PiercingRay ray) {
        ClientPlayerEntity player = ray.player();
        double reach = player.getAttackRange().getEffectiveMaxRange(player);
        double maximum = reach * reach;
        if (blocks.getValue()) {
            return maximum;
        }
        BlockHitResult hit = raycastBlocks(ray);
        if (hit.getType() == HitResult.Type.MISS) {
            return maximum;
        }
        double hitDistance = ray.start().squaredDistanceTo(hit.getPos());
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
        ClientPlayerEntity player = client.player;
        Vec3d eye = player.getCameraPosVec(1.0F);
        Vec3d end = eye.add(player.getRotationVec(1.0F).multiply(player.getBlockInteractionRange()));
        return new PiercingRay(player, eye, end);
    }

    private BlockHitResult raycastBlocks(PiercingRay ray) {
        return client.world.raycast(new RaycastContext(ray.start(), ray.end(), RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE, ray.player()));
    }

    private List<PiercingTarget> collectTargets(PiercingRay ray, double maximumDistanceSquared) {
        List<PiercingTarget> targets = new ArrayList<>();
        Box search = ray.player().getBoundingBox().stretch(ray.end().subtract(ray.start())).expand(1.0, 1.0, 1.0);
        for (Entity entity : client.world.getOtherEntities(ray.player(), search, EntityPredicates.CAN_HIT)) {
            if (!(entity instanceof LivingEntity living) || !TargetSelection.isValidTarget(living)) {
                continue;
            }
            Box bounds = entity.getBoundingBox().expand(entity.getTargetingMargin());
            Optional<Vec3d> intersection = bounds.raycast(ray.start(), ray.end());
            if (intersection.isEmpty()) {
                continue;
            }
            Vec3d position = intersection.get();
            if (ray.start().squaredDistanceTo(position) > maximumDistanceSquared) {
                continue;
            }
            if (!ray.player().getAttackRange().isWithinRange(ray.player(), position)) {
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
