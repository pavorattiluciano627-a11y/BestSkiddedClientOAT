package dev.echo.client.events.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.echo.client.events.Event;
import dev.echo.client.events.EventPhase;

/** Outline-framebuffer hook payload, sharing Event's mutable cancellation state. */
public class OutlineFramebufferEvent extends Event {
    private final RenderTarget framebuffer;
    private final boolean canDrawEntityOutlines;

    private OutlineFramebufferEvent(EventPhase phase, RenderTarget framebuffer,
                                    boolean canDrawEntityOutlines) {
        super(phase);
        this.framebuffer = framebuffer;
        this.canDrawEntityOutlines = canDrawEntityOutlines;
    }

    public RenderTarget getFramebuffer() {
        return framebuffer;
    }

    public boolean canDrawEntityOutlines() {
        return canDrawEntityOutlines;
    }

    /** Preparation hook; its original phase identity is POST. */
    public static class Prepare extends OutlineFramebufferEvent {
        public Prepare(RenderTarget framebuffer, boolean canDrawEntityOutlines) {
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
