package dev.echo.client.integration;

/** Native render window synchronized with Minecraft's main window. */
public interface NativeWindowBackend extends AutoCloseable {
    /** Returns the owned GLFW window handle. */
    long windowHandle();

    /** Updates position, size and framebuffer dimensions from the main window. */
    void synchronizeWithMainWindow();

    /** Shows or hides the render window, avoiding redundant visibility transitions. */
    void setVisible(boolean visible);

    /** Releases native integration resources and destroys the owned GLFW window. */
    @Override
    void close();

    int framebufferWidth();

    int framebufferHeight();
}
