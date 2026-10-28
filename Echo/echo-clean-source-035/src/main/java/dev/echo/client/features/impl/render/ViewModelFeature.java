package dev.echo.client.features.impl.render;

import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.render.hand.*;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.utils.strings.Concat;
import java.util.Arrays;
import java.util.List;

/** First-person item settings and the ordered collection of all seventeen animation strategies. */
public class ViewModelFeature extends Feature {
    public List<HandAnimation> animations;
    public static BooleanSetting eatingAnimation;
    public static FloatSetting eatingBob;
    public static ChoiceSetting styles;
    public static FloatSetting strengthMain;
    public static FloatSetting strengthOff;
    public static FloatSetting scaleMain;
    public static FloatSetting scaleOff;
    public static FloatSetting posXMain;
    public static FloatSetting posYMain;
    public static FloatSetting posZMain;
    public static FloatSetting posXOff;
    public static FloatSetting posYOff;
    public static FloatSetting posZOff;
    public static FloatSetting rotXMain;
    public static FloatSetting rotYMain;
    public static FloatSetting rotZMain;
    public static FloatSetting rotXOff;
    public static FloatSetting rotYOff;
    public static FloatSetting rotZOff;

    public ViewModelFeature() {
        super(new FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("View Model"), dev.echo.client.bootstrap.CatalogText.literal("Modifies hand/item rendering"), dev.echo.client.features.Category.RENDER, false));
    }

    @Override
    public void postInitialize() {
        animations = Arrays.asList(
                new DefaultHandAnimation(), new NoneHandAnimation(), new CustomHandAnimation(),
                new SmoothHandAnimation(), new BounceHandAnimation(), new SlashHandAnimation(),
                new PunchHandAnimation(), new SwipeHandAnimation(), new Spin360HandAnimation(),
                new ScaleHandAnimation(), new SpinHandAnimation(), new KatanaHandAnimation(),
                new SigmaHandAnimation(), new PushHandAnimation(), new StabHandAnimation(),
                new SliceHandAnimation(), new ExhibitionHandAnimation());
        eatingAnimation.tooltip("Appearance");
        scaleMain.tooltip("Appearance");
        scaleOff.tooltip("Appearance");
    }

    public HandAnimation getSelectedAnimation() {
        for (HandAnimation animation : animations) {
            if (styles.isSelected(animation.displayName())) {
                return animation;
            }
        }
        return animations.get(0);
    }

}
