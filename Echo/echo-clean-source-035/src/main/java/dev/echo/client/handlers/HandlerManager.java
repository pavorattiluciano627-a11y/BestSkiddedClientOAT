package dev.echo.client.handlers;

import dev.echo.client.events.EventBus;
import java.util.ArrayList;

/** Owns the client's handler instances and their ordered event subscriptions. */
public class HandlerManager {
    private final ArrayList<ClientHandler> handlers = new ArrayList<>();
    private final RotationManager rotationManager = new RotationManager();

    /** Returns the rotation manager created with this handler manager. */
    public RotationManager getRotationManager() {
        return rotationManager;
    }

    /** Constructs all handlers in original order, then registers their subscribers. */
    public void initialize() {
        if (!handlers.isEmpty()) {
            return;
        }
        handlers.add(rotationManager);
        handlers.add(new FeatureBindHandler());
        handlers.add(new AttackCooldownTracker());
        handlers.add(new SprintOverrideHandler());
        handlers.add(new DeferredActionQueue());
        handlers.add(new BlockingTicksTracker());
        handlers.add(new ServerFallDistanceTracker());
        handlers.add(new EntityPositionTracker());
        handlers.add(new LatencyTracker());
        for (ClientHandler handler : handlers) {
            EventBus.register(handler);
        }
    }

    /** Unregisters handlers, clears deferred actions and resets rotation state. */
    public void shutdown() {
        for (ClientHandler handler : handlers) {
            EventBus.unregister(handler);
        }
        DeferredActionQueue.clear();
        rotationManager.reset();
        handlers.clear();
    }
}
