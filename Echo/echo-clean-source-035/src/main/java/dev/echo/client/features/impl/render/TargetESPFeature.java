package dev.echo.client.features.impl.render;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.render.WorldRenderEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.render.TargetMotionTrail;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.ColorSetting;

/** Target highlight selection and the original stateful circle trail renderer. */
public class TargetESPFeature extends Feature {
    public static TargetESPFeature instance;
    private static final CharSequence modeOption = CatalogText.literal("Circle");
    private static final CharSequence chainsModeOption = CatalogText.literal("Chains");
    public static ChoiceSetting mode;
    private static ColorSetting color;
    private TargetMotionTrail trail;

    public TargetESPFeature() {
        super(new dev.echo.client.features.FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Target ESP"), dev.echo.client.bootstrap.CatalogText.literal("Highlights current target"), dev.echo.client.features.Category.RENDER));
    }

    @Override
    public void postInitialize() {
        trail = new TargetMotionTrail(color, 48, 0.0f);
        instance = this;
        // Binding the method reference checks its receiver now, before tooltip setup.
        mode.onChange(trail::clearHistory);
        color.tooltip("Appearance");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        try {
            Class.forName(TargetMotionTrail.class.getName(), true, TargetMotionTrail.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        TargetMotionTrail currentTrail = trail;
        try {
            Class.forName(TargetMotionTrail.class.getName(), true, TargetMotionTrail.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        currentTrail.clearHistory();
    }

    @Subscribe
    private void onWorldRender(WorldRenderEvent event) {
        boolean checkMode = FreecamFeature.isPerspectiveRefreshEnabled();
        boolean render = isOutsideWorld();
        if (checkMode) {
            if (render) {
                return;
            }
            render = mode.isSelected(modeOption);
        }
        if (render) {
            trail.render(event);
        }
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(TargetESPFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, TargetESPFeature.mode);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, TargetESPFeature.color);
    }

    /** Reads the shared modeOption default without copying its value. */
    public static java.lang.CharSequence bootstrapModeOption() {
        return modeOption;
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(TargetESPFeature feature, dev.echo.client.setting.ColorSetting preparedColor, dev.echo.client.setting.ChoiceSetting preparedMode) {
        TargetESPFeature.color = preparedColor;
        TargetESPFeature.mode = preparedMode;
    }
}
