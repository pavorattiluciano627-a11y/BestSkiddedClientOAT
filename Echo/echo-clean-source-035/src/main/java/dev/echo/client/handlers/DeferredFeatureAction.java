package dev.echo.client.handlers;

import dev.echo.client.features.Feature;

/** Mutable top-of-stack slot request with its original baseline and completion action. */
final class DeferredFeatureAction {
    final Feature owner;
    final int baselineSlot;
    int selectedSlot;
    boolean restoreAll;
    long expiresAtTick;
    ActionPhase phase;
    Runnable callback;

    DeferredFeatureAction(Feature owner, int baselineSlot, int selectedSlot, boolean restoreAll,
                          long expiresAtTick, ActionPhase phase, Runnable callback) {
        this.owner = owner;
        this.baselineSlot = baselineSlot;
        this.selectedSlot = selectedSlot;
        this.restoreAll = restoreAll;
        this.expiresAtTick = expiresAtTick;
        this.phase = phase;
        this.callback = callback;
    }

    boolean isDue(long tick, ActionPhase currentPhase) {
        return expiresAtTick != Long.MAX_VALUE && tick >= expiresAtTick && currentPhase.ordinal() >= phase.ordinal();
    }
}
