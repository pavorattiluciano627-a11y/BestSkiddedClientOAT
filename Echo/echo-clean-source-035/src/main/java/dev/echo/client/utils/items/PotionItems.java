package dev.echo.client.utils.items;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/**
 * Searches splash-potion registry entries in the hotbar and offhand.
 * Recovery candidate: original binding, initialization and guard domains are
 * recorded in the accompanying README and member receipt.
 */
public class PotionItems {
    /** Preserves the original public, stateless constructor. */
    public PotionItems() {}

    /** Returns the first matching hotbar index, or -1; visits slots 0 through 8. */
    public static int findPotion(Predicate<Holder<Potion>> predicate) {
        Inventory inventory = Minecraft.getInstance().player.getInventory();
        for (int slot = 0; slot <= 8; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() != Items.SPLASH_POTION) {
                continue;
            }
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents == null) {
                continue;
            }
            Optional<Holder<Potion>> potion = contents.potion();
            if (potion.isPresent() && predicate.test(potion.get())) {
                return slot;
            }
        }
        return -1;
    }

    public static int findNightVisionPotion() {
        return findPotion(potion -> potion == Potions.NIGHT_VISION
                || potion == Potions.LONG_NIGHT_VISION);
    }

    public static int findInvisibilityPotion() {
        return findPotion(potion -> potion == Potions.INVISIBILITY
                || potion == Potions.LONG_INVISIBILITY);
    }

    public static int findJumpBoostPotion() {
        return findPotion(potion -> potion == Potions.LONG_LEAPING
                || potion == Potions.LEAPING);
    }

    public static int findFireResistancePotion() {
        return findPotion(potion -> potion == Potions.FIRE_RESISTANCE
                || potion == Potions.LONG_FIRE_RESISTANCE);
    }

    public static int findSpeedPotion() {
        return findPotion(potion -> potion == Potions.STRONG_SWIFTNESS
                || potion == Potions.SWIFTNESS);
    }

    public static int findSlownessPotion() {
        return findPotion(potion -> potion == Potions.STRONG_SLOWNESS
                || potion == Potions.SLOWNESS || potion == Potions.LONG_SLOWNESS);
    }

    public static int findTurtleMasterPotion() {
        return findPotion(potion -> potion == Potions.TURTLE_MASTER
                || potion == Potions.LONG_TURTLE_MASTER || potion == Potions.STRONG_TURTLE_MASTER);
    }

    public static int findWaterBreathingPotion() {
        return findPotion(potion -> potion == Potions.WATER_BREATHING
                || potion == Potions.LONG_WATER_BREATHING);
    }

    public static int findHealingPotion() {
        return findPotion(potion -> potion == Potions.STRONG_HEALING || potion == Potions.HEALING);
    }

    public static int findHarmingPotion() {
        return findPotion(potion -> potion == Potions.STRONG_HARMING || potion == Potions.HARMING);
    }

    public static int findPoisonPotion() {
        return findPotion(potion -> potion == Potions.STRONG_POISON
                || potion == Potions.POISON || potion == Potions.LONG_POISON);
    }

    public static int findStrengthPotion() {
        return findPotion(potion -> potion == Potions.STRONG_STRENGTH || potion == Potions.STRENGTH);
    }

    public static int findRegenerationPotion() {
        return findPotion(potion -> potion == Potions.REGENERATION
                || potion == Potions.LONG_REGENERATION || potion == Potions.STRONG_REGENERATION);
    }

    public static int findWeaknessPotion() {
        return findPotion(potion -> potion == Potions.WEAKNESS || potion == Potions.LONG_WEAKNESS);
    }

    public static int findLuckPotion() {
        return findPotion(potion -> potion == Potions.LUCK);
    }

    public static int findSlowFallingPotion() {
        return findPotion(potion -> potion == Potions.SLOW_FALLING
                || potion == Potions.LONG_SLOW_FALLING);
    }

    public static int findWindChargedPotion() {
        return findPotion(potion -> potion == Potions.WIND_CHARGED);
    }

    public static int findWeavingPotion() {
        return findPotion(potion -> potion == Potions.WEAVING);
    }

    public static int findOozingPotion() {
        return findPotion(potion -> potion == Potions.OOZING);
    }

    public static int findInfestedPotion() {
        return findPotion(potion -> potion == Potions.INFESTED);
    }

    /** Tests only the offhand stack's base potion entry, not custom status effects. */
    public static boolean holdsPotion(Predicate<Holder<Potion>> predicate) {
        ItemStack stack = Minecraft.getInstance().player.getOffhandItem();
        if (stack.getItem() != Items.SPLASH_POTION) {
            return false;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return false;
        }
        Optional<Holder<Potion>> potion = contents.potion();
        return potion.isPresent() && predicate.test(potion.get());
    }

    public static boolean holdsHealingPotion() {
        return holdsPotion(potion -> potion == Potions.STRONG_HEALING || potion == Potions.HEALING);
    }

    public static boolean holdsSpeedPotion() {
        return holdsPotion(potion -> potion == Potions.STRONG_SWIFTNESS || potion == Potions.SWIFTNESS);
    }

    public static boolean holdsStrengthPotion() {
        return holdsPotion(potion -> potion == Potions.STRONG_STRENGTH || potion == Potions.STRENGTH);
    }
}
