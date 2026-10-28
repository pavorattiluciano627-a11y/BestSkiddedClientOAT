package dev.echo.client.render;

import dev.echo.client.features.impl.render.ChinaHatFeature;
import dev.echo.client.features.impl.render.FreecamFeature;
import dev.echo.render.MatrixStackTransforms;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

/**
 * Player-model layer invoking the actual China Hat feature geometry.
 * Original key/caller/cache conditions remain in the source receipt. The compiler
 * supplies the original erased-state render bridge through this typed override.
 */
public class PlayerOverlayFeature extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    private static final float MAX_MODEL_SCALE_SQUARED = 16.0f;
    private static final float HEAD_ROTATION_DEGREES = 180.0f;
    private static final float HEAD_OFFSET = 0.42f;

    public PlayerOverlayFeature(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue commands, int light,
                       PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        boolean bypass = FreecamFeature.isPerspectiveRefreshBypassed();
        ChinaHatFeature feature = ChinaHatFeature.instance;
        if (!bypass && feature == null) {
            return;
        }
        if (!feature.isEnabled()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || state.id != client.player.getId()) {
            return;
        }

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float scaleSquared = matrix.m00() * matrix.m00()
                + matrix.m01() * matrix.m01() + matrix.m02() * matrix.m02();
        if (!bypass) {
            // Preserve the original > comparison, including its NaN behavior.
            if (scaleSquared > MAX_MODEL_SCALE_SQUARED) {
                return;
            }
            matrices.push();
        }

        PlayerEntityModel model = getContextModel();
        model.getRootPart().applyTransform(matrices);
        model.head.applyTransform(matrices);
        MatrixStackTransforms.rotate(matrices, RotationAxis.POSITIVE_Z.rotationDegrees(HEAD_ROTATION_DEGREES));
        matrices.translate(0.0f, HEAD_OFFSET, 0.0f);
        feature.render(matrices.peek().getPositionMatrix());
        // The original alternate-control path skips push/scale but retains this pop.
        // Original failures before this point do not execute a synthetic finally-pop.
        matrices.pop();
    }
}
