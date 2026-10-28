package dev.echo.client.events.render;

import dev.echo.client.events.Event;
import dev.echo.client.events.EventPhase;
import net.minecraft.client.gl.Framebuffer;

/** Outline-framebuffer hook payload, sharing Event's mutable cancellation state. */
public class OutlineFramebufferEvent extends Event {
    private final Framebuffer framebuffer;
    private final boolean canDrawEntityOutlines;

    private OutlineFramebufferEvent(EventPhase phase, Framebuffer framebuffer,
                                    boolean canDrawEntityOutlines) {
        super(phase);
        this.framebuffer = framebuffer;
        this.canDrawEntityOutlines = canDrawEntityOutlines;
    }

    public Framebuffer getFramebuffer() {
        return framebuffer;
    }

    public boolean canDrawEntityOutlines() {
        return canDrawEntityOutlines;
    }

    /** Preparation hook; its original phase identity is POST. */
    public static class Prepare extends OutlineFramebufferEvent {
        public Prepare(Framebuffer framebuffer, boolean canDrawEntityOutlines) {
            super(EventPhase.POST, framebuffer, canDrawEntityOutlines);
        }
    }

    /** Render hook; the original payload is a null framebuffer and false capability. */
    public static class Render extends OutlineFramebufferEvent {
        public Render() {
            super(EventPhase.PRE, null, false);
        }
    }
}
