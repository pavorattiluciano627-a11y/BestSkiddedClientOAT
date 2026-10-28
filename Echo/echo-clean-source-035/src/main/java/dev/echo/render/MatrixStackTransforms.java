package dev.echo.render;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;

/** Quaternion transforms applied to the current Minecraft matrix-stack entry. */
public final class MatrixStackTransforms {
    private static final String UTILITY_CLASS_MESSAGE =
            "This is a utility class and cannot be instantiated";

    private MatrixStackTransforms() {
        throw new UnsupportedOperationException(UTILITY_CLASS_MESSAGE);
    }

    /** Multiplies the current entry by the supplied rotation. */
    public static void rotate(PoseStack matrices, Quaternionf rotation) {
        matrices.mulPose(rotation);
    }
}
