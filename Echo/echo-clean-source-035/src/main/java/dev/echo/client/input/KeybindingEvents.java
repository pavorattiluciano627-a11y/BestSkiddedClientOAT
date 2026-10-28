package dev.echo.client.input;

import dev.echo.client.Echo;
import dev.echo.client.MinecraftAccess;
import dev.echo.client.events.Event;
import dev.echo.client.events.EventPhase;
import dev.echo.client.events.input.AttackKeyEvent;
import dev.echo.client.events.input.DropKeyEvent;
import dev.echo.client.events.input.HotbarKeyEvent;
import dev.echo.client.events.input.KeybindingEvent;
import dev.echo.client.events.input.PickItemKeyEvent;
import dev.echo.client.events.input.SwapHandsKeyEvent;
import dev.echo.client.events.input.UseKeyEvent;
import dev.echo.client.mixin.KeyBindingAccessor;
import java.util.function.IntFunction;
import net.minecraft.client.option.KeyBinding;

/** Intercepts pending key clicks before Minecraft consumes them in each input cycle. */
public final class KeybindingEvents {
    private static boolean attackHandled;
    private static boolean useHandled;
    private static boolean pickBlockHandled;
    private static boolean swapHandsHandled;
    private static boolean dropHandled;
    private static int nextHotbarSlot;

    private KeybindingEvents() {
    }

    /** Resets the five one-shot latches and the hotbar cursor in original write order. */
    public static void beginInputCycle() {
        attackHandled = false;
        useHandled = false;
        boolean dispatchStateClear = EventPhase.isDispatchStateClear();
        pickBlockHandled = false;
        swapHandsHandled = false;
        dropHandled = false;
        nextHotbarSlot = 0;
        if (Event.getEventCounter() != 0) {
            EventPhase.setDispatchState(!dispatchStateClear);
        }
    }

    public static void onAttack() {
        boolean dispatchStateClear = EventPhase.isDispatchStateClear();
        boolean handled = attackHandled;
        if (!dispatchStateClear) {
            if (handled) return;
            handled = true;
        }
        attackHandled = handled;
        hasAction(MinecraftAccess.CLIENT.options.attackKey, AttackKeyEvent::new);
    }

    public static void onUse() {
        boolean dispatchState = EventPhase.getDispatchState();
        boolean handled = useHandled;
        if (dispatchState) {
            if (handled) return;
            handled = true;
        }
        useHandled = handled;
        hasAction(MinecraftAccess.CLIENT.options.useKey, UseKeyEvent::new);
    }

    public static void onPickBlock() {
        boolean dispatchStateClear = EventPhase.isDispatchStateClear();
        boolean handled = pickBlockHandled;
        if (!dispatchStateClear) {
            if (handled) return;
            handled = true;
        }
        pickBlockHandled = handled;
        hasAction(MinecraftAccess.CLIENT.options.pickItemKey, PickItemKeyEvent::new);
    }

    public static void onSwapHands() {
        boolean dispatchStateClear = EventPhase.isDispatchStateClear();
        boolean handled = swapHandsHandled;
        if (!dispatchStateClear) {
            if (handled) return;
            handled = true;
        }
        swapHandsHandled = handled;
        hasAction(MinecraftAccess.CLIENT.options.swapHandsKey, SwapHandsKeyEvent::new);
    }

    public static void onDrop() {
        boolean dispatchStateClear = EventPhase.isDispatchStateClear();
        boolean handled = dropHandled;
        if (!dispatchStateClear) {
            if (handled) return;
            handled = true;
        }
        dropHandled = handled;
        hasAction(MinecraftAccess.CLIENT.options.dropKey, DropKeyEvent::new);
    }

    /** Each invocation advances the cursor, including out-of-range invocations. */
    public static void onHotbarSlot() {
        boolean dispatchStateClear = EventPhase.isDispatchStateClear();
        int slot = nextHotbarSlot++;
        if (!dispatchStateClear && slot < 0) return;
        if (slot >= MinecraftAccess.CLIENT.options.hotbarKeys.length) return;
        hasAction(MinecraftAccess.CLIENT.options.hotbarKeys[slot],
                clickCount -> new HotbarKeyEvent(slot, clickCount));
    }

    /** Posts, replays, then reconciles the same payload against the live pending count. */
    private static void hasAction(KeyBinding binding, IntFunction<KeybindingEvent> factory) {
        boolean dispatchState = EventPhase.getDispatchState();
        if (Echo.eventsDisabled || binding == null || MinecraftAccess.CLIENT.player == null) return;
        KeyBindingAccessor accessor = (KeyBindingAccessor) binding;
        KeybindingEvent event = factory.apply(accessor.getTimesPressed());
        int clickCount;
        if (dispatchState) {
            event.post();
            InputState.applyExtraClicks(event);
            clickCount = event.cancelled ? 0 : event.getClickCount();
        } else {
            clickCount = event.getClickCount();
        }
        if (clickCount != accessor.getTimesPressed()) {
            accessor.setTimesPressed(clickCount);
        }
    }
}
