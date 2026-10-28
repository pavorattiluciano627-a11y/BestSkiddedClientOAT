package dev.echo.client.scripting;

import dev.echo.client.events.Event;
import dev.echo.client.events.EventBus;
import dev.echo.client.events.input.KeyboardEvent;
import dev.echo.client.events.interaction.AttackAttemptEvent;
import dev.echo.client.events.interaction.HandSwingEvent;
import dev.echo.client.events.lifecycle.ClientTickEvent;
import dev.echo.client.events.lifecycle.PlayerTickEvent;
import dev.echo.client.events.movement.MovementPacketEvent;
import dev.echo.client.events.network.PacketReceiveEvent;
import dev.echo.client.events.network.PacketSendEvent;
import dev.echo.client.events.render.HudRenderEvent;
import dev.echo.client.events.render.WorldRenderEvent;
import dev.echo.client.hud.HudSurfacePreview;
import dev.echo.client.scripting.api.AttackEventView;
import dev.echo.client.scripting.api.HandSwingEventView;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Structured event-callback adapter. Literal and binding provenance, including
 * the canonical shared-cache prerequisite, is recorded in analysis/fixed-v2.
 */
public record ScriptCallbacks(ScriptEngine engine, ScriptDefinition definition) {
    private static final Map<String, Class<? extends Event>> EVENT_TYPES = Map.ofEntries(
        Map.entry("tick", ClientTickEvent.class),
        Map.entry("playerTick", PlayerTickEvent.class),
        Map.entry("render2d", HudRenderEvent.class),
        Map.entry("render3d", WorldRenderEvent.class),
        Map.entry("packetSend", PacketSendEvent.class),
        Map.entry("packetReceive", PacketReceiveEvent.class),
        Map.entry("move", MovementPacketEvent.class),
        Map.entry("key", KeyboardEvent.class),
        Map.entry("attack", AttackAttemptEvent.class),
        Map.entry("swingHand", HandSwingEvent.class)
    );
    private static final Map<Class<? extends Event>, Function<Event, Object>> EVENT_ADAPTERS = Map.of(
        AttackAttemptEvent.class, event -> new AttackEventView((AttackAttemptEvent) event),
        HandSwingEvent.class, event -> new HandSwingEventView((HandSwingEvent) event),
        HudRenderEvent.class, event -> new HudSurfacePreview((HudRenderEvent) event)
    );

    /** Returns the map's immutable supported-name key set. */
    public static Set<String> eventNames() { return EVENT_TYPES.keySet(); }

    /** Registers every supplied callback with this record as its owner. */
    public void register() {
        definition.events().forEach((name, callback) -> {
            Class<? extends Event> type = EVENT_TYPES.get(name);
            Function<Event, Object> adapter = EVENT_ADAPTERS.get(type);
            EventBus.registerCallback(type, this, event -> invoke(callback, adapter, event));
        });
    }

    /** Removes this record's functional subscriptions from every event type. */
    public void unregister() { EventBus.unregisterCallbacks(this); }

    private void invoke(org.mozilla.javascript.Function callback, Function<Event, Object> adapter, Event event) {
        try {
            Object argument = adapter == null ? event : adapter.apply(event);
            engine.call(callback, definition.scope(), definition.scriptObject(), new Object[]{argument});
        } catch (Exception exception) {
            ScriptErrorReporter.report(ScriptEngine.LOGGER, definition.id(), "event handler", exception);
        }
    }
}
