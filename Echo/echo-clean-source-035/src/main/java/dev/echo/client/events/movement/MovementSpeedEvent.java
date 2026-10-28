package dev.echo.client.events.movement;

import dev.echo.client.events.Event;

/**
 * Independent movement-query results shared by the player movement hooks.
 * Analysis candidate: field linkage awaits key proof.
 */
public class MovementSpeedEvent extends Event {
    private float activeItemSpeedMultiplier;
    private boolean blockedFromSprinting;
    private boolean slowDown;
    private boolean hasVehicle;

    /** Captures the multiplier, sprint-block, slowdown and vehicle-query results in order. */
    public MovementSpeedEvent(float activeItemSpeedMultiplier, boolean blockedFromSprinting,
                              boolean slowDown, boolean hasVehicle) {
        this.activeItemSpeedMultiplier = activeItemSpeedMultiplier;
        this.blockedFromSprinting = blockedFromSprinting;
        this.slowDown = slowDown;
        this.hasVehicle = hasVehicle;
    }

    /** Returns the active-item movement multiplier. */
    public float getActiveItemSpeedMultiplier() { return activeItemSpeedMultiplier; }

    /** Returns the sprint-block query result. */
    public boolean isBlockedFromSprinting() { return blockedFromSprinting; }

    /** Returns the slowdown query result. */
    public boolean shouldSlowDown() { return slowDown; }

    /** Returns the vehicle-query override. */
    public boolean hasVehicle() { return hasVehicle; }

    /** Replaces the multiplier without clamping it. */
    public void setActiveItemSpeedMultiplier(float multiplier) { activeItemSpeedMultiplier = multiplier; }

    /** Replaces the sprint-block result independently of other query results. */
    public void setBlockedFromSprinting(boolean blocked) { blockedFromSprinting = blocked; }

    /** Replaces the slowdown result independently of other query results. */
    public void setSlowDown(boolean slowDown) { this.slowDown = slowDown; }

    /** Replaces the vehicle-query result independently of other query results. */
    public void setHasVehicle(boolean hasVehicle) { this.hasVehicle = hasVehicle; }
}
