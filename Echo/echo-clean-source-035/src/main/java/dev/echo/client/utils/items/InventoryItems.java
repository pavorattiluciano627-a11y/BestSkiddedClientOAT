package dev.echo.client.utils.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.BaseRailBlock;

/**
 * Readable inventory operations recovered under the non-null guard branch.
 * This candidate preserves the artifact's differing sword sets, potion early exit,
 * slot bounds, and local-selection versus packet-selection distinction.
 */
public final class InventoryItems {
    public InventoryItems() {}

    public static void selectHotbarSlot(int slot) {
        if (slot < 0 || slot > 8) {
            return;
        }
        Inventory inventory = Minecraft.getInstance().player.getInventory();
        if (inventory.getSelectedSlot() != slot) {
            inventory.setSelectedSlot(slot);
        }
    }

    public static void swapHands() {
        Minecraft.getInstance().player.connection.send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
    }

    public static boolean selectItemFromHotbar(Predicate<Item> matches) {
        Inventory inventory = Minecraft.getInstance().player.getInventory();
        for (int slot = 0; slot < 9; slot++) {
            if (matches.test(inventory.getItem(slot).getItem())) {
                inventory.setSelectedSlot(slot);
                return true;
            }
        }
        return false;
    }

    public static boolean selectItemFromHotbar(Item item) {
        return selectItemFromHotbar(candidate -> candidate == item);
    }

    public static boolean containsItemInHotbar(Predicate<Item> matches) {
        Inventory inventory = Minecraft.getInstance().player.getInventory();
        for (int slot = 0; slot < 9; slot++) {
            if (matches.test(inventory.getItem(slot).getItem())) {
                return true;
            }
        }
        return false;
    }

    public static int findStackInHotbar(Predicate<ItemStack> matches) {
        Inventory inventory = Minecraft.getInstance().player.getInventory();
        for (int slot = 0; slot <= 8; slot++) {
            if (matches.test(inventory.getItem(slot))) {
                return slot;
            }
        }
        return -1;
    }

    public static int countItems(Predicate<Item> matches) {
        Inventory inventory = Minecraft.getInstance().player.getInventory();
        int count = 0;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (matches.test(stack.getItem())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static int countStorageItems(Predicate<Item> matches) {
        Inventory inventory = Minecraft.getInstance().player.getInventory();
        int count = 0;
        for (int slot = 9; slot < 36; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (matches.test(stack.getItem())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static int findAnySwordInHotbar() {
        Inventory inventory = Minecraft.getInstance().player.getInventory();
        for (int slot = 0; slot < 9; slot++) {
            Item item = inventory.getItem(slot).getItem();
            if (isLegacySword(item) || item == Items.COPPER_SWORD) {
                return slot;
            }
        }
        return -1;
    }

    public static boolean isLegacySword(Item item) {
        return item == Items.WOODEN_SWORD || item == Items.STONE_SWORD || item == Items.IRON_SWORD
                || item == Items.GOLDEN_SWORD || item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD;
    }

    public static boolean isFood(ItemStack stack) {
        return stack.getComponents().has(DataComponents.FOOD);
    }

    public static boolean selectSword() {
        int slot = findAnySwordInHotbar();
        if (slot == -1) {
            return false;
        }
        selectHotbarSlot(slot);
        return true;
    }

    public static int findTotemInHotbar() {
        assert Minecraft.getInstance().player != null;
        Inventory inventory = Minecraft.getInstance().player.getInventory();
        for (int slot = 0; slot < 9; slot++) {
            if (inventory.getItem(slot).getItem() == Items.TOTEM_OF_UNDYING) {
                return slot;
            }
        }
        return -1;
    }

    public static boolean selectAxe() {
        int slot = findAxeInHotbar();
        if (slot == -1) {
            return false;
        }
        Minecraft.getInstance().player.getInventory().setSelectedSlot(slot);
        return true;
    }

    public static int findRandomStorageTotem() {
        Inventory inventory = Minecraft.getInstance().player.getInventory();
        Random random = new Random();
        List<Integer> slots = new ArrayList<>();
        for (int slot = 9; slot < 36; slot++) {
            if (inventory.getItem(slot).getItem() == Items.TOTEM_OF_UNDYING) {
                slots.add(slot);
            }
        }
        return slots.isEmpty() ? -1 : slots.get(random.nextInt(slots.size()));
    }

    public static int findSplashPotion(String effectText) {
        Inventory inventory = Minecraft.getInstance().player.getInventory();
        Random random = new Random();
        int start = random.nextInt(27) + 9;
        for (int offset = 0; offset < 27; offset++) {
            // Original wraps modulo 36, so a search can reach hotbar slots.
            int slot = (start + offset) % 36;
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof SplashPotionItem) {
                // A nonmatching first potion ends the search in the artifact.
                return stack.get(DataComponents.POTION_CONTENTS).getAllEffects().toString().contains(effectText) ? slot : -1;
            }
        }
        return -1;
    }

    public static List<Integer> emptyHotbarSlots() {
        Inventory inventory = Minecraft.getInstance().player.getInventory();
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < 9; slot++) {
            if (inventory.getItem(slot).isEmpty()) {
                slots.add(slot);
            }
        }
        return slots;
    }

    public static int findAxeInHotbar() {
        Inventory inventory = Minecraft.getInstance().player.getInventory();
        for (int slot = 0; slot < 9; slot++) {
            if (isAxe(inventory.getItem(slot))) {
                return slot;
            }
        }
        return -1;
    }

    public static int countItem(Item item) {
        return countItems(candidate -> candidate == item);
    }

    private static boolean isChestplate(Item item) {
        return item == Items.NETHERITE_CHESTPLATE || item == Items.CHAINMAIL_CHESTPLATE
                || item == Items.COPPER_CHESTPLATE || item == Items.IRON_CHESTPLATE
                || item == Items.GOLDEN_CHESTPLATE || item == Items.DIAMOND_CHESTPLATE || item == Items.LEATHER_CHESTPLATE;
    }

    public static boolean holdsChestplateInMainHand() {
        return isChestplate(Minecraft.getInstance().player.getMainHandItem().getItem());
    }

    public static boolean holdsChestplateInOffHand() {
        return isChestplate(Minecraft.getInstance().player.getOffhandItem().getItem());
    }

    public static int findChestplateInHotbar() {
        Inventory inventory = Minecraft.getInstance().player.getInventory();
        for (int slot = 0; slot < 9; slot++) {
            if (isChestplate(inventory.getItem(slot).getItem())) {
                return slot;
            }
        }
        return -1;
    }

    public static boolean wearsElytra() {
        return Minecraft.getInstance().player.getInventory().getItem(38).getItem() == Items.ELYTRA;
    }

    public static ItemStack mainHandStack() {
        return Minecraft.getInstance().player.getMainHandItem();
    }

    public static boolean holdsInMainHand(Item item) {
        return Minecraft.getInstance().player.getMainHandItem().getItem() == item;
    }

    public static boolean holdsInEitherHand(Item item) {
        return holdsInMainHand(item) || holdsInOffHand(item);
    }

    public static boolean holdsInOffHand(Item item) {
        return Minecraft.getInstance().player.getOffhandItem().getItem() == item;
    }

    public static boolean isChargedCrossbow(ItemStack stack) {
        return stack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(stack);
    }

    public static boolean isRail(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof BaseRailBlock;
    }

    public static boolean holdsSword() {
        Item item = Minecraft.getInstance().player.getMainHandItem().getItem();
        return item == Items.WOODEN_SWORD || item == Items.STONE_SWORD || item == Items.COPPER_SWORD
                || item == Items.IRON_SWORD || item == Items.GOLDEN_SWORD || item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD;
    }

    public static int findSwordInHotbar() {
        return findStackInHotbar(stack -> isLegacySword(stack.getItem()));
    }

    public static boolean holdsSpear() {
        return isSpear(Minecraft.getInstance().player.getMainHandItem().getItem());
    }

    public static int findSpearInHotbar() {
        return findStackInHotbar(stack -> isSpear(stack.getItem()));
    }

    public static boolean isSpear(Item item) {
        return item == Items.WOODEN_SPEAR || item == Items.STONE_SPEAR || item == Items.COPPER_SPEAR
                || item == Items.IRON_SPEAR || item == Items.GOLDEN_SPEAR || item == Items.DIAMOND_SPEAR || item == Items.NETHERITE_SPEAR;
    }

    public static boolean isAxe(ItemStack stack) {
        return stack.is(ItemTags.AXES);
    }

    public static boolean holdsAxe() {
        Item item = Minecraft.getInstance().player.getMainHandItem().getItem();
        return item == Items.WOODEN_AXE || item == Items.STONE_AXE || item == Items.COPPER_AXE
                || item == Items.IRON_AXE || item == Items.GOLDEN_AXE || item == Items.DIAMOND_AXE || item == Items.NETHERITE_AXE;
    }

    public static boolean holdsPickaxe() {
        Item item = Minecraft.getInstance().player.getMainHandItem().getItem();
        return item == Items.WOODEN_PICKAXE || item == Items.STONE_PICKAXE || item == Items.COPPER_PICKAXE
                || item == Items.IRON_PICKAXE || item == Items.GOLDEN_PICKAXE || item == Items.DIAMOND_PICKAXE || item == Items.NETHERITE_PICKAXE;
    }

    public static boolean holdsShovel() {
        Item item = Minecraft.getInstance().player.getMainHandItem().getItem();
        return item == Items.WOODEN_SHOVEL || item == Items.STONE_SHOVEL || item == Items.COPPER_SHOVEL
                || item == Items.IRON_SHOVEL || item == Items.GOLDEN_SHOVEL || item == Items.DIAMOND_SHOVEL || item == Items.NETHERITE_SHOVEL;
    }

    public static boolean holdsHoe() {
        Item item = Minecraft.getInstance().player.getMainHandItem().getItem();
        return item == Items.WOODEN_HOE || item == Items.STONE_HOE || item == Items.COPPER_HOE
                || item == Items.IRON_HOE || item == Items.GOLDEN_HOE || item == Items.DIAMOND_HOE || item == Items.NETHERITE_HOE;
    }

    public static boolean hasEnchantment(ItemStack stack, ResourceKey<Enchantment> enchantment) {
        return stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
                .keySet().stream().anyMatch(entry -> entry.is(enchantment));
    }

    public static int getEnchantmentLevel(ItemStack stack, ResourceKey<Enchantment> enchantment) {
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Holder<Enchantment> entry : enchantments.keySet()) {
            if (entry.is(enchantment)) {
                return enchantments.getLevel(entry);
            }
        }
        return 0;
    }

    public static int findItemWithEnchantmentInHotbar(Predicate<ItemStack> matches,
                                                     ResourceKey<Enchantment> enchantment, boolean allowFallback) {
        if (enchantment != null) {
            int slot = findStackInHotbar(stack -> !stack.isEmpty() && matches.test(stack) && hasEnchantment(stack, enchantment));
            if (slot != -1) {
                return slot;
            }
        }
        return allowFallback ? findStackInHotbar(stack -> !stack.isEmpty() && matches.test(stack)) : -1;
    }

    public static int findMaceWithEnchantmentInHotbar(ResourceKey<Enchantment> enchantment, boolean allowFallback) {
        return findItemWithEnchantmentInHotbar(stack -> stack.getItem() instanceof MaceItem, enchantment, allowFallback);
    }

    public static void sendSelectedSlot(int slot) {
        Minecraft.getInstance().player.connection.send(new ServerboundSetCarriedItemPacket(slot));
    }

    public static void sendCurrentSlot() {
        Minecraft.getInstance().player.connection.send(
                new ServerboundSetCarriedItemPacket(Minecraft.getInstance().player.getInventory().getSelectedSlot()));
    }
}
