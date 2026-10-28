package dev.echo.render;

import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;

/** Quaternion transforms applied to the current Minecraft matrix-stack entry. */
public final class MatrixStackTransforms {
    private static final String UTILITY_CLASS_MESSAGE =
            "This is a utility class and cannot be instantiated";

    private MatrixStackTransforms() {
        throw new UnsupportedOperationException(UTILITY_CLASS_MESSAGE);
    }

    /** Multiplies the current entry by the supplied rotation. */
    public static void rotate(MatrixStack matrices, Quaternionf rotation) {
        matrices.multiply(rotation);
    }
}
