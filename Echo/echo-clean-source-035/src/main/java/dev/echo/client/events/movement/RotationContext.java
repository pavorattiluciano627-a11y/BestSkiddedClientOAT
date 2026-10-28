package dev.echo.client.events.movement;

/** The operation requesting yaw/pitch, in the original name and ordinal order. */
public enum RotationContext {
    LOOK,
    HEAD,
    BODY,
    ATTACK,
    RENDER_PITCH,
    INTERACT,
    MOVEMENT
}
