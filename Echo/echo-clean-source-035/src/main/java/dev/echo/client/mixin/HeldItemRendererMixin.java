package dev.echo.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.echo.client.Echo;
import dev.echo.client.features.impl.render.ViewModelFeature;
import dev.echo.render.HandChamsRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class HeldItemRendererMixin {
    @Inject(method = "renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V",
            at = @At("HEAD"))
    private void beginHandChams(float tickProgress, PoseStack matrices, SubmitNodeCollector queue,
            LocalPlayer player, int light, CallbackInfo callback) {
        HandChamsRenderer.beginCapture();
    }

    @Inject(method = "renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V",
            at = @At("RETURN"))
    private void finishHandChams(float tickProgress, PoseStack matrices, SubmitNodeCollector queue,
            LocalPlayer player, int light, CallbackInfo callback) {
        HandChamsRenderer.endCapture();
        HandChamsRenderer.flushCapturedOutlines();
    }

    @Inject(method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER))
    private void applyViewModelTransform(AbstractClientPlayer player, float tickProgress, float pitch,
            InteractionHand hand, float swingProgress, ItemStack stack, float equipProgress, PoseStack matrices,
            SubmitNodeCollector queue, int light, CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        if (Echo.featureManager == null) {
            return;
        }
        ViewModelFeature viewModel = (ViewModelFeature) Echo.featureManager.findFeature(ViewModelFeature.class);
        if (viewModel == null || !viewModel.isEnabled()) {
            return;
        }
        float x = hand == InteractionHand.MAIN_HAND ? ViewModelFeature.posXMain.getValue() : ViewModelFeature.posXOff.getValue();
        float y = hand == InteractionHand.MAIN_HAND ? ViewModelFeature.posYMain.getValue() : ViewModelFeature.posYOff.getValue();
        float z = hand == InteractionHand.MAIN_HAND ? ViewModelFeature.posZMain.getValue() : ViewModelFeature.posZOff.getValue();
        float rotationX = hand == InteractionHand.MAIN_HAND ? ViewModelFeature.rotXMain.getValue() : ViewModelFeature.rotXOff.getValue();
        float rotationY = hand == InteractionHand.MAIN_HAND ? ViewModelFeature.rotYMain.getValue() : ViewModelFeature.rotYOff.getValue();
        float rotationZ = hand == InteractionHand.MAIN_HAND ? ViewModelFeature.rotZMain.getValue() : ViewModelFeature.rotZOff.getValue();
        float scale = hand == InteractionHand.MAIN_HAND ? ViewModelFeature.scaleMain.getValue() : ViewModelFeature.scaleOff.getValue();
        matrices.translate(x, y, z);
        matrices.mulPose(Axis.XP.rotationDegrees(rotationX));
        matrices.mulPose(Axis.YP.rotationDegrees(rotationY));
        matrices.mulPose(Axis.ZP.rotationDegrees(rotationZ));
        matrices.scale(scale, scale, scale);
    }

    @Inject(method = "swingArm(FLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V",
            at = @At("HEAD"), cancellable = true)
    private void replaceSwingAnimation(float swingProgress, PoseStack matrices, int armSign,
            HumanoidArm arm, CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        if (Echo.featureManager != null) {
            ViewModelFeature viewModel = (ViewModelFeature) Echo.featureManager.findFeature(ViewModelFeature.class);
            if (viewModel != null && viewModel.isEnabled()) {
                callback.cancel();
                viewModel.getSelectedAnimation().apply(swingProgress, matrices, armSign, arm);
            }
        }
    }

    @Inject(method = "applyEatTransform(Lcom/mojang/blaze3d/vertex/PoseStack;FLnet/minecraft/world/entity/HumanoidArm;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)V",
            at = @At("HEAD"), cancellable = true)
    private void modifyEatingAnimation(PoseStack matrices, float tickProgress, HumanoidArm arm, ItemStack stack,
            Player player, CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        if (Echo.featureManager == null) {
            return;
        }
        ViewModelFeature viewModel = (ViewModelFeature) Echo.featureManager.findFeature(ViewModelFeature.class);
        if (viewModel == null || !viewModel.isEnabled()) {
            return;
        }
        if (!ViewModelFeature.eatingAnimation.getValue()) {
            callback.cancel();
            return;
        }
        float strength = Mth.clamp(ViewModelFeature.eatingBob.getValue(), 0.0F, 2.0F);
        if (!Mth.equal(strength, 1.0F)) {
            callback.cancel();
            float remainingTicks = player.getUseItemRemainingTicks() - tickProgress + 1.0F;
            float useFraction = remainingTicks / stack.getUseDuration(player);
            if (useFraction < 0.8F) {
                float bob = Mth.abs(Mth.cos(remainingTicks / 4.0F * (float) Math.PI) * 0.1F) * strength;
                matrices.translate(0.0F, bob, 0.0F);
            }
            float progress = (1.0F - (float) Math.pow(useFraction, 27.0)) * strength;
            int armSign = arm == HumanoidArm.RIGHT ? 1 : -1;
            matrices.translate(progress * 0.6F * armSign, progress * -0.5F, 0.0F);
            matrices.mulPose(Axis.YP.rotationDegrees(armSign * progress * 90.0F));
            matrices.mulPose(Axis.XP.rotationDegrees(progress * 10.0F));
            matrices.mulPose(Axis.ZP.rotationDegrees(armSign * progress * 30.0F));
        }
    }
}
