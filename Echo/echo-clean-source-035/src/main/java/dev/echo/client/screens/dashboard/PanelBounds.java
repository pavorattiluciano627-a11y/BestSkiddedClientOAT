package dev.echo.client.screens.dashboard;

import dev.echo.client.ui.UiRect;

/** Anchor and popup panel rectangles; original encrypted accessor targets remain pending. */
public record PanelBounds(UiRect anchor, UiRect popup) {
}
