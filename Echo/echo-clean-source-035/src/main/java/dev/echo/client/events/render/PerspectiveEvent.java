package dev.echo.client.events.render;

import dev.echo.client.events.Event;
import net.minecraft.client.CameraType;

/** Retains the perspective supplied to the options setter. */
public class PerspectiveEvent extends Event {
    private final CameraType perspective;

    public PerspectiveEvent(CameraType perspective) {
        this.perspective = perspective;
    }

    public CameraType getPerspective() {
        return perspective;
    }
}
