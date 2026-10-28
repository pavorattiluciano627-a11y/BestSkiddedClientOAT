package dev.echo.client.events.movement;

import dev.echo.client.events.Event;
import dev.echo.client.events.EventPhase;

/** Mutable position, ground state and rotation used by movement-packet hooks. */
public class MovementPacketEvent extends Event {
    private double x;
    private double y;
    private double z;
    private boolean onGround;
    private double yaw;
    private double pitch;

    public MovementPacketEvent(EventPhase phase, double x, double y, double z,
                               boolean onGround, double yaw, double pitch) {
        super(phase);
        this.x = x;
        this.y = y;
        this.z = z;
        this.onGround = onGround;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public double getYaw() {
        return yaw;
    }

    public double getPitch() {
        return pitch;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    public void setYaw(double yaw) {
        this.yaw = yaw;
    }

    public void setPitch(double pitch) {
        this.pitch = pitch;
    }

    /** Payload captured before sending movement packets. */
    public static class Pre extends MovementPacketEvent {
        public Pre(double x, double y, double z, boolean onGround, double yaw, double pitch) {
            super(EventPhase.PRE, x, y, z, onGround, yaw, pitch);
        }
    }

    /** Payload captured by the return hook after sending movement packets. */
    public static class Post extends MovementPacketEvent {
        public Post(double x, double y, double z, boolean onGround, double yaw, double pitch) {
            super(EventPhase.POST, x, y, z, onGround, yaw, pitch);
        }
    }
}
