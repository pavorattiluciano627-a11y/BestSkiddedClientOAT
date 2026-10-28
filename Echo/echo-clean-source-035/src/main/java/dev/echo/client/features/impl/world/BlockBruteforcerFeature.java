package dev.echo.client.features.impl.world;

import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.PlayerInputEvent;
import dev.echo.client.events.lifecycle.ClientTickEvent;
import dev.echo.client.events.network.PacketReceiveEvent;
import dev.echo.client.events.render.WorldRenderEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.render.WorldGeometry;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.IntSetting;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionHand;

/** Ordered block probes and confirmation tracking; conditional linkage receipt applies. */
public class BlockBruteforcerFeature extends Feature {
    private static final CharSequence START = CatalogText.literal("Start");
    private static final CharSequence ABORT = CatalogText.literal("Abort");
    private static IntSetting radius;
    private static IntSetting up;
    private static IntSetting down;
    private static IntSetting delay;
    private static IntSetting blocksPerTick;
    private static BooleanSetting freezeMovement;
    private static ChoiceSetting mode;
    private List<BlockPos> pendingProbePositions;
    private Set<BlockPos> probedPositions;
    private Set<BlockPos> confirmedPositions;
    private int delayTicks;
    private boolean probing;
    private BlockPos scanOrigin;

    public BlockBruteforcerFeature() {
        super(new FeatureInfo(CatalogText.literal("Block Bruteforcer"),
                CatalogText.literal("Reveals obfuscated blocks by bruteforcing each block"), Category.WORLD));
    }

    public static void appendBootstrapSettings(BlockBruteforcerFeature feature) {
        CatalogSettings.append(feature, radius);
        CatalogSettings.append(feature, up);
        CatalogSettings.append(feature, down);
        CatalogSettings.append(feature, delay);
        CatalogSettings.append(feature, blocksPerTick);
        CatalogSettings.append(feature, freezeMovement);
        CatalogSettings.append(feature, mode);
    }

    public static void assignBootstrapSettings(BlockBruteforcerFeature feature, IntSetting preparedRadius,
            IntSetting preparedUp, IntSetting preparedDelay, IntSetting preparedDown,
            IntSetting preparedBlocksPerTick, ChoiceSetting preparedMode, BooleanSetting preparedFreezeMovement) {
        radius = preparedRadius;
        up = preparedUp;
        delay = preparedDelay;
        down = preparedDown;
        blocksPerTick = preparedBlocksPerTick;
        mode = preparedMode;
        freezeMovement = preparedFreezeMovement;
    }

    @Override
    public void postInitialize() {
        pendingProbePositions = new ArrayList<>();
        probedPositions = new HashSet<>();
        confirmedPositions = new HashSet<>();
        delayTicks = 0;
        probing = false;
        delay.tooltip("Timing");
        blocksPerTick.tooltip("Timing");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        Objects.requireNonNull(pendingProbePositions, "object reference is null").clear();
        Objects.requireNonNull(probedPositions, "object reference is null").clear();
        Objects.requireNonNull(confirmedPositions, "object reference is null").clear();
        rebuildProbeQueue();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        Objects.requireNonNull(pendingProbePositions, "object reference is null").clear();
        Objects.requireNonNull(probedPositions, "object reference is null").clear();
        Objects.requireNonNull(confirmedPositions, "object reference is null").clear();
        probing = false;
        scanOrigin = null;
    }

    @Subscribe
    private void onPacketReceive(PacketReceiveEvent event) {
        if (isOutsideWorld()) return;
        if (event.getPacket() instanceof ClientboundPlayerPositionPacket) {
            setEnabled(false);
            return;
        }
        if (event.getPacket() instanceof ClientboundBlockUpdatePacket update) {
            BlockPos position = update.getPos();
            if (probedPositions.contains(position)) confirmedPositions.add(position);
        }
    }

    @Subscribe
    private void onTick(ClientTickEvent event) {
        if (isOutsideWorld()) {
            setEnabled(false);
            return;
        }
        if (probing) probeNextBatch();
    }

    @Subscribe
    private void onPlayerInput(PlayerInputEvent event) {
        if (isOutsideWorld()) return;
        if (!probing || !freezeMovement.getValue()) return;
        event.forward = false;
        event.backward = false;
        event.left = false;
        event.right = false;
    }

    private void rebuildProbeQueue() {
        probing = true;
        pendingProbePositions.clear();
        probedPositions.clear();
        BlockPos origin = client.player.blockPosition();
        scanOrigin = origin;
        int horizontal = radius.getValue();
        int above = up.getValue();
        int below = down.getValue();
        for (int y = -below; y <= above; ++y) {
            for (int x = -horizontal; x <= horizontal; ++x) {
                for (int z = -horizontal; z <= horizontal; ++z) {
                    BlockPos position = new BlockPos(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (!client.level.isEmptyBlock(position) && !probedPositions.contains(position)) {
                        pendingProbePositions.add(position);
                    }
                }
            }
        }
    }

    private void probeNextBatch() {
        if (pendingProbePositions.isEmpty()) {
            probing = false;
            setEnabled(false);
            return;
        }
        // This is deliberately AND: the original proceeds when either provider exists.
        if (client.getConnection() == null && client.gameMode == null) {
            probing = false;
            return;
        }
        ++delayTicks;
        if (delayTicks < delay.getValue()) return;
        delayTicks = 0;
        int count = Math.min(blocksPerTick.getValue(), pendingProbePositions.size());
        for (int index = 0; index < count; ++index) {
            BlockPos position = pendingProbePositions.remove(0);
            probedPositions.add(position);
            if (mode.isSelected(START)) {
                client.getConnection().send(new ServerboundPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, position, Direction.UP));
            } else if (mode.isSelected(ABORT)) {
                client.getConnection().send(new ServerboundPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, position, Direction.UP));
            }
            client.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    @Subscribe
    private void onWorldRender(WorldRenderEvent event) {
        if (isOutsideWorld() || scanOrigin == null) return;
        int horizontal = radius.getValue();
        int above = up.getValue();
        int below = down.getValue();
        WorldGeometry geometry = event.getRenderContext().getDepthTestedGeometry();
        double x0 = scanOrigin.getX() - horizontal;
        double y0 = scanOrigin.getY() - below;
        double z0 = scanOrigin.getZ() - horizontal;
        double x1 = scanOrigin.getX() + horizontal + 1;
        double y1 = scanOrigin.getY() + above + 1;
        double z1 = scanOrigin.getZ() + horizontal + 1;
        int color = ARGB.colorFromFloat(0.8F, 1.0F, 1.0F, 1.0F);
        geometry.drawLine(x0, y0, z0, x1, y0, z0, color);
        geometry.drawLine(x1, y0, z0, x1, y0, z1, color);
        geometry.drawLine(x1, y0, z1, x0, y0, z1, color);
        geometry.drawLine(x0, y0, z1, x0, y0, z0, color);
        geometry.drawLine(x0, y1, z0, x1, y1, z0, color);
        geometry.drawLine(x1, y1, z0, x1, y1, z1, color);
        geometry.drawLine(x1, y1, z1, x0, y1, z1, color);
        geometry.drawLine(x0, y1, z1, x0, y1, z0, color);
        geometry.drawLine(x0, y0, z0, x0, y1, z0, color);
        geometry.drawLine(x1, y0, z0, x1, y1, z0, color);
        geometry.drawLine(x1, y0, z1, x1, y1, z1, color);
        geometry.drawLine(x0, y0, z1, x0, y1, z1, color);
    }

    /** Original returns the mutable live confirmation set. */
    public Set<BlockPos> getConfirmedPositions() { return confirmedPositions; }
    public boolean isProbing() { return probing; }
    public int getProgressPercent() {
        int total = pendingProbePositions.size() + probedPositions.size();
        return total == 0 ? 100 : probedPositions.size() * 100 / total;
    }
}
