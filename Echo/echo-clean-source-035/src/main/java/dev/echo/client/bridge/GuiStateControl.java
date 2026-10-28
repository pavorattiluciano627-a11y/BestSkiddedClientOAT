package dev.echo.client.bridge;

/** Tracks the root-layer boundary at which a screen starts rendering. */
public interface GuiStateControl {
    void markScreenLayerStart();

    /** Installs the screen boundary as the blur layer if no blur layer was selected. */
    boolean prepareScreenBlurLayer();

    boolean isScreenBlurLayerInstalled();
}
