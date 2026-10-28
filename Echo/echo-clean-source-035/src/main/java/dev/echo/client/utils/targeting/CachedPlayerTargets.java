package dev.echo.client.utils.targeting;

import static dev.echo.client.MinecraftAccess.CLIENT;

import dev.echo.client.features.internals.TargetsFeature;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Original per-player/per-age bot cache and ordered stateful classifier chain.
 * Key, string, sentinel and shared-cache applicability remain separately gated.
 */
public final class CachedPlayerTargets {
    private static final List<PlayerRule> RULES = List.of(
            new NameRule(), new BlissRule(), new MovementRule(),
            new ScaleRule(), new PlayerListRule(), new TextureProfileRule());
    private static final Map<PlayerEntity, Long> RESULTS = new WeakHashMap<>();

    private CachedPlayerTargets() {}

    public static boolean isBot(PlayerEntity player, TargetsFeature targets) {
        Long cached = RESULTS.get(player);
        if (cached != null && (cached >> 1) == (long) player.age) {
            return (cached & 1L) != 0L;
        }
        boolean result = classify(player, targets);
        RESULTS.put(player, ((long) player.age << 1) | (result ? 1L : 0L));
        return result;
    }

    private static boolean classify(PlayerEntity player, TargetsFeature targets) {
        for (PlayerRule rule : RULES) {
            if (rule.isBot(player, targets)) {
                return true;
            }
        }
        return false;
    }

    /** Inlined source contracts for the six original QueueFactory implementations. */
    private interface PlayerRule {
        CharSequence name();
        boolean isBot(PlayerEntity player, TargetsFeature targets);
    }

    // Original RecordController. Default-locale conversion and double name read are deliberate.
    private static final class NameRule implements PlayerRule {
        public CharSequence name() { return TargetsFeature.NAME; }

        public boolean isBot(PlayerEntity player, TargetsFeature targets) {
            if (!TargetsFeature.antiBotChecks.isSelected(name())) {
                return false;
            }
            String name = normalizedDisplayName(player);
            return name.isEmpty() || name.contains(":") || name.contains("+") || name.contains(" ")
                    || name.startsWith("cit") || name.startsWith("npc");
        }

        private String normalizedDisplayName(PlayerEntity player) {
            if (player.getDisplayName() == null) {
                return "";
            }
            String name = player.getDisplayName().getString();
            return name == null ? "" : name.toLowerCase();
        }
    }

    // Original SessionStore. Its gliding exemption and transient air/fall state differ.
    private static final class BlissRule implements PlayerRule {
        private static final int REQUIRED_AIRBORNE_CHECKS = 2;
        private final Map<UUID, Integer> airborneChecks = new HashMap<>();
        private final Set<UUID> seenFalling = new HashSet<>();
        private final Set<UUID> seenGliding = new HashSet<>();

        public CharSequence name() { return TargetsFeature.BLISS; }

        public boolean isBot(PlayerEntity player, TargetsFeature targets) {
            if (!TargetsFeature.antiBotChecks.isSelected(name())) {
                resetAll(player);
                return false;
            }
            if (player.isGliding()) {
                seenGliding.add(player.getUuid());
            }
            if (seenGliding.contains(player.getUuid())) {
                resetTransient(player);
                return false;
            }
            if (!player.isAlive() || player.isRemoved()) {
                resetAll(player);
                return false;
            }
            if (!hasAnyArmor(player)) {
                resetAll(player);
                return false;
            }
            if (player.isOnGround()) {
                resetAll(player);
                return false;
            }
            UUID id = player.getUuid();
            if (player.fallDistance > 0.0) {
                seenFalling.add(id);
            }
            int count = airborneChecks.getOrDefault(id, 0) + 1;
            airborneChecks.put(id, count);
            if (count < REQUIRED_AIRBORNE_CHECKS) {
                return false;
            }
            return !seenFalling.contains(id);
        }

        private boolean hasAnyArmor(PlayerEntity player) {
            return !player.getEquippedStack(EquipmentSlot.HEAD).isEmpty()
                    || !player.getEquippedStack(EquipmentSlot.CHEST).isEmpty()
                    || !player.getEquippedStack(EquipmentSlot.LEGS).isEmpty()
                    || !player.getEquippedStack(EquipmentSlot.FEET).isEmpty();
        }

        private void resetAll(PlayerEntity player) {
            UUID id = player.getUuid();
            resetTransient(player);
            seenGliding.remove(id);
        }

        private void resetTransient(PlayerEntity player) {
            UUID id = player.getUuid();
            airborneChecks.remove(id);
            seenFalling.remove(id);
        }
    }

    // Original LayoutController. Disabling the check does not clear its UUID set.
    private static final class MovementRule implements PlayerRule {
        private final Set<UUID> movedPlayers = new HashSet<>();

        public CharSequence name() { return TargetsFeature.MOVEMENT; }

        public boolean isBot(PlayerEntity player, TargetsFeature targets) {
            if (!TargetsFeature.antiBotChecks.isSelected(name())) {
                return false;
            }
            return !hasMoved(player);
        }

        private boolean hasMoved(PlayerEntity player) {
            if (CLIENT.getNetworkHandler() == null) {
                return false;
            }
            if (player.lastX != player.getX() || player.lastZ != player.getZ()) {
                movedPlayers.add(player.getUuid());
            }
            return movedPlayers.contains(player.getUuid());
        }
    }

    // Original TemplateHandler. Inclusive scale endpoints are accepted; NaN is not rejected.
    private static final class ScaleRule implements PlayerRule {
        public CharSequence name() { return TargetsFeature.SCALE; }

        public boolean isBot(PlayerEntity player, TargetsFeature targets) {
            if (!TargetsFeature.antiBotChecks.isSelected(name())) {
                return false;
            }
            double scale = player.getAttributeValue(EntityAttributes.SCALE);
            return scale < 0.75 || scale > 1.25;
        }
    }

    // Original BindingStrategy. The second network-handler lookup stays separate.
    private static final class PlayerListRule implements PlayerRule {
        public CharSequence name() { return TargetsFeature.TAB_LIST; }

        public boolean isBot(PlayerEntity player, TargetsFeature targets) {
            if (!TargetsFeature.antiBotChecks.isSelected(name())) {
                return false;
            }
            if (CLIENT.getNetworkHandler() == null) {
                return false;
            }
            return CLIENT.getNetworkHandler().getPlayerListEntry(player.getUuid()) == null;
        }
    }

    // Original AttributeMapper. Only the original high-ID texture-property test is applied.
    private static final class TextureProfileRule implements PlayerRule {
        private static final int MINIMUM_ENTITY_ID = 200000;

        public CharSequence name() { return TargetsFeature.ZETA_MMC; }

        public boolean isBot(PlayerEntity player, TargetsFeature targets) {
            if (!TargetsFeature.antiBotChecks.isSelected(name())) {
                return false;
            }
            if (player.getId() < MINIMUM_ENTITY_ID) {
                return false;
            }
            return !player.getGameProfile().properties().containsKey("textures");
        }
    }
}
