package dev.echo.client.utils.placement;

import dev.echo.client.MinecraftAccess;
import dev.echo.client.events.interaction.ItemUseEvent;
import dev.echo.client.input.KeybindingCache;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.FloatRangeSetting;
import dev.echo.client.setting.HotbarSlotsSetting;
import dev.echo.client.setting.IntSetting;
import dev.echo.client.setting.ItemListSetting;
import dev.echo.client.setting.Setting;
import dev.echo.client.utils.RandomMath;
import dev.echo.client.utils.items.InventoryItems;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Classic anchor placement, charging and detonation controller. Direct operations
 * and control flow use the conditional domains documented in anchor-phase2.
 */
public class AnchorController implements MinecraftAccess {
    private static final int PLACEMENT_CONFIRMATION_TICKS = 20;
    private static final int MAX_ANCHOR_CHARGES = 4;
    private static final int EXPLOSION_EFFECT_WINDOW_TICKS = 20;
    private static final int DETONATION_RETRY_TICKS = 20;
    private static final List<Item> DEFAULT_WHITELIST = List.of(
            Items.RESPAWN_ANCHOR, Items.GLOWSTONE, Items.WOODEN_SWORD, Items.STONE_SWORD,
            Items.IRON_SWORD, Items.GOLDEN_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD);

    private final Predicate<Object> visibleWhen;
    public final BooleanSetting autoCharge;
    public final IntSetting chargeChance;
    public final FloatRangeSetting charges;
    public final FloatRangeSetting glowstoneDelay;
    public final IntSetting glowstoneChance;
    public final BooleanSetting autoExplode;
    public final IntSetting explodeSlot;
    public final FloatRangeSetting explodeDelay;
    public final IntSetting explodeChance;
    public final BooleanSetting safeAnchor;
    public final FloatRangeSetting safeBlockDelay;
    public final ItemListSetting itemWhitelist;
    public final HotbarSlotsSetting hotbarWhitelist;
    public final FloatRangeSetting switchDelay;
    public final IntSetting switchChance;

    private int switchTicks;
    private int glowstoneTicks;
    private int explodeTicks;
    private int safetyTicks;
    private boolean interacting;
    private boolean restoreUseKey;
    private int selectedAutomationSlot;
    private BlockPos pendingSafetyAnchor;
    private BlockPos safetyAnchor;
    private boolean safetyBlockPlaced;
    private BlockPos recentExplodedAnchor;
    private int recentDetonationTick;
    private BlockPos chargeTarget;
    private int targetCharges;
    private volatile BlockPos pendingPlacement;
    private volatile BlockPos rejectedPlacement;
    private int placementTick;
    private Vec3 lastExplosionPosition;
    private int lastExplosionTick;

    public AnchorController(Predicate<Object> visibleWhen) {
        autoCharge = new BooleanSetting("Auto Charge", true);
        chargeChance = new IntSetting("Charge Chance", 100, 0, 100, "%");
        charges = new FloatRangeSetting("Charges", 1.0F, 1.0F, 1.0F, 4.0F, 1.0F, " charges");
        glowstoneDelay = new FloatRangeSetting("Glowstone Delay", 0.0F, 1.0F, 0.0F, 20.0F, 1.0F, " ticks");
        glowstoneChance = new IntSetting("Glowstone Chance", 100, 0, 100, "%");
        autoExplode = new BooleanSetting("Auto Explode", true);
        explodeSlot = new IntSetting("Explode Slot", 1, 1, 9);
        explodeDelay = new FloatRangeSetting("Explode Delay", 0.0F, 1.0F, 0.0F, 20.0F, 1.0F, " ticks");
        explodeChance = new IntSetting("Explode Chance", 100, 0, 100, "%");
        safeAnchor = new BooleanSetting("Safe Anchor", false);
        safeBlockDelay = new FloatRangeSetting("Safe Block Delay", 0.0F, 1.0F, 0.0F, 20.0F, 1.0F, " ticks");
        itemWhitelist = new ItemListSetting("Item Whitelist");
        hotbarWhitelist = new HotbarSlotsSetting("Hotbar Whitelist");
        switchDelay = new FloatRangeSetting("Switch Delay", 0.0F, 1.0F, 0.0F, 20.0F, 1.0F, " ticks");
        switchChance = new IntSetting("Switch Chance", 100, 0, 100, "%");
        selectedAutomationSlot = -1;
        this.visibleWhen = visibleWhen;
        for (Item item : DEFAULT_WHITELIST) {
            itemWhitelist.setEntrySelected(item, true);
        }
        autoCharge.setVisibilityPredicate(this.visibleWhen);
        chargeChance.setVisibilityPredicate(this::showChargeChance);
        charges.setVisibilityPredicate(this::showCharges);
        glowstoneDelay.setVisibilityPredicate(this::showGlowstoneDelay);
        glowstoneChance.setVisibilityPredicate(this::showGlowstoneChance);
        autoExplode.setVisibilityPredicate(this.visibleWhen);
        explodeSlot.setVisibilityPredicate(this.visibleWhen);
        explodeDelay.setVisibilityPredicate(this::showExplodeDelay);
        explodeChance.setVisibilityPredicate(this::showExplodeChance);
        safeAnchor.setVisibilityPredicate(this.visibleWhen);
        safeBlockDelay.setVisibilityPredicate(this::showSafeBlockDelay);
        switchDelay.setVisibilityPredicate(this.visibleWhen);
        switchChance.setVisibilityPredicate(this.visibleWhen);
        itemWhitelist.setVisibilityPredicate(this.visibleWhen);
        hotbarWhitelist.setVisibilityPredicate(this.visibleWhen);
    }

    public List<Setting> getSettings() {
        return List.of(autoCharge, chargeChance, charges, glowstoneDelay, glowstoneChance,
                autoExplode, explodeSlot, explodeDelay, explodeChance, safeAnchor, safeBlockDelay,
                switchDelay, switchChance, itemWhitelist, hotbarWhitelist);
    }

    public void reset() {
        switchTicks = 0;
        glowstoneTicks = 0;
        explodeTicks = 0;
        safetyTicks = 0;
        interacting = false;
        selectedAutomationSlot = -1;
        chargeTarget = null;
        pendingPlacement = null;
        rejectedPlacement = null;
        lastExplosionPosition = null;
        resetSafety();
    }

    /** Returns whether the original item-use attempt was cancelled by this controller. */
    public boolean onItemUse(ItemUseEvent.Pre event) {
        if (event.getPlayer() != CLIENT.player || event.getHand() != InteractionHand.MAIN_HAND) {
            return false;
        }
        if (interacting || !isAllowedHeldItem()) {
            return false;
        }
        BlockHitResult hit = event.getBlockHit();
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        if (safetyAnchor != null && !hasUnchargedSafetyAnchor()) {
            resetSafety();
        }
        if (hasUnchargedSafetyAnchor() && !PlacementWorldQueries.isBlock(hit.getBlockPos(), Blocks.RESPAWN_ANCHOR)) {
            event.cancel();
            return true;
        }
        if (event.getStack().is(Items.RESPAWN_ANCHOR)
                && !PlacementWorldQueries.isBlock(hit.getBlockPos(), Blocks.RESPAWN_ANCHOR)) {
            resetDelays();
            pendingPlacement = placementPosition(hit).immutable();
            rejectedPlacement = null;
            placementTick = CLIENT.player.tickCount;
            pendingSafetyAnchor = safeAnchor.getValue() ? placementPosition(hit) : null;
            safetyAnchor = null;
            safetyBlockPlaced = false;
            return false;
        }
        if (!PlacementWorldQueries.isBlock(hit.getBlockPos(), Blocks.RESPAWN_ANCHOR)) {
            return false;
        }
        boolean needsCharge = needsMoreCharges(hit.getBlockPos());
        if (needsCharge && !autoCharge.getValue()) {
            return false;
        }
        if (!needsCharge && !autoExplode.getValue()) {
            return false;
        }
        event.cancel();
        return true;
    }

    public void onItemUseComplete(ItemUseEvent.Post event) {
        if (event.getPlayer() != CLIENT.player || event.getHand() != InteractionHand.MAIN_HAND || pendingSafetyAnchor == null) {
            return;
        }
        safetyAnchor = pendingSafetyAnchor;
        if (!safeAnchor.getValue() || !PlacementWorldQueries.isUnchargedRespawnAnchor(safetyAnchor)) {
            safetyAnchor = null;
        }
        pendingSafetyAnchor = null;
    }

    public void onBlockUpdate(ClientboundBlockUpdatePacket packet) {
        BlockPos position = packet.getPos();
        if (!position.equals(pendingPlacement) && !position.equals(rejectedPlacement)) {
            return;
        }
        boolean anchorPresent = packet.getBlockState().is(Blocks.RESPAWN_ANCHOR);
        CLIENT.execute(() -> applyBlockUpdate(position, anchorPresent));
    }

    public void onExplosion(Vec3 position) {
        CLIENT.execute(() -> rememberExplosion(position));
    }

    private boolean shouldDelayCharging(BlockPos position) {
        if (CLIENT.player.tickCount - placementTick > PLACEMENT_CONFIRMATION_TICKS) {
            pendingPlacement = null;
            rejectedPlacement = null;
            return false;
        }
        if (position.equals(rejectedPlacement)) {
            return true;
        }
        return position.equals(pendingPlacement)
                && (isNearRecentExplosion(position) || wouldIntersectMovingEntity(position));
    }

    private boolean isNearRecentExplosion(BlockPos position) {
        return lastExplosionPosition != null
                && CLIENT.player.tickCount - lastExplosionTick <= EXPLOSION_EFFECT_WINDOW_TICKS
                && lastExplosionPosition.distanceToSqr(Vec3.atCenterOf(position)) <= 36.0;
    }

    static boolean wouldIntersectMovingEntity(BlockPos position) {
        AABB placement = new AABB(position);
        for (Entity entity : CLIENT.level.getEntities(CLIENT.player, placement.inflate(3.0))) {
            if (!entity.isAlive() || entity.isSpectator()) {
                continue;
            }
            Vec3 displacement = entity.position().subtract(entity.xo, entity.yo, entity.zo);
            if (entity.getBoundingBox().inflate(1.0 + displacement.length() * 5.0).intersects(placement)) {
                return true;
            }
        }
        return false;
    }

    private void resetDelays() {
        switchTicks = 0;
        glowstoneTicks = 0;
        explodeTicks = 0;
        safetyTicks = 0;
    }

    public void onInputPre() {
        if (MinecraftRenderState.getScreen() != null || CLIENT.player == null || CLIENT.level == null) {
            return;
        }
        if (recentExplodedAnchor != null && !PlacementWorldQueries.isBlock(recentExplodedAnchor, Blocks.RESPAWN_ANCHOR)) {
            recentExplodedAnchor = null;
        }
        if (!KeybindingCache.isPressed(CLIENT.options.keyUse)) {
            selectedAutomationSlot = -1;
            resetSafety();
        } else if (safetyAnchor != null && !hasUnchargedSafetyAnchor()) {
            resetSafety();
        }
        if (hasUnchargedSafetyAnchor() && !safetyBlockPlaced
                && CLIENT.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                && !PlacementWorldQueries.isBlock(hit.getBlockPos(), Blocks.RESPAWN_ANCHOR)) {
            if (safetyTicks++ >= sampleDelay(safeBlockDelay)) {
                placeSafetyBlock(hit);
            }
            return;
        }
        switchTicks++;
        glowstoneTicks++;
        explodeTicks++;
        if (!(CLIENT.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        if (!KeybindingCache.isPressed(CLIENT.options.keyUse) || !isAllowedHeldItem()) {
            return;
        }
        if (!PlacementWorldQueries.isBlock(hit.getBlockPos(), Blocks.RESPAWN_ANCHOR)) {
            if (isHoldingSelectedGlowstone()) {
                suppressUseKey();
            }
            return;
        }
        if (hit.getBlockPos().equals(recentExplodedAnchor) && CLIENT.player.tickCount - recentDetonationTick <= DETONATION_RETRY_TICKS) {
            suppressUseKey();
            return;
        }
        suppressUseKey();
        if (needsMoreCharges(hit.getBlockPos())) {
            chargeAnchor(hit);
        } else {
            explodeAnchor(hit);
        }
    }

    private void chargeAnchor(BlockHitResult hit) {
        if (!passesChance(chargeChance.getValue())) {
            return;
        }
        if (switchTicks < sampleDelay(switchDelay)) {
            return;
        }
        if (!CLIENT.player.getMainHandItem().is(Items.GLOWSTONE)) {
            if (!passesChance(switchChance.getValue())) {
                return;
            }
            switchTicks = 0;
            InventoryItems.selectItemFromHotbar(Items.GLOWSTONE);
            selectedAutomationSlot = CLIENT.player.getInventory().getSelectedSlot();
        }
        if (!autoCharge.getValue() || !CLIENT.player.getMainHandItem().is(Items.GLOWSTONE)) {
            return;
        }
        if (glowstoneTicks < sampleDelay(glowstoneDelay) || shouldDelayCharging(hit.getBlockPos())) {
            return;
        }
        if (!passesChance(glowstoneChance.getValue())) {
            return;
        }
        glowstoneTicks = 0;
        explodeTicks = 0;
        switchTicks = 0;
        interact(hit);
    }

    private void explodeAnchor(BlockHitResult hit) {
        int slot = explodeSlot.getValue() - 1;
        if (CLIENT.player.getInventory().getSelectedSlot() != slot) {
            if (switchTicks < sampleDelay(switchDelay) || !passesChance(switchChance.getValue())) {
                return;
            }
            switchTicks = 0;
            InventoryItems.selectHotbarSlot(slot);
            selectedAutomationSlot = slot;
        }
        if (CLIENT.player.getInventory().getSelectedSlot() != slot || !autoExplode.getValue()) {
            return;
        }
        if (explodeTicks < sampleDelay(explodeDelay) || !passesChance(explodeChance.getValue())) {
            return;
        }
        explodeTicks = 0;
        interact(hit);
        recentExplodedAnchor = hit.getBlockPos();
        recentDetonationTick = CLIENT.player.tickCount;
    }

    public void onInputPost() {
        if (!restoreUseKey) {
            return;
        }
        restoreUseKey = false;
        if (MinecraftRenderState.getScreen() == null && KeybindingCache.isPressed(CLIENT.options.keyUse)) {
            CLIENT.options.keyUse.setDown(true);
        }
    }

    private void suppressUseKey() {
        restoreUseKey = true;
        CLIENT.options.keyUse.setDown(false);
    }

    private void interact(BlockHitResult hit) {
        interacting = true;
        suppressUseKey();
        PlacementWorldQueries.interactMainHand(hit, true);
        // The original has no finally: a throwing interaction leaves this flag set.
        interacting = false;
    }

    private void placeSafetyBlock(BlockHitResult hit) {
        int slot = InventoryItems.findStackInHotbar(AnchorController::isSafetyBlock);
        if (slot == -1) {
            return;
        }
        InventoryItems.selectHotbarSlot(slot);
        selectedAutomationSlot = slot;
        if (!CLIENT.player.getMainHandItem().is(Items.GLOWSTONE)) {
            return;
        }
        interacting = true;
        suppressUseKey();
        InteractionResult result = PlacementWorldQueries.interactMainHand(hit, true);
        interacting = false;
        safetyBlockPlaced = result.consumesAction();
    }

    public boolean isInteracting() {
        return interacting;
    }

    private boolean hasUnchargedSafetyAnchor() {
        return safetyAnchor != null && PlacementWorldQueries.isUnchargedRespawnAnchor(safetyAnchor);
    }

    private void resetSafety() {
        pendingSafetyAnchor = null;
        safetyAnchor = null;
        safetyBlockPlaced = false;
        safetyTicks = 0;
    }

    static BlockPos placementPosition(BlockHitResult hit) {
        return new BlockPlaceContext(CLIENT.player, InteractionHand.MAIN_HAND,
                CLIENT.player.getMainHandItem(), hit).getClickedPos();
    }

    private boolean needsMoreCharges(BlockPos position) {
        int currentCharges = CLIENT.level.getBlockState(position).getValue(RespawnAnchorBlock.CHARGE);
        if (currentCharges == 0) {
            chargeTarget = null;
            return true;
        }
        if (!autoCharge.getValue()) {
            return false;
        }
        if (!position.equals(chargeTarget)) {
            chargeTarget = position.immutable();
            int minimum = Math.round(charges.getLower());
            int maximum = Math.round(charges.getUpper());
            targetCharges = Math.clamp((long) RandomMath.randomInt(minimum, maximum + 1), 1, MAX_ANCHOR_CHARGES);
        }
        return currentCharges < targetCharges;
    }

    private boolean isHoldingSelectedGlowstone() {
        return selectedAutomationSlot != -1
                && CLIENT.player.getInventory().getSelectedSlot() == selectedAutomationSlot
                && CLIENT.player.getMainHandItem().is(Items.GLOWSTONE);
    }

    private boolean isAllowedHeldItem() {
        if (selectedAutomationSlot != -1 && CLIENT.player.getInventory().getSelectedSlot() == selectedAutomationSlot) {
            return true;
        }
        return itemWhitelist.acceptsHeldItem() && hotbarWhitelist.permitsCurrentSlot();
    }

    private static int sampleDelay(FloatRangeSetting setting) {
        return Math.max(0, Math.round(setting.sampleGaussian()));
    }

    private static boolean passesChance(int percent) {
        return percent >= 100 || RandomMath.randomInt(1, 100) <= percent;
    }

    static boolean isSafetyBlock(ItemStack stack) {
        return stack.is(Items.GLOWSTONE);
    }

    private void rememberExplosion(Vec3 position) {
        if (CLIENT.player == null) {
            return;
        }
        lastExplosionPosition = position;
        lastExplosionTick = CLIENT.player.tickCount;
    }

    private void applyBlockUpdate(BlockPos position, boolean anchorPresent) {
        if (!position.equals(pendingPlacement) && !position.equals(rejectedPlacement)) {
            return;
        }
        pendingPlacement = null;
        rejectedPlacement = anchorPresent ? null : position;
    }

    private boolean showSafeBlockDelay(Object context) { return visibleWhen.test(context) && safeAnchor.getValue(); }
    private boolean showExplodeChance(Object context) { return visibleWhen.test(context) && autoExplode.getValue(); }
    private boolean showExplodeDelay(Object context) { return visibleWhen.test(context) && autoExplode.getValue(); }
    private boolean showGlowstoneChance(Object context) { return visibleWhen.test(context) && autoCharge.getValue(); }
    private boolean showGlowstoneDelay(Object context) { return visibleWhen.test(context) && autoCharge.getValue(); }
    private boolean showCharges(Object context) { return visibleWhen.test(context) && autoCharge.getValue(); }
    private boolean showChargeChance(Object context) { return visibleWhen.test(context) && autoCharge.getValue(); }
}
