package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.events.EventBus;
import dev.echo.client.events.input.HandleInputEvent;
import dev.echo.client.events.input.InputPacketEvent;
import dev.echo.client.events.input.UsingItemQueryEvent;
import dev.echo.client.events.interaction.AttackAttemptEvent;
import dev.echo.client.events.interaction.BlockBreakingEvent;
import dev.echo.client.events.interaction.ItemUseEvent;
import dev.echo.client.events.lifecycle.ClientTickEvent;
import dev.echo.client.events.lifecycle.FrameStartEvent;
import dev.echo.client.events.lifecycle.RenderTickEvent;
import dev.echo.client.events.lifecycle.ScreenChangeEvent;
import dev.echo.client.events.lifecycle.WorldTickContextEvent;
import dev.echo.client.events.render.EntityOutlineEvent;
import dev.echo.client.input.KeybindingEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {
    @Shadow @Nullable public ClientLevel level;
    @Shadow public HitResult hitResult;
    @Shadow public LocalPlayer player;
    @Shadow @Final public Options options;
    @Shadow @Nullable public MultiPlayerGameMode gameMode;
    @Unique private ClientLevel echo$lastNotifiedWorld;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void beforeClientTick(CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        // This second volatile read is also present in the original tick hook.
        if (!Echo.eventsDisabled && level != null && player != null && level != echo$lastNotifiedWorld) {
            echo$lastNotifiedWorld = level;
            new WorldTickContextEvent().post();
        }
        ClientTickEvent event = new ClientTickEvent();
        event.post();
        if (event.cancelled) {
            callback.cancel();
        }
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    private void beforeFrame(boolean tick, CallbackInfo callback) {
        if (Minecraft.getInstance().level != null) {
            EventBus.post(new FrameStartEvent());
        }
    }

    @Inject(method = "runTick", at = @At("TAIL"))
    private void presentRenderBackend(boolean tick, CallbackInfo callback) {
        if (Echo.externalRenderer != null) {
            Echo.externalRenderer.endFrame();
        }
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void closeRenderBackend(CallbackInfo callback) {
        if (Echo.externalRenderer != null) {
            Echo.externalRenderer.close();
            Echo.externalRenderer = null;
        }
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;runAllTasks()V", shift = At.Shift.BEFORE))
    private void beforeRenderTasks(boolean tick, CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        new RenderTickEvent().post();
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void beforeAttackAttempt(CallbackInfoReturnable<Boolean> callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        if (player != null) {
            Entity target = hitResult != null && hitResult.getType() == HitResult.Type.ENTITY
                    ? ((EntityHitResult) hitResult).getEntity() : null;
            AttackAttemptEvent event = new AttackAttemptEvent(player, target);
            event.post();
            if (event.cancelled) {
                callback.setReturnValue(false);
                callback.cancel();
            }
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    public void beforeBlockBreaking(CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        BlockBreakingEvent.Pre event = new BlockBreakingEvent.Pre();
        event.post();
        if (event.cancelled) {
            callback.cancel();
        }
    }

    @Redirect(method = "handleKeybinds", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z", ordinal = 0))
    private boolean queryUsingItem(LocalPlayer queriedPlayer) {
        boolean usingItem = queriedPlayer.isUsingItem();
        if (Echo.eventsDisabled) {
            return usingItem;
        }
        UsingItemQueryEvent event = new UsingItemQueryEvent(usingItem);
        event.post();
        if (usingItem && !event.isUsingItem() && !options.keyUse.isDown()) {
            gameMode.releaseUsingItem(queriedPlayer);
        }
        return event.isUsingItem();
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void beforeItemUse(CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        if (player != null) {
            ItemUseEvent.Pre event = new ItemUseEvent.Pre(player, InteractionHand.MAIN_HAND, player.getMainHandItem(), hitResult);
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }

    @Inject(method = "startUseItem", at = @At("RETURN"))
    private void afterItemUse(CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        if (player != null) {
            new ItemUseEvent.Post(player, InteractionHand.MAIN_HAND, player.getMainHandItem(), hitResult).post();
        }
    }

    @Inject(method = "destroy", at = @At("HEAD"))
    private void autosaveFeatureConfig(CallbackInfo callback) {
        if (!Echo.eventsDisabled && Echo.featureConfig != null) {
            Echo.featureConfig.save("_autosave");
        }
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void beforeScreenChange(Screen screen, CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        ScreenChangeEvent event = new ScreenChangeEvent(screen);
        event.post();
        if (event.cancelled) {
            callback.cancel();
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;handleKeybinds()V", shift = At.Shift.BEFORE))
    private void beforeInputEvents(CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        new HandleInputEvent.Pre().post();
    }

    @Inject(method = "handleKeybinds", at = @At("HEAD"), cancellable = true)
    private void resetKeybindingEvents(CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        KeybindingEvents.beginInputCycle();
    }

    @Inject(method = "handleKeybinds", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z", ordinal = 2))
    private void beforeHotbarKey(CallbackInfo callback) {
        KeybindingEvents.onHotbarSlot();
    }

    @Inject(method = "handleKeybinds", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z", ordinal = 7))
    private void beforeSwapHandsKey(CallbackInfo callback) {
        KeybindingEvents.onSwapHands();
    }

    @Inject(method = "handleKeybinds", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z", ordinal = 8))
    private void beforeDropKey(CallbackInfo callback) {
        KeybindingEvents.onDrop();
    }

    @Inject(method = "handleKeybinds", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z", ordinal = 14))
    private void beforeAttackKey(CallbackInfo callback) {
        KeybindingEvents.onAttack();
    }

    @Inject(method = "handleKeybinds", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z", ordinal = 15))
    private void beforeUseKey(CallbackInfo callback) {
        KeybindingEvents.onUse();
    }

    @Inject(method = "handleKeybinds", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z", ordinal = 16))
    private void beforePickBlockKey(CallbackInfo callback) {
        KeybindingEvents.onPickBlock();
    }

    @Inject(method = "handleKeybinds", at = @At("RETURN"))
    private void afterInputEvents(CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        new HandleInputEvent.Post().post();
    }

    @Redirect(method = "handleKeybinds", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;sendPacket(Lnet/minecraft/network/protocol/Packet;)V"))
    private void sendInputPacket(ClientPacketListener network, Packet<?> packet) {
        if (Echo.eventsDisabled) {
            network.send(packet);
            return;
        }
        if (packet instanceof ServerboundPlayerActionPacket action
                && action.getAction() == ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
            InputPacketEvent event = new InputPacketEvent();
            event.post();
            if (event.cancelled) {
                return;
            }
        }
        network.send(packet);
    }

    @Inject(method = "setLevel", at = @At("TAIL"))
    public void afterWorldJoin(@Nullable ClientLevel joinedWorld, CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        if (joinedWorld != null && player != null && joinedWorld != echo$lastNotifiedWorld) {
            echo$lastNotifiedWorld = joinedWorld;
            new WorldTickContextEvent().post();
        } else if (joinedWorld == null) {
            echo$lastNotifiedWorld = null;
        }
    }

    @Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true)
    private void queryEntityOutline(Entity entity, CallbackInfoReturnable<Boolean> callback) {
        EntityOutlineEvent event = new EntityOutlineEvent(entity, callback.getReturnValueZ());
        event.post();
        if (event.isOutlined() != callback.getReturnValueZ()) {
            callback.setReturnValue(event.isOutlined());
        }
    }
}
