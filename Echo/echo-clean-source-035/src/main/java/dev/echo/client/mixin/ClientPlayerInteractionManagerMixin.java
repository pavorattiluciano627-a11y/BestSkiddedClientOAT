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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Inject(method = {"clickSlot", "clickSlot"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void protectOffhandTotem(int syncId, int slotId, int button, SlotActionType action,
            PlayerEntity player, CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || Echo.featureManager == null) {
            return;
        }
        PreventFeature prevent = (PreventFeature) Echo.featureManager.findFeature(PreventFeature.class);
        if (prevent != null && prevent.isEnabled() && PreventFeature.untotem.getValue()
                && PreventFeature.inInventory.getValue() && !InventoryActionScheduler.isTransferring()) {
            boolean hasTotem = client.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING;
            SlotAction slotAction = SlotPackets.fromEnum(action);
            if (slotId == 45 && hasTotem) {
                callback.cancel();
            } else if (slotAction == SlotAction.SWAP && button == 40 && hasTotem) {
                callback.cancel();
            }
        }
    }

    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void beforeItemInteraction(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> callback) {
        if (Echo.featureManager != null && !Echo.eventsDisabled) {
            HandInteractEvent.Pre event = new HandInteractEvent.Pre(player, hand);
            event.post();
            if (event.cancelled) {
                callback.setReturnValue(ActionResult.PASS);
            }
        }
    }

    @ModifyArg(method = "interactItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;sendSequencedPacket(Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/client/network/SequencedPacketCreator;)V"), index = 1)
    private SequencedPacketCreator rotateItemInteractionPacket(SequencedPacketCreator original) {
        return sequence -> {
            var packet = original.predict(sequence);
            if (packet instanceof PlayerInteractItemC2SPacket interaction) {
                RotationEvent rotation = RotationEvent.dispatch(RotationContext.INTERACT,
                        interaction.getYaw(), interaction.getPitch());
                return new PlayerInteractItemC2SPacket(interaction.getHand(), sequence,
                        rotation.getYaw(), rotation.getPitch());
            }
            return packet;
        };
    }

    @Inject(method = "interactBlockInternal", at = @At("HEAD"), cancellable = true)
    private void beforeBlockInteraction(ClientPlayerEntity player, Hand hand, BlockHitResult hit,
            CallbackInfoReturnable<ActionResult> callback) {
        if (Echo.featureManager != null && !Echo.eventsDisabled) {
            BlockInteractEvent.Pre event = new BlockInteractEvent.Pre(player, hand, hit);
            event.post();
            if (event.cancelled) {
                callback.setReturnValue(ActionResult.PASS);
            }
        }
    }

    @Inject(method = "interactBlockInternal", at = @At("RETURN"))
    private void afterBlockInteraction(ClientPlayerEntity player, Hand hand, BlockHitResult hit,
            CallbackInfoReturnable<ActionResult> callback) {
        if (Echo.featureManager != null && !Echo.eventsDisabled) {
            new BlockInteractEvent.Post(player, hand, hit).post();
        }
    }

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void beforeEntityAttack(PlayerEntity player, Entity target, CallbackInfo callback) {
        if (!Echo.eventsDisabled && Echo.featureManager != null) {
            EntityAttackEvent.Pre event = new EntityAttackEvent.Pre(player, target);
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }

    @Inject(method = "attackEntity", at = @At("TAIL"))
    private void afterEntityAttack(PlayerEntity player, Entity target, CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        TargetSelection.recordAttack(target);
        if (Echo.featureManager != null) {
            new EntityAttackEvent.Post(player, target).post();
        }
    }

    @ModifyConstant(method = "updateBlockBreakingProgress", constant = @Constant(intValue = 5))
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
