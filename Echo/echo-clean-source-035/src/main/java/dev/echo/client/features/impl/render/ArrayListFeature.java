package dev.echo.client.features.impl.render;

import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.features.FeatureManager;
import dev.echo.client.features.hud.EchoHudElement;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.hud.HudElementLayout;
import dev.echo.client.hud.HudElementType;
import dev.echo.client.hud.HudRenderer;
import dev.echo.client.hud.KeybindRow;
import dev.echo.render.FontAsset;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.MultiChoiceSetting;
import dev.echo.client.ui.UiId;
import dev.echo.client.ui.UiSize;
import dev.echo.client.utils.strings.Concat;
import java.util.ArrayList;
import java.util.List;

/** Animated enabled-feature rows, configurable suffixes and name-based visibility lists. */
public class ArrayListFeature extends EchoHudElement {
    private static final CharSequence BLACKLIST = CatalogText.literal("Blacklist");
    private static final float ROW_SPACING = 39.5f;
    private static final CharSequence WHITELIST = CatalogText.literal("Whitelist");
    private BooleanSetting suffixEnabled;
    private BooleanSetting lowercase;
    private ChoiceSetting visibilityFilter;
    private final MultiChoiceSetting blacklistedFeatures;
    private final MultiChoiceSetting whitelistedFeatures;
    private static final String ANIMATION_PREFIX = "hud/overlay/array/";

    public ArrayListFeature() {
        super(new FeatureInfo(CatalogText.literal("Array List"),
                CatalogText.literal("Enabled modules and their binds"), Category.RENDER), HudElementType.ARRAY_LIST);
        blacklistedFeatures = new MultiChoiceSetting(CatalogText.literal("Blacklisted features")).selectionMode();
        whitelistedFeatures = new MultiChoiceSetting(CatalogText.literal("Whitelisted features")).selectionMode();
    }

    @Override
    public void postInitialize() {
        super.postInitialize();
        for (Feature feature : FeatureManager.allFeatures()) {
            whitelistedFeatures.getMutableOptions().put(feature.getName(), false);
            blacklistedFeatures.getMutableOptions().put(feature.getName(), false);
        }
        whitelistedFeatures.setVisibilityPredicate(context -> visibilityFilter.isSelected(WHITELIST));
        blacklistedFeatures.setVisibilityPredicate(context -> visibilityFilter.isSelected(BLACKLIST));
    }

    public boolean isFeatureVisible(Feature feature) {
        if (feature == this) {
            return false;
        }
        if (visibilityFilter.isSelected(WHITELIST)) {
            return whitelistedFeatures.isSelected(feature.getName());
        }
        return !blacklistedFeatures.isSelected(feature.getName());
    }

    public CharSequence normalizeLabel(Feature feature, CharSequence label) {
        return lowercase.getValue() ? Concat.sequences(label).lowercaseView() : label;
    }

    public CharSequence getFeatureSuffix(Feature feature) {
        if (!suffixEnabled.getValue()) {
            return "";
        }
        CharSequence suffix = feature.getSuffix();
        if (suffix == null || suffix.isEmpty()) {
            return "";
        }
        return lowercase.getValue() ? Concat.sequences(suffix).lowercaseView() : suffix;
    }

    @Override
    protected boolean shouldRender() {
        return !client.debugHudEntryList.isF3Enabled();
    }

    @Override
    public UiSize measure(HudElementLayout layout) {
        return HudRenderer.INSTANCE.measureElement(layout, HudElementType.ARRAY_LIST);
    }

    @Override
    protected void renderContents(HudElementLayout layout) {
        HudRenderer hud = HudRenderer.INSTANCE;
        boolean rightAligned = isRightOfScreenCenter();
        List<KeybindRow> rows = hud.getArrayListRows();
        List<String> order = hud.orderArrayListRows(layout);
        ArrayList<String> completed = new ArrayList<>();
        for (KeybindRow row : rows) {
            UiId identity = UiId.ROOT.child(ANIMATION_PREFIX + row.id());
            float opacity = hud.arrayRowOpacity(identity, row.enabled());
            if (opacity <= 0.002f) {
                if (!row.enabled()) {
                    completed.add(row.id());
                }
                continue;
            }
            int index = order.indexOf(row.id());
            if (index < 0) {
                continue;
            }
            float rowY = hud.arrayRowY(identity, HudElementType.ARRAY_LIST.defaultY() + index * ROW_SPACING);
            String binding = hud.formatArrayRowBinding(row);
            float bindingWidth = binding.isEmpty() ? 0.0f
                    : layout.textWidth(FontAsset.INTER, "[" + binding + "]", 12.0f);
            float width = hud.measureArrayListRow(layout, row);
            float x = HudElementType.ARRAY_LIST.defaultX()
                    + (rightAligned ? layout.getWidth() - width : 0.0f)
                    + hud.arrayRowHorizontalOffset(identity, row.enabled(), rightAligned);
            hud.drawPanel(layout, x, rowY, width, 35.0f, 10.0f, opacity, 28.0f);
            layout.drawIcon(row.icon(), x + 12.0f, rowY + 9.5f, 16.0f, 16.0f, -1, opacity);
            layout.drawText(FontAsset.INTER_SEMI_BOLD, row.name(), x + 36.0f, rowY + 10.0f, 13.0f, -1, opacity);
            if (!row.detail().isEmpty()) {
                float nameWidth = layout.textWidth(FontAsset.INTER_SEMI_BOLD, row.name(), 13.0f);
                layout.drawText(FontAsset.INTER_SEMI_BOLD, row.detail(), x + 36.0f + nameWidth + 5.0f,
                        rowY + 10.0f, 13.0f, -1711276033, opacity);
            }
            if (!binding.isEmpty()) {
                float separatorX = x + width - 14.0f - bindingWidth - 9.0f;
                layout.drawRoundedRect(separatorX, rowY + 13.0f, 1.0f, 9.0f, 0.0f, 0x40ffffff, opacity);
                layout.drawText(FontAsset.INTER, "[" + binding + "]", separatorX + 9.0f,
                        rowY + 10.5f, 12.0f, -1711276033, opacity);
            }
        }
        hud.removeCompletedArrayRows(completed);
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(ArrayListFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, feature.suffixEnabled);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, feature.lowercase);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, feature.visibilityFilter);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, feature.blacklistedFeatures);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, feature.whitelistedFeatures);
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(ArrayListFeature feature, dev.echo.client.setting.ChoiceSetting preparedVisibilityFilter, dev.echo.client.setting.BooleanSetting preparedLowercase, dev.echo.client.setting.BooleanSetting preparedSuffixEnabled) {
        feature.visibilityFilter = preparedVisibilityFilter;
        feature.lowercase = preparedLowercase;
        feature.suffixEnabled = preparedSuffixEnabled;
    }
}
