package dev.echo.client.events.render;

import dev.echo.client.events.Event;
import net.minecraft.client.option.Perspective;

/** Retains the perspective supplied to the options setter. */
public class PerspectiveEvent extends Event {
    private final Perspective perspective;

    public PerspectiveEvent(Perspective perspective) {
        this.perspective = perspective;
    }

    public Perspective getPerspective() {
        return perspective;
    }
}
