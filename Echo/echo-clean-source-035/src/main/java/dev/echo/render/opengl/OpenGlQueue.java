package dev.echo.render.opengl;

import dev.echo.render.command.CommandEncoder;
import dev.echo.render.queue.CommandQueue;
import static org.lwjgl.opengl.GL11C.glFinish;

/** OpenGL executes encoder calls immediately on the current context. */
public class OpenGlQueue extends CommandQueue {
    private final OpenGlDevice device;
    private final int capabilities;

    OpenGlQueue(OpenGlDevice device, int capabilities) {
        this.device = device;
        this.capabilities = capabilities;
    }

    @Override public int capabilities() { return capabilities; }
    @Override public CommandEncoder beginCommands() { return device.commandEncoder(); }

    @Override
    public void submit(CommandEncoder commands) {
        // Original immediate-mode queue has no deferred command submission (one-byte return body).
    }

    @Override public void waitIdle() { glFinish(); }

    @Override
    public void destroy() {
        // This adapter owns neither the host context nor an OpenGL queue object.
    }

    static int supportedCapabilities(OpenGlDevice device) {
        return 13 | (device.capabilities().computeShaders() ? 2 : 0);
    }
}
