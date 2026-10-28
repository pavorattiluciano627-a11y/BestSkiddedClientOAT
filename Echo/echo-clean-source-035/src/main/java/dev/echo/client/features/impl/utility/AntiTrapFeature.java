package dev.echo.client.features.impl.utility;

import dev.echo.client.Echo;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.HandleInputEvent;
import dev.echo.client.events.interaction.AttackAttemptEvent;
import dev.echo.client.events.interaction.BlockBreakingEvent;
import dev.echo.client.events.interaction.BlockInteractEvent;
import dev.echo.client.events.interaction.EntityAttackEvent;
import dev.echo.client.feature.utility.AntiTrapTargeting;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.RegistryListSetting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Prevents configured decorative blocks and entities from obstructing interactions. */
public final class AntiTrapFeature extends Feature {
    private static BooleanSetting noInteract;
    private static BooleanSetting armorStands;
    private static BooleanSetting itemFrames;
    private static BooleanSetting glowFrames;
    private static BooleanSetting paintings;
    private static BooleanSetting chestMinecarts;
    private static BooleanSetting signs;
    private static BooleanSetting hangingSigns;
    private static RegistryListSetting<Block> blockedBlocks =
            new AntiTrapBlockSelectionSetting(dev.echo.client.bootstrap.CatalogText.literal("Blocks"), () -> BuiltInRegistries.BLOCK);

    /** Creates the feature after class initialization creates the shared block selector. */
    public AntiTrapFeature() {
        super(new FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Anti Trap"), dev.echo.client.bootstrap.CatalogText.literal("Helps you to not get trapped"), dev.echo.client.features.Category.UTILITY));
    }

    @Override
    public void postInitialize() {
        glowFrames.tooltip("Appearance");
    }

    @Subscribe
    private void onHandleInput(HandleInputEvent.Pre event) {
        if (!noInteract.getValue() || isOutsideWorld()) {
            return;
        }
        if (!(client.hitResult instanceof BlockHitResult hit)) {
            return;
        }
        if (!isBlockedBlock(client.level.getBlockState(hit.getBlockPos()))) {
            return;
        }
        HitResult replacement = AntiTrapTargeting.pickPastBlockedBlocks(client, this::isBlockedBlock, this::isBlockedEntityType);
        client.hitResult = replacement;
        client.crosshairPickEntity = replacement instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
    }

    @Subscribe
    private void onAttackAttempt(AttackAttemptEvent event) {
        if (isCurrentBlockBlocked()) {
            event.cancel();
        }
    }

    @Subscribe
    private void onBlockBreaking(BlockBreakingEvent.Pre event) {
        if (isCurrentBlockBlocked()) {
            event.cancel();
        }
    }

    @Subscribe
    private void onBlockInteract(BlockInteractEvent.Pre event) {
        if (isBlockInteractionBlocked(event.getHit())) {
            event.cancel();
        }
    }

    @Subscribe
    private void onEntityAttack(EntityAttackEvent.Pre event) {
        if (isEntityInteractionBlocked(event.getTarget())) {
            event.cancel();
        }
    }

    private boolean isCurrentBlockBlocked() {
        if (!noInteract.getValue() || isOutsideWorld()) {
            return false;
        }
        if (!(client.hitResult instanceof BlockHitResult hit)) {
            return false;
        }
        return isBlockInteractionBlocked(hit);
    }

    private boolean isBlockInteractionBlocked(BlockHitResult hit) {
        if (!noInteract.getValue() || isOutsideWorld() || hit == null) {
            return false;
        }
        if (hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        return isBlockedBlock(client.level.getBlockState(hit.getBlockPos()));
    }

    private boolean isEntityInteractionBlocked(Entity entity) {
        if (!noInteract.getValue() || entity == null) {
            return false;
        }
        return isBlockedEntityType(entity.getType());
    }

    private boolean isBlockedEntityType(EntityType<?> type) {
        if (type == null) {
            return false;
        }
        if (armorStands.getValue() && type == EntityTypes.ARMOR_STAND) {
            return true;
        }
        if (itemFrames.getValue() && type == EntityTypes.ITEM_FRAME) {
            return true;
        }
        if (glowFrames.getValue() && type == EntityTypes.GLOW_ITEM_FRAME) {
            return true;
        }
        if (paintings.getValue() && type == EntityTypes.PAINTING) {
            return true;
        }
        return chestMinecarts.getValue() && type == EntityTypes.CHEST_MINECART;
    }

    /** Consults the live feature-manager instance, including the global shutdown gate. */
    public static boolean shouldIgnoreEntity(Entity entity) {
        if (Echo.eventsDisabled || Echo.featureManager == null) {
            return false;
        }
        AntiTrapFeature feature = (AntiTrapFeature) Echo.featureManager.findFeature(AntiTrapFeature.class);
        if (feature == null || !feature.isEnabled() || !noInteract.getValue()) {
            return false;
        }
        return entity != null && feature.isBlockedEntityType(entity.getType());
    }

    private boolean isBlockedBlock(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        if (signs.getValue() && (state.is(BlockTags.STANDING_SIGNS) || state.is(BlockTags.WALL_SIGNS))) {
            return true;
        }
        if (hangingSigns.getValue()
                && (state.is(BlockTags.CEILING_HANGING_SIGNS) || state.is(BlockTags.WALL_HANGING_SIGNS))) {
            return true;
        }
        return blockedBlocks.isEntrySelected(state.getBlock());
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(AntiTrapFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AntiTrapFeature.noInteract);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AntiTrapFeature.armorStands);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AntiTrapFeature.itemFrames);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AntiTrapFeature.glowFrames);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AntiTrapFeature.paintings);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AntiTrapFeature.chestMinecarts);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AntiTrapFeature.signs);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AntiTrapFeature.hangingSigns);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, AntiTrapFeature.blockedBlocks);
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(AntiTrapFeature feature, dev.echo.client.setting.BooleanSetting preparedSigns, dev.echo.client.setting.BooleanSetting preparedHangingSigns, dev.echo.client.setting.BooleanSetting preparedGlowFrames, dev.echo.client.setting.BooleanSetting preparedPaintings, dev.echo.client.setting.BooleanSetting preparedChestMinecarts, dev.echo.client.setting.BooleanSetting preparedArmorStands, dev.echo.client.setting.BooleanSetting preparedNoInteract, dev.echo.client.setting.BooleanSetting preparedItemFrames) {
        AntiTrapFeature.signs = preparedSigns;
        AntiTrapFeature.hangingSigns = preparedHangingSigns;
        AntiTrapFeature.glowFrames = preparedGlowFrames;
        AntiTrapFeature.paintings = preparedPaintings;
        AntiTrapFeature.chestMinecarts = preparedChestMinecarts;
        AntiTrapFeature.armorStands = preparedArmorStands;
        AntiTrapFeature.noInteract = preparedNoInteract;
        AntiTrapFeature.itemFrames = preparedItemFrames;
    }
}
