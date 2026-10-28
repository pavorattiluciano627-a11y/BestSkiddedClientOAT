package dev.echo.client.features.impl.movement;

import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.PlayerInputEvent;
import dev.echo.client.events.network.PacketReceiveEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.mixin.LivingEntityAccessor;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.setting.MultiChoiceSetting;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;

/** Jumps on the first knockback tick when the selected damage and environment checks pass. */
public class JumpResetFeature extends Feature {
    private static final CharSequence PLAYER_SOURCE = CatalogText.literal("Player");
    private static final CharSequence MOB_SOURCE = CatalogText.literal("Mob");
    private static final CharSequence PROJECTILE_SOURCE = CatalogText.literal("Projectile");
    private static final CharSequence EXPLOSION_SOURCE = CatalogText.literal("Explosion");
    private static final CharSequence FALL_SOURCE = CatalogText.literal("Fall");
    private static final CharSequence FIRE_SOURCE = CatalogText.literal("Fire");
    private static final CharSequence MAGIC_SOURCE = CatalogText.literal("Magic");
    private static final CharSequence OTHER_SOURCE = CatalogText.literal("Other");
    private static final String WEB_TRANSLATION_FRAGMENT = "web";

    private static FloatSetting chance;
    private static MultiChoiceSetting damageSources;
    private static BooleanSetting liquidCheck;
    private static BooleanSetting webCheck;
    private Holder<DamageType> lastDamageType;

    public JumpResetFeature() {
        super(new FeatureInfo(CatalogText.literal("Jump Reset"),
                CatalogText.literal("Automatically jumps on knockback to reduce incoming velocity"), Category.MOVEMENT));
    }

    public static void appendBootstrapSettings(JumpResetFeature feature) {
        CatalogSettings.append(feature, chance);
        CatalogSettings.append(feature, damageSources);
        CatalogSettings.append(feature, liquidCheck);
        CatalogSettings.append(feature, webCheck);
    }

    /** Called only after the catalog has constructed every setting in the metadata vector. */
    public static void assignBootstrapSettings(JumpResetFeature feature, MultiChoiceSetting preparedDamageSources,
            FloatSetting preparedChance, BooleanSetting preparedWebCheck, BooleanSetting preparedLiquidCheck) {
        damageSources = preparedDamageSources;
        chance = preparedChance;
        webCheck = preparedWebCheck;
        liquidCheck = preparedLiquidCheck;
    }

    @Override
    public void postInitialize() {
        damageSources.setSelected(CatalogText.literal("Player"), true);
        damageSources.setSelected(CatalogText.literal("Mob"), true);
        damageSources.setSelected(CatalogText.literal("Projectile"), false);
        damageSources.setSelected(CatalogText.literal("Explosion"), false);
    }

    @Subscribe
    public void onDamagePacket(PacketReceiveEvent event) {
        if (!(event.getPacket() instanceof ClientboundDamageEventPacket damage)) return;
        if (client.player == null || damage.entityId() != client.player.getId()) return;
        lastDamageType = damage.sourceType();
    }

    @Subscribe
    public void onPlayerInput(PlayerInputEvent event) {
        if (isOutsideWorld() || client.player == null) return;
        if (client.player.hurtTime != ((LivingEntityAccessor) client.player).getMaxHurtTime() - 1) return;
        if (!client.player.onGround()) return;
        if (!isDamageSourceAllowed()) return;
        if (!passesJumpChecks()) return;
        event.jump = true;
    }

    private boolean isDamageSourceAllowed() {
        // An empty selection accepts every source; absence of a damage packet also accepts it.
        if (lastDamageType == null || !damageSources.getOptions().values().stream().anyMatch(selected -> selected)) {
            return true;
        }
        if (lastDamageType.is(DamageTypes.PLAYER_ATTACK)) return damageSources.isSelected(PLAYER_SOURCE);
        if (lastDamageType.is(DamageTypes.MOB_ATTACK) || lastDamageType.is(DamageTypes.MOB_ATTACK_NO_AGGRO)) {
            return damageSources.isSelected(MOB_SOURCE);
        }
        if (lastDamageType.is(DamageTypes.ARROW)
                || lastDamageType.is(DamageTypes.TRIDENT)
                || lastDamageType.is(DamageTypes.MOB_PROJECTILE)
                || lastDamageType.is(DamageTypes.FIREBALL)
                || lastDamageType.is(DamageTypes.WITHER_SKULL)
                || lastDamageType.is(DamageTypes.WIND_CHARGE)
                || lastDamageType.is(DamageTypes.THROWN)
                || lastDamageType.is(DamageTypes.SPIT)
                || lastDamageType.is(DamageTypes.FIREWORKS)
                || lastDamageType.is(DamageTypes.UNATTRIBUTED_FIREBALL)
                || lastDamageType.is(DamageTypes.SPEAR)) {
            return damageSources.isSelected(PROJECTILE_SOURCE);
        }
        if (lastDamageType.is(DamageTypes.EXPLOSION) || lastDamageType.is(DamageTypes.PLAYER_EXPLOSION)) {
            return damageSources.isSelected(EXPLOSION_SOURCE);
        }
        if (lastDamageType.is(DamageTypes.FALL) || lastDamageType.is(DamageTypes.ENDER_PEARL)) {
            return damageSources.isSelected(FALL_SOURCE);
        }
        if (lastDamageType.is(DamageTypes.IN_FIRE)
                || lastDamageType.is(DamageTypes.ON_FIRE)
                || lastDamageType.is(DamageTypes.LAVA)
                || lastDamageType.is(DamageTypes.HOT_FLOOR)
                || lastDamageType.is(DamageTypes.CAMPFIRE)) {
            return damageSources.isSelected(FIRE_SOURCE);
        }
        if (lastDamageType.is(DamageTypes.MAGIC)
                || lastDamageType.is(DamageTypes.INDIRECT_MAGIC)
                || lastDamageType.is(DamageTypes.WITHER)) {
            return damageSources.isSelected(MAGIC_SOURCE);
        }
        return damageSources.isSelected(OTHER_SOURCE);
    }

    private boolean passesJumpChecks() {
        // The negated <= also preserves the original dcmpl rejection of NaN.
        if (!(Math.random() * 100.0 <= chance.getValue())) return false;
        if (liquidCheck.getValue() && (client.player.isInWater() || client.player.isInLava())) return false;
        return !webCheck.getValue() || !client.level.getBlockState(client.player.blockPosition()).getBlock()
                .getDescriptionId().contains(WEB_TRANSLATION_FRAGMENT);
    }
}
