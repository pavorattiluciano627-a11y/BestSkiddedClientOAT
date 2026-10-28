package dev.echo.client.utils.inventory;

/** Analysis-only enum candidate; declaration order is preserved from the artifact. */
public enum InventoryActionState {
    IDLE,
    WAIT_BEFORE_OPEN,
    WAIT_AFTER_OPEN,
    WAIT_BEFORE_CLOSE
}
