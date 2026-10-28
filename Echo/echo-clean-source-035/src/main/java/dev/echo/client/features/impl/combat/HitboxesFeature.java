package dev.echo.client.features.impl.combat;

import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.world.EntityHitboxEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.EntityTypeListSetting;
import dev.echo.client.setting.FloatSetting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/** Configurable entity hitbox expansion, including gliding-player height restoration. */
public class HitboxesFeature extends Feature {
    private static final double STANDING_PLAYER_HEIGHT = 1.8;
    public static BooleanSetting uniform;
    public static FloatSetting expandX;
    public static FloatSetting expandYUp;
    public static FloatSetting expandYDown;
    public static FloatSetting expandZ;
    public static FloatSetting expandSize;
    public static BooleanSetting staticHitbox;
    public static EntityTypeListSetting entityTypes = new EntityTypeListSetting(CatalogText.literal("Targets"));
    private static String expansionSection;
    private static String staticHitboxSection;

    public HitboxesFeature() {
        this(new FeatureInfo(CatalogText.literal("Hitboxes"), CatalogText.literal("Modifies hitbox sizes"),
            Category.COMBAT, false));
    }

    public HitboxesFeature(FeatureInfo info) {
        super(info);
    }

    @Override
    public void postInitialize() {
        expandX.setVisibilityPredicate(ignored -> !uniform.getValue());
        expandYUp.setVisibilityPredicate(ignored -> !uniform.getValue());
        expandYDown.setVisibilityPredicate(ignored -> !uniform.getValue());
        expandZ.setVisibilityPredicate(ignored -> !uniform.getValue());
        expandSize.setVisibilityPredicate(ignored -> uniform.getValue());
        expandX.setAllowOutOfRange(true);
        expandYUp.setAllowOutOfRange(true);
        expandYDown.setAllowOutOfRange(true);
        expandZ.setAllowOutOfRange(true);
        if (expansionSection == null) expansionSection = new String("Appearance");
        expandSize.tooltip(expansionSection);
        if (staticHitboxSection == null) staticHitboxSection = new String("Appearance");
        staticHitbox.tooltip(staticHitboxSection);
    }

    @Subscribe
    private void onHitbox(EntityHitboxEvent event) {
        Entity entity = event.getEntity();
        if (!entityTypes.accepts(entity)) {
            return;
        }
        AABB box = baseBoundingBox(entity).inflate((double) entity.getPickRadius());
        if (uniform.getValue()) {
            float expansion = expandSize.getValue();
            event.setBoundingBox(box.inflate((double) expansion, (double) expansion, (double) expansion));
            return;
        }
        float x = expandX.getValue();
        float up = expandYUp.getValue();
        float down = expandYDown.getValue();
        float z = expandZ.getValue();
        event.setBoundingBox(new AABB(box.minX - (double) x, box.minY - (double) down,
            box.minZ - (double) z, box.maxX + (double) x, box.maxY + (double) up, box.maxZ + (double) z));
    }

    private static AABB baseBoundingBox(Entity entity) {
        AABB box = entity.getBoundingBox();
        if (staticHitbox.getValue() && entity instanceof Player player && player.isFallFlying()) {
            box = new AABB(box.minX, box.minY, box.minZ, box.maxX,
                box.minY + STANDING_PLAYER_HEIGHT, box.maxZ);
        }
        return box;
    }

    public static void appendBootstrapSettings(HitboxesFeature feature) {
        CatalogSettings.append(feature, uniform);
        CatalogSettings.append(feature, expandX);
        CatalogSettings.append(feature, expandYUp);
        CatalogSettings.append(feature, expandYDown);
        CatalogSettings.append(feature, expandZ);
        CatalogSettings.append(feature, expandSize);
        CatalogSettings.append(feature, staticHitbox);
        CatalogSettings.append(feature, entityTypes);
    }

    public static void assignBootstrapSettings(HitboxesFeature feature, BooleanSetting preparedStaticHitbox,
            FloatSetting preparedExpandX, BooleanSetting preparedUniform, FloatSetting preparedExpandYUp,
            FloatSetting preparedExpandZ, FloatSetting preparedExpandSize, FloatSetting preparedExpandYDown) {
        staticHitbox = preparedStaticHitbox;
        expandX = preparedExpandX;
        uniform = preparedUniform;
        expandYUp = preparedExpandYUp;
        expandZ = preparedExpandZ;
        expandSize = preparedExpandSize;
        expandYDown = preparedExpandYDown;
    }
}
