package dev.echo.render.opengl;

/** Host framebuffer operations, routed through the host's GL state tracking. */
public interface GlFramebufferAccess {
    /** Returns the host framebuffer's depth texture format as a GL internal format. */
    int depthInternalFormat();

    void bindFramebuffer(int target, int framebuffer);

    void framebufferTexture2D(int target, int attachment, int textureTarget,
                              int texture, int mipLevel);

    void deleteFramebuffer(int framebuffer);
}
