package dev.echo.client.utils.interaction;

import dev.echo.client.Echo;
import dev.echo.client.MinecraftAccess;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

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
        if (CLIENT.player.getActiveHand() == Hand.OFF_HAND) {
            CLIENT.interactionManager.stopUsingItem(CLIENT.player);
        } else {
            CLIENT.player.clearActiveItem();
        }
    }

    public static boolean isUsingOffhand() {
        return CLIENT.player != null && CLIENT.player.isUsingItem() && CLIENT.player.getActiveHand() == Hand.OFF_HAND;
    }

    public static boolean isBlockingOffhand() {
        return CLIENT.player != null && CLIENT.player.isUsingItem() && CLIENT.player.getOffHandStack().getItem() == Items.SHIELD
                && CLIENT.player.getActiveHand() == Hand.OFF_HAND;
    }

    public static boolean isBlockingMainHand() {
        return CLIENT.player != null && CLIENT.player.isUsingItem() && CLIENT.player.getMainHandStack().getItem() == Items.SHIELD
                && CLIENT.player.getActiveHand() == Hand.MAIN_HAND;
    }

    public static boolean isBlocking() {
        return isBlockingOffhand() || isBlockingMainHand();
    }

    public static boolean holdsShield() {
        return CLIENT.player != null && (CLIENT.player.getMainHandStack().getItem() == Items.SHIELD
                || CLIENT.player.getOffHandStack().getItem() == Items.SHIELD);
    }

    public static boolean hasMovementInput() {
        int counter = getInteractionCounterFallbackValue();
        if (counter != 0 && CLIENT.player == null) {
            return false;
        }
        return CLIENT.player.input.getMovementInput().length() != 0.0F;
    }

    public static double predictedFallDistance() {
        if (CLIENT.player == null) {
            return 0.0;
        }
        return predictedFallDistance(CLIENT.player);
    }

    public static void setVelocityX(double x) {
        CLIENT.player.setVelocity(x, CLIENT.player.getVelocity().y, CLIENT.player.getVelocity().z);
    }

    public static void setVelocityY(double y) {
        CLIENT.player.setVelocity(CLIENT.player.getVelocity().x, y, CLIENT.player.getVelocity().z);
    }

    public static void setVelocityZ(double z) {
        CLIENT.player.setVelocity(CLIENT.player.getVelocity().x, CLIENT.player.getVelocity().y, z);
    }

    public static double predictedFallDistance(PlayerEntity player) {
        if (player == null) {
            return 0.0;
        }
        double previousY = player.lastY;
        double currentY = player.getY();
        double verticalVelocity = player.getVelocity().y;
        double fallDistance = player.fallDistance;
        double predictedVelocity = verticalVelocity + 0.08;
        if (predictedVelocity < -3.92) {
            predictedVelocity = -3.92;
        }
        double predictedY = currentY + predictedVelocity;
        if (player.isGliding()) {
            predictedY += 1.2000000000000002;
        }
        double result = fallDistance + (previousY - predictedY);
        return result < 0.0 ? 0.0 : result;
    }

    public static Boolean intersects(LivingEntity first, LivingEntity second) {
        return first != null && second != null && first.getBoundingBox().intersects(second.getBoundingBox());
    }

    public static Boolean hasNearbyEntity(LivingEntity entity) {
        return entity != null && CLIENT.world != null
                && !CLIENT.world.getOtherEntities(entity, entity.getBoundingBox().expand(0.2)).isEmpty();
    }

    public static boolean hasNearbyBoat(LivingEntity entity) {
        if (entity == null || CLIENT.world == null) {
            return false;
        }
        for (Entity nearby : CLIENT.world.getOtherEntities(entity, entity.getBoundingBox().expand(0.2))) {
            if (nearby instanceof AbstractBoatEntity) {
                return true;
            }
        }
        return false;
    }

    public static LivingEntity getManualSelectTarget() {
        if (CLIENT.player == null || CLIENT.world == null) {
            return null;
        }
        float tickProgress = CLIENT.getRenderTickCounter().getTickProgress(false);
        float yaw = CLIENT.player.getYaw(tickProgress);
        float pitch = CLIENT.player.getPitch(tickProgress);
        Vec3d start = CLIENT.player.getCameraPosVec(tickProgress);
        Vec3d direction = CLIENT.player.getRotationVector(pitch, yaw);
        double reach = CLIENT.player.getEntityInteractionRange();
        Vec3d end = start.add(direction.multiply(reach));
        Box searchBounds = CLIENT.player.getBoundingBox().stretch(direction.multiply(reach)).expand(1.0);
        EntityHitResult hit = ProjectileUtil.raycast(CLIENT.player, start, end, searchBounds,
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
