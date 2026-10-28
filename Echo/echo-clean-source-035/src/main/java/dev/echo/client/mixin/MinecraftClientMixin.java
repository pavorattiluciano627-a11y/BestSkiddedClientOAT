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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
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

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Shadow @Nullable public ClientWorld world;
    @Shadow public HitResult crosshairTarget;
    @Shadow public ClientPlayerEntity player;
    @Shadow @Final public GameOptions options;
    @Shadow @Nullable public ClientPlayerInteractionManager interactionManager;
    @Unique private ClientWorld echo$lastNotifiedWorld;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void beforeClientTick(CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        // This second volatile read is also present in the original tick hook.
        if (!Echo.eventsDisabled && world != null && player != null && world != echo$lastNotifiedWorld) {
            echo$lastNotifiedWorld = world;
            new WorldTickContextEvent().post();
        }
        ClientTickEvent event = new ClientTickEvent();
        event.post();
        if (event.cancelled) {
            callback.cancel();
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void beforeFrame(boolean tick, CallbackInfo callback) {
        if (MinecraftClient.getInstance().world != null) {
            EventBus.post(new FrameStartEvent());
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
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

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;runTasks()V", shift = At.Shift.BEFORE))
    private void beforeRenderTasks(boolean tick, CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        new RenderTickEvent().post();
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void beforeAttackAttempt(CallbackInfoReturnable<Boolean> callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        if (player != null) {
            Entity target = crosshairTarget != null && crosshairTarget.getType() == HitResult.Type.ENTITY
                    ? ((EntityHitResult) crosshairTarget).getEntity() : null;
            AttackAttemptEvent event = new AttackAttemptEvent(player, target);
            event.post();
            if (event.cancelled) {
                callback.setReturnValue(false);
                callback.cancel();
            }
        }
    }

    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
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

    @Redirect(method = "handleInputEvents", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z", ordinal = 0))
    private boolean queryUsingItem(ClientPlayerEntity queriedPlayer) {
        boolean usingItem = queriedPlayer.isUsingItem();
        if (Echo.eventsDisabled) {
            return usingItem;
        }
        UsingItemQueryEvent event = new UsingItemQueryEvent(usingItem);
        event.post();
        if (usingItem && !event.isUsingItem() && !options.useKey.isPressed()) {
            interactionManager.stopUsingItem(queriedPlayer);
        }
        return event.isUsingItem();
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void beforeItemUse(CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        if (player != null) {
            ItemUseEvent.Pre event = new ItemUseEvent.Pre(player, Hand.MAIN_HAND, player.getMainHandStack(), crosshairTarget);
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }

    @Inject(method = "doItemUse", at = @At("RETURN"))
    private void afterItemUse(CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        if (player != null) {
            new ItemUseEvent.Post(player, Hand.MAIN_HAND, player.getMainHandStack(), crosshairTarget).post();
        }
    }

    @Inject(method = "stop", at = @At("HEAD"))
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
            target = "Lnet/minecraft/client/MinecraftClient;handleInputEvents()V", shift = At.Shift.BEFORE))
    private void beforeInputEvents(CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        new HandleInputEvent.Pre().post();
    }

    @Inject(method = "handleInputEvents", at = @At("HEAD"), cancellable = true)
    private void resetKeybindingEvents(CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        KeybindingEvents.beginInputCycle();
    }

    @Inject(method = "handleInputEvents", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/option/KeyBinding;wasPressed()Z", ordinal = 2))
    private void beforeHotbarKey(CallbackInfo callback) {
        KeybindingEvents.onHotbarSlot();
    }

    @Inject(method = "handleInputEvents", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/option/KeyBinding;wasPressed()Z", ordinal = 7))
    private void beforeSwapHandsKey(CallbackInfo callback) {
        KeybindingEvents.onSwapHands();
    }

    @Inject(method = "handleInputEvents", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/option/KeyBinding;wasPressed()Z", ordinal = 8))
    private void beforeDropKey(CallbackInfo callback) {
        KeybindingEvents.onDrop();
    }

    @Inject(method = "handleInputEvents", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/option/KeyBinding;wasPressed()Z", ordinal = 14))
    private void beforeAttackKey(CallbackInfo callback) {
        KeybindingEvents.onAttack();
    }

    @Inject(method = "handleInputEvents", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/option/KeyBinding;wasPressed()Z", ordinal = 15))
    private void beforeUseKey(CallbackInfo callback) {
        KeybindingEvents.onUse();
    }

    @Inject(method = "handleInputEvents", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/option/KeyBinding;wasPressed()Z", ordinal = 16))
    private void beforePickBlockKey(CallbackInfo callback) {
        KeybindingEvents.onPickBlock();
    }

    @Inject(method = "handleInputEvents", at = @At("RETURN"))
    private void afterInputEvents(CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        new HandleInputEvent.Post().post();
    }

    @Redirect(method = "handleInputEvents", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V"))
    private void sendInputPacket(ClientPlayNetworkHandler network, Packet<?> packet) {
        if (Echo.eventsDisabled) {
            network.sendPacket(packet);
            return;
        }
        if (packet instanceof PlayerActionC2SPacket action
                && action.getAction() == PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
            InputPacketEvent event = new InputPacketEvent();
            event.post();
            if (event.cancelled) {
                return;
            }
        }
        network.sendPacket(packet);
    }

    @Inject(method = "joinWorld", at = @At("TAIL"))
    public void afterWorldJoin(@Nullable ClientWorld joinedWorld, CallbackInfo callback) {
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

    @Inject(method = "hasOutline", at = @At("RETURN"), cancellable = true)
    private void queryEntityOutline(Entity entity, CallbackInfoReturnable<Boolean> callback) {
        EntityOutlineEvent event = new EntityOutlineEvent(entity, callback.getReturnValueZ());
        event.post();
        if (event.isOutlined() != callback.getReturnValueZ()) {
            callback.setReturnValue(event.isOutlined());
        }
    }
}
