package dev.echo.client.bridge;

/** Marks the main GUI pass so auxiliary GUI draws do not re-emit HUD events. */
public interface GuiRenderControl {
    void setMainGuiPass(boolean mainGuiPass);
}
