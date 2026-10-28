package dev.echo.client.features.internals;

import static dev.echo.client.bootstrap.CatalogText.literal;

import dev.echo.client.Echo;
import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.config.FeatureNameAssignments;
import dev.echo.client.events.EventBus;
import dev.echo.client.events.EventPriority;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.AttackKeyEvent;
import dev.echo.client.events.lifecycle.ClientTickEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.handlers.AttackCooldownTracker;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.setting.IntSetting;
import dev.echo.client.setting.KeybindSetting;
import dev.echo.client.setting.MultiChoiceSetting;
import dev.echo.client.utils.interaction.EntityInteractions;
import dev.echo.client.utils.items.InventoryItems;
import dev.echo.client.utils.strings.Concat;
import dev.echo.client.utils.targeting.EntityPredicates;
import dev.echo.client.utils.targeting.TargetSelection;
import java.util.Optional;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.scores.PlayerTeam;

/**
 * Global combat targeting preferences and the independently subscribed target controls.
 * Original binding, initialization and control-state applicability remain recorded
 * in the accompanying TargetsFeature recovery receipt.
 */
public class TargetsFeature extends Feature {
    private static final CharSequence SCOREBOARD = literal("Scoreboard");
    private static final CharSequence COLOR = literal("Color");
    public static final CharSequence NAME = literal("Name");
    public static final CharSequence PLAYERS = literal("Players");
    public static final CharSequence HOSTILE = literal("Hostile");
    public static final CharSequence PASSIVE = literal("Passive");
    public static final CharSequence NEUTRAL = literal("Neutral");
    public static final CharSequence ARMOR_STANDS = literal("Armor Stands");
    public static final CharSequence INVISIBLE = literal("Invisible");
    public static final CharSequence NAKED = literal("Naked");
    public static final CharSequence MOVEMENT = literal("Movement");
    public static final CharSequence TAB_LIST = literal("Tab List");
    public static final CharSequence SCALE = literal("Scale");
    public static final CharSequence BLISS = literal("Bliss");
    public static final CharSequence ZETA_MMC = literal("Zeta / MMC");
    private static final CharSequence SHIELD_TICK = literal("Tick");
    private static final CharSequence SHIELD_VANILLA = literal("Vanilla");

    // Retain original declaration order independently of metadata construction order.
    public static MultiChoiceSetting entityTypes;
    public static BooleanSetting ignoreTeams;
    public static MultiChoiceSetting teamChecks;
    public static BooleanSetting ignoreFriends;
    public static BooleanSetting antiBot;
    public static MultiChoiceSetting antiBotChecks;
    public static BooleanSetting keepTarget;
    public static BooleanSetting lastAttacked;
    public static FloatSetting keepTargetRange;
    public static KeybindSetting manualTargetSelect;
    public static KeybindSetting friendBind;
    public static ChoiceSetting shieldDetection;
    public static IntSetting shieldTicks;
    public static BooleanSetting pingCompensation;

    public static LivingEntity currentTarget = null;
    public static boolean shieldBreakActive = false;
    private boolean manualSelectWasPressed;
    private boolean friendBindWasPressed;

    /** Fixed metadata convenience constructor; settings are installed in a later phase. */
    public TargetsFeature() {
        this(new FeatureInfo(literal("Targets"), literal("Global target settings for combat modules"),
            Category.INTERNALS));
    }

    /** Allows the catalog to construct FeatureInfo before allocating this feature. */
    public TargetsFeature(FeatureInfo info) {
        super(info);
    }

    /** Publishes the catalog's fully constructed metadata vector in assignment order. */
    public static void assignBootstrapSettings(TargetsFeature feature,
            BooleanSetting preparedAntiBot, FloatSetting preparedKeepTargetRange,
            BooleanSetting preparedIgnoreFriends, BooleanSetting preparedLastAttacked,
            IntSetting preparedShieldTicks, MultiChoiceSetting preparedAntiBotChecks,
            KeybindSetting preparedManualTargetSelect, ChoiceSetting preparedShieldDetection,
            BooleanSetting preparedIgnoreTeams, MultiChoiceSetting preparedEntityTypes,
            MultiChoiceSetting preparedTeamChecks, BooleanSetting preparedKeepTarget,
            KeybindSetting preparedFriendBind, BooleanSetting preparedPingCompensation) {
        antiBot = preparedAntiBot;
        keepTargetRange = preparedKeepTargetRange;
        ignoreFriends = preparedIgnoreFriends;
        lastAttacked = preparedLastAttacked;
        shieldTicks = preparedShieldTicks;
        antiBotChecks = preparedAntiBotChecks;
        manualTargetSelect = preparedManualTargetSelect;
        shieldDetection = preparedShieldDetection;
        ignoreTeams = preparedIgnoreTeams;
        entityTypes = preparedEntityTypes;
        teamChecks = preparedTeamChecks;
        keepTarget = preparedKeepTarget;
        friendBind = preparedFriendBind;
        pingCompensation = preparedPingCompensation;
    }

    /** Reads one field at a time in the recorded declaring-class collection order. */
    public static void appendBootstrapSettings(TargetsFeature feature) {
        CatalogSettings.append(feature, entityTypes);
        CatalogSettings.append(feature, ignoreTeams);
        CatalogSettings.append(feature, teamChecks);
        CatalogSettings.append(feature, ignoreFriends);
        CatalogSettings.append(feature, antiBot);
        CatalogSettings.append(feature, antiBotChecks);
        CatalogSettings.append(feature, keepTarget);
        CatalogSettings.append(feature, lastAttacked);
        CatalogSettings.append(feature, keepTargetRange);
        CatalogSettings.append(feature, manualTargetSelect);
        CatalogSettings.append(feature, friendBind);
        CatalogSettings.append(feature, shieldDetection);
        CatalogSettings.append(feature, shieldTicks);
        CatalogSettings.append(feature, pingCompensation);
    }

    /** Returns the original shared Tick option, without allocating an equal replacement. */
    public static CharSequence bootstrapShieldDetectionOption() {
        return SHIELD_TICK;
    }

    /** Returns the original shared Vanilla option, without allocating an equal replacement. */
    public static CharSequence bootstrapShieldDetectionOption2() {
        return SHIELD_VANILLA;
    }

    @Override
    public void postInitialize() {
        antiBotChecks.setVisibilityPredicate(ignored -> antiBot.getValue());
        antiBotChecks.setSelected(literal("Name"), true);
        antiBotChecks.setSelected(literal("Movement"), true);
        antiBotChecks.setSelected(literal("Tab List"), true);
        antiBotChecks.setSelected(literal("Scale"), true);
        antiBotChecks.setSelected(literal("Bliss"), false);
        antiBotChecks.setSelected(ZETA_MMC, false);
        teamChecks.setVisibilityPredicate(ignored -> ignoreTeams.getValue());
        teamChecks.setSelected(literal("Color"), true);
        teamChecks.setSelected(literal("Name"), false);
        teamChecks.setSelected(literal("Scoreboard"), true);
        lastAttacked.setVisibilityPredicate(ignored -> keepTarget.getValue());
        shieldTicks.setVisibilityPredicate(ignored -> !usesVanillaShieldDetection());
        pingCompensation.setVisibilityPredicate(ignored -> !usesVanillaShieldDetection());
        keepTarget.onChange(TargetsFeature::onKeepTargetChanged);
        EventBus.register(this);
        entityTypes.tooltip(literal("Entity Types"));
        ignoreTeams.tooltip(literal("Teams"));
        teamChecks.tooltip(literal("Teams"));
        ignoreFriends.tooltip(literal("Friends"));
        antiBot.tooltip(literal("AntiBot"));
        antiBotChecks.tooltip(literal("AntiBot"));
        keepTarget.tooltip(literal("Keep Target"));
        lastAttacked.tooltip(literal("Keep Target"));
        keepTargetRange.tooltip(literal("Keep Target"));
        manualTargetSelect.tooltip(literal("Keep Target"));
        friendBind.tooltip(literal("Friends"));
        shieldDetection.tooltip(literal("Shield"));
        shieldTicks.tooltip(literal("Shield"));
        pingCompensation.tooltip(literal("Shield"));
    }

    /** Original toggle body is a single return: this settings service cannot be toggled. */
    @Override
    public void toggle() {
    }

    /** Original state-transition override deliberately ignores enable/disable requests. */
    @Override
    public void setEnabled(boolean requested) {
    }

    /** Listener registration is explicit in postInitialize, independent of this UI state. */
    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public boolean isKeybindable() {
        return false;
    }

    /** Reads the current choice using the actual setting's representation-aware comparison. */
    public static boolean usesVanillaShieldDetection() {
        return shieldDetection.isSelected(SHIELD_VANILLA);
    }

    private static void onKeepTargetChanged() {
        if (!keepTarget.getValue()) {
            TargetSelection.clearTargets();
        }
    }

    @Subscribe
    public void onFriendBind(ClientTickEvent event) {
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) {
            return;
        }
        boolean pressed = friendBind.getKey().isPressed();
        boolean justPressed = pressed && !friendBindWasPressed;
        friendBindWasPressed = pressed;
        if (!justPressed || Echo.friendManager == null) {
            return;
        }
        LivingEntity target = EntityInteractions.getManualSelectTarget();
        if (!(target instanceof Player player)) {
            return;
        }
        String name = player.getName().getString();
        if (Echo.friendManager.remove(name)) {
            FeatureNameAssignments.postNotification(this, Concat.strings("Removed ", name, " from friends"));
            return;
        }
        Echo.friendManager.add(name);
        FeatureNameAssignments.postNotification(this, Concat.strings("Added ", name, " to friends"));
    }

    @Subscribe(priority = EventPriority.HIGH)
    public void onManualTargetSelect(AttackKeyEvent event) {
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) {
            return;
        }
        if (!manualTargetSelect.getKey().isPressed()) {
            manualSelectWasPressed = false;
            return;
        }
        if (manualSelectWasPressed) {
            return;
        }
        manualSelectWasPressed = true;
        if (!keepTarget.getValue()) {
            return;
        }
        LivingEntity target = EntityInteractions.getManualSelectTarget();
        if (target == null || !TargetSelection.isValidTarget(target)) {
            return;
        }
        TargetSelection.selectTarget(target);
    }

    @Subscribe(priority = EventPriority.LOWEST)
    public void onShieldTarget(AttackKeyEvent event) {
        if (isOutsideWorld() || !event.hasClicks()) {
            return;
        }
        if (!(client.hitResult instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof Player player)) {
            return;
        }
        if (!EntityPredicates.isBlocking(player) || EntityPredicates.isOutsideBlockingArc(player)
                || AttackCooldownTracker.hasNonFireHurtWindow(player)
                || !TargetSelection.isValidTarget(player)) {
            return;
        }
        if (!InventoryItems.isAxe(client.player.getMainHandItem())) {
            return;
        }
        currentTarget = player;
    }

    /** Tests the configured team criteria without adding an ignoreTeams availability check. */
    public boolean isSameTeam(Entity entity) {
        if (!(entity instanceof Player player) || client.player == null || client.level == null) {
            return false;
        }
        if (teamChecks.isSelected(SCOREBOARD)) {
            PlayerTeam other = client.level.getScoreboard().getPlayerTeam(player.getScoreboardName());
            PlayerTeam local = client.level.getScoreboard().getPlayerTeam(client.player.getScoreboardName());
            if (other != null && other == local) {
                return true;
            }
        }
        if (teamChecks.isSelected(COLOR)) {
            TextColor other = getNameColor(player);
            TextColor local = getNameColor(client.player);
            if (other != null && other.equals(local)) {
                return true;
            }
        }
        if (teamChecks.isSelected(NAME)) {
            String other = bracketedNameTag(player.getDisplayName().getString());
            String local = bracketedNameTag(client.player.getDisplayName().getString());
            if (other != null && other.equals(local)) {
                return true;
            }
        }
        return false;
    }

    /** Color of the first styled display-name segment containing the scoreboard name. */
    public static TextColor getNameColor(Player player) {
        String name = player.getScoreboardName();
        return player.getDisplayName().visit((style, segment) -> {
            if (style.getColor() != null && segment.contains(name)) {
                return Optional.of(style.getColor());
            }
            return Optional.<TextColor>empty();
        }, Style.EMPTY).orElse(null);
    }

    private static String bracketedNameTag(String text) {
        int start = text.indexOf('[');
        if (start == -1) {
            return null;
        }
        int end = text.indexOf(']', start);
        return end == -1 ? null : text.substring(start, end + 1);
    }

}
