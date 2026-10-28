package dev.echo.client.screens.dashboard;

import dev.echo.client.ui.UiRect;

/** Exact original configuration-card button geometry on the dashboard canvas. */
public final class ConfigCardLayout {
    private ConfigCardLayout() {
    }

    /** Reserves the secondary width and its eight-unit gap only when an action exists. */
    public static UiRect primaryButton(float cardX, float cardY, ConfigPrimaryAction action) {
        float width = action == null ? 461.0f : 461.0f - action.secondaryWidth - 8.0f;
        return new UiRect(cardX + 16.0f, cardY + 97.0f, width, 31.0f);
    }

    /** Aligns the original secondary action against the card's right inset. */
    public static UiRect secondaryButton(float cardX, float cardY, ConfigPrimaryAction action) {
        return new UiRect(cardX + 477.0f - action.secondaryWidth, cardY + 97.0f,
                action.secondaryWidth, 31.0f);
    }
}
