package dev.echo.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.echo.client.features.impl.render.ChinaHatFeature;
import dev.echo.client.features.impl.render.FreecamFeature;
import dev.echo.render.MatrixStackTransforms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.joml.Matrix4f;

/**
 * Player-model layer invoking the actual China Hat feature geometry.
 * Original key/caller/cache conditions remain in the source receipt. The compiler
 * supplies the original erased-state render bridge through this typed override.
 */
public class PlayerOverlayFeature extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final float MAX_MODEL_SCALE_SQUARED = 16.0f;
    private static final float HEAD_ROTATION_DEGREES = 180.0f;
    private static final float HEAD_OFFSET = 0.42f;

    public PlayerOverlayFeature(RenderLayerParent<AvatarRenderState, PlayerModel> context) {
        super(context);
    }

    @Override
    public void submit(PoseStack matrices, SubmitNodeCollector commands, int light,
                       AvatarRenderState state, float limbAngle, float limbDistance) {
        boolean bypass = FreecamFeature.isPerspectiveRefreshBypassed();
        ChinaHatFeature feature = ChinaHatFeature.instance;
        if (!bypass && feature == null) {
            return;
        }
        if (!feature.isEnabled()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || state.id != client.player.getId()) {
            return;
        }

        Matrix4f matrix = matrices.last().pose();
        float scaleSquared = matrix.m00() * matrix.m00()
                + matrix.m01() * matrix.m01() + matrix.m02() * matrix.m02();
        if (!bypass) {
            // Preserve the original > comparison, including its NaN behavior.
            if (scaleSquared > MAX_MODEL_SCALE_SQUARED) {
                return;
            }
            matrices.pushPose();
        }

        PlayerModel model = getParentModel();
        model.root().translateAndRotate(matrices);
        model.head.translateAndRotate(matrices);
        MatrixStackTransforms.rotate(matrices, Axis.ZP.rotationDegrees(HEAD_ROTATION_DEGREES));
        matrices.translate(0.0f, HEAD_OFFSET, 0.0f);
        feature.render(matrices.last().pose());
        // The original alternate-control path skips push/scale but retains this pop.
        // Original failures before this point do not execute a synthetic finally-pop.
        matrices.popPose();
    }
}
