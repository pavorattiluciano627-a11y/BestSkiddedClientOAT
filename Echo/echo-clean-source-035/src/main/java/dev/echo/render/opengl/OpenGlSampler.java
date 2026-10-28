package dev.echo.render.opengl;

import dev.echo.render.sampler.Filter;
import dev.echo.render.sampler.MipmapMode;
import dev.echo.render.sampler.Sampler;
import dev.echo.render.sampler.WrapMode;
import static org.lwjgl.opengl.GL33C.*;

/** An owning OpenGL sampler. Release through the creating device. */
public class OpenGlSampler extends Sampler {
    public final int samplerId;

    public OpenGlSampler(Filter filter, MipmapMode mipMode, WrapMode wrap) {
        super(filter, mipMode, wrap);
        samplerId = glGenSamplers();
        int magnification = switch (filter) {
            case NEAREST -> GL_NEAREST;
            case LINEAR -> GL_LINEAR;
        };
        int minification = switch (mipMode) {
            case NONE -> magnification;
            case NEAREST -> filter == Filter.NEAREST ? GL_NEAREST_MIPMAP_NEAREST : GL_LINEAR_MIPMAP_NEAREST;
            case LINEAR -> filter == Filter.NEAREST ? GL_NEAREST_MIPMAP_LINEAR : GL_LINEAR_MIPMAP_LINEAR;
        };
        int addressing = switch (wrap) {
            case REPEAT -> GL_REPEAT;
            case CLAMP_TO_EDGE -> GL_CLAMP_TO_EDGE;
        };
        glSamplerParameteri(samplerId, GL_TEXTURE_MAG_FILTER, magnification);
        glSamplerParameteri(samplerId, GL_TEXTURE_MIN_FILTER, minification);
        glSamplerParameteri(samplerId, GL_TEXTURE_WRAP_S, addressing);
        glSamplerParameteri(samplerId, GL_TEXTURE_WRAP_T, addressing);
        glSamplerParameteri(samplerId, GL_TEXTURE_WRAP_R, addressing);
    }

    @Override
    public void destroy() {
        glDeleteSamplers(samplerId);
    }
}
