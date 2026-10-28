package dev.echo.client.feature.utility;

/** Inventory, throw and restoration phases of an instant pearl catch. */
public enum PearlCatchInstantPhase {
    IDLE,
    SELECT_WIND_CHARGE,
    SWAP_OFFHAND,
    SELECT_PEARL,
    THROW,
    THROW_FOLLOW_UP,
    RESTORE_SELECT_WIND_CHARGE,
    RESTORE_SWAP_OFFHAND,
    RESTORE_SLOT
}
