package dev.echo.client.ui;

/** Immutable interaction result in the original hovered/held/pressed/released/clicked order. */
public record UiInteraction(boolean hovered, boolean held, boolean pressed, boolean released, boolean clicked) {
    public static final UiInteraction NONE = new UiInteraction(false, false, false, false, false);
}
