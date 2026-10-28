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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Original per-player/per-age bot cache and ordered stateful classifier chain.
 * Key, string, sentinel and shared-cache applicability remain separately gated.
 */
public final class CachedPlayerTargets {
    private static final List<PlayerRule> RULES = List.of(
            new NameRule(), new BlissRule(), new MovementRule(),
            new ScaleRule(), new PlayerListRule(), new TextureProfileRule());
    private static final Map<Player, Long> RESULTS = new WeakHashMap<>();

    private CachedPlayerTargets() {}

    public static boolean isBot(Player player, TargetsFeature targets) {
        Long cached = RESULTS.get(player);
        if (cached != null && (cached >> 1) == (long) player.tickCount) {
            return (cached & 1L) != 0L;
        }
        boolean result = classify(player, targets);
        RESULTS.put(player, ((long) player.tickCount << 1) | (result ? 1L : 0L));
        return result;
    }

    private static boolean classify(Player player, TargetsFeature targets) {
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
        boolean isBot(Player player, TargetsFeature targets);
    }

    // Original RecordController. Default-locale conversion and double name read are deliberate.
    private static final class NameRule implements PlayerRule {
        public CharSequence name() { return TargetsFeature.NAME; }

        public boolean isBot(Player player, TargetsFeature targets) {
            if (!TargetsFeature.antiBotChecks.isSelected(name())) {
                return false;
            }
            String name = normalizedDisplayName(player);
            return name.isEmpty() || name.contains(":") || name.contains("+") || name.contains(" ")
                    || name.startsWith("cit") || name.startsWith("npc");
        }

        private String normalizedDisplayName(Player player) {
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

        public boolean isBot(Player player, TargetsFeature targets) {
            if (!TargetsFeature.antiBotChecks.isSelected(name())) {
                resetAll(player);
                return false;
            }
            if (player.isFallFlying()) {
                seenGliding.add(player.getUUID());
            }
            if (seenGliding.contains(player.getUUID())) {
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
            if (player.onGround()) {
                resetAll(player);
                return false;
            }
            UUID id = player.getUUID();
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

        private boolean hasAnyArmor(Player player) {
            return !player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
                    || !player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                    || !player.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
                    || !player.getItemBySlot(EquipmentSlot.FEET).isEmpty();
        }

        private void resetAll(Player player) {
            UUID id = player.getUUID();
            resetTransient(player);
            seenGliding.remove(id);
        }

        private void resetTransient(Player player) {
            UUID id = player.getUUID();
            airborneChecks.remove(id);
            seenFalling.remove(id);
        }
    }

    // Original LayoutController. Disabling the check does not clear its UUID set.
    private static final class MovementRule implements PlayerRule {
        private final Set<UUID> movedPlayers = new HashSet<>();

        public CharSequence name() { return TargetsFeature.MOVEMENT; }

        public boolean isBot(Player player, TargetsFeature targets) {
            if (!TargetsFeature.antiBotChecks.isSelected(name())) {
                return false;
            }
            return !hasMoved(player);
        }

        private boolean hasMoved(Player player) {
            if (CLIENT.getConnection() == null) {
                return false;
            }
            if (player.xo != player.getX() || player.zo != player.getZ()) {
                movedPlayers.add(player.getUUID());
            }
            return movedPlayers.contains(player.getUUID());
        }
    }

    // Original TemplateHandler. Inclusive scale endpoints are accepted; NaN is not rejected.
    private static final class ScaleRule implements PlayerRule {
        public CharSequence name() { return TargetsFeature.SCALE; }

        public boolean isBot(Player player, TargetsFeature targets) {
            if (!TargetsFeature.antiBotChecks.isSelected(name())) {
                return false;
            }
            double scale = player.getAttributeValue(Attributes.SCALE);
            return scale < 0.75 || scale > 1.25;
        }
    }

    // Original BindingStrategy. The second network-handler lookup stays separate.
    private static final class PlayerListRule implements PlayerRule {
        public CharSequence name() { return TargetsFeature.TAB_LIST; }

        public boolean isBot(Player player, TargetsFeature targets) {
            if (!TargetsFeature.antiBotChecks.isSelected(name())) {
                return false;
            }
            if (CLIENT.getConnection() == null) {
                return false;
            }
            return CLIENT.getConnection().getPlayerInfo(player.getUUID()) == null;
        }
    }

    // Original AttributeMapper. Only the original high-ID texture-property test is applied.
    private static final class TextureProfileRule implements PlayerRule {
        private static final int MINIMUM_ENTITY_ID = 200000;

        public CharSequence name() { return TargetsFeature.ZETA_MMC; }

        public boolean isBot(Player player, TargetsFeature targets) {
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
