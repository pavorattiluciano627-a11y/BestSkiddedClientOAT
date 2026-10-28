package dev.echo.client.hud;

import com.mojang.blaze3d.textures.GpuTextureView;
import dev.echo.client.Echo;
import dev.echo.client.config.FeatureBindSnapshot;
import dev.echo.client.config.FeatureNameAssignments;
import dev.echo.client.config.FeatureSnapshotEntry;
import dev.echo.client.config.FeatureSnapshots;
import dev.echo.client.config.NameAssignmentHistory;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.render.HudRenderEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.features.Category;
import dev.echo.client.features.FeatureManager;
import dev.echo.client.features.hud.EchoHudElement;
import dev.echo.client.features.impl.render.KeybindsFeature;
import dev.echo.client.features.impl.render.NotificationsFeature;
import dev.echo.client.features.impl.render.ArrayListFeature;
import dev.echo.client.input.InputKey;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.render.GlTextureBridge;
import dev.echo.client.render.UiRenderer;
import dev.echo.render.FontAsset;
import dev.echo.client.ui.UiSize;
import dev.echo.client.ui.UiId;
import dev.echo.client.ui.animation.AnimationRegistry;
import dev.echo.client.ui.animation.SpringSpec;
import dev.echo.client.ui.animation.TransitionSpec;
import dev.echo.client.utils.targeting.TargetSelection;
import dev.echo.client.utils.strings.Concat;
import dev.echo.render.ImageDecoder;
import dev.echo.render.device.RenderDevice;
import dev.echo.render.svg.SvgTextureCache;
import dev.echo.render.texture.TextureView;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.AttackIndicator;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;

/**
 * Original HUD renderer, measurements, animations and frame state with direct calls.
 * Caller, cache, guard and initialization applicability are recorded in the V4 receipt.
 */
public final class HudRenderer {
    public static final HudRenderer INSTANCE = new HudRenderer();
    private static final SpringSpec ROW_SPRING = new SpringSpec(5.6f, 0.82f);
    private static final SpringSpec ENTRY_SPRING = new SpringSpec(4.8f, 0.86f);
    private static final SpringSpec TEXT_SPRING = new SpringSpec(16.0f, 0.88f);
    private static final SpringSpec SIZE_SPRING = new SpringSpec(11.5f, 0.92f);
    private static final TransitionSpec TARGET_TEXT_TRANSITION = TransitionSpec.DURATION_260_MS;
    private static final TransitionSpec HOTBAR_TRANSITION = TransitionSpec.DURATION_140_MS;
    private static final UiId TARGET_ID = UiId.ROOT.child("hud/overlay/target");
    public static final String[] HOTBAR_SLOT_LABELS = {"1", "2", "3", "4", "5", "6", "7", "8", "9"};
    private static final UiId HOTBAR_SELECTION_ID = UiId.ROOT.child("hud/overlay/hotbar/selection");
    private static final EquipmentSlot[] ARMOR_ORDER = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public static final int PANEL_COLOR = 0xcc0f1011;
    public static final int WHITE = -1;
    public static final int SECONDARY_TEXT_COLOR = 0x99ffffff;
    public static final int MUTED_TEXT_COLOR = 0x8cffffff;
    public static final int BORDER_COLOR = 0x40ffffff;
    public static final int SHADOW_COLOR = 0x80000000;

    private RenderDevice renderDevice;
    private SvgTextureCache svgTextures;
    private TextureView albumArt;
    private TextureView targetHead;
    private long frameStartNanos;
    private long animationStartNanos;
    private long entryGeneration;
    private long secondaryEntryGeneration;
    private boolean animationsEnabled;
    private float notificationDurationSeconds;
    private List<KeybindRow> moduleRows;
    private List<KeybindRow> keybindRows;
    private Set<String> activeKeybindIds;
    private boolean keybindHudEnabled;
    private final ArrayDeque<NotificationEntry> pendingNotifications;
    private final List<NotificationEntry> visibleNotifications;
    private final List<String> arrayRowOrder;
    private final AnimationRegistry animations;
    private long lastPreparedFrame;
    private long previousFrameNanos;
    private long lastNotificationSequence;
    private long lastNotificationFrameNanos;
    private int framesPerSecond;
    private float frameTimeMillis;
    private String usernameFallback;
    private String targetName;
    private float targetHealth;
    private float targetMaximumHealth;
    private String coordinateX;
    private String coordinateY;
    private String coordinateZ;
    private final EnumMap<HudElementType, UiSize> elementSizes;
    private final ItemStack[] hotbar;
    private final ItemStack[] armor;
    private ItemStack offhand;
    private int selectedHotbarSlot;
    private float attackCooldown;
    private PlayerEntity inventoryPlayer;
    private float selectedSlotAnimation;
    private boolean hotbarReplacement;
    private LivingEntity target;
    private LivingEntity previousTarget;
    private long targetChangedAtNanos;
    private final ItemStack[] targetArmor;
    private ItemStack targetMainHand;
    private final Map<GpuTextureView, TextureView> importedTextures;

    private HudRenderer() {
        frameStartNanos = System.nanoTime();
        animationStartNanos = frameStartNanos;
        animationsEnabled = true;
        notificationDurationSeconds = 1.5f;
        moduleRows = List.of();
        keybindRows = List.of();
        activeKeybindIds = Set.of();
        pendingNotifications = new ArrayDeque<>();
        visibleNotifications = new ArrayList<>();
        arrayRowOrder = new ArrayList<>();
        animations = new AnimationRegistry();
        lastPreparedFrame = -1L;
        previousFrameNanos = System.nanoTime();
        usernameFallback = "Player";
        targetName = "No target";
        targetMaximumHealth = 20.0f;
        coordinateX = "—";
        coordinateY = "—";
        coordinateZ = "—";
        elementSizes = new EnumMap<>(HudElementType.class);
        hotbar = new ItemStack[9];
        armor = new ItemStack[4];
        offhand = ItemStack.EMPTY;
        attackCooldown = 1.0f;
        targetArmor = new ItemStack[4];
        targetMainHand = ItemStack.EMPTY;
        importedTextures = new HashMap<>();
        clearInventory();
        for (HudElementType element : HudElementType.values()) {
            elementSizes.put(element, new UiSize(element.defaultWidth(), element.defaultHeight()));
        }
    }

    /** Original AccountService.stopEvent: complete frame-preparation sequence. */
    public void prepareHudFrame(UiRenderer renderer) {
        if (svgTextures == null || lastPreparedFrame == Echo.frameNumber) return;
        lastPreparedFrame = Echo.frameNumber;
        long now = System.nanoTime();
        float deltaSeconds = Math.min(0.05f, Math.max(0.0f, (float) (now - previousFrameNanos) / 1.0e9f));
        previousFrameNanos = now;
        frameTimeMillis += (deltaSeconds * 1000.0f - frameTimeMillis) * 0.1f;
        animations.beginFrame(now / 1.0e9, deltaSeconds);
        updatePlayerState();

        List<FeatureBindSnapshot> snapshots = FeatureNameAssignments.capture();
        moduleRows = buildModuleRows(snapshots);
        keybindRows = buildKeybindRows(snapshots);
        keybindHudEnabled = isFeatureEnabled(KeybindsFeature.class);
        Set<String> ids = new HashSet<>();
        for (KeybindRow row : keybindRows) ids.add(row.id());
        activeKeybindIds = Set.copyOf(ids);

        if (isFeatureEnabled(NotificationsFeature.class)) {
            updateNotifications();
        } else {
            lastNotificationSequence = FeatureNameAssignments.latestChangeSequence();
            FeatureNameAssignments.acknowledgeChanges(lastNotificationSequence);
            pendingNotifications.clear();
            visibleNotifications.clear();
            lastNotificationFrameNanos = 0L;
        }

        HudElementLayout layout = new HudElementLayout(this, renderer, null, getCanvasScale(), 1.0f);
        for (Feature feature : FeatureManager.allFeatures()) {
            if (feature instanceof EchoHudElement element) {
                // Disabled elements are also measured, preserving editor/layout state.
                elementSizes.put(element.getElementType(), element.measure(layout));
            }
        }
        if (MinecraftRenderState.getScreen() == null && !MinecraftRenderState.isHudHidden()) {
            animations.prune(600L);
        }
    }

    /** Original render-event binding; preparation precedes the screen/hidden-HUD checks. */
    @Subscribe
    public void onHudRender(HudRenderEvent event) {
        if (Echo.eventsDisabled || MinecraftClient.getInstance().player == null || !isInitialized()) return;
        prepareHudFrame(event.getRenderer());
        if (MinecraftRenderState.getScreen() != null || MinecraftRenderState.isHudHidden()) return;
        for (Feature feature : FeatureManager.allFeatures()) {
            if (feature instanceof EchoHudElement element && element.isEnabled()) {
                element.render(createLayout(event.getRenderer(), event.getTexture(), element.getScale(), element.getOpacity()));
            }
        }
    }

    public void initialize(RenderDevice device) {
        if (renderDevice == device && svgTextures != null) return;
        releaseRendererResources();
        renderDevice = device;
        svgTextures = new SvgTextureCache(device, Echo.LOGGER);
        albumArt = ImageDecoder.loadTexture(device, "gui/echo/hud/album-art.png");
        targetHead = ImageDecoder.loadTexture(device, "gui/echo/hud/target-head.png");
        preloadIcons();
    }

    public void releaseRendererResources() {
        if (svgTextures != null) {
            svgTextures.close();
            svgTextures = null;
        }
        releaseTexture(albumArt);
        releaseTexture(targetHead);
        targetHead = null;
        albumArt = null;
        renderDevice = null;
    }

    private void releaseTexture(TextureView texture) {
        if (renderDevice != null && texture != null) renderDevice.release(texture.texture);
    }

    private void preloadIcons() {
        float rasterScale = Math.max(0.01f, 2.0f);
        for (HudIcon icon : HudIcon.values()) {
            float size = icon == HudIcon.LOGO ? 20.0f : 16.0f;
            svgTextures.getOrSchedule(icon.path, Math.max(1, (int) Math.ceil(size * rasterScale)),
                    Math.max(1, (int) Math.ceil(size * rasterScale)), icon.mode);
        }
    }

    public boolean isInitialized() { return svgTextures != null; }
    SvgTextureCache svgTextures() { return svgTextures; }
    PlayerEntity inventoryPlayer() { return inventoryPlayer; }
    boolean replacesVanillaHotbar() { return hotbarReplacement; }
    public boolean animationsEnabled() { return animationsEnabled; }
    public HudElementLayout createLayout(UiRenderer renderer, TextureView backdrop, float scale, float opacity) {
        return new HudElementLayout(this, renderer, backdrop, getCanvasScale() * scale, opacity);
    }
    public float getCanvasScale() {
        int width = (int) Math.ceil(MinecraftClient.getInstance().getWindow().getFramebufferWidth() / 2.0);
        int height = (int) Math.ceil(MinecraftClient.getInstance().getWindow().getFramebufferHeight() / 2.0);
        return Math.min((float) width / 1920.0f, (float) height / 1080.0f);
    }
    private static boolean isFeatureEnabled(Class<?> type) {
        return FeatureManager.find(type).map(Feature::isEnabled).orElse(false);
    }

    private static List<KeybindRow> buildModuleRows(List<FeatureBindSnapshot> snapshots) {
        List<KeybindRow> result = new ArrayList<>();
        ArrayListFeature display = FeatureManager.find(ArrayListFeature.class)
                .map(ArrayListFeature.class::cast).orElse(null);
        for (FeatureBindSnapshot snapshot : snapshots) {
            if (!snapshot.enabled()) continue;
            if (display != null && !display.isFeatureVisible(snapshot.feature())) continue;
            String key = snapshot.binding() == InputKey.NONE ? "" : snapshot.binding().displayLabel();
            CharSequence label = display == null ? snapshot.label()
                    : display.normalizeLabel(snapshot.feature(), snapshot.label());
            CharSequence suffix = display == null ? "" : display.getFeatureSuffix(snapshot.feature());
            result.add(new KeybindRow("live/" + snapshot.id(), label, suffix, key,
                    iconForCategory(snapshot.feature().getCategory()), true));
        }
        return List.copyOf(result);
    }

    private static List<KeybindRow> buildKeybindRows(List<FeatureBindSnapshot> snapshots) {
        List<KeybindRow> result = new ArrayList<>();
        for (FeatureSnapshotEntry entry : FeatureSnapshots.keybindRows(snapshots)) {
            result.add(new KeybindRow("live/" + entry.id(), entry.name(), "", entry.keyLabel(),
                    iconForCategory(entry.category()), entry.enabled()));
        }
        return List.copyOf(result);
    }

    private static HudIcon iconForCategory(Category category) {
        return switch (category) {
            case COMBAT -> HudIcon.DANGER;
            case MOVEMENT -> HudIcon.FLASH;
            case RENDER -> HudIcon.SCAN;
            case WORLD -> HudIcon.PLANET;
            case UTILITY -> HudIcon.SHIELD_TICK;
            case INTERNALS -> HudIcon.GLOBAL;
        };
    }
    public UiSize getElementSize(HudElementType type) { return elementSizes.get(type); }
    public UiSize measureElement(HudElementLayout layout, HudElementType type) {
        return switch (type) {
            case ARRAY_LIST -> measureArrayList();
            case NOTIFICATIONS -> new UiSize(measureNotificationsWidth(layout), HudElementType.NOTIFICATIONS.defaultHeight());
            default -> new UiSize(type.defaultWidth(), type.defaultHeight());
        };
    }

    private UiSize measureArrayList() {
        int count = 0;
        for (KeybindRow row : moduleRows) {
            if (row.enabled() || arrayRowOrder.contains(row.id())) count++;
        }
        HudElementType type = HudElementType.ARRAY_LIST;
        if (count == 0) return new UiSize(type.defaultWidth(), type.defaultHeight());
        return new UiSize(type.defaultWidth(), 35.0f + (count - 1) * 39.5f);
    }

    private float measureNotificationsWidth(HudElementLayout layout) {
        float width = HudElementType.NOTIFICATIONS.defaultWidth();
        for (NotificationEntry entry : visibleNotifications) {
            width = Math.max(width, notificationWidth(layout, entry.message.text()));
        }
        for (NotificationEntry entry : pendingNotifications) {
            width = Math.max(width, notificationWidth(layout, entry.message.text()));
        }
        return roundWidth(width);
    }

    private float notificationWidth(HudElementLayout layout, CharSequence text) {
        float width = layout.textWidth(FontAsset.INTER_SEMI_BOLD, text, 13.0f);
        return roundWidth(45.0f + width + 14.0f);
    }

    private float measureKeybindsWidth(HudElementLayout layout, List<KeybindRow> rows) {
        float width = HudElementType.KEYBINDS.defaultWidth();
        for (KeybindRow row : rows) {
            width = Math.max(width, keybindRowWidth(layout.textWidth(FontAsset.INTER, row.name(), 12.5f),
                    layout.textWidth(FontAsset.INTER, "[" + row.binding() + "]", 12.0f)));
        }
        return width;
    }

    public static float keybindRowWidth(float nameWidth, float bindingWidth) {
        return roundWidth(Math.max(HudElementType.KEYBINDS.defaultWidth(),
                38.0f + Math.max(0.0f, nameWidth) + 12.0f + Math.max(0.0f, bindingWidth) + 16.0f));
    }
    public void setNotificationDuration(float seconds) { notificationDurationSeconds = Math.max(0.25f, seconds); }
    public void updateHotbarReplacement(boolean enabled) { hotbarReplacement = enabled; }
    public String getCoordinateXText() { return coordinateX; }
    public String getCoordinateYText() { return coordinateY; }
    public String getCoordinateZText() { return coordinateZ; }
    public int getFramesPerSecond() { return framesPerSecond; }
    public float getFrameTimeMillis() { return frameTimeMillis; }
    public String getUsernameFallback() { return usernameFallback; }
    public String getTargetName() { return targetName; }
    public float getTargetHealth() { return targetHealth; }
    public float getTargetMaximumHealth() { return targetMaximumHealth; }
    public TextureView getMusicPlaceholder() { return albumArt; }
    public TextureView getFallbackTargetPortrait() { return targetHead; }
    public ItemStack getHotbarStack(int slot) { return hotbar[slot]; }
    public ItemStack getEquippedArmor(int slot) { return armor[slot]; }
    public ItemStack getOffhandStack() { return offhand; }
    public int getSelectedHotbarSlot() { return selectedHotbarSlot; }
    public float getHotbarIndicatorProgress() { return attackCooldown; }
    public List<KeybindRow> getArrayListRows() { return moduleRows; }
    public List<KeybindRow> getKeybindRows() { return keybindRows; }

    public float getHotbarIndicatorX(float left) {
        float destination = left + 6.0f + 39.0f * slotIndex(selectedHotbarSlot, 9) + 5.0f;
        if (!animationsEnabled) {
            animations.snap(HOTBAR_SELECTION_ID, "trail-x", destination);
            return animations.snap(HOTBAR_SELECTION_ID, "head-x", destination);
        }
        return animations.spring(HOTBAR_SELECTION_ID, "head-x", destination, destination, TEXT_SPRING);
    }

    public float getHotbarIndicatorTargetX(float left) {
        float destination = left + 6.0f + 39.0f * slotIndex(selectedHotbarSlot, 9) + 5.0f;
        if (!animationsEnabled) return animations.snap(HOTBAR_SELECTION_ID, "trail-x", destination);
        return animations.spring(HOTBAR_SELECTION_ID, "trail-x", destination, destination, SIZE_SPRING);
    }

    private static int slotIndex(int slot, int count) {
        return Math.max(0, Math.min(Math.max(0, count - 1), slot));
    }

    public List<String> orderArrayListRows(HudElementLayout layout) {
        List<String> allIds = new ArrayList<>(moduleRows.size());
        List<String> enabledIds = new ArrayList<>(moduleRows.size());
        Map<String, Float> widths = new HashMap<>();
        for (KeybindRow row : moduleRows) {
            allIds.add(row.id());
            if (row.enabled()) enabledIds.add(row.id());
            widths.put(row.id(), measureArrayListRow(layout, row));
        }
        List<String> sorted = sortArrayRows(arrayRowOrder, allIds, enabledIds, widths);
        arrayRowOrder.clear();
        arrayRowOrder.addAll(sorted);
        return List.copyOf(arrayRowOrder);
    }

    static List<String> sortArrayRows(List<String> previous, List<String> current,
                                      List<String> enabled, Map<String, Float> widths) {
        List<String> result = new ArrayList<>();
        for (String id : previous) {
            if (current.contains(id) && !result.contains(id)) result.add(id);
        }
        for (String id : enabled) {
            if (current.contains(id) && !result.contains(id)) result.add(id);
        }
        result.sort((first, second) -> Float.compare(widths.getOrDefault(second, 0.0f),
                widths.getOrDefault(first, 0.0f)));
        return List.copyOf(result);
    }

    public void removeCompletedArrayRows(List<String> ids) { arrayRowOrder.removeAll(ids); }

    public String formatArrayRowBinding(KeybindRow row) {
        String binding = row.binding();
        if (binding == null || binding.isEmpty()) return "";
        return keybindHudEnabled && activeKeybindIds.contains(row.id()) ? "" : binding;
    }

    public float measureArrayListRow(HudElementLayout layout, KeybindRow row) {
        return measureArrayListRow(layout, row, formatArrayRowBinding(row));
    }

    private float measureArrayListRow(HudElementLayout layout, KeybindRow row, String binding) {
        float nameWidth = layout.textWidth(FontAsset.INTER_SEMI_BOLD, row.name(), 13.0f);
        if (!row.detail().isEmpty()) {
            nameWidth += 5.0f + layout.textWidth(FontAsset.INTER_SEMI_BOLD, row.detail(), 13.0f);
        }
        float bindingWidth = binding.isEmpty() ? 0.0f : layout.textWidth(FontAsset.INTER, "[" + binding + "]", 12.0f);
        float width = 36.0f + nameWidth + 14.0f;
        if (!binding.isEmpty()) width += 17.0f + bindingWidth;
        return roundWidth(width);
    }

    public float arrayRowY(UiId identity, float y) {
        return animationsEnabled ? animations.spring(identity, "y", y, y, ENTRY_SPRING)
                : animations.snap(identity, "y", y);
    }

    public float arrayRowOpacity(UiId identity, boolean enabled) {
        return rowVisibility(animations, identity, enabled);
    }

    private float rowVisibility(AnimationRegistry registry, UiId identity, boolean enabled) {
        float targetValue = enabled ? 1.0f : 0.0f;
        if (!animationsEnabled) return registry.snap(identity, "visibility", targetValue);
        return registry.tween(identity, "visibility", 0.0f, targetValue, TransitionSpec.easeOut(0.22f));
    }

    public float arrayRowHorizontalOffset(UiId identity, boolean enabled, boolean rightAligned) {
        if (!animationsEnabled) return animations.snap(identity, "slide-x", 0.0f);
        return animations.spring(identity, "slide-x", rowHorizontalOffset(false, false, rightAligned),
                rowHorizontalOffset(enabled, false, rightAligned), ROW_SPRING);
    }

    static float rowHorizontalOffset(boolean enabled, boolean stationary, boolean rightAligned) {
        if (enabled || stationary) return 0.0f;
        return rightAligned ? 32.0f : -32.0f;
    }

    public float entryProgress(String identity, long delayMillis, boolean secondaryClock) {
        return entryProgress(animations, identity, delayMillis, secondaryClock, true);
    }

    private float entryProgress(AnimationRegistry registry, String identity, long delayMillis,
                                boolean secondaryClock, boolean visible) {
        if (!animationsEnabled) return visible ? 1.0f : 0.0f;
        long startedAt = secondaryClock ? animationStartNanos : frameStartNanos;
        long elapsedMillis = Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
        float targetValue = visible && elapsedMillis >= delayMillis ? 1.0f : 0.0f;
        long generation = secondaryClock ? secondaryEntryGeneration : entryGeneration;
        UiId key = UiId.ROOT.child("hud/overlay/" + generation + "/" + identity);
        return registry.tween(key, "presence", 0.0f, targetValue, TransitionSpec.easeOut(0.22f));
    }

    public static float roundWidth(float width) { return (float) Math.ceil(Math.max(0.0f, width) / 5.0f) * 5.0f; }
    public static float clampFraction(float value) { return Math.max(0.0f, Math.min(1.0f, value)); }
    public static int multiplyAlpha(int color, float opacity) {
        int alpha = Math.round((color >>> 24 & 0xff) * clampFraction(opacity));
        return color & 0x00ffffff | alpha << 24;
    }

    public static String formatTargetHealth(float health) {
        float value = Float.isFinite(health) ? Math.max(0.0f, health) : 0.0f;
        return Math.round(value) + "hp";
    }

    public float pulse(float periodSeconds) {
        if (!animationsEnabled) return 0.5f;
        double elapsed = (System.nanoTime() - frameStartNanos) / 1.0e9;
        return (float) (0.5 + 0.5 * Math.sin(elapsed * Math.PI * 2.0 / Math.max(0.2, periodSeconds)));
    }

    public void drawPanel(HudElementLayout layout, float x, float y, float width, float height,
                          float radius, float opacity, float shadowBlur) {
        layout.drawGlow(x, y + 6.0f, width, height, radius, Math.min(shadowBlur, 8.0f), SHADOW_COLOR, opacity);
        layout.drawBackdropRoundedRect(x, y, width, height, radius, PANEL_COLOR, opacity);
    }

    private void drawNotificationBackground(HudElementLayout layout, float x, float y, float width,
                                            float height, float opacity, float shadowOffset) {
        layout.drawGlow(x, y + shadowOffset, width, height, 10.0f, 8.0f, SHADOW_COLOR, opacity);
        layout.drawBackdropRoundedRect(x, y, width, height, 10.0f, PANEL_COLOR, opacity);
    }

    static String formatGroupedInteger(int value) {
        String digits = Long.toString(Math.abs((long) value));
        StringBuilder result = new StringBuilder(digits.length() + digits.length() / 3);
        int first = digits.length() % 3;
        if (first == 0) first = 3;
        result.append(digits, 0, first);
        for (int index = first; index < digits.length(); index += 3) result.append('.').append(digits, index, index + 3);
        return value < 0 ? "-" + result : result.toString();
    }

    static String formatSeconds(float seconds) {
        float value = Float.isFinite(seconds) ? Math.max(0.0f, seconds) : 0.0f;
        float tenths = Math.round(value * 10.0f) / 10.0f;
        int integer = Math.round(tenths);
        if (Math.abs(tenths - integer) < 0.001f) return integer + "s";
        return String.format(Locale.ROOT, "%.1fs", tenths);
    }

    public static float hotbarSlotLeft(float left, int slot) { return left + 6.0f + 39.0f * slotIndex(slot, 9); }
    public static float hotbarItemX(float left, int slot) { return hotbarSlotLeft(left, slot) + 10.5f; }
    public static float hotbarCountRight(float left, int slot) { return left + 26.0f + 39.0f * slotIndex(slot, 9); }
    public static float armorSlotLeft(float left, int slot) { return left + 8.0f + 39.0f * slotIndex(slot, 4); }
    public static float armorSeparatorX(float left, int separator) { return left + 43.0f + 39.0f * slotIndex(separator, 3); }
    public static float armorItemX(float left, int slot) { return armorSlotLeft(left, slot) + 8.0f; }
    public static float targetEquipmentX(int slot) { return 932.0f + 18.0f * slotIndex(slot, 5); }

    private void captureInventory(PlayerEntity player) {
        inventoryPlayer = player;
        if (player == null) { clearInventory(); return; }
        for (int slot = 0; slot < 9; slot++) hotbar[slot] = player.getInventory().getStack(slot);
        selectedHotbarSlot = Math.max(0, Math.min(8, player.getInventory().getSelectedSlot()));
        for (int slot = 0; slot < 4; slot++) armor[slot] = player.getEquippedStack(ARMOR_ORDER[slot]);
        offhand = player.getOffHandStack();
    }

    private void updatePlayerState() {
        MinecraftClient client = MinecraftClient.getInstance();
        framesPerSecond = Math.max(0, client.getCurrentFps());
        String username = client.getSession().getUsername();
        usernameFallback = username == null || username.isBlank() ? "Player" : username;
        PlayerEntity player = client.player != null && !client.player.isSpectator()
                && client.getCameraEntity() instanceof PlayerEntity cameraPlayer ? cameraPlayer : null;
        captureInventory(player);
        attackCooldown = player != null && client.options.getAttackIndicator().getValue() == AttackIndicator.HOTBAR
                ? clampFraction(player.getAttackCooldownProgress(0.0f)) : 1.0f;
        updateTargetState(client);
        if (client.world == null || client.player == null) {
            coordinateX = coordinateY = coordinateZ = "—";
            return;
        }
        BlockPos position = client.player.getBlockPos();
        coordinateX = formatGroupedInteger(position.getX());
        coordinateY = formatGroupedInteger(position.getY());
        coordinateZ = formatGroupedInteger(position.getZ());
    }

    private void updateTargetState(MinecraftClient client) {
        long now = System.nanoTime();
        LivingEntity recent = TargetSelection.recentlyAttacked(20);
        if (recent != null && recent != client.player) {
            target = recent;
            targetChangedAtNanos = now;
        } else if (client.targetedEntity instanceof PlayerEntity hovered
                && hovered != client.player && TargetSelection.isValidTarget(hovered)) {
            target = hovered;
            targetChangedAtNanos = now;
        }
        boolean retained = target != null && target.isAlive() && target.getEntityWorld() == client.world
                && now - targetChangedAtNanos <= 2_000_000_000L;
        if (!retained) {
            target = null;
            if (getTargetVisibility() <= 0.0f) clearTargetData();
            return;
        }
        if (target != previousTarget) {
            animations.snap(TARGET_ID, "text", 0.0f);
            previousTarget = target;
        }
        String name = target.getName().getString();
        targetName = name == null || name.isBlank() ? "Player" : name;
        targetHealth = Math.max(0.0f, target.getHealth());
        targetMaximumHealth = Math.max(1.0f, target.getMaxHealth());
        for (int slot = 0; slot < 4; slot++) targetArmor[slot] = target.getEquippedStack(ARMOR_ORDER[slot]);
        targetMainHand = target.getMainHandStack();
    }

    private void clearTargetData() {
        previousTarget = null;
        targetName = "No target";
        targetHealth = 0.0f;
        targetMaximumHealth = 20.0f;
        Arrays.fill(targetArmor, ItemStack.EMPTY);
        targetMainHand = ItemStack.EMPTY;
    }

    public ItemStack getTargetArmor(int slot) {
        ItemStack stack = targetArmor[slot];
        return stack == null ? ItemStack.EMPTY : stack;
    }
    public ItemStack getTargetMainHand() { return targetMainHand; }
    public LivingEntity getTarget() { return target; }
    public TextureView getTargetSkin() {
        if (!(previousTarget instanceof PlayerEntity player)) return null;
        GpuTextureView skin = MinecraftRenderState.playerSkin(player);
        if (skin == null) return null;
        return importedTextures.computeIfAbsent(skin, view -> GlTextureBridge.importColor(renderDevice, view));
    }
    public float getTargetVisibility() {
        float present = target == null ? 0.0f : 1.0f;
        if (!animationsEnabled) return animations.snap(TARGET_ID, "presence", present);
        return animations.tween(TARGET_ID, "presence", 0.0f, present, TransitionSpec.DURATION_220_MS);
    }

    public float getTargetTextOpacity() {
        boolean present = target != null;
        float targetValue = present ? 1.0f : 0.0f;
        if (!animationsEnabled) return animations.snap(TARGET_ID, "text", targetValue);
        return animations.tween(TARGET_ID, "text", 0.0f, targetValue,
                present ? TARGET_TEXT_TRANSITION : TransitionSpec.DURATION_140_MS);
    }

    public List<StatusEffectRow> getEffectRows() {
        var player = MinecraftClient.getInstance().player;
        if (player == null) return List.of();
        List<StatusEffectRow> result = new ArrayList<>();
        for (StatusEffectInstance effect : player.getStatusEffects()) {
            String name = effect.getEffectType().value().getName().getString();
            int amplifier = effect.getAmplifier();
            String suffix = amplifier > 0 ? effectLevel(amplifier + 1) : "";
            int seconds = effect.getDuration() / 20;
            String duration = seconds / 60 + ":" + String.format(Locale.ROOT, "%02d", seconds % 60);
            float remaining = Math.min(1.0f, effect.getDuration() / 1200.0f);
            result.add(new StatusEffectRow(name, suffix, duration, remaining, iconForEffect(effect.getEffectType())));
        }
        return result;
    }

    private static String effectLevel(int level) {
        return switch (level) {
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> Integer.toString(level);
        };
    }

    private static HudIcon iconForEffect(RegistryEntry<StatusEffect> effect) {
        if (effect.matches(StatusEffects.SPEED) || effect.matches(StatusEffects.HASTE)
                || effect.matches(StatusEffects.JUMP_BOOST)) return HudIcon.FLASH;
        if (effect.matches(StatusEffects.SLOWNESS) || effect.matches(StatusEffects.MINING_FATIGUE)) return HudIcon.FLASH_SLASH;
        if (effect.matches(StatusEffects.INVISIBILITY)) return HudIcon.EYE_SLASH;
        if (effect.matches(StatusEffects.NIGHT_VISION) || effect.matches(StatusEffects.GLOWING)) return HudIcon.EYE;
        if (effect.matches(StatusEffects.REGENERATION) || effect.matches(StatusEffects.HEALTH_BOOST)
                || effect.matches(StatusEffects.ABSORPTION)) return HudIcon.HEART;
        if (effect.matches(StatusEffects.RESISTANCE) || effect.matches(StatusEffects.FIRE_RESISTANCE)) return HudIcon.SHIELD_TICK;
        if (effect.matches(StatusEffects.STRENGTH)) return HudIcon.DANGER;
        return switch (effect.value().getCategory()) {
            case HARMFUL -> HudIcon.DANGER;
            case BENEFICIAL -> HudIcon.SHIELD_SECURITY;
            case NEUTRAL -> HudIcon.STAR;
        };
    }

    private void clearInventory() {
        for (int slot = 0; slot < 9; slot++) hotbar[slot] = ItemStack.EMPTY;
        for (int slot = 0; slot < 4; slot++) armor[slot] = ItemStack.EMPTY;
        offhand = ItemStack.EMPTY;
        selectedHotbarSlot = 0;
        attackCooldown = 1.0f;
        inventoryPlayer = null;
    }

    public void resetTransientState() {
        moduleRows = List.of(); keybindRows = List.of(); activeKeybindIds = Set.of();
        keybindHudEnabled = false;
        pendingNotifications.clear(); visibleNotifications.clear(); arrayRowOrder.clear();
        lastNotificationSequence = 0L; lastNotificationFrameNanos = 0L;
    }

    private void updateNotifications() {
        for (NameAssignmentHistory change : FeatureNameAssignments.changesAfter(lastNotificationSequence)) {
            lastNotificationSequence = Math.max(lastNotificationSequence, change.sequence());
            String state = change.enabled() ? "Enabled!" : "Disabled!";
            CharSequence text = change.suffix() != null ? change.suffix()
                    : Concat.sequences(change.label(), " was ", state);
            NotificationMessage message = new NotificationMessage(change.sequence(), text,
                    iconForCategory(change.feature().getCategory()));
            pendingNotifications.addLast(new NotificationEntry(message));
        }
        FeatureNameAssignments.acknowledgeChanges(lastNotificationSequence);
    }

    public void drawNotifications(HudElementLayout layout) {
        long now = System.nanoTime();
        long lifetimeNanos = Math.max(250_000_000L, Math.round((double) notificationDurationSeconds * 1.0e9));
        long exitNanos = animationsEnabled
                ? Math.round((double) (0.22f * animations.durationScale()) * 1.0e9) : 0L;
        long exitAtNanos = NotificationTiming.remainingMillis(lifetimeNanos / 1_000_000L,
                exitNanos / 1_000_000L) * 1_000_000L;
        long elapsed = lastNotificationFrameNanos == 0L ? 0L : Math.max(0L, now - lastNotificationFrameNanos);
        lastNotificationFrameNanos = now;

        Iterator<NotificationEntry> iterator = visibleNotifications.iterator();
        while (iterator.hasNext()) {
            NotificationEntry entry = iterator.next();
            entry.visibleNanos = Math.min(lifetimeNanos, entry.visibleNanos + elapsed);
            if (!entry.exiting && entry.visibleNanos >= exitAtNanos) entry.exiting = true;
            float presence = notificationPresence(animations, entry.message.id(), !entry.exiting);
            if (entry.exiting && presence <= 0.002f) iterator.remove();
        }
        drainNotifications(pendingNotifications, visibleNotifications);
        markOverflowNotifications(visibleNotifications);

        int total = visibleNotifications.size();
        int shown = Math.min(5, total);
        float baseline = notificationBaseline();
        for (int index = 0; index < total; index++) {
            NotificationEntry entry = visibleNotifications.get(index);
            NotificationMessage message = entry.message;
            float presence = notificationPresence(animations, message.id(), !entry.exiting);
            if (entry.dismissWhenVisible && presence >= 0.62f) {
                entry.dismissWhenVisible = false;
                entry.exiting = true;
                entry.forcedExit = true;
                presence = notificationPresence(animations, message.id(), false);
            }
            if (presence <= 0.002f) continue;

            int slot = notificationSlot(index, total);
            float textWidth = layout.textWidth(FontAsset.INTER_SEMI_BOLD, message.text(), 13.0f);
            float width = roundWidth(45.0f + textWidth + 14.0f);
            float x = HudElementType.NOTIFICATIONS.defaultX() + (float) Math.floor((layout.getWidth() - width) * 0.5f);
            UiId identity = UiId.ROOT.child("hud/overlay/live-notification/" + message.id());
            float desiredY = notificationRowY(slot, shown) + notificationOverflowOffset(index, total);
            float overlap = (baseline - desiredY) / 30.0f;
            float animatedOverlap = animationsEnabled
                    ? animations.spring(identity, "overlap", overlap, overlap, ENTRY_SPRING)
                    : animations.snap(identity, "overlap", overlap);
            float enterY = notificationEdgeOffset(animations, identity, !entry.exiting, entry.forcedExit);
            float y = baseline - 30.0f * animatedOverlap + enterY;
            float shadowOffset = notificationShadowOffset(slot, shown);
            float stackOpacity = NotificationTiming.stackOpacity(slot, shown) * notificationOverflowOpacity(index, total);
            float opacity = presence * notificationStackOpacity(animations, identity, stackOpacity);

            drawNotificationBackground(layout, x, y, width, 42.0f, opacity, shadowOffset);
            layout.drawIcon(message.icon(), x + 12.0f, y + 15.0f, 12.0f, 12.0f, -1, opacity);
            layout.drawSeparator(x + 34.0f, y + 16.5f, opacity);
            layout.drawText(FontAsset.INTER_SEMI_BOLD, message.text(), x + 45.0f, y + 13.5f, 13.0f, -1, opacity);
            long visibleMillis = entry.visibleNanos / 1_000_000L;
            long lifetimeMillis = Math.max(1L, lifetimeNanos / 1_000_000L);
            float remainingWidth = NotificationTiming.fadeAfterHold(textWidth, visibleMillis, 0L, lifetimeMillis);
            layout.drawHeaderHighlight(x + 45.0f, y + 36.0f, remainingWidth, 2.0f, 1.0f, opacity);
        }
    }

    private float notificationPresence(AnimationRegistry registry, long messageId, boolean visible) {
        UiId identity = UiId.ROOT.child("hud/overlay/live-notification/" + messageId);
        float targetValue = visible ? 1.0f : 0.0f;
        if (!animationsEnabled) return registry.snap(identity, "presence", targetValue);
        return registry.tween(identity, "presence", 0.0f, targetValue, TransitionSpec.easeOut(0.22f));
    }

    private float notificationEdgeOffset(AnimationRegistry registry, UiId identity, boolean visible, boolean forcedExit) {
        if (!animationsEnabled) return registry.snap(identity, "edge-y", 0.0f);
        float targetValue = visible ? 0.0f : -3.0f;
        if (forcedExit) targetValue = -3.0f;
        return registry.spring(identity, "edge-y", 14.0f, targetValue, ROW_SPRING);
    }

    private float notificationStackOpacity(AnimationRegistry registry, UiId identity, float opacity) {
        if (!animationsEnabled) return registry.snap(identity, "stack-opacity", opacity);
        return registry.tween(identity, "stack-opacity", opacity, opacity, TransitionSpec.easeOut(0.18f));
    }

    private float notificationStackOpacity(AnimationRegistry registry, UiId identity, int slot, int count) {
        return notificationStackOpacity(registry, identity, NotificationTiming.stackOpacity(slot, count));
    }

    private SpringSpec rowSpring() { return ROW_SPRING; }
    private SpringSpec entrySpring() { return ENTRY_SPRING; }
    private SpringSpec textSpring() { return TEXT_SPRING; }
    private SpringSpec sizeSpring() { return SIZE_SPRING; }

    static void drainNotifications(ArrayDeque<NotificationEntry> pending, List<NotificationEntry> visible) {
        while (!pending.isEmpty()) visible.add(pending.removeFirst());
    }

    private static void markOverflowNotifications(List<NotificationEntry> entries) {
        List<Boolean> leaving = new ArrayList<>(entries.size());
        for (NotificationEntry entry : entries) leaving.add(entry.exiting || entry.dismissWhenVisible);
        for (int index : overflowNotificationIndices(leaving)) entries.get(index).dismissWhenVisible = true;
    }

    static List<Integer> overflowNotificationIndices(List<Boolean> leaving) {
        int active = 0;
        for (boolean value : leaving) if (!value) active++;
        int overflow = Math.max(0, active - 5);
        if (overflow == 0) return List.of();
        List<Integer> result = new ArrayList<>(overflow);
        for (int index = 0; index < leaving.size() && result.size() < overflow; index++) {
            if (!leaving.get(index)) result.add(index);
        }
        return List.copyOf(result);
    }

    static int notificationSlot(int index, int count) {
        int size = Math.max(0, count);
        int shown = Math.min(5, size);
        if (shown == 0) return 0;
        int overflow = Math.max(0, size - shown);
        return Math.max(0, Math.min(shown - 1, Math.max(0, index) - overflow));
    }

    static float notificationOverflowOffset(int index, int count) {
        int overflow = Math.max(0, Math.max(0, count) - 5);
        if (index >= 0 && index < overflow) return -10.0f * (overflow - index) / overflow;
        return 0.0f;
    }

    static float notificationOverflowOpacity(int index, int count) {
        int overflow = Math.max(0, Math.max(0, count) - 5);
        if (index < 0 || index >= overflow) return 1.0f;
        return Math.max(0.2f, (float) Math.pow(0.72f, overflow - index));
    }

    private static float notificationBaseline() {
        return HudElementType.NOTIFICATIONS.defaultY() + HudElementType.NOTIFICATIONS.defaultHeight() - 42.0f;
    }

    static float notificationRowY(int slot, int count) {
        int size = Math.max(1, count);
        int index = Math.max(0, Math.min(slot, size - 1));
        return notificationBaseline() - (size - 1 - index) * 30.0f;
    }

    static float notificationShadowOffset(int slot, int count) {
        int size = Math.max(1, count);
        int index = Math.max(0, Math.min(slot, size - 1));
        return index == size - 1 ? -11.0f : 6.0f;
    }

    public static float notificationOffset(float height) {
        float extra = Math.max(0.0f, Math.max(0.0f, height) - HudElementType.NOTIFICATIONS.defaultHeight());
        return extra <= 0.0f ? 0.0f : -extra;
    }

    private int visibleNotificationCount() {
        return Math.min(5, visibleNotifications.size() + pendingNotifications.size());
    }

    static float notificationHeight(int count) {
        if (count <= 1) return HudElementType.NOTIFICATIONS.defaultHeight();
        return roundWidth(Math.max(HudElementType.NOTIFICATIONS.defaultHeight(), 42.0f + (count - 1) * 30.0f));
    }

    static float notificationOverflowPadding(int count) { return count > 5 ? 10.0f : 0.0f; }
    static float visibleNotificationHeight(float height) { return Math.max(0.0f, height + notificationOffset(height)); }

    static long notificationCycleMillis(int count, long lifetimeMillis) {
        if (count <= 0) return 1L;
        return NotificationTiming.staggerMillis(count - 1, lifetimeMillis) + Math.max(1L, lifetimeMillis) + 300L;
    }

    static long notificationCycle(long elapsedMillis, int count, long lifetimeMillis) {
        if (elapsedMillis <= 0L || count <= 0) return 0L;
        return elapsedMillis / notificationCycleMillis(count, lifetimeMillis);
    }

    static float centeredNotificationX(float width) {
        return HudElementType.NOTIFICATIONS.defaultX()
                + (float) Math.floor((HudElementType.NOTIFICATIONS.defaultWidth() - width) * 0.5f);
    }

    private static float centeredNotificationX(HudElementLayout layout, float width) {
        return HudElementType.NOTIFICATIONS.defaultX() + (float) Math.floor((layout.getWidth() - width) * 0.5f);
    }

    static ItemRenderPlacement itemPlacement(float x, float y, float size, float scale) {
        float physicalScale = Math.max(1.0e-4f, scale);
        float physicalSize = Math.max(0.0f, size) * physicalScale;
        boolean small = physicalSize < 16.0f;
        int integerScale = small ? 0 : Math.max(1, Math.round(physicalSize / 16.0f));
        float snappedSize = small ? nearestSmallItemSize(physicalSize) : 16.0f * integerScale;
        float logicalSize = snappedSize / physicalScale;
        float left = x + (size - logicalSize) * 0.5f;
        float top = y + (size - logicalSize) * 0.5f;
        float snappedX = Math.round(left * physicalScale) / physicalScale;
        float snappedY = Math.round(top * physicalScale) / physicalScale;
        return new ItemRenderPlacement(snappedX, snappedY, logicalSize, integerScale);
    }

    private static float nearestSmallItemSize(float size) {
        float lower = 16.0f;
        while (lower > size && lower > 1.0f) lower *= 0.5f;
        float upper = Math.min(16.0f, lower * 2.0f);
        return size - lower < upper - size ? lower : upper;
    }

    public void collectKeybindIds(Collection<String> result) {
        collectKeybindIds(moduleRows, result);
        collectKeybindIds(keybindRows, result);
    }
    private static void collectKeybindIds(List<KeybindRow> rows, Collection<String> result) {
        for (KeybindRow row : rows) if (!row.id().isEmpty()) result.add(row.id());
    }
}
