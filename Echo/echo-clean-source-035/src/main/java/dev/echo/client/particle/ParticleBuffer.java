package dev.echo.client.particle;

import java.nio.ByteBuffer;

/**
 * Complete decoded-texture record in the fixed field domain. Cache applicability
 * remains open; initialization obligations are preserved by explicit accessors.
 */
public record ParticleBuffer(Class<?> particleType, ByteBuffer pixels, int width, int height) {
    @Override
    public Class<?> particleType() {
        try {
            Class.forName(Class.class.getName(), true, Class.class.getClassLoader());
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Resolved class initialization failed", exception);
        }
        return particleType;
    }

    @Override
    public ByteBuffer pixels() {
        try {
            Class.forName(ByteBuffer.class.getName(), true, ByteBuffer.class.getClassLoader());
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Resolved class initialization failed", exception);
        }
        return pixels;
    }
}
