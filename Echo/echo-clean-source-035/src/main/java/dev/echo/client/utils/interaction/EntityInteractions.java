package dev.echo.client.utils.interaction;

import dev.echo.client.Echo;
import dev.echo.client.MinecraftAccess;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Live player interaction and target queries in the documented normal key/guard domain. */
public final class EntityInteractions implements MinecraftAccess {
    private static int interactionCounter;

    static {
        if (getInteractionCounterFallbackValue() == 0) {
            setInteractionCounter(59);
        }
    }

    /** Original shared control-state write; initialization/cache applicability remains conditional. */
    public static void setInteractionCounter(int value) {
        interactionCounter = value;
    }

    public static int getInteractionCounter() {
        return interactionCounter;
    }

    public static int getInteractionCounterFallbackValue() {
        return getInteractionCounter() == 0 ? 70 : 0;
    }

    public EntityInteractions() {}

    public static void stopUsingItem() {
        if (CLIENT.player == null || !CLIENT.player.isUsingItem()) {
            return;
        }
        if (CLIENT.player.getUsedItemHand() == InteractionHand.OFF_HAND) {
            CLIENT.gameMode.releaseUsingItem(CLIENT.player);
        } else {
            CLIENT.player.stopUsingItem();
        }
    }

    public static boolean isUsingOffhand() {
        return CLIENT.player != null && CLIENT.player.isUsingItem() && CLIENT.player.getUsedItemHand() == InteractionHand.OFF_HAND;
    }

    public static boolean isBlockingOffhand() {
        return CLIENT.player != null && CLIENT.player.isUsingItem() && CLIENT.player.getOffhandItem().getItem() == Items.SHIELD
                && CLIENT.player.getUsedItemHand() == InteractionHand.OFF_HAND;
    }

    public static boolean isBlockingMainHand() {
        return CLIENT.player != null && CLIENT.player.isUsingItem() && CLIENT.player.getMainHandItem().getItem() == Items.SHIELD
                && CLIENT.player.getUsedItemHand() == InteractionHand.MAIN_HAND;
    }

    public static boolean isBlocking() {
        return isBlockingOffhand() || isBlockingMainHand();
    }

    public static boolean holdsShield() {
        return CLIENT.player != null && (CLIENT.player.getMainHandItem().getItem() == Items.SHIELD
                || CLIENT.player.getOffhandItem().getItem() == Items.SHIELD);
    }

    public static boolean hasMovementInput() {
        int counter = getInteractionCounterFallbackValue();
        if (counter != 0 && CLIENT.player == null) {
            return false;
        }
        return CLIENT.player.input.getMoveVector().length() != 0.0F;
    }

    public static double predictedFallDistance() {
        if (CLIENT.player == null) {
            return 0.0;
        }
        return predictedFallDistance(CLIENT.player);
    }

    public static void setVelocityX(double x) {
        CLIENT.player.setDeltaMovement(x, CLIENT.player.getDeltaMovement().y, CLIENT.player.getDeltaMovement().z);
    }

    public static void setVelocityY(double y) {
        CLIENT.player.setDeltaMovement(CLIENT.player.getDeltaMovement().x, y, CLIENT.player.getDeltaMovement().z);
    }

    public static void setVelocityZ(double z) {
        CLIENT.player.setDeltaMovement(CLIENT.player.getDeltaMovement().x, CLIENT.player.getDeltaMovement().y, z);
    }

    public static double predictedFallDistance(Player player) {
        if (player == null) {
            return 0.0;
        }
        double previousY = player.yo;
        double currentY = player.getY();
        double verticalVelocity = player.getDeltaMovement().y;
        double fallDistance = player.fallDistance;
        double predictedVelocity = verticalVelocity + 0.08;
        if (predictedVelocity < -3.92) {
            predictedVelocity = -3.92;
        }
        double predictedY = currentY + predictedVelocity;
        if (player.isFallFlying()) {
            predictedY += 1.2000000000000002;
        }
        double result = fallDistance + (previousY - predictedY);
        return result < 0.0 ? 0.0 : result;
    }

    public static Boolean intersects(LivingEntity first, LivingEntity second) {
        return first != null && second != null && first.getBoundingBox().intersects(second.getBoundingBox());
    }

    public static Boolean hasNearbyEntity(LivingEntity entity) {
        return entity != null && CLIENT.level != null
                && !CLIENT.level.getEntities(entity, entity.getBoundingBox().inflate(0.2)).isEmpty();
    }

    public static boolean hasNearbyBoat(LivingEntity entity) {
        if (entity == null || CLIENT.level == null) {
            return false;
        }
        for (Entity nearby : CLIENT.level.getEntities(entity, entity.getBoundingBox().inflate(0.2))) {
            if (nearby instanceof AbstractBoat) {
                return true;
            }
        }
        return false;
    }

    public static LivingEntity getManualSelectTarget() {
        if (CLIENT.player == null || CLIENT.level == null) {
            return null;
        }
        float tickProgress = CLIENT.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float yaw = CLIENT.player.getViewYRot(tickProgress);
        float pitch = CLIENT.player.getViewXRot(tickProgress);
        Vec3 start = CLIENT.player.getEyePosition(tickProgress);
        Vec3 direction = CLIENT.player.calculateViewVector(pitch, yaw);
        double reach = CLIENT.player.entityInteractionRange();
        Vec3 end = start.add(direction.scale(reach));
        AABB searchBounds = CLIENT.player.getBoundingBox().expandTowards(direction.scale(reach)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(CLIENT.player, start, end, searchBounds,
                EntityInteractions::isManualSelectionCandidate, reach * reach);
        if (hit == null) {
            return null;
        }
        Entity target = hit.getEntity();
        return target instanceof LivingEntity living ? living : null;
    }

    public static boolean isFriend(String name) {
        return Echo.friendManager != null && Echo.friendManager.contains(name);
    }

    private static boolean isManualSelectionCandidate(Entity entity) {
        return !entity.isSpectator() && entity.isAlive();
    }
}
