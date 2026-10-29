package dev.echo.client.diag;

import dev.echo.client.Echo;
import dev.echo.client.events.EventBus;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.KeyboardEvent;
import dev.echo.client.events.lifecycle.ClientTickEvent;
import dev.echo.client.events.render.HudRenderEvent;
import dev.echo.client.features.Feature;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One-shot startup audit that answers "why does this module do nothing?".
 *
 * <p>Every counter here is fed by an event that can only exist if the mixin which
 * posts it actually bound. That makes the numbers evidence, not guesses:
 *
 * <ul>
 *   <li>{@code ClientTickEvent > 0} proves the {@code Minecraft.tick} hook applied.</li>
 *   <li>{@code KeyboardEvent > 0} proves the {@code KeyboardHandler.keyPress} hook applied.</li>
 *   <li>{@code HudRenderEvent > 0} proves the HUD pass is running.</li>
 * </ul>
 *
 * <p>It also lists the modules that are enabled and how many event subscribers are
 * attached, which separates "the module is off" from "the module is on but its
 * effects are never driven".
 */
public final class StartupDiagnostics {
    private static final AtomicInteger TICKS = new AtomicInteger();
    private static final AtomicInteger KEYBOARD = new AtomicInteger();
    private static final AtomicInteger HUD = new AtomicInteger();
    private static final AtomicInteger EVENTS = new AtomicInteger();
    private static final int FIRST_DUMP = 60;
    private static final int SECOND_DUMP = 600;

    private StartupDiagnostics() {
    }

    /** Registers the audit; called once from the bootstrap after handlers exist. */
    public static void install() {
        EventBus.register(new Subscribers());
        Echo.LOGGER.info("[echo][diag] audit installed");
    }

    /** Counts every dispatched event, whatever its type. */
    public static void countDispatch() {
        EVENTS.incrementAndGet();
    }

    private static final class Subscribers {
        @Subscribe
        public void onTick(ClientTickEvent event) {
            int tick = TICKS.incrementAndGet();
            if (tick == FIRST_DUMP || tick == SECOND_DUMP) {
                dump(tick);
            }
        }

        @Subscribe
        public void onKey(KeyboardEvent event) {
            KEYBOARD.incrementAndGet();
        }

        @Subscribe
        public void onHud(HudRenderEvent event) {
            HUD.incrementAndGet();
        }
    }

    private static void dump(int tick) {
        List<String> enabled = new ArrayList<>();
        int total = 0;
        if (Echo.featureManager != null) {
            for (Feature feature : Echo.featureManager.getFeatures()) {
                total++;
                if (feature.isEnabled()) {
                    enabled.add(feature.getName() == null ? "?" : feature.getName().toString());
                }
            }
        }
        Echo.LOGGER.info("[echo][diag] tick={} events={} ticks={} keyboard={} hud={} "
                        + "features={} enabled={} handlers={} uiRenderer={}",
                tick, EVENTS.get(), TICKS.get(), KEYBOARD.get(), HUD.get(),
                total, enabled.size(),
                Echo.handlerManager == null ? -1 : Echo.handlerManager.size(),
                Echo.uiRenderer != null);
        if (!enabled.isEmpty()) {
            Echo.LOGGER.info("[echo][diag] enabled modules: {}", enabled);
        }
        // Interpret the counters so the next report does not need a second round trip.
        if (KEYBOARD.get() == 0) {
            Echo.LOGGER.warn("[echo][diag] no KeyboardEvent was ever posted: the "
                    + "KeyboardHandler.keyPress hook is not bound, so keybinds and every "
                    + "input-driven module are dead");
        }
        if (HUD.get() == 0) {
            Echo.LOGGER.warn("[echo][diag] no HudRenderEvent was posted: the HUD pass is "
                    + "not running, so HUD and most render modules cannot draw");
        }
    }
}
