package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.events.interaction.BlockInteractEvent;
import dev.echo.client.events.interaction.EntityAttackEvent;
import dev.echo.client.events.interaction.HandInteractEvent;
import dev.echo.client.events.movement.RotationContext;
import dev.echo.client.events.movement.RotationEvent;
import dev.echo.client.features.impl.internals.PreventFeature;
import dev.echo.client.features.impl.world.NoBreakDelayFeature;
import dev.echo.client.utils.inventory.InventoryActionScheduler;
import dev.echo.client.utils.inventory.SlotAction;
import dev.echo.client.utils.inventory.SlotPackets;
import dev.echo.client.utils.targeting.TargetSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Inject(method = {"handleInventoryMouseClick", "handleInventoryMouseClick"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void protectOffhandTotem(int syncId, int slotId, int button, ClickAction action,
            Player player, CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || Echo.featureManager == null) {
            return;
        }
        PreventFeature prevent = (PreventFeature) Echo.featureManager.findFeature(PreventFeature.class);
        if (prevent != null && prevent.isEnabled() && PreventFeature.untotem.getValue()
                && PreventFeature.inInventory.getValue() && !InventoryActionScheduler.isTransferring()) {
            boolean hasTotem = client.player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING;
            SlotAction slotAction = SlotPackets.fromEnum(action);
            if (slotId == 45 && hasTotem) {
                callback.cancel();
            } else if (slotAction == SlotAction.SWAP && button == 40 && hasTotem) {
                callback.cancel();
            }
        }
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void beforeItemInteraction(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        if (Echo.featureManager != null && !Echo.eventsDisabled) {
            HandInteractEvent.Pre event = new HandInteractEvent.Pre(player, hand);
            event.post();
            if (event.cancelled) {
                callback.setReturnValue(InteractionResult.PASS);
            }
        }
    }

    @ModifyArg(method = "useItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;startPrediction(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/multiplayer/prediction/PredictiveAction;)V"), index = 1)
    private PredictiveAction rotateItemInteractionPacket(PredictiveAction original) {
        return sequence -> {
            var packet = original.predict(sequence);
            if (packet instanceof ServerboundUseItemPacket interaction) {
                RotationEvent rotation = RotationEvent.dispatch(RotationContext.INTERACT,
                        interaction.getYRot(), interaction.getXRot());
                return new ServerboundUseItemPacket(interaction.getHand(), sequence,
                        rotation.getYaw(), rotation.getPitch());
            }
            return packet;
        };
    }

    @Inject(method = "performUseItemOn", at = @At("HEAD"), cancellable = true)
    private void beforeBlockInteraction(LocalPlayer player, InteractionHand hand, BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> callback) {
        if (Echo.featureManager != null && !Echo.eventsDisabled) {
            BlockInteractEvent.Pre event = new BlockInteractEvent.Pre(player, hand, hit);
            event.post();
            if (event.cancelled) {
                callback.setReturnValue(InteractionResult.PASS);
            }
        }
    }

    @Inject(method = "performUseItemOn", at = @At("RETURN"))
    private void afterBlockInteraction(LocalPlayer player, InteractionHand hand, BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> callback) {
        if (Echo.featureManager != null && !Echo.eventsDisabled) {
            new BlockInteractEvent.Post(player, hand, hit).post();
        }
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void beforeEntityAttack(Player player, Entity target, CallbackInfo callback) {
        if (!Echo.eventsDisabled && Echo.featureManager != null) {
            EntityAttackEvent.Pre event = new EntityAttackEvent.Pre(player, target);
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }

    @Inject(method = "attack", at = @At("TAIL"))
    private void afterEntityAttack(Player player, Entity target, CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        TargetSelection.recordAttack(target);
        if (Echo.featureManager != null) {
            new EntityAttackEvent.Post(player, target).post();
        }
    }

    @ModifyConstant(method = "continueDestroyBlock", constant = @Constant(intValue = 5))
    public int removeBlockBreakingDelay(int original) {
        if (Echo.eventsDisabled) {
            return original;
        }
        if (Echo.featureManager == null) {
            return original;
        }
        return ((NoBreakDelayFeature) Echo.featureManager.findFeature(NoBreakDelayFeature.class)).isEnabled() ? 0 : original;
    }
}
