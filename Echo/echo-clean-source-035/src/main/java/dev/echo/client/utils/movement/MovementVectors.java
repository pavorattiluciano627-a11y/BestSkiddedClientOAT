package dev.echo.client.utils.movement;

import dev.echo.client.MinecraftAccess;
import dev.echo.client.events.Event;
import dev.echo.client.utils.interaction.EntityInteractions;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;

/**
 * Input-relative yaw and the original distinct replacement/additive horizontal
 * velocity operations. The hand-movement receipt records binding and packed-key
 * domains, including the original PlayerInput.DEFAULT reads and shared counters.
 */
public final class MovementVectors implements MinecraftAccess {
    private MovementVectors() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /** Adds the input-vector angle to the local player's yaw, with float negation/addition. */
    public static float yawFromInput(float forward, float sideways) {
        return CLIENT.player.getYaw() + (float) Math.toDegrees(MathHelper.atan2((double) -sideways, (double) forward));
    }

    /**
     * Reads the original Minecraft input record and both shared control counters.
     * DEFAULT is the original field identity; it is not a synthesized input value.
     */
    public static double directionRadians() {
        int counter = EntityInteractions.getInteractionCounterFallbackValue();
        float yaw = CLIENT.player.getYaw();
        PlayerInput input = PlayerInput.DEFAULT;
        if (counter != 0 && input.forward()) {
            yaw += 0.0F;
        } else {
            if (counter != 0) {
                input = PlayerInput.DEFAULT;
            }
            if (input.backward()) {
                yaw += 180.0F;
            }
        }
        float strafeFactor = 1.0F;
        input = PlayerInput.DEFAULT;
        if (counter != 0) {
            if (input.backward()) {
                strafeFactor = -0.5F;
            } else {
                input = PlayerInput.DEFAULT;
                if (input.forward()) {
                    strafeFactor = 0.5F;
                }
            }
            input = PlayerInput.DEFAULT;
            if (input.left()) {
                yaw += 90.0F * strafeFactor;
            }
            input = PlayerInput.DEFAULT;
        }
        if (input.right()) {
            yaw -= 90.0F * strafeFactor;
        }
        double direction = Math.toRadians((double) yaw);
        if (Event.getEventCounter() != 0) {
            EntityInteractions.setInteractionCounter(++counter);
        }
        return direction;
    }

    /** Replaces horizontal velocity using Minecraft float trig; retains the current Y velocity. */
    public static void setHorizontalSpeed(float speed) {
        if (!EntityInteractions.hasMovementInput()) {
            return;
        }
        double direction = directionRadians();
        CLIENT.player.setVelocity(
                (double) (-MathHelper.sin((double) (float) direction) * speed),
                CLIENT.player.getVelocity().y,
                (double) (MathHelper.cos((double) (float) direction) * speed));
    }

    /** Adds a double-trig horizontal delta, retaining the original separate velocity reads. */
    public static void addHorizontalSpeed(float speed) {
        if (!EntityInteractions.hasMovementInput()) {
            return;
        }
        double direction = directionRadians();
        CLIENT.player.setVelocity(
                CLIENT.player.getVelocity().x - Math.sin(direction) * (double) speed,
                CLIENT.player.getVelocity().y,
                CLIENT.player.getVelocity().z + Math.cos(direction) * (double) speed);
    }
}
