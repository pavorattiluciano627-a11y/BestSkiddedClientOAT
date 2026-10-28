package dev.echo.client.features.impl.combat;

import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.lifecycle.FrameStartEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.mixin.KeyBindingAccessor;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.FloatRangeSetting;
import dev.echo.client.setting.HotbarSlotsSetting;
import dev.echo.client.setting.ItemListSetting;
import dev.echo.client.utils.ActionTimer;
import dev.echo.client.utils.RandomMath;

/**
 * Attack-key timing and suppression. This complete source candidate is scoped to
 * the original null-reader/owned-call bindings; see the accompanying proof receipt.
 */
public class AutoClickerFeature extends Feature {
    private static final CharSequence randomizationOption2 = CatalogText.literal("None");
    private static final CharSequence randomizationOption = CatalogText.literal("Normal");
    private static FloatRangeSetting cps;
    private static ChoiceSetting randomization;
    private static ItemListSetting itemWhitelist;
    private static HotbarSlotsSetting hotbarWhitelist;
    private static BooleanSetting requireClick;
    private static BooleanSetting breakBlocks;
    private ActionTimer clickRateTimer;
    private ActionTimer burstTimer;
    private int burstClickCount;
    private long nextClickMillis;
    private double intervalScale;
    private double currentClicksPerSecond;
    private double targetClicksPerSecond;
    private long nextRateChangeMillis;
    private double previousIntervalMillis;
    private int similarIntervalCount;

    public AutoClickerFeature() {
        super(new FeatureInfo(CatalogText.literal("Auto Clicker"),
                CatalogText.literal("Clicks the attack button for you"), Category.COMBAT, false));
    }

    /** Catalog calls this once after every setting constructor and assignment. */
    public static void appendBootstrapSettings(AutoClickerFeature feature) {
        CatalogSettings.append(feature, cps);
        CatalogSettings.append(feature, randomization);
        CatalogSettings.append(feature, itemWhitelist);
        CatalogSettings.append(feature, hotbarWhitelist);
        CatalogSettings.append(feature, requireClick);
        CatalogSettings.append(feature, breakBlocks);
    }

    public static CharSequence bootstrapRandomizationOption() { return randomizationOption; }
    public static CharSequence bootstrapRandomizationOption2() { return randomizationOption2; }

    public static void assignBootstrapSettings(AutoClickerFeature feature,
            HotbarSlotsSetting preparedHotbarWhitelist, BooleanSetting preparedBreakBlocks,
            ItemListSetting preparedItemWhitelist, BooleanSetting preparedRequireClick,
            FloatRangeSetting preparedCps, ChoiceSetting preparedRandomization) {
        hotbarWhitelist = preparedHotbarWhitelist;
        breakBlocks = preparedBreakBlocks;
        itemWhitelist = preparedItemWhitelist;
        requireClick = preparedRequireClick;
        cps = preparedCps;
        randomization = preparedRandomization;
    }

    @Override
    public void postInitialize() {
        clickRateTimer = new ActionTimer();
        burstTimer = new ActionTimer();
        intervalScale = 0.85;
        cps.setAllowOutOfRange(true);
        requireClick.description(CatalogText.literal("Only click while the attack button is held"));
        breakBlocks.description(CatalogText.literal("Keep clicking while breaking a block"));
        randomization.description(CatalogText.literal("None clicks at a fixed rate in the middle of the CPS range"));
        cps.tooltip(CatalogText.literal("Timing"));
        randomization.tooltip(CatalogText.literal("Timing"));
        itemWhitelist.tooltip(CatalogText.literal("Conditions"));
        hotbarWhitelist.tooltip(CatalogText.literal("Conditions"));
    }

    /** NB-065: preserve super-first, explicit initialization and null-failure order. */
    @Override
    public void onEnable() {
        super.onEnable();
        try {
            Class.forName(ActionTimer.class.getName(), true, ActionTimer.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        java.util.Objects.requireNonNull(clickRateTimer, "object reference is null").reset();
        try {
            Class.forName(FloatRangeSetting.class.getName(), true, FloatRangeSetting.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        float initialRate = java.util.Objects.requireNonNull(cps, "object reference is null").sampleGaussian();
        targetClicksPerSecond = initialRate;
        currentClicksPerSecond = initialRate;
        nextRateChangeMillis = 0L;
        nextClickMillis = 0L;
        try {
            Class.forName(ActionTimer.class.getName(), true, ActionTimer.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        java.util.Objects.requireNonNull(burstTimer, "object reference is null").reset();
        burstClickCount = 0;
    }

    // The original has no disable override: Feature unregisters its subscribers.
    @Subscribe
    private void onFrameStart(FrameStartEvent event) {
        long now = System.nanoTime() / 1_000_000L;
        if (shouldSuppressClick()) {
            nextClickMillis = Math.max(nextClickMillis, now);
            return;
        }
        if (now < nextClickMillis) return;
        client.attackCooldown = 0;
        KeyBindingAccessor attack = (KeyBindingAccessor) client.options.attackKey;
        attack.setTimesPressed(attack.getTimesPressed() + 1);
        nextClickMillis = Math.max(nextClickMillis, now - 40L) + computeClickIntervalMillis();
    }

    private boolean shouldSuppressClick() {
        return isOutsideWorld() || MinecraftRenderState.getScreen() != null
                || client.player.isUsingItem() || client.interactionManager == null
                || (!breakBlocks.getValue() && client.interactionManager.isBreakingBlock())
                || (requireClick.getValue() && !client.options.attackKey.isPressed())
                || !itemWhitelist.acceptsHeldItem() || !hotbarWhitelist.permitsCurrentSlot();
    }

    private long computeClickIntervalMillis() {
        if (randomization.isSelected(randomizationOption2)) {
            // Addition is deliberately float arithmetic before widening to double.
            return (long) (2000.0 / (cps.getLower() + cps.getUpper()));
        }
        if (clickRateTimer.hasElapsedMillis((double) nextRateChangeMillis)) {
            targetClicksPerSecond = cps.sampleGaussian();
            nextRateChangeMillis = (long) (2500.0 + RandomMath.random.nextDouble() * 3500.0);
            clickRateTimer.reset();
        }
        currentClicksPerSecond += (targetClicksPerSecond - currentClicksPerSecond) * 0.05;
        currentClicksPerSecond = Math.max(cps.getLower(), Math.min(cps.getUpper(), currentClicksPerSecond));
        double nominalInterval = 1000.0 / currentClicksPerSecond;
        double scaledInterval = nominalInterval * intervalScale;
        double interval = scaledInterval + RandomMath.random.nextGaussian() * scaledInterval * 0.2
                - Math.log(1.0 - RandomMath.random.nextDouble()) * scaledInterval * 0.1;
        if (RandomMath.random.nextDouble() < 0.04) {
            interval += 140.0 + RandomMath.random.nextDouble() * 260.0;
        }
        interval = Math.max(scaledInterval * 0.55, interval);
        double similarityThreshold = scaledInterval * 0.3;
        if (Math.abs(interval - previousIntervalMillis) < similarityThreshold && ++similarIntervalCount >= 5) {
            interval += 60.0 + RandomMath.random.nextDouble() * 100.0;
            similarIntervalCount = 0;
        } else if (Math.abs(interval - previousIntervalMillis) >= similarityThreshold) {
            similarIntervalCount = 0;
        }
        previousIntervalMillis = interval;
        intervalScale = Math.max(0.5, Math.min(1.1,
                intervalScale + (nominalInterval - interval) / nominalInterval * 0.03));
        return (long) interval;
    }
}
