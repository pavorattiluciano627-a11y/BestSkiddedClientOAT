package dev.echo.client.features;

import dev.echo.client.Echo;
import dev.echo.client.MinecraftAccess;
import dev.echo.client.events.EventBus;
import dev.echo.client.handlers.RotationManager;
import dev.echo.client.input.InputKey;
import dev.echo.client.setting.Setting;
import java.lang.reflect.Field;
import java.util.ArrayList;
import net.minecraft.client.MinecraftClient;

/** Metadata, settings, key activation, and listener lifecycle shared by features. */
public class Feature implements MinecraftAccess {
    protected static final MinecraftClient client = MinecraftClient.getInstance();

    public ArrayList<Setting> settings;
    private boolean enabled;
    private InputKey key;
    private boolean holdMode;
    private boolean visible;
    private final FeatureInfo info;

    public Feature(FeatureInfo info) {
        settings = new ArrayList<>();
        enabled = false;
        this.info = info;
        key = info == null ? InputKey.NONE : info.key();
        visible = info == null || info.visible();
    }

    protected final RotationManager rotations() {
        return Echo.handlerManager.getRotationManager();
    }

    /**
     * Collects instance and static settings from the concrete class up through
     * Feature, then invokes the feature's post-initialization extension point.
     */
    public void initSettings(boolean clearExisting) {
        if (clearExisting) {
            settings.clear();
        }
        Class<?> type = getClass();
        while (type != null && Feature.class.isAssignableFrom(type)) {
            collectDeclaredSettings(type);
            type = type.getSuperclass();
        }
        postInitialize();
    }

    public void initSettings() {
        initSettings(true);
    }

    private void collectDeclaredSettings(Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(this);
                if (value instanceof Setting setting && !settings.contains(setting)) {
                    settings.add(setting);
                }
            } catch (IllegalAccessException failure) {
                failure.printStackTrace();
            }
        }
    }

    /** Original base extension point has an empty body; feature overrides supply work. */
    public void postInitialize() {
    }

    public CharSequence getName() {
        if (info == null) {
            return getClass().getSimpleName();
        }
        CharSequence name = info.name();
        return name == null || name.length() == 0 ? getClass().getSimpleName() : name;
    }

    public CharSequence getDescription() {
        if (info == null) {
            return "";
        }
        CharSequence description = info.description();
        return description == null ? "" : description;
    }

    public Category getCategory() {
        return info == null ? Category.INTERNALS : info.category();
    }

    /** Registers the feature's actual annotated event subscribers. */
    public void onEnable() {
        EventBus.register(this);
    }

    /** Unregisters the feature's actual annotated event subscribers. */
    public void onDisable() {
        EventBus.unregister(this);
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    /**
     * Publishes state before invoking the corresponding hook. Hook failures
     * propagate with the new state intact; reentrant transitions execute inline.
     */
    public void setEnabled(boolean requested) {
        if (enabled == requested) {
            return;
        }
        if (requested && !canEnable()) {
            return;
        }
        enabled = requested;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Boolean hasSettings() {
        return !settings.isEmpty();
    }

    public void setKey(InputKey key) {
        this.key = key;
    }

    public InputKey getKey() {
        return key;
    }

    public void setHoldMode(boolean holdMode) {
        this.holdMode = holdMode;
    }

    public boolean isHoldMode() {
        return holdMode;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /** Optional string-valued extension; the original base returns an empty string. */
    public String getInfoText() {
        return "";
    }

    /** Feature-specific status appended by the enabled-feature list. */
    public CharSequence getSuffix() {
        return "";
    }

    /** Controls insertion of enabled-state changes into the bounded feature history. */
    public boolean recordsStateChanges() {
        return true;
    }

    public boolean canEnable() {
        return true;
    }

    public boolean isFilteredFromDefaultList() {
        return false;
    }

    public boolean isKeybindable() {
        return true;
    }

    public boolean requiresMovementCorrection() {
        return false;
    }

    public boolean isPersistent() {
        return true;
    }

    public boolean persistsEnabledState() {
        return true;
    }

    public boolean isOutsideWorld() {
        return client.player == null || client.world == null;
    }
}
