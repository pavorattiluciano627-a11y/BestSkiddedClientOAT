package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.features.impl.render.ViewModelFeature;
import dev.echo.render.HandChamsRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
            at = @At("HEAD"))
    private void beginHandChams(float tickProgress, MatrixStack matrices, OrderedRenderCommandQueue queue,
            ClientPlayerEntity player, int light, CallbackInfo callback) {
        HandChamsRenderer.beginCapture();
    }

    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
            at = @At("RETURN"))
    private void finishHandChams(float tickProgress, MatrixStack matrices, OrderedRenderCommandQueue queue,
            ClientPlayerEntity player, int light, CallbackInfo callback) {
        HandChamsRenderer.endCapture();
        HandChamsRenderer.flushCapturedOutlines();
    }

    @Inject(method = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;push()V", shift = At.Shift.AFTER))
    private void applyViewModelTransform(AbstractClientPlayerEntity player, float tickProgress, float pitch,
            Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices,
            OrderedRenderCommandQueue queue, int light, CallbackInfo callback) {
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
        float x = hand == Hand.MAIN_HAND ? ViewModelFeature.posXMain.getValue() : ViewModelFeature.posXOff.getValue();
        float y = hand == Hand.MAIN_HAND ? ViewModelFeature.posYMain.getValue() : ViewModelFeature.posYOff.getValue();
        float z = hand == Hand.MAIN_HAND ? ViewModelFeature.posZMain.getValue() : ViewModelFeature.posZOff.getValue();
        float rotationX = hand == Hand.MAIN_HAND ? ViewModelFeature.rotXMain.getValue() : ViewModelFeature.rotXOff.getValue();
        float rotationY = hand == Hand.MAIN_HAND ? ViewModelFeature.rotYMain.getValue() : ViewModelFeature.rotYOff.getValue();
        float rotationZ = hand == Hand.MAIN_HAND ? ViewModelFeature.rotZMain.getValue() : ViewModelFeature.rotZOff.getValue();
        float scale = hand == Hand.MAIN_HAND ? ViewModelFeature.scaleMain.getValue() : ViewModelFeature.scaleOff.getValue();
        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotationX));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationY));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotationZ));
        matrices.scale(scale, scale, scale);
    }

    @Inject(method = "swingArm(FLnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/util/Arm;)V",
            at = @At("HEAD"), cancellable = true)
    private void replaceSwingAnimation(float swingProgress, MatrixStack matrices, int armSign,
            Arm arm, CallbackInfo callback) {
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

    @Inject(method = "applyEatOrDrinkTransformation(Lnet/minecraft/client/util/math/MatrixStack;FLnet/minecraft/util/Arm;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/PlayerEntity;)V",
            at = @At("HEAD"), cancellable = true)
    private void modifyEatingAnimation(MatrixStack matrices, float tickProgress, Arm arm, ItemStack stack,
            PlayerEntity player, CallbackInfo callback) {
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
        float strength = MathHelper.clamp(ViewModelFeature.eatingBob.getValue(), 0.0F, 2.0F);
        if (!MathHelper.approximatelyEquals(strength, 1.0F)) {
            callback.cancel();
            float remainingTicks = player.getItemUseTimeLeft() - tickProgress + 1.0F;
            float useFraction = remainingTicks / stack.getMaxUseTime(player);
            if (useFraction < 0.8F) {
                float bob = MathHelper.abs(MathHelper.cos(remainingTicks / 4.0F * (float) Math.PI) * 0.1F) * strength;
                matrices.translate(0.0F, bob, 0.0F);
            }
            float progress = (1.0F - (float) Math.pow(useFraction, 27.0)) * strength;
            int armSign = arm == Arm.RIGHT ? 1 : -1;
            matrices.translate(progress * 0.6F * armSign, progress * -0.5F, 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(armSign * progress * 90.0F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(progress * 10.0F));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(armSign * progress * 30.0F));
        }
    }
}
