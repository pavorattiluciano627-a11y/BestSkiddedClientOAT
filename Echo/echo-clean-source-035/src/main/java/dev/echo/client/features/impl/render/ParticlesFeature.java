package dev.echo.client.features.impl.render;

import dev.echo.client.Echo;
import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.lifecycle.ClientTickEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.particle.AnimatedParticle;
import dev.echo.client.particle.ParticleManager;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ColorSetting;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.setting.IntSetting;
import dev.echo.client.setting.Setting;
import dev.echo.client.utils.RandomMath;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Ambient ember producer. Binding/cache and lifecycle domains accompany this candidate. */
public class ParticlesFeature extends Feature {
    public static ParticlesFeature instance;
    private ArrayList<AnimatedParticle> particles;
    private static ColorSetting color;
    private static IntSetting density;
    private static IntSetting radius;
    private static FloatSetting size;
    private static FloatSetting speed;
    private static BooleanSetting twinkle;

    static {
        // Retain the original caller lookup; encrypted key/string machinery is replaced by literals.
        MethodHandles.lookup().lookupClass();
    }

    public ParticlesFeature() {
        super(new FeatureInfo(CatalogText.literal("Particles"),
                CatalogText.literal("Fills the world with ambient ember particles"), Category.RENDER));
    }

    public static void appendBootstrapSettings(ParticlesFeature feature) {
        CatalogSettings.append(feature, color);
        CatalogSettings.append(feature, density);
        CatalogSettings.append(feature, radius);
        CatalogSettings.append(feature, size);
        CatalogSettings.append(feature, speed);
        CatalogSettings.append(feature, twinkle);
    }

    public static void assignBootstrapSettings(ParticlesFeature feature, ColorSetting preparedColor,
            IntSetting preparedRadius, FloatSetting preparedSpeed, IntSetting preparedDensity,
            BooleanSetting preparedTwinkle, FloatSetting preparedSize) {
        color = preparedColor;
        radius = preparedRadius;
        speed = preparedSpeed;
        density = preparedDensity;
        twinkle = preparedTwinkle;
        size = preparedSize;
    }

    @Override
    public void postInitialize() {
        ArrayList<AnimatedParticle> freshParticles = new ArrayList<>();
        initializeReferencedClass(ArrayList.class);
        particles = freshParticles;
        instance = this;
        initializeReferencedClass(ColorSetting.class);
        ColorSetting colorSetting = color;
        initializeReferencedClass(Setting.class);
        colorSetting.tooltip("Appearance");
        initializeReferencedClass(FloatSetting.class);
        FloatSetting sizeSetting = size;
        initializeReferencedClass(Setting.class);
        sizeSetting.tooltip("Appearance");
        initializeReferencedClass(FloatSetting.class);
        FloatSetting speedSetting = speed;
        initializeReferencedClass(Setting.class);
        speedSetting.tooltip("Timing");
    }

    public static boolean isActive() {
        boolean checkAvailability = FreecamFeature.isPerspectiveRefreshEnabled();
        if (checkAvailability) {
            if (instance == null) return false;
            if (!instance.isEnabled()) return false;
        }
        boolean outsideWorld = instance.isOutsideWorld();
        return checkAvailability ? !outsideWorld : outsideWorld;
    }

    public static float getSizeMultiplier() {
        initializeReferencedClass(FloatSetting.class);
        FloatSetting setting = size;
        initializeReferencedClass(FloatSetting.class);
        return setting.getValue();
    }

    public static float getSpeedMultiplier() {
        initializeReferencedClass(FloatSetting.class);
        FloatSetting setting = speed;
        initializeReferencedClass(FloatSetting.class);
        return setting.getValue();
    }

    public static boolean isPulsing() {
        initializeReferencedClass(BooleanSetting.class);
        BooleanSetting setting = twinkle;
        initializeReferencedClass(BooleanSetting.class);
        return setting.getValue();
    }

    public static int getRed() {
        initializeReferencedClass(ColorSetting.class);
        ColorSetting setting = color;
        initializeReferencedClass(ColorSetting.class);
        return setting.getRed();
    }

    public static int getGreen() {
        initializeReferencedClass(ColorSetting.class);
        ColorSetting setting = color;
        initializeReferencedClass(ColorSetting.class);
        return setting.getGreen();
    }

    public static int getBlue() {
        initializeReferencedClass(ColorSetting.class);
        ColorSetting setting = color;
        initializeReferencedClass(ColorSetting.class);
        return setting.getBlue();
    }

    public static int getAlpha() {
        initializeReferencedClass(ColorSetting.class);
        ColorSetting setting = color;
        initializeReferencedClass(ColorSetting.class);
        return setting.getAlpha();
    }

    @Subscribe
    private void onClientTick(ClientTickEvent event) {
        boolean checkAvailability = FreecamFeature.isPerspectiveRefreshEnabled();
        if (checkAvailability && isOutsideWorld()) return;
        initializeReferencedClass(ArrayList.class);
        particles.removeIf(AnimatedParticle::isDead);
        initializeReferencedClass(IntSetting.class);
        IntSetting densitySetting = density;
        initializeReferencedClass(IntSetting.class);
        int desiredCount = densitySetting.getValue();
        initializeReferencedClass(ArrayList.class);
        int remaining = desiredCount - particles.size();
        int spawnCount = remaining;
        if (checkAvailability) {
            if (remaining <= 0) return;
            spawnCount = Math.min(remaining, Math.max(1, desiredCount / 40));
        }
        initializeReferencedClass(Minecraft.class);
        double playerX = client.player.getX();
        initializeReferencedClass(Minecraft.class);
        double playerY = client.player.getY();
        initializeReferencedClass(Minecraft.class);
        double playerZ = client.player.getZ();
        initializeReferencedClass(IntSetting.class);
        IntSetting radiusSetting = radius;
        initializeReferencedClass(IntSetting.class);
        int spawnRadius = radiusSetting.getValue();
        for (int index = 0; index < spawnCount; index++) {
            for (int attempt = 0; attempt < 4; attempt++) {
                double angle = RandomMath.randomDouble(0.0, Math.PI * 2.0);
                double distance = Math.sqrt(RandomMath.randomDouble(0.0, 1.0)) * spawnRadius;
                double x = playerX + Math.cos(angle) * distance;
                double y = playerY + RandomMath.randomDouble(-6.0, 14.0);
                // The alternate control state consumes these three draws, then leaves both loops.
                if (!checkAvailability) return;
                double z = playerZ + Math.sin(angle) * distance;
                initializeReferencedClass(Minecraft.class);
                if (!client.level.getBlockState(BlockPos.containing(x, y, z)).isAir()) continue;
                AnimatedParticle particle = new AnimatedParticle(x, y, z);
                initializeReferencedClass(ArrayList.class);
                particles.add(particle);
                initializeReferencedClass(ParticleManager.class);
                ParticleManager manager = Echo.particleManager;
                initializeReferencedClass(ParticleManager.class);
                manager.add(particle);
                break;
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        MethodHandles.lookup();
        initializeReferencedClass(ArrayList.class);
        Objects.requireNonNull(particles, "object reference is null").clear();
    }

    /** Type initialization only; no reflective member invocation or application dispatch. */
    private static void initializeReferencedClass(Class<?> type) {
        try {
            Class.forName(type.getName(), true, type.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
    }
}
