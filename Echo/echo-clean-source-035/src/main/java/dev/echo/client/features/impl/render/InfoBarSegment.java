package dev.echo.client.features.impl.render;

import dev.echo.client.hud.HudIcon;

/** An Info Bar segment retains its icon, leading text and value in record order. */
public record InfoBarSegment(HudIcon icon, String label, String value) {}
