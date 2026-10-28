package dev.echo.client.features.impl.movement;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.PlayerInputEvent;
import dev.echo.client.events.lifecycle.ClientTickEvent;
import dev.echo.client.events.network.PacketSendEvent;
import dev.echo.client.events.render.WorldRenderEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.handlers.SprintOverrideHandler;
import dev.echo.client.input.KeybindingCache;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.setting.ChoiceSetting;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import static dev.echo.client.features.FeatureLiterals.literal;

/** Physical menu movement with the original inventory-packet delay sequence. */
public class GUIWalkFeature extends Feature {
    private Queue<Packet<?>> queuedPackets;
    private boolean flushingPackets;
    private boolean packetDelayActive;
    private int packetDelayCounter;
    private KeyMapping[] movementKeys;
    private static ChoiceSetting mode;
    private static final String VANILLA_MODE = "Vanilla";

    public GUIWalkFeature() {
        super(new FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("GUI Walk"), dev.echo.client.bootstrap.CatalogText.literal("Lets you walk inside a menu"), dev.echo.client.features.Category.MOVEMENT));
    }

    @Override
    public void postInitialize() {
        queuedPackets = new ConcurrentLinkedDeque<>();
        flushingPackets = false;
        packetDelayActive = false;
        packetDelayCounter = 0;
        super.postInitialize();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        java.util.Objects.requireNonNull(queuedPackets, "object reference is null").clear();
        packetDelayActive = false;
        packetDelayCounter = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        flushQueuedPackets();
        packetDelayActive = false;
        packetDelayCounter = 0;
        SprintOverrideHandler.setSuppressSprint(false);
    }

    private KeyMapping[] getMovementKeys() {
        if (movementKeys == null) {
            movementKeys = new KeyMapping[]{client.options.keyUp, client.options.keyRight,
                    client.options.keyLeft, client.options.keyJump, client.options.keyDown};
        }
        return movementKeys;
    }

    public static boolean isPhysicallyPressed(KeyMapping key) {
        return KeybindingCache.isPressed(key);
    }

    @Subscribe
    private void onWorldRender(WorldRenderEvent event) {
        if (isOutsideWorld()) return;
        if (!(MinecraftRenderState.getScreen() instanceof ChatScreen)) {
            for (KeyMapping key : getMovementKeys()) {
                key.setDown(isPhysicallyPressed(key));
            }
        }
    }

    @Subscribe
    private void onPacketSend(PacketSendEvent event) {
        if (isOutsideWorld() || flushingPackets || mode.isSelected(VANILLA_MODE)) return;
        Packet<?> packet = event.getPacket();
        if (packet instanceof ServerboundContainerClickPacket || packet instanceof ServerboundContainerClosePacket
                || packet instanceof ServerboundChatPacket || packet instanceof ServerboundChatCommandPacket) {
            queuedPackets.add(packet);
            event.cancel();
            packetDelayActive = true;
            packetDelayCounter = 0;
            SprintOverrideHandler.setSuppressSprint(true);
            return;
        }
        if (packetDelayActive && packet instanceof ServerboundMovePlayerPacket) {
            packetDelayCounter++;
            if (packetDelayCounter >= 1) {
                flushQueuedPackets();
                packetDelayActive = false;
                packetDelayCounter = 0;
                SprintOverrideHandler.setSuppressSprint(false);
            }
        }
    }

    @Subscribe
    private void onClientTick(ClientTickEvent event) {
        if (isOutsideWorld()) return;
        if (!queuedPackets.isEmpty() && !packetDelayActive
                && !(MinecraftRenderState.getScreen() instanceof AbstractContainerScreen<?>)) {
            packetDelayActive = true;
            packetDelayCounter = 0;
            SprintOverrideHandler.setSuppressSprint(true);
        }
    }

    private void flushQueuedPackets() {
        ClientPacketListener handler = client.getConnection();
        if (handler == null) {
            queuedPackets.clear();
            return;
        }
        flushingPackets = true;
        while (!queuedPackets.isEmpty()) {
            Packet<?> packet = queuedPackets.poll();
            if (packet != null) handler.send(packet);
        }
        // Deliberately not a finally: a send failure leaves the original reentrancy flag set.
        flushingPackets = false;
    }

    @Subscribe
    private void onPlayerInput(PlayerInputEvent event) {
        if (isOutsideWorld() || !packetDelayActive) return;
        event.forward = false;
        event.backward = false;
        event.left = false;
        event.right = false;
        event.jump = false;
        event.sneak = false;
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(GUIWalkFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, GUIWalkFeature.mode);
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(GUIWalkFeature feature, dev.echo.client.setting.ChoiceSetting preparedMode) {
        GUIWalkFeature.mode = preparedMode;
    }
}
