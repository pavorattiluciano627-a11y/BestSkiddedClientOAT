package dev.echo.client.events.input;

import dev.echo.client.events.Event;
import net.minecraft.util.PlayerInput;

/** Mutable key states together with an immutable snapshot of the original input. */
public class PlayerInputEvent extends Event {
    public static PlayerInputEvent lastInput;
    public static boolean inputProcessed;

    public boolean forward;
    public boolean backward;
    public boolean left;
    public boolean right;
    public boolean jump;
    public boolean sneak;
    public boolean sprint;
    public boolean allowMovementInFreecam;
    public final PlayerInput originalInput;

    public PlayerInputEvent(boolean forward, boolean backward, boolean left, boolean right,
                            boolean jump, boolean sneak, boolean sprint) {
        this.allowMovementInFreecam = false;
        this.forward = forward;
        this.backward = backward;
        this.left = left;
        String[] sentinel = KeybindingEvent.getInputSentinel();
        this.right = right;
        this.jump = jump;
        this.sneak = sneak;
        this.sprint = sprint;
        this.originalInput = new PlayerInput(forward, backward, left, right, jump, sneak, sprint);
        if (sentinel != null) {
            int nextCounter = Event.getEventCounterFallbackValue() + 1;
            Event.setEventCounter(nextCounter);
        }
    }

    /** Preserves both original branches using the actual shared input sentinel. */
    public static float movementMultiplier(boolean positive, boolean negative) {
        String[] sentinel = KeybindingEvent.getInputSentinel();
        if (sentinel == null && positive == negative) {
            return 0.0F;
        }
        return positive ? 1.0F : -1.0F;
    }
}
