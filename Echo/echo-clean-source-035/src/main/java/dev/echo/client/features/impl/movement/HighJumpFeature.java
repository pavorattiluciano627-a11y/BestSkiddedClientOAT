package dev.echo.client.features.impl.movement;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.PlayerInputEvent;
import dev.echo.client.events.interaction.BlockInteractEvent;
import dev.echo.client.events.lifecycle.ClientTickEvent;
import dev.echo.client.events.lifecycle.ScreenChangeEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.setting.IntSetting;
import dev.echo.client.utils.strings.Concat;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Normal jump boosting and shulker interaction with the original twenty-tick trigger window. */
public class HighJumpFeature extends Feature {
    private static final CharSequence NORMAL_MODE = Concat.strings("Normal".split(""));
    private static final CharSequence SHULKER_MODE = Concat.strings("Shulker".split(""));
    private static final int PLACEMENT_TIMEOUT = 20;
    private static ChoiceSetting mode;
    private static FloatSetting lift;
    private static IntSetting ticks;
    private net.minecraft.util.math.BlockPos pendingPlacement;
    private int placementTimeoutTicks;
    private int remainingJumpTicks;

    public HighJumpFeature() {
        super(new FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("High Jump"), dev.echo.client.bootstrap.CatalogText.literal("Boosts upward when jumping"), dev.echo.client.features.Category.MOVEMENT, false));
    }

    @Override
    public void postInitialize() {
        ticks.tooltip("Timing");
    }

    @Override
    public CharSequence getSuffix() {
        return mode.getValue();
    }

    @Subscribe
    private void onPlayerInput(PlayerInputEvent event) {
        if (isOutsideWorld() || !mode.isSelected(NORMAL_MODE)) {
            return;
        }
        if (!event.jump || !client.player.isOnGround()) {
            return;
        }
        remainingJumpTicks = ticks.getValue();
    }

    @Subscribe
    private void onBlockInteract(BlockInteractEvent.Post event) {
        if (isOutsideWorld() || !mode.isSelected(SHULKER_MODE)) {
            return;
        }
        BlockPos position = event.getHit().getBlockPos();
        if (!isShulkerBox(position) || !isPlayerNear(position)) {
            return;
        }
        pendingPlacement = position.toImmutable();
        placementTimeoutTicks = PLACEMENT_TIMEOUT;
    }

    @Subscribe
    private void onScreenChange(ScreenChangeEvent event) {
        if (!mode.isSelected(SHULKER_MODE) || !(event.getScreen() instanceof ShulkerBoxScreen)) {
            return;
        }
        if (pendingPlacement == null || placementTimeoutTicks <= 0) {
            return;
        }
        if (!isShulkerBox(pendingPlacement) || !isPlayerNear(pendingPlacement)) {
            return;
        }
        remainingJumpTicks = ticks.getValue();
        pendingPlacement = null;
        placementTimeoutTicks = 0;
    }

    @Subscribe
    private void onClientTick(ClientTickEvent event) {
        if (isOutsideWorld()) {
            clearJumpState();
            return;
        }
        if (placementTimeoutTicks > 0 && --placementTimeoutTicks == 0) {
            pendingPlacement = null;
        }
        if (remainingJumpTicks <= 0) {
            return;
        }
        if (client.player.hasVehicle() || client.player.isTouchingWater() || client.player.isInLava()) {
            remainingJumpTicks = 0;
            return;
        }
        Vec3d velocity = client.player.getVelocity();
        client.player.setVelocity(velocity.x, Math.max(velocity.y, lift.getValue()), velocity.z);
        client.player.setOnGround(false);
        remainingJumpTicks--;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        try {
            Class.forName(net.minecraft.util.math.BlockPos.class.getName(), true,
                    net.minecraft.util.math.BlockPos.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        pendingPlacement = null;
        placementTimeoutTicks = 0;
        remainingJumpTicks = 0;
    }

    private boolean isShulkerBox(BlockPos position) {
        return client.world.getBlockState(position).getBlock() instanceof ShulkerBoxBlock;
    }

    private boolean isPlayerNear(BlockPos position) {
        return client.player.getBoundingBox().intersects(new Box(position).expand(0.25));
    }

    private void clearJumpState() {
        try {
            Class.forName(BlockPos.class.getName(), true, BlockPos.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        pendingPlacement = null;
        placementTimeoutTicks = 0;
        remainingJumpTicks = 0;
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(HighJumpFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, HighJumpFeature.mode);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, HighJumpFeature.lift);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, HighJumpFeature.ticks);
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(HighJumpFeature feature, dev.echo.client.setting.ChoiceSetting preparedMode, dev.echo.client.setting.IntSetting preparedTicks, dev.echo.client.setting.FloatSetting preparedLift) {
        HighJumpFeature.mode = preparedMode;
        HighJumpFeature.ticks = preparedTicks;
        HighJumpFeature.lift = preparedLift;
    }
}
