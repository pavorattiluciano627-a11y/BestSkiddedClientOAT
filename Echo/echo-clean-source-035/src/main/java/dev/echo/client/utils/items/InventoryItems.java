package dev.echo.client.utils.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

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
        PlayerInventory inventory = MinecraftClient.getInstance().player.getInventory();
        if (inventory.getSelectedSlot() != slot) {
            inventory.setSelectedSlot(slot);
        }
    }

    public static void swapHands() {
        MinecraftClient.getInstance().player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
    }

    public static boolean selectItemFromHotbar(Predicate<Item> matches) {
        PlayerInventory inventory = MinecraftClient.getInstance().player.getInventory();
        for (int slot = 0; slot < 9; slot++) {
            if (matches.test(inventory.getStack(slot).getItem())) {
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
        PlayerInventory inventory = MinecraftClient.getInstance().player.getInventory();
        for (int slot = 0; slot < 9; slot++) {
            if (matches.test(inventory.getStack(slot).getItem())) {
                return true;
            }
        }
        return false;
    }

    public static int findStackInHotbar(Predicate<ItemStack> matches) {
        PlayerInventory inventory = MinecraftClient.getInstance().player.getInventory();
        for (int slot = 0; slot <= 8; slot++) {
            if (matches.test(inventory.getStack(slot))) {
                return slot;
            }
        }
        return -1;
    }

    public static int countItems(Predicate<Item> matches) {
        PlayerInventory inventory = MinecraftClient.getInstance().player.getInventory();
        int count = 0;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (matches.test(stack.getItem())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static int countStorageItems(Predicate<Item> matches) {
        PlayerInventory inventory = MinecraftClient.getInstance().player.getInventory();
        int count = 0;
        for (int slot = 9; slot < 36; slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (matches.test(stack.getItem())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static int findAnySwordInHotbar() {
        PlayerInventory inventory = MinecraftClient.getInstance().player.getInventory();
        for (int slot = 0; slot < 9; slot++) {
            Item item = inventory.getStack(slot).getItem();
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
        return stack.getComponents().contains(DataComponentTypes.FOOD);
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
        assert MinecraftClient.getInstance().player != null;
        PlayerInventory inventory = MinecraftClient.getInstance().player.getInventory();
        for (int slot = 0; slot < 9; slot++) {
            if (inventory.getStack(slot).getItem() == Items.TOTEM_OF_UNDYING) {
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
        MinecraftClient.getInstance().player.getInventory().setSelectedSlot(slot);
        return true;
    }

    public static int findRandomStorageTotem() {
        PlayerInventory inventory = MinecraftClient.getInstance().player.getInventory();
        Random random = new Random();
        List<Integer> slots = new ArrayList<>();
        for (int slot = 9; slot < 36; slot++) {
            if (inventory.getStack(slot).getItem() == Items.TOTEM_OF_UNDYING) {
                slots.add(slot);
            }
        }
        return slots.isEmpty() ? -1 : slots.get(random.nextInt(slots.size()));
    }

    public static int findSplashPotion(String effectText) {
        PlayerInventory inventory = MinecraftClient.getInstance().player.getInventory();
        Random random = new Random();
        int start = random.nextInt(27) + 9;
        for (int offset = 0; offset < 27; offset++) {
            // Original wraps modulo 36, so a search can reach hotbar slots.
            int slot = (start + offset) % 36;
            ItemStack stack = inventory.getStack(slot);
            if (stack.getItem() instanceof SplashPotionItem) {
                // A nonmatching first potion ends the search in the artifact.
                return stack.get(DataComponentTypes.POTION_CONTENTS).getEffects().toString().contains(effectText) ? slot : -1;
            }
        }
        return -1;
    }

    public static List<Integer> emptyHotbarSlots() {
        PlayerInventory inventory = MinecraftClient.getInstance().player.getInventory();
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < 9; slot++) {
            if (inventory.getStack(slot).isEmpty()) {
                slots.add(slot);
            }
        }
        return slots;
    }

    public static int findAxeInHotbar() {
        PlayerInventory inventory = MinecraftClient.getInstance().player.getInventory();
        for (int slot = 0; slot < 9; slot++) {
            if (isAxe(inventory.getStack(slot))) {
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
        return isChestplate(MinecraftClient.getInstance().player.getMainHandStack().getItem());
    }

    public static boolean holdsChestplateInOffHand() {
        return isChestplate(MinecraftClient.getInstance().player.getOffHandStack().getItem());
    }

    public static int findChestplateInHotbar() {
        PlayerInventory inventory = MinecraftClient.getInstance().player.getInventory();
        for (int slot = 0; slot < 9; slot++) {
            if (isChestplate(inventory.getStack(slot).getItem())) {
                return slot;
            }
        }
        return -1;
    }

    public static boolean wearsElytra() {
        return MinecraftClient.getInstance().player.getInventory().getStack(38).getItem() == Items.ELYTRA;
    }

    public static ItemStack mainHandStack() {
        return MinecraftClient.getInstance().player.getMainHandStack();
    }

    public static boolean holdsInMainHand(Item item) {
        return MinecraftClient.getInstance().player.getMainHandStack().getItem() == item;
    }

    public static boolean holdsInEitherHand(Item item) {
        return holdsInMainHand(item) || holdsInOffHand(item);
    }

    public static boolean holdsInOffHand(Item item) {
        return MinecraftClient.getInstance().player.getOffHandStack().getItem() == item;
    }

    public static boolean isChargedCrossbow(ItemStack stack) {
        return stack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(stack);
    }

    public static boolean isRail(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof AbstractRailBlock;
    }

    public static boolean holdsSword() {
        Item item = MinecraftClient.getInstance().player.getMainHandStack().getItem();
        return item == Items.WOODEN_SWORD || item == Items.STONE_SWORD || item == Items.COPPER_SWORD
                || item == Items.IRON_SWORD || item == Items.GOLDEN_SWORD || item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD;
    }

    public static int findSwordInHotbar() {
        return findStackInHotbar(stack -> isLegacySword(stack.getItem()));
    }

    public static boolean holdsSpear() {
        return isSpear(MinecraftClient.getInstance().player.getMainHandStack().getItem());
    }

    public static int findSpearInHotbar() {
        return findStackInHotbar(stack -> isSpear(stack.getItem()));
    }

    public static boolean isSpear(Item item) {
        return item == Items.WOODEN_SPEAR || item == Items.STONE_SPEAR || item == Items.COPPER_SPEAR
                || item == Items.IRON_SPEAR || item == Items.GOLDEN_SPEAR || item == Items.DIAMOND_SPEAR || item == Items.NETHERITE_SPEAR;
    }

    public static boolean isAxe(ItemStack stack) {
        return stack.isIn(ItemTags.AXES);
    }

    public static boolean holdsAxe() {
        Item item = MinecraftClient.getInstance().player.getMainHandStack().getItem();
        return item == Items.WOODEN_AXE || item == Items.STONE_AXE || item == Items.COPPER_AXE
                || item == Items.IRON_AXE || item == Items.GOLDEN_AXE || item == Items.DIAMOND_AXE || item == Items.NETHERITE_AXE;
    }

    public static boolean holdsPickaxe() {
        Item item = MinecraftClient.getInstance().player.getMainHandStack().getItem();
        return item == Items.WOODEN_PICKAXE || item == Items.STONE_PICKAXE || item == Items.COPPER_PICKAXE
                || item == Items.IRON_PICKAXE || item == Items.GOLDEN_PICKAXE || item == Items.DIAMOND_PICKAXE || item == Items.NETHERITE_PICKAXE;
    }

    public static boolean holdsShovel() {
        Item item = MinecraftClient.getInstance().player.getMainHandStack().getItem();
        return item == Items.WOODEN_SHOVEL || item == Items.STONE_SHOVEL || item == Items.COPPER_SHOVEL
                || item == Items.IRON_SHOVEL || item == Items.GOLDEN_SHOVEL || item == Items.DIAMOND_SHOVEL || item == Items.NETHERITE_SHOVEL;
    }

    public static boolean holdsHoe() {
        Item item = MinecraftClient.getInstance().player.getMainHandStack().getItem();
        return item == Items.WOODEN_HOE || item == Items.STONE_HOE || item == Items.COPPER_HOE
                || item == Items.IRON_HOE || item == Items.GOLDEN_HOE || item == Items.DIAMOND_HOE || item == Items.NETHERITE_HOE;
    }

    public static boolean hasEnchantment(ItemStack stack, RegistryKey<Enchantment> enchantment) {
        return stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT)
                .getEnchantments().stream().anyMatch(entry -> entry.matchesKey(enchantment));
    }

    public static int getEnchantmentLevel(ItemStack stack, RegistryKey<Enchantment> enchantment) {
        ItemEnchantmentsComponent enchantments = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        for (RegistryEntry<Enchantment> entry : enchantments.getEnchantments()) {
            if (entry.matchesKey(enchantment)) {
                return enchantments.getLevel(entry);
            }
        }
        return 0;
    }

    public static int findItemWithEnchantmentInHotbar(Predicate<ItemStack> matches,
                                                     RegistryKey<Enchantment> enchantment, boolean allowFallback) {
        if (enchantment != null) {
            int slot = findStackInHotbar(stack -> !stack.isEmpty() && matches.test(stack) && hasEnchantment(stack, enchantment));
            if (slot != -1) {
                return slot;
            }
        }
        return allowFallback ? findStackInHotbar(stack -> !stack.isEmpty() && matches.test(stack)) : -1;
    }

    public static int findMaceWithEnchantmentInHotbar(RegistryKey<Enchantment> enchantment, boolean allowFallback) {
        return findItemWithEnchantmentInHotbar(stack -> stack.getItem() instanceof MaceItem, enchantment, allowFallback);
    }

    public static void sendSelectedSlot(int slot) {
        MinecraftClient.getInstance().player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    public static void sendCurrentSlot() {
        MinecraftClient.getInstance().player.networkHandler.sendPacket(
                new UpdateSelectedSlotC2SPacket(MinecraftClient.getInstance().player.getInventory().getSelectedSlot()));
    }
}
