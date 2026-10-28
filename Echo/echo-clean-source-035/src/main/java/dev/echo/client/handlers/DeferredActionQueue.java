package dev.echo.client.handlers;

import dev.echo.client.events.EventPriority;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.HandleInputEvent;
import dev.echo.client.events.interaction.EntityAttackEvent;
import dev.echo.client.events.network.PacketReceiveEvent;
import dev.echo.client.events.network.PacketSendEvent;
import dev.echo.client.features.Feature;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;

/**
 * LIFO hotbar ownership and phase/tick-driven completion callbacks.
 * Conditional reconstruction: original key, shared-cache and opaque-state
 * applicability obligations are retained in the provider receipt.
 */
public final class DeferredActionQueue extends ClientHandler {
    private static final Deque<DeferredFeatureAction> actions = new ArrayDeque<>();
    private static boolean switchingBlocked;

    public static boolean select(Feature owner, int slot) {
        return select(owner, slot, 0, true, null);
    }

    public static boolean select(Feature owner, int slot, int delayTicks) {
        return select(owner, slot, delayTicks, true, null);
    }

    public static boolean select(Feature owner, int slot, int delayTicks, boolean restoreAll) {
        return select(owner, slot, delayTicks, restoreAll, null);
    }

    public static boolean select(Feature owner, int slot, int delayTicks, Runnable callback) {
        return select(owner, slot, delayTicks, true, callback);
    }

    /**
     * Selects a hotbar slot and pushes or updates the owner's request.
     * Negative delay creates an indefinite request. A buried owner cannot replace
     * a newer owner's request; the top owner may update its request in place.
     */
    public static boolean select(Feature owner, int slot, int delayTicks, boolean restoreAll, Runnable callback) {
        if (owner == null || CLIENT.player == null || slot < 0 || slot > 8) return false;
        if (switchingBlocked && slot != selectedSlot()) return false;
        DeferredFeatureAction top = actions.peek();
        if (top != null && top.owner == owner) {
            top.selectedSlot = slot;
            top.restoreAll = restoreAll;
            top.expiresAtTick = delayTicks < 0 ? Long.MAX_VALUE : (long) (CLIENT.player.tickCount + delayTicks);
            top.phase = delayTicks <= 0 ? ActionPhase.POST : ActionPhase.EARLY;
            top.callback = callback;
            setHotbarSlot(slot);
            return true;
        }
        if (contains(owner)) return false;
        int current = selectedSlot();
        if (current == slot) return false;
        DeferredFeatureAction oldest = actions.peekLast();
        int baseline = oldest == null ? current : oldest.baselineSlot;
        long deadline = delayTicks < 0 ? Long.MAX_VALUE : (long) (CLIENT.player.tickCount + delayTicks);
        ActionPhase phase = delayTicks <= 0 ? ActionPhase.POST : ActionPhase.EARLY;
        actions.push(new DeferredFeatureAction(owner, baseline, slot, restoreAll, deadline, phase, callback));
        setHotbarSlot(slot);
        return true;
    }

    public static boolean ensureSelected(Feature owner, int slot) {
        return ensureSelected(owner, slot, 0, true, null);
    }

    public static boolean ensureSelected(Feature owner, int slot, int delayTicks) {
        return ensureSelected(owner, slot, delayTicks, true, null);
    }

    public static boolean ensureSelected(Feature owner, int slot, int delayTicks, boolean restoreAll) {
        return ensureSelected(owner, slot, delayTicks, restoreAll, null);
    }

    public static boolean ensureSelected(Feature owner, int slot, int delayTicks, Runnable callback) {
        return ensureSelected(owner, slot, delayTicks, true, callback);
    }

    /** Like select, but an already-selected unowned slot succeeds without a new callback. */
    public static boolean ensureSelected(Feature owner, int slot, int delayTicks, boolean restoreAll, Runnable callback) {
        if (owner == null || CLIENT.player == null || slot < 0 || slot > 8) return false;
        if (selectedSlot() == slot && !contains(owner)) return true;
        return select(owner, slot, delayTicks, restoreAll, callback);
    }

    public static boolean contains(Feature owner) {
        if (owner == null) return false;
        for (DeferredFeatureAction action : actions) if (action.owner == owner) return true;
        return false;
    }

    public static boolean contains(Class<?> featureType) {
        if (featureType == null) return false;
        for (DeferredFeatureAction action : actions) if (featureType.isInstance(action.owner)) return true;
        return false;
    }

    public static boolean hasPendingActions() { return !actions.isEmpty(); }

    /** Updates only the top owner's recorded slot, without selecting it immediately. */
    public static boolean updateSelectedSlot(Feature owner, int slot) {
        if (owner == null) return false;
        DeferredFeatureAction top = actions.peek();
        if (top == null || top.owner != owner) return false;
        top.selectedSlot = slot;
        return true;
    }

    public static int selectedSlot(Feature owner) {
        if (owner == null) return -1;
        DeferredFeatureAction top = actions.peek();
        return top == null || top.owner != owner ? -1 : top.selectedSlot;
    }

    public static int selectedSlot() {
        if (CLIENT.player == null) return -1;
        DeferredFeatureAction top = actions.peek();
        return top == null ? CLIENT.player.getInventory().getSelectedSlot() : top.selectedSlot;
    }

    public static void finish(Feature owner) { finish(owner, true); }

    /** Restores/drains the stack or pops through the named owner, preserving callback order. */
    public static void finish(Feature owner, boolean restoreAll) {
        if (owner == null || actions.isEmpty() || !contains(owner)) return;
        if (restoreAll) {
            restoreAndDrain(actions.peekLast());
            return;
        }
        while (!actions.isEmpty()) {
            DeferredFeatureAction action = actions.pop();
            finishAction(action, false, true);
            if (action.owner == owner) break;
        }
    }

    /** Drops only the owner's requests, without slot restoration or callbacks. */
    public static void drop(Feature owner) {
        if (owner != null) actions.removeIf(action -> action.owner == owner);
    }

    /** Clears requests without callbacks; the packet-boundary flag is intentionally retained. */
    public static void clear() { actions.clear(); }

    @Subscribe(priority = EventPriority.LOWEST)
    private void beforeInput(HandleInputEvent.Pre event) { executePhase(ActionPhase.EARLY); }

    @Subscribe(priority = EventPriority.LOWEST)
    private void afterInput(HandleInputEvent.Post event) { executePhase(ActionPhase.POST); }

    @Subscribe
    private void afterAttack(EntityAttackEvent.Post event) { switchingBlocked = true; }

    @Subscribe
    private void onPacketSent(PacketSendEvent event) {
        Packet<?> packet = event.getPacket();
        if (packet instanceof ServerboundPlayerActionPacket action && action.getAction() == ServerboundPlayerActionPacket.Action.STAB) {
            switchingBlocked = true;
        } else if (event.getPacket() instanceof ServerboundMovePlayerPacket || event.getPacket() instanceof ServerboundClientTickEndPacket) {
            switchingBlocked = false;
        }
    }

    @Subscribe
    private void onPacketReceived(PacketReceiveEvent event) {
        if (event.getPacket() instanceof ClientboundRespawnPacket) {
            clear();
        } else if (event.getPacket() instanceof ClientboundPlayerPositionPacket) {
            CLIENT.execute(() -> { if (!hasFiniteDeadline()) clear(); });
        }
    }

    private static boolean hasFiniteDeadline() {
        for (DeferredFeatureAction action : actions) if (action.expiresAtTick != Long.MAX_VALUE) return true;
        return false;
    }

    private static void executePhase(ActionPhase phase) {
        if (CLIENT.player == null) { clear(); return; }
        while (!actions.isEmpty()) {
            DeferredFeatureAction action = actions.peek();
            if (!action.isDue(CLIENT.player.tickCount, phase)) break;
            actions.pop();
            if (action.restoreAll) {
                restoreAndDrain(action);
                runCallback(action);
                return;
            }
            finishAction(action, false, false);
        }
    }

    private static void restoreAndDrain(DeferredFeatureAction fallback) {
        DeferredFeatureAction baseline = actions.peekLast();
        if (baseline == null) baseline = fallback;
        if (baseline != null && CLIENT.player != null) setHotbarSlot(baseline.baselineSlot);
        while (!actions.isEmpty()) runCallback(actions.pop());
    }

    private static void finishAction(DeferredFeatureAction action, boolean restoreSlot, boolean forceRestore) {
        if (action == null) return;
        if (restoreSlot && CLIENT.player != null && (forceRestore || action.restoreAll)) setHotbarSlot(action.baselineSlot);
        runCallback(action);
    }

    private static void runCallback(DeferredFeatureAction action) {
        if (action.callback == null) return;
        try {
            action.callback.run();
        } catch (Throwable failure) {
            failure.printStackTrace();
        }
    }

    private static void setHotbarSlot(int slot) {
        if (slot < 0 || slot > 8) return;
        if (CLIENT.player.getInventory().getSelectedSlot() != slot) CLIENT.player.getInventory().setSelectedSlot(slot);
    }
}
