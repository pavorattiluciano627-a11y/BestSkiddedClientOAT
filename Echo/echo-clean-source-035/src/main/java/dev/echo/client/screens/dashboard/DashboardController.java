package dev.echo.client.screens.dashboard;

import dev.echo.client.Echo;
import dev.echo.client.config.FeatureNameAssignments;
import dev.echo.client.config.ConfigMetadata;
import dev.echo.client.config.NameAssignmentResult;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.render.HudRenderEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.features.Category;
import dev.echo.client.features.FeatureManager;
import dev.echo.client.features.internals.TargetsFeature;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.render.UiRenderer;
import dev.echo.render.FontAsset;
import dev.echo.client.screens.DashboardScreen;
import dev.echo.client.screens.HudEditorScreen;
import dev.echo.client.scripting.ScriptFiles;
import dev.echo.client.input.InputKey;
import dev.echo.client.input.BindMode;
import dev.echo.client.hud.ItemHudLayout;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.Setting;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.setting.IntSetting;
import dev.echo.client.setting.FloatRangeSetting;
import dev.echo.client.setting.ColorSetting;
import dev.echo.client.setting.RegistryListSetting;
import dev.echo.client.setting.TextSetting;
import dev.echo.client.setting.HotbarSlotsSetting;
import dev.echo.client.setting.NamedRangeGroupSetting;
import dev.echo.client.setting.MultiChoiceSetting;
import dev.echo.client.setting.CustomWidgetSetting;
import dev.echo.client.setting.KeybindSetting;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.range.NamedRange;
import dev.echo.client.setting.range.RangeBounds;
import dev.echo.client.setting.range.RangeBand;
import dev.echo.client.setting.list.RegistryEntryGroup;
import dev.echo.client.setting.list.SelectionState;
import dev.echo.client.ui.CanvasMetrics;
import dev.echo.client.ui.ImmediateUiContext;
import dev.echo.client.ui.UiId;
import dev.echo.client.ui.UiInputState;
import dev.echo.client.ui.UiRect;
import dev.echo.client.ui.UiLayoutMath;
import dev.echo.client.ui.KeyStroke;
import dev.echo.client.ui.animation.AnimationRegistry;
import dev.echo.client.ui.animation.SpringSpec;
import dev.echo.client.ui.animation.TransitionSpec;
import dev.echo.client.utils.strings.Concat;
import dev.echo.render.device.RenderDevice;
import dev.echo.render.ImageDecoder;
import dev.echo.render.svg.SvgTextureCache;
import dev.echo.render.texture.TextureView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import org.lwjgl.glfw.GLFW;

/**
 * Concrete dashboard frame, pages, widgets and input state from the original members.
 * Caller, cache, guard and initialization applicability are recorded in the V4 receipt.
 */
public final class DashboardController {
    // Preserve the dedicated non-interned String object from original decoder slot 289.
    private static final String SIDEBAR_MODULE_INDICATOR = new String("sidebar/module-indicator");
    private static final String MODULE_SEARCH_ID = new String("module/search");
    private static final String SEARCH_SHOWN_PROPERTY = new String("shown");
    private static final String MODULES_LABEL = new String("Modules");
    private static final String OVERLAYS_LABEL = new String("Overlays");
    private static final String OVERLAYS_DESCRIPTION = new String("Choose which overlays appear in-game. Right-click one to open its settings.");
    private static final String OVERLAY_ID_PREFIX = new String("overlays/");
    private static final String HOVER_PROPERTY = new String("hover");
    private static final String TOGGLE_ID_SUFFIX = new String("/toggle");
    private static final String MODULE_SEARCH_FIELD_ID = new String("module/search");
    private static final String FOCUS_PROPERTY = new String("focus");
    private static final String MODULE_SEARCH_PLACEHOLDER = new String("Search modules");
    private static final String SINGLE_RESULT = new String(" result");
    private static final String MULTIPLE_RESULTS = new String(" results");
    private static final String TOGGLE_POSITION_PROPERTY = new String("position");
    private static final String TOGGLE_VELOCITY_PROPERTY = new String("position");
    private static final String SIDEBAR_CATEGORY_PREFIX = new String("sidebar/category/");
    private static final String CATEGORY_OPEN_PROPERTY = new String("open");
    private static final String CATEGORIES_LABEL = new String("CATEGORIES");
    private static final String SIDEBAR_MODULE_PREFIX = new String("sidebar/module/");
    private static final String ENABLED_PROPERTY = new String("enabled");
    private static final String CATEGORY_TINT_ID = new String("sidebar/category");
    private static final String CATEGORY_TINT_PROPERTY = new String("tint");
    private static final String SIDEBAR_APP_PREFIX = new String("sidebar/app/");
    private static final String APP_TINT_PROPERTY = new String("tint");
    private static final String CONFIGS_NAV_LABEL = new String("Configs");
    private static final String FRIENDS_NAV_LABEL = new String("Friends");
    private static final String OVERLAYS_NAV_LABEL = new String("Overlays");
    private static final String MODIFY_HUD_NAV_LABEL = new String("Modify HUD");
    private static final String FRIENDS_TITLE = new String("Friends");
    private static final String FRIENDS_DESCRIPTION = new String("Players Echo will never target");
    private static final String FRIEND_SEARCH_PLACEHOLDER = new String("Search friends...");
    private static final String FRIEND_COUNT_SUFFIX = new String(" friends");
    private static final String FRIEND_ADD_ID = new String("friends/add");
    private static final String FRIEND_ADD_FOCUS_PROPERTY = new String("focus");
    private static final String FRIEND_NAME_PLACEHOLDER = new String("Add a player by name...");
    private static final String ADD_LABEL = new String("Add");
    private static final String NO_FRIENDS_LABEL = new String("No friends yet — add one above.");
    private static final String NO_FRIEND_MATCHES_LABEL = new String("No friends match that search.");
    private static final String FRIEND_CARD_PREFIX = new String("cards/friend/");
    private static final String FRIEND_LABEL = new String("Friend");
    private static final String REMOVE_LABEL = new String("Remove");
    private static final String CARD_HOVER_PROPERTY = new String("hover");
    private static final String CONFIGS_TITLE = new String("Configs");
    private static final String FRIEND_SEARCH_ID = new String("friends/search");
    private static final String CONFIG_SEARCH_ID = new String("configs/search");
    private static final String SCRIPT_SEARCH_ID = new String("scripts/search");
    private static final String RESET_FILTER_LABEL = new String("Reset");
    private static final String FILTER_LABEL = new String("Filter");
    private static final String UPLOAD_LABEL = new String("Upload");
    private static final String SCRIPTS_TITLE = new String("Scripts");
    private static final String SCRIPTS_DESCRIPTION = new String("Browse, enable and manage your scripts");
    private static final String SCRIPT_SEARCH_PLACEHOLDER = new String("Search scripts...");
    private static final String SCRIPT_SEGMENT_ID = new String("scripts/segment");
    private static final String PUBLIC_SEGMENT_LABEL = new String("Public");
    private static final String PRIVATE_SEGMENT_LABEL = new String("Private");
    private static final String SCRIPT_COUNT_SUFFIX = new String(" scripts");
    private static final String NEW_SCRIPT_LABEL = new String("New Script");
    private static final String SCRIPT_CARD_PREFIX = new String("cards/script/");
    private static final String SCRIPT_PUBLIC_LABEL = new String("Public");
    private static final String SCRIPT_PRIVATE_LABEL = new String("Private");
    private static final String BY_AUTHOR_PREFIX = new String("by ");
    private static final String SCRIPT_ID_PREFIX = new String("scripts/");
    private static final String SCRIPT_TOGGLE_SUFFIX = new String("/toggle");
    private static final String SCRIPT_ENABLED_LABEL = new String("Enabled");
    private static final String SCRIPT_DISABLED_LABEL = new String("Disabled");
    private static final String EDIT_SCRIPT_LABEL = new String("Edit");
    private static final String SEGMENT_SELECTION_PROPERTY = new String("selection");
    private static final String CONFIGS_DESCRIPTION = new String("Browse, search and manage your configurations");
    private static final String CONFIG_SEARCH_PLACEHOLDER = new String("Search configs...");
    private static final String CONFIG_SEGMENT_ID = new String("configs/segment");
    private static final String YOUR_CONFIGS_LABEL = new String("Your configs");
    private static final String BROWSE_CONFIGS_LABEL = new String("Browse");
    private static final String CONFIG_COUNT_SUFFIX = new String(" configs");
    private static final String COPIES_OF_PREFIX = new String("Copies of ");
    private static final String NEW_CONFIG_LABEL = new String("New Config");
    private static final String SINGLEPLAYER_ID = new String("singleplayer");
    private static final String SINGLEPLAYER_LABEL = new String("Singleplayer");
    private static final String REFRESH_CONFIGS_ERROR = new String("Failed to refresh cloud configs UI");
    private static final String CONFIG_ICON_ERROR = new String("Failed to decode config server icon for profile '{}': {}");
    private static final String MONTHS_AGO_SUFFIX = new String("mo ago");
    private static final String WEEKS_AGO_SUFFIX = new String("w ago");
    private static final String DAYS_AGO_SUFFIX = new String("d ago");
    private static final String HOURS_AGO_SUFFIX = new String("h ago");
    private static final String MINUTES_AGO_SUFFIX = new String("m ago");
    private static final String CONFIG_CARD_PREFIX = new String("cards/config/");
    private static final String ACTIVE_CONFIG_LABEL = new String("Active");
    private static final String OWN_CONFIG_LABEL = new String("Yours");
    private static final String CONFIG_COPIES_SUFFIX = new String(" copies");
    private static final String ADDED_CONFIG_LABEL = new String("Added");
    private static final String CONFIG_AUTHOR_PREFIX = new String("by ");
    private static final String CONFIG_SOURCE_AUTHOR_PREFIX = new String("from ");
    private static final String UNPUBLISHED_CHANGES_LABEL = new String("Unpublished changes");
    private static final String UPDATE_AVAILABLE_LABEL = new String("Update available");
    private static final String CONFIG_MENU_ACTION_SEPARATOR = new String("/menu/");
    private static final String SETTING_ID_SEGMENT = new String("setting");
    private static final String CHOICE_OPTION_ID_SEGMENT = new String("option");
    private static final String DEFAULT_SETTING_SECTION = new String("Settings");
    private static final String CONTINUED_SECTION_SUFFIX = new String(" ·");
    private static final String EMPTY_SETTINGS_TITLE = new String("Settings");
    private static final String EMPTY_SETTINGS_DESCRIPTION = new String("This module has no settings.");
    private static final float[] SETTING_COLUMN_X;
    private static final String MAIN_SETTINGS_LABEL = new String("Main");
    private static final String TOGGLE_FEATURE_LABEL = new String("Toggle");
    private static final String VISIBLE_FEATURE_LABEL = new String("Visible");
    private static final String FEATURE_VISIBLE_SUFFIX = new String("/visible");
    private static final String BIND_LABEL = new String("Bind");
    private static final String BIND_ID_PREFIX = new String("bind/");
    private static final String BIND_MODE_ID_PREFIX = new String("bind/");
    private static final String BIND_MODE_SUFFIX = new String("/mode");
    private static final String BIND_WIDTH_PROPERTY = new String("width");
    private static final String PRESS_KEY_LABEL = new String("Press a key...");
    private static final String SIDEBAR_CATEGORY_INPUT_ID = new String("sidebar/category");
    private static final String SIDEBAR_MODULE_INPUT_PREFIX = new String("sidebar/module/");
    private static final String SIDEBAR_CONFIGS_INPUT_ID = new String("sidebar/configs");
    private static final String SIDEBAR_FRIENDS_INPUT_ID = new String("sidebar/friends");
    private static final String SIDEBAR_OVERLAYS_INPUT_ID = new String("sidebar/overlays");
    private static final String SIDEBAR_HUD_EDITOR_INPUT_ID = new String("sidebar/hud-editor");
    private static final String AUTOSAVE_CONFIG_ID = new String("_autosave");
    private static final String FRIEND_SEARCH_INPUT_ID = new String("friends/search");
    private static final String FRIEND_NAME_INPUT_ID = new String("friends/name");
    private static final String FRIEND_ADD_INPUT_ID = new String("friends/add");
    private static final String FRIEND_INPUT_PREFIX = new String("friends/");
    private static final String FRIEND_REMOVE_SUFFIX = new String("/remove");
    private static final String NEW_EDITOR_SCRIPT_LABEL = new String("New Script");
    private static final String DEFAULT_EDITOR_LANGUAGE = new String("Lua");
    private static final String EDITOR_IMPORT_LABEL = new String("Import");
    private static final String EDITOR_SAVE_LABEL = new String("Save");
    private static final String EDITOR_COMPILE_LABEL = new String("Compile");
    private static final String EDITOR_SCROLL_ID = new String("editor/scroll");
    private static final String EDITOR_SCROLL_PROPERTY = new String("line");
    private static final String EDITOR_ACTIVE_LINE_ID = new String("editor/active-line");
    private static final String EDITOR_ACTIVE_LINE_PROPERTY = new String("wash");
    private static final String EDITOR_OUTPUT_LABEL = new String("Output");
    private static final String EDITOR_SAVED_LABEL = new String("Saved");
    private static final String EDITOR_ENCODING_LINE_PREFIX = new String(" · UTF-8 · Ln ");
    private static final String EDITOR_COLUMN_PREFIX = new String(", Col ");
    private static final String EDITOR_STATUS_SEPARATOR = new String(" · ");
    private static final String EDITOR_OUTPUT_PREFIX = new String("> ");
    private static final String EDITOR_MODIFIED_LABEL = new String("Modified");
    private static final String SYNTAX_COMMENT_PREFIX = new String("--");
    private static final String PULSE_COMMENT_PREFIX = new String("--");
    private static final Set<String> EDITOR_KEYWORDS;
    private static final String MODAL_NEW_TITLE = new String("New Config");
    private static final String MODAL_EDIT_TITLE = new String("Edit Config");
    private static final String MODAL_COPY_TITLE = new String("Add Another Copy");
    private static final String MODAL_PUBLISH_TITLE = new String("Publish Config");
    private static final String MODAL_UPDATE_TITLE = new String("Update Public Config");
    private static final String MODAL_NEW_DESCRIPTION = new String("Save your current settings as a new configuration");
    private static final String MODAL_EDIT_ACTIVE_DESCRIPTION = new String("Saves these details and your current settings");
    private static final String MODAL_EDIT_DESCRIPTION = new String("Rename or describe this configuration");
    private static final String MODAL_COPY_DESCRIPTION = new String("Keeps your current copy and adds a fresh one, then loads it");
    private static final String MODAL_PUBLISH_DESCRIPTION = new String("Share a snapshot. Later changes stay private until you update it");
    private static final String MODAL_UPDATE_DESCRIPTION = new String("Replace the public version with this config's current settings");
    private static final String MODAL_PUBLIC_NAME_LABEL = new String("Public name");
    private static final String MODAL_NAME_LABEL = new String("Name");
    private static final String MODAL_NAME_ID = new String("modal/name");
    private static final String MODAL_NAME_PLACEHOLDER = new String("e.g. rage pvp");
    private static final String MODAL_TYPE_LABEL = new String("Type");
    private static final String MODAL_GHOST_LABEL = new String("Ghost");
    private static final String MODAL_RAGE_LABEL = new String("Rage");
    private static final String MODAL_DESCRIPTION_LABEL = new String("Description (optional)");
    private static final String MODAL_DESCRIPTION_ID = new String("modal/description");
    private static final String MODAL_DESCRIPTION_PLACEHOLDER = new String("What is this config for?");
    private static final String MODAL_FRIENDS_NOTICE = new String("Your friends list isn't shared");
    private static final String MODAL_CANCEL_LABEL = new String("Cancel");
    private static final String MODAL_CREATE_LABEL = new String("Create");
    private static final String MODAL_SAVE_LABEL = new String("Save");
    private static final String MODAL_ADD_LABEL = new String("Add");
    private static final String MODAL_PUBLISH_LABEL = new String("Publish");
    private static final String MODAL_UPDATE_LABEL = new String("Update");
    private static final String TYPE_CHOICE_ID_PREFIX = new String("choice/");
    private static final String CONFIG_MENU_INPUT_PREFIX = new String("configs/menu/");
    private static final String CONFIG_MENU_INPUT_SEPARATOR = new String("/menu/");
    private static final String MODAL_CLOSE_INPUT_ID = new String("modal/close");
    private static final String MODAL_CANCEL_INPUT_ID = new String("modal/cancel");
    private static final String MODAL_COMMIT_INPUT_ID = new String("modal/commit");
    private static final String MODAL_NAME_INPUT_ID = new String("modal/name");
    private static final String MODAL_GHOST_INPUT_ID = new String("modal/type/ghost");
    private static final String MODAL_RAGE_INPUT_ID = new String("modal/type/rage");
    private static final String MODAL_DESCRIPTION_INPUT_ID = new String("modal/description");
    private static final String RAGE_TYPE_ID = new String("RAGE");
    private static final String COPY_NUMBER_PREFIX = new String(" (");
    private static final String UNTITLED_CONFIG_NAME = new String("untitled config");
    private static final String MODAL_GHOST_TYPE_ID = new String("GHOST");
    private static final String MODAL_RAGE_TYPE_ID = new String("RAGE");
    private static final String CONFIG_SAVED_STATUS = new String("Saved");
    private static final String CONFIGS_YOURS_INPUT_ID = new String("configs/yours");
    private static final String CONFIGS_BROWSE_INPUT_ID = new String("configs/browse");
    private static final String CONFIG_SEARCH_INPUT_ID = new String("configs/search");
    private static final String CONFIG_SOURCE_FILTER_INPUT_ID = new String("configs/source-filter");
    private static final String CONFIG_MORE_INPUT_PREFIX = new String("configs/");
    private static final String CONFIG_MORE_SUFFIX = new String("/more");
    private static final String CONFIG_ACTION_INPUT_PREFIX = new String("configs/");
    private static final String CONFIG_PRIMARY_SUFFIX = new String("/primary");
    private static final String CONFIG_SECONDARY_SUFFIX = new String("/secondary");
    private static final String NEW_CONFIG_INPUT_ID = new String("configs/new");
    private static final String FEATURE_BIND_INPUT_SUFFIX = new String("/bind");
    private static final String FEATURE_TOGGLE_MODE_INPUT_SUFFIX = new String("/bind-mode/toggle");
    private static final String FEATURE_HOLD_MODE_INPUT_SUFFIX = new String("/bind-mode/hold");
    private static final String FEATURE_VISIBLE_INPUT_SUFFIX = new String("/visible");
    private static final String SLIDER_VISUAL_VALUE = new String("visual-value");
    private static final String SLIDER_VISUAL_MINIMUM = new String("visual-min");
    private static final String SLIDER_VISUAL_MAXIMUM = new String("visual-max");
    private static final String RANGE_VALUE_SEPARATOR;
    private static final String SETTING_FLOAT_FORMAT = new String("%.2f");
    private static final String NONEDITABLE_SETTING_LABEL = new String("Not editable here");
    private static final String CHOICE_CHEVRON_ID = new String("chevron");
    private static final String CHOICE_CHEVRON_PROPERTY = new String("up");
    private static final String SETTING_PRESS_KEY_LABEL = new String("Press a key...");
    private static final String MULTI_SEARCH_ID_SEGMENT = new String("search");
    private static final String MULTI_SEARCH_PLACEHOLDER = new String("Search options");
    private static final String NO_MULTI_MATCHES_LABEL = new String("No options match that search.");
    private static final String OPTION_FILL_PROPERTY = new String("fill");
    private static final String OPTION_INDICATOR_PROPERTY = new String("indicator");
    private static final String PREVIEW_PREVIOUS_ID = new String("prev");
    private static final String PREVIEW_NEXT_ID = new String("next");
    private static final String EDIT_SETTING_LABEL = new String("Edit");
    private static final String ANY_REGISTRY_ENTRY_LABEL = new String("Any");
    private static final String SELECTED_REGISTRY_ENTRIES_SUFFIX = new String(" selected");
    private static final String COLOR_PICKER_TITLE = new String("Color");
    private static final String COLOR_HEX_FORMAT = new String("#%02X%02X%02X");
    private static final String COLOR_PRESETS_LABEL = new String("PRESETS");
    private static final String COLOR_TARGET_ID_SEGMENT = new String("color-target");
    private static final String COLOR_TARGET_SELECTION = new String("selection");
    private static final int[] COLOR_PRESETS;
    private static final String REGISTRY_SEARCH_ID = new String("picker/search");
    private static final String REGISTRY_SEARCH_PLACEHOLDER = new String("Search entries");
    private static final String REGISTRY_GROUPS_LABEL = new String("GROUPS");
    private static final String REGISTRY_NO_MATCHES_LABEL = new String("No entries match that search.");
    private static final String REGISTRY_ANY_ALLOWED_LABEL = new String("Any entry allowed");
    private static final String REGISTRY_SELECTION_SUFFIX = new String(" selected");
    private static final String REGISTRY_CLEAR_LABEL = new String("Clear");
    private static final String REGISTRY_ENTRY_ID_PREFIX = new String("picker/entry/");
    private static final String RANGE_DRAG_OFFSET_PROPERTY = new String("offset");
    private static final String RANGE_OFFSET_PROPERTY = new String("offset");
    private static final String RANGE_EDITOR_ID = new String("editor");
    private static final String RANGE_EDITOR_OPEN_PROPERTY = new String("open");
    private static final String RANGE_CHIP_ID = new String("chip");
    private static final String RANGE_CHIP_LIT_PROPERTY = new String("lit");
    private static final String RANGE_TOGGLE_ID = new String("toggle");
    private static final String NAMED_RANGE_SLIDER_ID = new String("range");
    private static final String ANY_RANGE_LABEL = new String("Any");
    private static final String UNDER_RANGE_PREFIX = new String("Under ");
    private static final String NAMED_RANGE_FLOAT_FORMAT = new String("%.1f");
    private static final String SCRIPT_PUBLIC_INPUT_ID = new String("scripts/public");
    private static final String SCRIPT_PRIVATE_INPUT_ID = new String("scripts/private");
    private static final String SCRIPT_SEARCH_INPUT_ID = new String("scripts/search");
    private static final String SCRIPT_FILTER_INPUT_ID = new String("scripts/filter");
    private static final String SCRIPT_UPLOAD_INPUT_ID = new String("scripts/upload");
    private static final String SCRIPT_INPUT_PREFIX = new String("scripts/");
    private static final String SCRIPT_EDIT_SUFFIX = new String("/edit");
    private static final String NEW_SCRIPT_INPUT_ID = new String("scripts/new");
    private static final String EDITOR_BACK_INPUT_ID = new String("editor/back");
    private static final String EDITOR_IMPORT_INPUT_ID = new String("editor/import");
    private static final String EDITOR_SAVE_INPUT_ID = new String("editor/save");
    private static final String EDITOR_COMPILE_INPUT_ID = new String("editor/compile");
    private static final String EDITOR_CODE_INPUT_ID = new String("editor/code");
    private static final String EDITOR_SCROLLBAR_INPUT_ID = new String("editor/scrollbar");
    private static final String EDITOR_TAB_TEXT = new String("    ");
    private static final String CLIPBOARD_WINDOWS_NEWLINE = new String("\r\n");
    private static final String SCRIPT_NOT_LOADED_MESSAGE = new String("not loaded — run to compile");
    private static final String SCRIPT_LOADED_PREFIX = new String("loaded as ");
    private static final String NEW_SCRIPT_MESSAGE = new String("new script — save to create the file");
    private static final String SCRIPT_WRITE_FAILURE_PREFIX = new String("could not write ");
    private static final String SCRIPT_SAVED_PREFIX = new String("saved ");
    private static final String LOCAL_SCRIPT_AUTHOR = new String("Local");
    private static final String LOCAL_SCRIPT_LANGUAGE = new String("JS");
    private static final String UNTITLED_SCRIPT_FILENAME = new String("untitled.js");
    private static final String UNTITLED_SCRIPT_PREFIX = new String("untitled-");
    private static final String SCRIPT_FILE_EXTENSION = new String(".js");
    private static final String NUMERIC_VALUE_INPUT_ID = new String("value");
    private static final String RANGE_MAX_INPUT_ID = new String("max");
    private static final String RANGE_MIN_INPUT_ID = new String("min");
    private static final String MULTI_SEARCH_INPUT_ID = new String("search");
    private static final String MULTI_OPTION_INPUT_ID = new String("option");
    private static final String PREVIEW_PREVIOUS_INPUT_ID = new String("prev");
    private static final String PREVIEW_NEXT_INPUT_ID = new String("next");
    private static final String COLOR_TARGET_INPUT_PREFIX = new String("color/target/");
    private static final String COLOR_SQUARE_INPUT_ID = new String("color/sb");
    private static final String COLOR_HUE_INPUT_ID = new String("color/hue");
    private static final String COLOR_ALPHA_INPUT_ID = new String("color/alpha");
    private static final String COLOR_PRESET_INPUT_PREFIX = new String("color/preset/");
    private static final String REGISTRY_SEARCH_INPUT_ID = new String("picker/search");
    private static final String REGISTRY_GROUP_INPUT_PREFIX = new String("picker/group/");
    private static final String REGISTRY_ENTRY_INPUT_PREFIX = new String("picker/entry/");
    private static final String REGISTRY_CLEAR_INPUT_ID = new String("picker/clear");
    private static final String NAMED_TOGGLE_INPUT_ID = new String("toggle");
    private static final String NAMED_CHIP_INPUT_ID = new String("chip");
    private static final String NAMED_UP_INPUT_ID = new String("up");
    private static final String NAMED_DOWN_INPUT_ID = new String("down");
    private static final String NAMED_SLIDER_INPUT_ID = new String("range");
    private static final String NAMED_PRESET_INPUT_ID = new String("preset");
    private static final String NAMED_MAX_INPUT_ID = new String("max");
    private static final String NAMED_MIN_INPUT_ID = new String("min");
    private static final String CONDITION_ID_PREFIX = new String("condition/");
    private static final String CONDITION_FILL_PROPERTY = new String("fill");
    private static final String CONDITION_INDICATOR_PROPERTY = new String("indicator");

    /** Original editor template, captured from the shipped string resolver. */
    private static final String DEFAULT_SCRIPT;

    public static final DashboardController INSTANCE;

    static {
        // Original functional store order: currentData, lastService, defaultClient,
        // internalAdapter, primaryPage, lastGroup. Both range paths share currentData.
        RANGE_VALUE_SEPARATOR = new String(" - ");
        DEFAULT_SCRIPT = new String("""
            const script = {
                name: "New Script",
                description: "",
                category: "UTILITY",
                key: -1,

                events: {
                    enable: function () {
                        Console.log("enabled");
                    },

                    disable: function () {
                    },

                    tick: function () {
                    }
                }
            };
            """);
        COLOR_PRESETS = new int[]{0xff00b294, 0xff48867b, 0xffd9511d, 0xffffffff,
                0xff111111, 0xffffb900, 0xffd8191e, 0xff8e44ad, 0xff45519c};
        SETTING_COLUMN_X = new float[]{330.0f, 860.0f, 1390.0f};
        EDITOR_KEYWORDS = Set.of(new String("local"), new String("function"),
                new String("if"), new String("then"), new String("end"), new String("return"),
                new String("and"), new String("or"), new String("not"));
        INSTANCE = new DashboardController();
    }

    private RenderDevice renderDevice;
    private SvgTextureCache svgTextures;
    private CanvasMetrics canvas;
    private final ImmediateUiContext ui;
    private final AnimationRegistry animations;
    private DashboardPage page;
    private Feature selectedFeature;
    private Category expandedCategory;
    private Feature selectedOverlay;
    private int keyboardNavigationIndex;
    private String moduleSearch;
    private String friendSearch;
    private String friendName;
    private String configSearch;
    private String scriptSearch;
    private boolean enabledScriptsOnly;
    private boolean publicScripts;
    private boolean browsePublicConfigs;
    private String copiesSourceId;
    private String copiesSourceName;
    private float copiesFilterWidth;
    private float configScroll;
    private final List<ScriptCard> scriptCards;
    private final List<SettingRow> settingRows;
    private final List<SettingSectionLayout> settingSections;
    private final Map<UiId, String> multiChoiceSearches;
    private NamedRange editedNamedRange;
    private NamedRange draggedNamedRange;
    private NamedRange numericNamedRange;
    private float namedRangeDragOffset;
    private FloatRangeSetting draggedFloatRange;
    private boolean draggingUpperRange;
    private boolean draggingUpperNamedRange;
    private boolean capturingBind;
    private Feature bindingFeature;
    private KeybindSetting bindingSetting;
    private boolean suppressCharacters;
    private boolean selectingEditorText;
    private ColorSetting openColorPicker;
    private RegistryListSetting<?> openRegistryPicker;
    private String registrySearch;
    private String registryCacheKey;
    private float registryScroll;
    private final List<RegistryPickerRow> registryRows;
    private final List<SelectionState> registryGroupStates;
    private Setting numericSetting;
    private boolean editingUpperRange;
    private String numericText;
    private TextSetting textSetting;
    private String settingText;
    private MultiChoiceSetting searchedMultiChoice;
    private final Map<UiId, Float> multiChoiceScroll;
    private final Map<Feature, Float> featureBindWidths;
    private final List<FeatureRow> sidebarRows;
    private final Map<Category, Float> categoryOpenness;
    private final Map<Category, Float> categoryHeadingY;
    private float sidebarScroll;
    private float sidebarContentHeight;
    private float sidebarFooterY;
    private final List<ConfigCard> configCards;
    private final Map<String, TextureView> serverIcons;
    private String activeConfigId;
    private final ScriptEditorState editor;
    private int selectedScriptIndex;
    private final List<String> editorOutput;
    private TextureView backdrop;
    private long openedAtNanos;
    private long pageChangedAtNanos;
    private long closingAtNanos;
    private long previousFrameNanos;
    private long editorPulseStartedNanos;
    private float deltaSeconds;
    private boolean closing;
    private float viewportScale;
    private float viewportLeft;
    private float viewportTop;
    private float zoom;
    private float openingOpacity;
    private float layerOpacity;
    private float animationScale;
    private float pivotX;
    private float pivotY;
    private float localOffsetX;
    private float localOffsetY;
    private float verticalOffset;
    private float iconRasterScale;
    private float sidebarIndicatorY;
    private int pageTransitionDirection;
    private float mouseX;
    private float mouseY;
    private boolean modalOpen;
    private ConfigCard modalCard;
    private ChoiceSetting openChoice;
    private UiRect choiceAnchor;
    private float minimumChoiceWidth;
    private float choiceScroll;
    private long choiceOpenedAtNanos;
    private ConfigModalMode modalMode;
    private long modalOpenedAtNanos;
    private String modalName;
    private String modalDescription;
    private boolean modalGhost;
    private TextInputFocus textFocus;
    private String contextMenuCardId;
    private UiRect contextMenuAnchor;
    private long contextMenuOpenedAtNanos;
    private String pendingConfirmationId;
    private long confirmationStartedAtMillis;
    private String statusConfigId;
    private String statusMessage;
    private long statusStartedAtMillis;

    private DashboardController() {
        page = DashboardPage.MODULE;
        configCards = new ArrayList<>();
        statusMessage = "";
        contextMenuAnchor = new UiRect(0.0f, 0.0f, 0.0f, 0.0f);
        modalMode = ConfigModalMode.NEW;
        scriptCards = new ArrayList<>();
        ui = new ImmediateUiContext();
        animations = new AnimationRegistry();
        editorOutput = new ArrayList<>();
        categoryOpenness = new HashMap<>();
        categoryHeadingY = new HashMap<>();
        featureBindWidths = new HashMap<>();
        settingRows = new ArrayList<>();
        settingSections = new ArrayList<>();
        registrySearch = "";
        choiceAnchor = new UiRect(0.0f, 0.0f, 0.0f, 0.0f);
        minimumChoiceWidth = 96.0f;
        settingText = "";
        numericText = "";
        modalGhost = true;
        copiesSourceName = "";
        textFocus = TextInputFocus.NONE;
        selectedScriptIndex = -1;
        modalName = "";
        modalDescription = "";
        configSearch = "";
        scriptSearch = "";
        friendSearch = "";
        moduleSearch = "";
        friendName = "";
        editor = new ScriptEditorState(DEFAULT_SCRIPT);
        canvas = CanvasMetrics.fit(1920.0f, 1080.0f, 1920.0f, 1080.0f);
        iconRasterScale = -1.0f;
        zoom = 1.0f;
        openingOpacity = 1.0f;
        animationScale = 1.0f;
        layerOpacity = 1.0f;
        mouseX = -1.0f;
        mouseY = -1.0f;
        sidebarIndicatorY = 212.0f;
        sidebarFooterY = 790.0f;
        previousFrameNanos = System.nanoTime();
        openedAtNanos = System.nanoTime();
        pageChangedAtNanos = System.nanoTime();
        sidebarRows = new ArrayList<>();
        activeConfigId = "";
        serverIcons = new HashMap<>();
        pageTransitionDirection = 1;
        multiChoiceScroll = new HashMap<>();
        multiChoiceSearches = new HashMap<>();
        registryRows = new ArrayList<>();
        registryGroupStates = new ArrayList<>();
        registryCacheKey = "";
        animations.setDurationScale(readMotionMultiplier());
    }

    public void initialize(RenderDevice device) {
        if (svgTextures != null && renderDevice != device) {
            svgTextures.close();
            svgTextures = null;
        }
        renderDevice = device;
        if (svgTextures == null) svgTextures = new SvgTextureCache(device, Echo.LOGGER);
        float scale = Math.min((float) guiWidth() / 1920.0f, (float) guiHeight() / 1080.0f);
        iconRasterScale = Math.max(0.01f, (float) ((double) scale * 2.0));
        preloadDashboardIcons(iconRasterScale);
    }

    public void onScreenOpened() {
        requestRefresh();
        refreshData();
        openedAtNanos = System.nanoTime();
        previousFrameNanos = openedAtNanos;
        closing = false;
        closingAtNanos = 0L;
        editorPulseStartedNanos = 0L;
        ui.setEnabled(false);
    }

    /** Original decodeLayer: preserve the selected object, including stale selections. */
    private void refreshData() {
        List<Category> categories = FeatureNameAssignments.categories();
        if (categories.isEmpty()) return;
        if (expandedCategory != null && !categories.contains(expandedCategory)) {
            expandedCategory = categories.getFirst();
        }
        if (selectedFeature != null) return;
        if (expandedCategory != null) {
            List<Feature> features = FeatureNameAssignments.features(expandedCategory, false);
            selectedFeature = features.isEmpty() ? null : features.getFirst();
        }
        if (selectedFeature == null) selectedFeature = findInitialFeature(categories);
        if (page == DashboardPage.MODULE) keyboardNavigationIndex = selectedFeatureIndex();
    }

    private Feature findInitialFeature(List<Category> categories) {
        Feature targets = FeatureManager.find(TargetsFeature.class).orElse(null);
        if (targets != null && targets.isVisible()) return targets;
        for (Category category : categories) {
            List<Feature> features = FeatureNameAssignments.features(category, false);
            if (!features.isEmpty()) return features.getFirst();
        }
        return null;
    }

    private int selectedFeatureIndex() {
        int index = 0;
        for (Category category : FeatureNameAssignments.categories()) {
            for (Feature feature : FeatureNameAssignments.features(category, false)) {
                if (feature == selectedFeature) return index;
                index++;
            }
        }
        return index;
    }

    private void requestRefresh() {
        if (Echo.featureConfig == null) return;
        Echo.featureConfig.listAsync().thenAcceptAsync(this::acceptConfigMetadata, MinecraftClient.getInstance());
    }

    /** Original setQuery, including the encompassing catch and ordered partial updates. */
    private void acceptConfigMetadata(List<ConfigMetadata> profiles) {
        try {
            configCards.clear();
            for (ConfigMetadata metadata : profiles) {
                if (metadata == null || metadata.name().startsWith("_")) continue;
                String server = metadata.serverName();
                String serverLabel = server == null || server.isBlank() || server.equals(SINGLEPLAYER_ID)
                        ? SINGLEPLAYER_LABEL : server;
                if (metadata.active()) activeConfigId = metadata.id();
                configCards.add(new ConfigCard(metadata, serverLabel, serverIcon(metadata),
                        formatUpdatedTime(metadata.updatedAt()), metadata.id().equals(activeConfigId) || metadata.active()));
            }
            for (ConfigCard card : configCards) {
                ConfigCard source = card.sourceId.isBlank() ? null : findConfig(card.sourceId);
                if (source != null && source.publicVisibility && !source.owned) card.sourceAuthor = source.author;
            }
            if (!configsAvailable()) browsePublicConfigs = false;
        } catch (Exception failure) {
            Echo.LOGGER.error(REFRESH_CONFIGS_ERROR, failure);
        }
    }

    private ConfigCard findConfig(String id) {
        if (id == null || id.isBlank()) return null;
        for (ConfigCard card : configCards) if (card.id.equals(id)) return card;
        return null;
    }

    private TextureView serverIcon(ConfigMetadata metadata) {
        if (metadata.serverIcon() == null || Echo.renderDevice == null) return null;
        try {
            return serverIcons.computeIfAbsent(metadata.id(), ignored ->
                    ImageDecoder.decodeTexture(Echo.renderDevice, metadata.serverIcon()));
        } catch (Exception failure) {
            Echo.LOGGER.warn(CONFIG_ICON_ERROR, metadata.name(), failure.getMessage());
            return null;
        }
    }

    private String formatUpdatedTime(long updatedAtMillis) {
        if (updatedAtMillis <= 0L) return "";
        long minutes = (System.currentTimeMillis() - updatedAtMillis) / 60_000L;
        if (minutes >= 43_200L) return minutes / 43_200L + MONTHS_AGO_SUFFIX;
        if (minutes >= 10_080L) return minutes / 10_080L + WEEKS_AGO_SUFFIX;
        if (minutes >= 1_440L) return minutes / 1_440L + DAYS_AGO_SUFFIX;
        if (minutes >= 60L) return minutes / 60L + HOURS_AGO_SUFFIX;
        return Math.max(1L, minutes) + MINUTES_AGO_SUFFIX;
    }

    private void preloadDashboardIcons(float scale) {
        preloadIcon(scale, DashboardIcon.SIDEBAR_GLOW, 270.0f, 80.0f);
        preloadIcon(scale, DashboardIcon.LOGO, 44.0f, 44.0f);
        for (DashboardIcon icon : new DashboardIcon[]{DashboardIcon.COMBAT, DashboardIcon.VISUALS,
                DashboardIcon.UTILITY, DashboardIcon.WORLD, DashboardIcon.PLANET, DashboardIcon.MANAGER,
                DashboardIcon.LEGIT, DashboardIcon.FILTER, DashboardIcon.EDITOR_IMPORT}) {
            preloadIcon(scale, icon, 16.0f, 16.0f);
        }
        for (DashboardIcon icon : new DashboardIcon[]{DashboardIcon.CONFIGS, DashboardIcon.SCRIPTS,
                DashboardIcon.FRIENDS, DashboardIcon.PROFILE, DashboardIcon.OVERLAYS, DashboardIcon.MODIFY_HUD,
                DashboardIcon.DELETE, DashboardIcon.MORE}) {
            preloadIcon(scale, icon, 20.0f, 20.0f);
        }
        for (DashboardIcon icon : new DashboardIcon[]{DashboardIcon.CHEVRON, DashboardIcon.GHOST,
                DashboardIcon.DANGER, DashboardIcon.SCRIPT_CARD, DashboardIcon.EDITOR_RUN}) {
            preloadIcon(scale, icon, 14.0f, 14.0f);
        }
        for (DashboardIcon icon : new DashboardIcon[]{DashboardIcon.PUBLIC, DashboardIcon.PRIVATE}) {
            preloadIcon(scale, icon, 12.0f, 12.0f);
        }
        preloadIcon(scale, DashboardIcon.PLUS, 24.0f, 24.0f);
        preloadIcon(scale, DashboardIcon.EDITOR_SCRIPT, 18.0f, 18.0f);
        preloadIcon(scale, DashboardIcon.BIND_TOGGLE, 11.25f, 11.25f);
        preloadIcon(scale, DashboardIcon.BIND_HOLD, 11.162f, 15.5625f);
    }

    /** Original TransportValidator.buildOption: complete frame/update orchestration. */
    @Subscribe
    public void onHudRender(HudRenderEvent event) {
        if (!(MinecraftRenderState.getScreen() instanceof DashboardScreen)) return;
        UiRenderer renderer = event.getRenderer();
        refreshData();
        updateCanvas();
        updateFrameClock();
        float width = guiWidth();
        float height = guiHeight();
        ui.beginFrame(width, height, 1920.0f, 1080.0f);
        animations.beginFrame(System.nanoTime() / 1.0e9, deltaSeconds);
        layoutSidebarRows();
        sidebarIndicatorY = animations.spring(UiId.ROOT.child(SIDEBAR_MODULE_INDICATOR), "y",
                sidebarIndicatorY, selectedSidebarIndicatorY(), SpringSpec.UNDERDAMPED_3_8_HZ);

        float opening = easedProgress(openedAtNanos, 220);
        boolean openingInProgress = progress(openedAtNanos, 220) < 1.0f;
        ui.setEnabled(!openingInProgress && !closing);
        if (ui.enabled()) processImmediateInput();
        if (Echo.eventsDisabled) return;

        float closingOpacity = closing ? 1.0f - easedProgress(closingAtNanos, 180) : 1.0f;
        float visible = Math.min(opening, closingOpacity);
        zoom = 0.985f + 0.015f * visible;
        openingOpacity = visible;
        verticalOffset = 10.0f * (1.0f - visible);
        layerOpacity = 1.0f;
        backdrop = renderer.fullResolutionBackdrop;

        float backgroundProgress = Math.min(1.0f, progress(openedAtNanos, 160));
        renderer.drawBackdrop(renderer.halfResolutionBackdrop, 0.0f, 0.0f, width, height, 0.0f,
                backgroundProgress * closingOpacity);
        renderer.drawRoundedRect(0.0f, 0.0f, width, height, 0.0f,
                DashboardColors.multiplyAlpha(0x8c000000, backgroundProgress * closingOpacity));

        float sidebarProgress = easedProgress(openedAtNanos, 220);
        localOffsetY = 4.0f * (1.0f - sidebarProgress);
        layerOpacity = sidebarProgress;
        renderSidebar(renderer);
        localOffsetY = 0.0f;
        layerOpacity = 1.0f;
        renderer.flush();

        float pageProgress = easedProgress(pageChangedAtNanos, 220);
        float slideX = pageTransitionDirection * 22.0f * (1.0f - pageProgress);
        float contentProgress = easedProgress(openedAtNanos + 35_000_000L, 220);
        localOffsetY = 4.0f * (1.0f - contentProgress);
        renderer.pushScissor(renderX(330.0f), renderY(30.0f), renderSize(1560.0f), renderSize(1020.0f));
        localOffsetX = slideX;
        layerOpacity = pageProgress * contentProgress;
        switch (page) {
            case MODULE -> renderModulePage(renderer);
            case OVERLAYS -> renderOverlaysPage(renderer);
            case CONFIGS -> renderConfigsPage(renderer);
            case FRIENDS -> renderFriendsPage(renderer);
            case SCRIPTS -> renderScriptsPage(renderer);
            case EDITOR -> renderEditorPage(renderer);
        }
        localOffsetX = 0.0f;
        localOffsetY = 0.0f;
        layerOpacity = 1.0f;
        renderer.popScissor();
        renderer.flush();

        if (!modalOpen) {
            renderSettingPopups(renderer);
            renderConfigContextMenu(renderer);
            renderer.flush();
        }
        if (modalOpen) {
            renderConfigModal(renderer);
            renderer.flush();
        }
        ui.endFrame();
        animations.prune(600L);
        if (closing && progress(closingAtNanos, 180) >= 1.0f) {
            MinecraftRenderState.setScreen(null);
        }
    }

    private static float readMotionMultiplier() {
        try {
            return Math.max(0.0f, Float.parseFloat(System.getProperty("renderbase.echo.motionMultiplier", "1")));
        } catch (NumberFormatException failure) {
            return 1.0f;
        }
    }

    private static int guiWidth() {
        return (int) Math.ceil(MinecraftClient.getInstance().getWindow().getFramebufferWidth() / 2.0);
    }
    private static int guiHeight() {
        return (int) Math.ceil(MinecraftClient.getInstance().getWindow().getFramebufferHeight() / 2.0);
    }
    private void updateCanvas() {
        canvas = CanvasMetrics.fit(guiWidth(), guiHeight(), 1920.0f, 1080.0f);
        viewportScale = canvas.scale();
        viewportLeft = canvas.offsetX();
        viewportTop = canvas.offsetY();
        float rasterScale = (float) ((double) viewportScale * 2.0);
        if (svgTextures != null && Math.abs(rasterScale - iconRasterScale) > 0.001f) {
            svgTextures.invalidateSized();
            iconRasterScale = rasterScale;
        }
    }
    private void updateFrameClock() {
        long now = System.nanoTime();
        float elapsed = Math.min(0.05f, Math.max(0.0f, (float) (now - previousFrameNanos) / 1.0e9f));
        previousFrameNanos = now;
        deltaSeconds = elapsed;
    }

    /** Original stopAdapter: scrolling is clamped against the preceding frame's height. */
    private void layoutSidebarRows() {
        sidebarRows.clear();
        sidebarScroll = Math.max(0.0f, Math.min(sidebarScroll,
                Math.max(0.0f, sidebarContentHeight - DashboardLayout.availableContentHeight() + 40.0f)));
        float y = 147.0f - sidebarScroll;
        boolean searching = isModuleSearchActive();
        for (Category category : FeatureNameAssignments.categories()) {
            List<Feature> features = searching ? filteredFeatures(category)
                    : FeatureNameAssignments.features(category, false);
            float openness = animations.spring(UiId.ROOT.child(SIDEBAR_CATEGORY_PREFIX + category.name()),
                    CATEGORY_OPEN_PROPERTY, categoryOpenness.getOrDefault(category, 0.0f),
                    searching || expandedCategory == category ? 1.0f : 0.0f, SpringSpec.CRITICAL_4_5_HZ);
            categoryOpenness.put(category, openness);
            categoryHeadingY.put(category, y);
            sidebarRows.add(new FeatureRow(category, null, y, 40.0f, 1.0f));
            y += 52.0f;
            float rowsHeight = features.size() * 32 * openness;
            if (rowsHeight > 0.5f) {
                for (int index = 0; index < features.size(); index++) {
                    sidebarRows.add(new FeatureRow(category, features.get(index), y + index * 32,
                            32.0f, openness));
                }
            }
            y += rowsHeight + 12.0f * openness;
        }
        sidebarContentHeight = y + sidebarScroll - 147.0f;
    }

    private DashboardCanvasPainter painter(UiRenderer renderer) {
        DashboardTransform transform = new DashboardTransform(viewportLeft, viewportTop, viewportScale, zoom,
                animationScale, pivotX, pivotY, localOffsetX, localOffsetY, verticalOffset);
        return new DashboardCanvasPainter(renderer, svgTextures, transform, backdrop, combinedOpacity(), iconRasterScale);
    }

    private float combinedOpacity() { return Math.max(0.0f, Math.min(1.0f, openingOpacity * layerOpacity)); }

    /** Original setFactory: sidebar chrome, clipped category rows, then the footer. */
    private void renderSidebar(UiRenderer renderer) {
        painter(renderer).backdropPanel(30.0f, 30.0f, 270.0f, 1020.0f, 12.0f, -653521908, 0.0f, 0);
        painter(renderer).roundedIcon(DashboardIcon.SIDEBAR_GLOW, 30.0f, 30.0f, 270.0f, 80.0f, 12.0f, 1.0f, -1);
        painter(renderer).icon(DashboardIcon.LOGO, 143.0f, 48.0f, 44.0f, 44.0f, 1.0f, -1);
        float opacity = Math.max(0.0f, Math.min(1.0f, openingOpacity * layerOpacity));
        renderer.drawRoundedRect(renderX(30.0f), renderY(110.0f), renderSize(270.0f), renderSize(940.0f),
                renderSize(6.0f), renderSize(6.0f), renderSize(12.0f), renderSize(12.0f),
                DashboardColors.multiplyAlpha(100663295, opacity), renderSize(1.0f),
                DashboardColors.multiplyAlpha(452984831, opacity));
        painter(renderer).text(FontAsset.INTER_SEMI_BOLD, CATEGORIES_LABEL, 42.0f, 122.0f, 11.0f, -2130706433);
        renderer.pushScissor(renderX(30.0f), renderY(140.0f), renderSize(270.0f),
                renderSize(DashboardLayout.availableContentHeight()));
        Category heading = null;
        for (FeatureRow row : sidebarRows) {
            if (row.feature() == null) {
                renderCategoryHeading(renderer, 42.0f, row.y(), 246.0f, 40.0f, row.category().name,
                        categoryIcon(row.category()), isModuleSearchActive() || expandedCategory == row.category(), true);
                heading = row.category();
                continue;
            }
            if (row.category() != heading) continue;
            boolean selected = page == DashboardPage.MODULE && row.feature() == selectedFeature;
            float enabled = animations.tween(UiId.ROOT.child(SIDEBAR_MODULE_PREFIX + FeatureNameAssignments.id(row.feature())),
                    ENABLED_PROPERTY, row.feature().isEnabled() ? 1.0f : 0.0f,
                    row.feature().isEnabled() ? 1.0f : 0.0f, TransitionSpec.DURATION_160_MS);
            float previousOpacity = layerOpacity;
            layerOpacity *= row.visibility();
            painter(renderer).text(FontAsset.INTER, row.feature().getName(), 63.0f, row.y() + 8.0f, 14.0f,
                    selected ? -1 : DashboardColors.interpolate(1090519039, -1275068417, enabled));
            if (selected) {
                painter(renderer).roundedRect(270.0f, sidebarIndicatorY, 6.0f, 6.0f, 3.0f,
                        DashboardColors.multiplyAlpha(-1, row.visibility()));
            }
            layerOpacity = previousOpacity;
        }
        renderer.popScissor();
        sidebarFooterY = DashboardLayout.contentHeight();
        drawRule(renderer, 31.0f, sidebarFooterY - 12.0f, 268.0f, 1.0f, -13421773);
        renderSidebarLink(renderer, 42.0f, sidebarFooterY, CONFIGS_NAV_LABEL, DashboardIcon.CONFIGS,
                page == DashboardPage.CONFIGS);
        renderSidebarLink(renderer, 42.0f, sidebarFooterY + 52.0f, FRIENDS_NAV_LABEL, DashboardIcon.FRIENDS,
                page == DashboardPage.FRIENDS);
        renderSidebarLink(renderer, 42.0f, sidebarFooterY + 104.0f, OVERLAYS_NAV_LABEL, DashboardIcon.OVERLAYS,
                page == DashboardPage.OVERLAYS);
        renderSidebarLink(renderer, 42.0f, sidebarFooterY + 156.0f, MODIFY_HUD_NAV_LABEL, DashboardIcon.MODIFY_HUD, false);
    }

    private void renderCategoryHeading(UiRenderer renderer, float x, float y, float width, float height,
                                       CharSequence label, DashboardIcon icon, boolean selected, boolean chevron) {
        float target = selected ? 1.0f : 0.0f;
        float tint = animations.tween(UiId.ROOT.child(CATEGORY_TINT_ID).child(label), CATEGORY_TINT_PROPERTY,
                target, target, TransitionSpec.DURATION_160_MS);
        if (tint > 0.001f) painter(renderer).roundedRect(x, y, width, height, 8.0f,
                DashboardColors.multiplyAlpha(268435455, tint));
        int iconColor = DashboardColors.interpolate(-7368817, -1, tint);
        painter(renderer).icon(icon, x + 12.0f, y + 12.0f, 16.0f, 16.0f, 1.0f, iconColor);
        painter(renderer).text(FontAsset.INTER, label, x + 40.0f, y + 12.0f, 14.0f,
                DashboardColors.interpolate(-1275068417, -1, tint));
        if (chevron) painter(renderer).icon(DashboardIcon.CHEVRON, x + width - 27.0f, y + 13.0f,
                14.0f, 14.0f, 1.0f, iconColor);
    }

    private void renderSidebarLink(UiRenderer renderer, float x, float y, String label, DashboardIcon icon,
                                   boolean selected) {
        float target = selected ? 1.0f : 0.0f;
        float tint = animations.tween(UiId.ROOT.child(SIDEBAR_APP_PREFIX + label), APP_TINT_PROPERTY,
                target, target, TransitionSpec.DURATION_160_MS);
        if (tint > 0.001f) painter(renderer).roundedRect(x, y, 246.0f, 40.0f, 8.0f,
                DashboardColors.multiplyAlpha(268435455, tint));
        painter(renderer).text(FontAsset.INTER, label, x + 12.0f, y + 12.0f, 14.0f,
                DashboardColors.interpolate(-1275068417, -1, tint));
        painter(renderer).icon(icon, x + 214.0f, y + 10.0f, 20.0f, 20.0f, 1.0f,
                DashboardColors.interpolate(-5000269, -1, tint));
    }

    private void renderModulePage(UiRenderer renderer) {
        layoutSettings();
        painter(renderer).backdropPanel(330.0f, 30.0f, 1560.0f, 52.0f, 8.0f, -435418100, 0.0f, 0);
        boolean searchShown = textFocus == TextInputFocus.MODULE_SEARCH || isModuleSearchActive();
        float target = searchShown ? 1.0f : 0.0f;
        float searchOpacity = animations.tween(UiId.ROOT.child(MODULE_SEARCH_ID), SEARCH_SHOWN_PROPERTY,
                target, target, TransitionSpec.DURATION_140_MS);
        float previousOpacity = layerOpacity;
        if (searchOpacity < 0.999f) {
            layerOpacity = previousOpacity * (1.0f - searchOpacity);
            CharSequence title = moduleHeaderName();
            CharSequence description = moduleHeaderDescription();
            float titleWidth = painter(renderer).textWidth(FontAsset.INTER, title, 14.0f) + 16.0f;
            painter(renderer).roundedRect(342.0f, 42.0f, titleWidth, 28.0f, 4.0f, 268435455);
            painter(renderer).text(FontAsset.INTER, title, 350.0f, 48.0f, 14.0f, -1);
            drawRule(renderer, 358.0f + titleWidth, 50.0f, 1.0f, 12.0f, 452984831);
            painter(renderer).text(FontAsset.INTER, description, 374.0f + titleWidth, 48.5f, 13.0f, -2130706433);
        }
        if (searchOpacity > 0.001f) {
            layerOpacity = previousOpacity * searchOpacity;
            renderModuleSearch(renderer);
        }
        layerOpacity = previousOpacity;
        if (selectedFeature == null) return;
        if (selectedFeature.isKeybindable()) renderFeatureBinding(renderer);
        renderSettingsRows(renderer);
    }

    private void renderOverlaysPage(UiRenderer renderer) {
        new DashboardWidgetRenderer(painter(renderer)).section(330.0f, 30.0f, 1560.0f, 398.0f, 36.0f, OVERLAYS_LABEL);
        painter(renderer).text(FontAsset.INTER, OVERLAYS_DESCRIPTION, 346.0f, 78.0f, 12.5f, -2130706433);
        List<Feature> overlays = FeatureNameAssignments.overlays();
        for (int index = 0; index < overlays.size(); index++) {
            Feature feature = overlays.get(index);
            renderOverlayCard(renderer, feature, DashboardLayout.moduleCard(index), feature == selectedOverlay);
        }
        layoutSettings();
        if (selectedOverlay != null) renderSettingsRows(renderer);
    }

    private void renderFriendsPage(UiRenderer renderer) {
        painter(renderer).backdropPanel(330.0f, 30.0f, 1560.0f, 1020.0f, 12.0f, -234091508, 1.0f, -14408668);
        renderPageHeader(renderer, FRIENDS_TITLE, FRIENDS_DESCRIPTION, FRIEND_SEARCH_PLACEHOLDER, false);
        List<String> friends = filteredFriends();
        new DashboardWidgetRenderer(painter(renderer)).rightAlignedText(FontAsset.INTER,
                friends.size() + FRIEND_COUNT_SUFFIX, 1866.0f, 123.0f, 13.0f, 1728053247);
        boolean focused = textFocus == TextInputFocus.FRIEND_NAME;
        float target = focused ? 1.0f : 0.0f;
        float focus = animations.tween(UiId.ROOT.child(FRIEND_ADD_ID), FRIEND_ADD_FOCUS_PROPERTY,
                target, target, TransitionSpec.DURATION_140_MS);
        painter(renderer).panel(354.0f, 112.0f, 380.0f, 37.0f, 6.0f, 234881023, 1.0f,
                DashboardColors.interpolate(-14408668, 1509949439, focus));
        String label = friendName.isEmpty() && !focused ? FRIEND_NAME_PLACEHOLDER : friendName;
        painter(renderer).text(FontAsset.INTER, label, 370.0f, 123.5f, 13.0f,
                friendName.isEmpty() ? DashboardColors.interpolate(1308622847, 872415231, focus) : -1);
        if (focused && caretVisible()) {
            float x = 370.0f + painter(renderer).textWidth(FontAsset.INTER, friendName, 13.0f);
            drawRule(renderer, x, 122.0f, 1.5f, 17.0f, -1);
        }
        painter(renderer).roundedRect(742.0f, 112.0f, 86.0f, 37.0f, 6.0f,
                friendName.isBlank() ? 184549375 : -335544321);
        new DashboardWidgetRenderer(painter(renderer)).centeredText(FontAsset.INTER_SEMI_BOLD,
                ADD_LABEL, 785.0f, 123.5f, 13.0f, friendName.isBlank() ? -1711276033 : -15987700);
        int count = Math.min(friends.size(), 12);
        for (int index = 0; index < count; index++) {
            renderFriendCard(renderer, friends.get(index), 354 + index % 4 * 383,
                    171 + index / 4 * 96, 367.0f, 80.0f);
        }
        if (friends.isEmpty()) new DashboardWidgetRenderer(painter(renderer)).centeredText(FontAsset.INTER,
                friendSearch.isEmpty() ? NO_FRIENDS_LABEL : NO_FRIEND_MATCHES_LABEL,
                1110.0f, 500.0f, 14.0f, -2130706433);
    }

    private void renderScriptsPage(UiRenderer renderer) {
        painter(renderer).backdropPanel(330.0f, 30.0f, 1560.0f, 1020.0f, 12.0f, -234091508, 1.0f, -14408668);
        renderPageHeader(renderer, SCRIPTS_TITLE, SCRIPTS_DESCRIPTION, SCRIPT_SEARCH_PLACEHOLDER, true);
        renderSegmentedControl(renderer, SCRIPT_SEGMENT_ID, 354.0f, 112.0f, 164.0f, 37.0f,
                publicScripts, PUBLIC_SEGMENT_LABEL, PRIVATE_SEGMENT_LABEL, true);
        List<ScriptCard> scripts = filteredScripts();
        new DashboardWidgetRenderer(painter(renderer)).rightAlignedText(FontAsset.INTER,
                scripts.size() + SCRIPT_COUNT_SUFFIX, 1866.0f, 123.0f, 13.0f, 1728053247);
        int count = Math.min(scripts.size(), 6);
        for (int index = 0; index < count; index++) {
            renderScriptCard(renderer, scripts.get(index), 354 + index % 3 * 509,
                    165 + index / 3 * 160, 493.0f, 144.0f);
        }
        renderNewCard(renderer, 354 + count % 3 * 509, 165 + count / 3 * 160, 493.0f, 144.0f, NEW_SCRIPT_LABEL);
    }

    private void renderEditorPage(UiRenderer renderer) {
        painter(renderer).backdropPanel(330.0f, 30.0f, 1560.0f, 1020.0f, 12.0f, -234091508, 1.0f, -14408668);
        drawRule(renderer, 330.0f, 95.0f, 1560.0f, 1.0f, -14408668);
        painter(renderer).roundedRect(354.0f, 47.0f, 32.0f, 32.0f, 8.0f, 234881023);
        painter(renderer).icon(DashboardIcon.EDITOR_SCRIPT, 361.0f, 54.0f, 18.0f, 18.0f, 1.0f, -1);
        String name = selectedScriptIndex >= 0 ? scriptCards.get(selectedScriptIndex).name : NEW_EDITOR_SCRIPT_LABEL;
        String language = selectedScriptIndex >= 0 ? scriptCards.get(selectedScriptIndex).typeLabel : DEFAULT_EDITOR_LANGUAGE;
        boolean published = selectedScriptIndex < 0 || scriptCards.get(selectedScriptIndex).publicVisibility;
        painter(renderer).text(FontAsset.INTER_SEMI_BOLD, name, 398.0f, 53.5f, 16.0f, -1);
        float nameRight = 398.0f + painter(renderer).textWidth(FontAsset.INTER_SEMI_BOLD, name, 16.0f);
        float languageWidth = Math.max(39.0f, painter(renderer).textWidth(FontAsset.INTER, language, 12.0f) + 18.0f);
        float languageX = nameRight + 12.0f;
        painter(renderer).roundedRect(languageX, 52.0f, languageWidth, 22.0f, 6.0f, 234881023);
        new DashboardWidgetRenderer(painter(renderer)).centeredText(FontAsset.INTER, language,
                languageX + languageWidth * 0.5f, 56.0f, 12.0f, -1711276033);
        String visibility = published ? PUBLIC_SEGMENT_LABEL : PRIVATE_SEGMENT_LABEL;
        float visibilityWidth = painter(renderer).textWidth(FontAsset.INTER, visibility, 11.0f) + 34.0f;
        float visibilityX = languageX + languageWidth + 12.0f;
        painter(renderer).roundedRect(visibilityX, 52.5f, visibilityWidth, 21.0f, 6.0f, 234881023);
        painter(renderer).icon(published ? DashboardIcon.PUBLIC : DashboardIcon.PRIVATE,
                visibilityX + 8.0f, 57.0f, 12.0f, 12.0f, 1.0f, -1);
        painter(renderer).text(FontAsset.INTER, visibility, visibilityX + 25.0f, 56.5f, 11.0f, -1929379841);
        painter(renderer).panel(1610.0f, 46.0f, 93.0f, 34.0f, 8.0f, 184549375, 1.0f, -14408668);
        painter(renderer).icon(DashboardIcon.EDITOR_IMPORT, 1622.0f, 55.0f, 16.0f, 16.0f, 1.0f, -1);
        painter(renderer).text(FontAsset.INTER, EDITOR_IMPORT_LABEL, 1646.0f, 55.5f, 13.0f, -1191182337);
        painter(renderer).panel(1713.0f, 46.5f, 62.0f, 33.0f, 8.0f, 184549375, 1.0f, -14408668);
        new DashboardWidgetRenderer(painter(renderer)).centeredText(FontAsset.INTER, EDITOR_SAVE_LABEL,
                1744.0f, 55.5f, 13.0f, -1191182337);
        painter(renderer).roundedRect(1785.0f, 46.5f, 81.0f, 33.0f, 8.0f, -335544321);
        painter(renderer).icon(DashboardIcon.EDITOR_RUN, 1795.0f, 56.0f, 14.0f, 14.0f, 1.0f, -1);
        painter(renderer).text(FontAsset.INTER_SEMI_BOLD, EDITOR_COMPILE_LABEL, 1815.0f, 55.5f, 12.0f, -15987700);
        painter(renderer).roundedRect(330.0f, 96.0f, 56.0f, 844.0f, 0.0f, 100663295);
        String[] lines = editor.lines();
        editor.setCharacterAdvance(painter(renderer).textWidth(FontAsset.JETBRAINS_MONO, "M", 13.0f));
        float firstLine = animations.spring(UiId.ROOT.child(EDITOR_SCROLL_ID), EDITOR_SCROLL_PROPERTY,
                editor.displayedFirstLine(), editor.firstVisibleLine(), SpringSpec.UNDERDAMPED_3_3_HZ);
        editor.setDisplayedFirstLine(firstLine);
        renderer.pushScissor(renderX(330.0f), renderY(96.0f), renderSize(1560.0f), renderSize(844.0f));
        int visibleLines = DashboardLayout.visibleEditorLines();
        int first = Math.max(0, (int) Math.floor(editor.displayedFirstLine()));
        int end = Math.min(lines.length, first + visibleLines + 2);
        int caretLine = editor.caretLine();
        float pulse = editorPulseStartedNanos == 0L ? -1.0f : progress(editorPulseStartedNanos, 720);
        if (textFocus == TextInputFocus.EDITOR && caretLine >= first && caretLine < end) {
            float y = 108.0f + (caretLine - editor.displayedFirstLine()) * 23.0f;
            float wash = animations.tween(UiId.ROOT.child(EDITOR_ACTIVE_LINE_ID), EDITOR_ACTIVE_LINE_PROPERTY,
                    1.0f, 1.0f, TransitionSpec.DURATION_140_MS);
            painter(renderer).roundedRect(386.0f, y, 1504.0f, 23.0f, 0.0f, DashboardColors.multiplyAlpha(234881023, wash));
        }
        renderEditorSelection(renderer, lines, first, end);
        for (int line = first; line < end; line++) {
            float y = 112.0f + (line - editor.displayedFirstLine()) * 23.0f;
            new DashboardWidgetRenderer(painter(renderer)).rightAlignedText(FontAsset.JETBRAINS_MONO,
                    Integer.toString(line + 1), 372.0f, y, 13.0f, 1090519039);
            renderSyntaxLine(renderer, lines[line], 402.0f, y);
            renderEditorCompilePulse(renderer, lines[line], 402.0f, y, line - first, pulse);
        }
        if (textFocus == TextInputFocus.EDITOR && caretVisible() && caretLine >= first && caretLine < first + visibleLines + 1) {
            float x = 402.0f + painter(renderer).textWidth(FontAsset.JETBRAINS_MONO, editor.linePrefix(), 13.0f);
            float y = 111.0f + (caretLine - editor.displayedFirstLine()) * 23.0f;
            drawRule(renderer, x, y, 1.5f, 17.0f, -1);
        }
        new DashboardWidgetRenderer(painter(renderer)).editorScrollbar(lines.length, editor.displayedFirstLine());
        renderer.popScissor();
        if (pulse >= 1.0f) editorPulseStartedNanos = 0L;
        painter(renderer).roundedRect(330.0f, 940.0f, 1560.0f, 110.0f, 0.0f, 100663295);
        drawRule(renderer, 330.0f, 940.0f, 1560.0f, 1.0f, -14408668);
        painter(renderer).text(FontAsset.INTER, EDITOR_OUTPUT_LABEL, 354.0f, 954.0f, 12.0f, -2130706433);
        String status = editorStatusText(language);
        new DashboardWidgetRenderer(painter(renderer)).rightAlignedText(FontAsset.INTER, status,
                1866.0f, 954.0f, 12.0f, 1509949439);
        int count = Math.min(3, editorOutput.size());
        int start = Math.max(0, editorOutput.size() - count);
        for (int index = 0; index < count; index++) {
            int color = index == 0 ? -1711276033 : index == 1 ? 1946157055 : 1728053247;
            painter(renderer).text(FontAsset.JETBRAINS_MONO, EDITOR_OUTPUT_PREFIX + editorOutput.get(start + index),
                    354.0f, 978 + index * 20, 12.0f, color);
        }
    }

    private String editorStatusText(String language) {
        int line = editor.caretLine() + 1;
        int column = editor.caretColumn() + 1;
        String status = editor.isSaved() ? EDITOR_SAVED_LABEL : EDITOR_MODIFIED_LABEL;
        return language + EDITOR_ENCODING_LINE_PREFIX + line + EDITOR_COLUMN_PREFIX + column + EDITOR_STATUS_SEPARATOR + status;
    }

    private void renderEditorSelection(UiRenderer renderer, String[] lines, int first, int end) {
        if (!editor.hasSelection()) return;
        int selectionStart = editor.selectionStart();
        int selectionEnd = editor.selectionEnd();
        int offset = 0;
        for (int line = 0; line < first; line++) offset += lines[line].length() + 1;
        int color = textFocus == TextInputFocus.EDITOR ? 956301311 : 620756991;
        for (int line = first; line < end; line++) {
            String text = lines[line];
            int lineEnd = offset + text.length();
            if (selectionEnd > offset && selectionStart <= lineEnd) {
                int left = Math.max(0, Math.min(text.length(), selectionStart - offset));
                int right = Math.max(0, Math.min(text.length(), selectionEnd - offset));
                boolean newlineSelected = line < lines.length - 1 && selectionEnd > lineEnd;
                float x = 402.0f + painter(renderer).textWidth(FontAsset.JETBRAINS_MONO, text.substring(0, left), 13.0f);
                float width = painter(renderer).textWidth(FontAsset.JETBRAINS_MONO, text.substring(left, Math.max(left, right)), 13.0f);
                if (newlineSelected) width += editor.characterAdvance();
                if (width > 0.01f) {
                    float y = 108.0f + (line - editor.displayedFirstLine()) * 23.0f;
                    painter(renderer).roundedRect(x, y, width, 23.0f, 0.0f, color);
                }
            }
            offset = lineEnd + 1;
        }
    }

    private void renderSyntaxLine(UiRenderer renderer, String text, float x, float y) {
        if (text.stripLeading().startsWith(SYNTAX_COMMENT_PREFIX)) {
            painter(renderer).text(FontAsset.JETBRAINS_MONO_ITALIC, text, x, y, 13.0f, 1476395007);
            return;
        }
        float cursor = x;
        int start = 0;
        while (start < text.length()) {
            char first = text.charAt(start);
            int end = start + 1;
            FontAsset font = FontAsset.JETBRAINS_MONO;
            if (Character.isWhitespace(first)) {
                while (end < text.length() && Character.isWhitespace(text.charAt(end))) end++;
                cursor += painter(renderer).textWidth(font, text.substring(start, end), 13.0f);
                start = end;
                continue;
            }
            int color;
            if (first == '"' || first == '\'') {
                boolean escaped = false;
                while (end < text.length()) {
                    char character = text.charAt(end++);
                    if (character == first && !escaped) break;
                    escaped = character == '\\' && !escaped;
                    if (character != '\\') escaped = false;
                }
                color = -1627389953;
            } else if (!Character.isDigit(first)) {
                if (!Character.isLetter(first) && first != '_') {
                    color = -2130706433;
                } else {
                    while (end < text.length() && (Character.isLetterOrDigit(text.charAt(end)) || text.charAt(end) == '_')) end++;
                    String token = text.substring(start, end);
                    if (EDITOR_KEYWORDS.contains(token)) {
                        font = FontAsset.JETBRAINS_MONO_MEDIUM;
                        color = -218103809;
                    } else {
                        int next = end;
                        while (next < text.length() && Character.isWhitespace(text.charAt(next))) next++;
                        color = next < text.length() && text.charAt(next) == '(' ? -771751937 : -637534209;
                    }
                }
            } else {
                while (end < text.length() && (Character.isDigit(text.charAt(end)) || text.charAt(end) == '.')) end++;
                color = -1191182337;
            }
            String token = text.substring(start, end);
            painter(renderer).text(font, token, cursor, y, 13.0f, color);
            cursor += painter(renderer).textWidth(font, token, 13.0f);
            start = end;
        }
    }

    private void renderEditorCompilePulse(UiRenderer renderer, String text, float x, float y, int row, float progress) {
        if (progress < 0.0f || progress >= 1.0f || text.isEmpty()) return;
        FontAsset font = text.stripLeading().startsWith(PULSE_COMMENT_PREFIX) ? FontAsset.JETBRAINS_MONO_ITALIC : FontAsset.JETBRAINS_MONO;
        float width = painter(renderer).textWidth(font, text, 13.0f);
        if (width <= 0.0f) return;
        int strips = Math.max(1, (int) Math.ceil(width / 12.0f));
        for (int index = 0; index < strips; index++) {
            float phase = progress * 1.75f - row * 0.035f - index * 0.018f;
            if (phase <= 0.0f || phase >= 0.42f) continue;
            float triangle = 1.0f - Math.abs(phase - 0.21f) / 0.21f;
            float opacity = triangle * triangle * 0.76f;
            float stripX = x + index * 12.0f;
            float stripWidth = Math.min(12.0f - 2.0f, x + width - stripX);
            if (stripWidth <= 0.0f) continue;
            renderer.pushScissor(renderX(stripX), renderY(y - 3.0f), renderSize(stripWidth), renderSize(23.0f));
            painter(renderer).text(font, text, x, y, 13.0f, DashboardColors.multiplyAlpha(-1, opacity));
            renderer.popScissor();
        }
    }

    private void renderConfigsPage(UiRenderer renderer) {
        painter(renderer).backdropPanel(330.0f, 30.0f, 1560.0f, 1020.0f, 12.0f, -234091508, 1.0f, -14408668);
        renderPageHeader(renderer, CONFIGS_TITLE, CONFIGS_DESCRIPTION, CONFIG_SEARCH_PLACEHOLDER, false);
        renderSegmentedControl(renderer, CONFIG_SEGMENT_ID, 354.0f, 112.0f, 190.0f, 37.0f,
                !browsePublicConfigs, YOUR_CONFIGS_LABEL, BROWSE_CONFIGS_LABEL, 104.0f, true, configsAvailable());
        List<ConfigCard> cards = filteredConfigs();
        new DashboardWidgetRenderer(painter(renderer)).rightAlignedText(FontAsset.INTER,
                cards.size() + CONFIG_COUNT_SUFFIX, 1866.0f, 123.0f, 13.0f, 1728053247);
        if (copiesSourceId != null) {
            String label = COPIES_OF_PREFIX + copiesSourceName;
            copiesFilterWidth = painter(renderer).textWidth(FontAsset.INTER, label, 13.0f) + 50.0f;
            boolean hovered = containsPointer(556.0f, 112.0f, copiesFilterWidth, 37.0f);
            painter(renderer).panel(556.0f, 112.0f, copiesFilterWidth, 37.0f, 8.0f, 184549375, 1.0f,
                    hovered ? 872415231 : 268435455);
            painter(renderer).text(FontAsset.INTER, label, 570.0f, 123.0f, 13.0f, -637534209);
            drawGlyph(renderer, 57778, 556.0f + copiesFilterWidth - 28.0f, 122.0f, 14.0f,
                    hovered ? -1 : -2130706433);
        }
        configScroll = Math.max(0.0f, Math.min(configScroll, maximumConfigScroll(cards.size())));
        renderer.pushScissor(renderX(330.0f), renderY(157.0f), renderSize(1560.0f), renderSize(885.0f));
        for (int index = 0; index < cards.size(); index++) {
            float y = configCardY(index);
            if (configCardVisible(y)) renderConfigCard(renderer, cards.get(index), 354 + index % 3 * 509, y);
        }
        if (canShowNewConfigCard()) {
            int index = cards.size();
            float y = configCardY(index);
            if (configCardVisible(y)) renderNewCard(renderer, 354 + index % 3 * 509, y, 493.0f, 144.0f, NEW_CONFIG_LABEL);
        }
        renderer.popScissor();
    }

    private static boolean configsAvailable() { return Echo.featureConfig != null && Echo.featureConfig.isAvailable(); }
    private boolean canShowNewConfigCard() { return !browsePublicConfigs && copiesSourceId == null; }
    private float configCardY(int index) { return 165.0f + index / 3 * 160 - configScroll; }
    private static boolean configCardVisible(float y) { return y + 144.0f >= 157.0f && y <= 1042.0f; }
    private float maximumConfigScroll(int count) {
        int cells = count + (canShowNewConfigCard() ? 1 : 0);
        int rows = (cells + 2) / 3;
        return Math.max(0.0f, rows * 160 - 16 - 877.0f);
    }
    private List<ConfigCard> filteredConfigs() {
        String query = configSearch.toLowerCase(Locale.ROOT).trim();
        List<ConfigCard> result = new ArrayList<>();
        for (ConfigCard card : configCards) {
            if (browsePublicConfigs != card.publicVisibility) continue;
            if (!browsePublicConfigs && copiesSourceId != null && !copiesSourceId.equals(card.sourceId)) continue;
            if (!query.isEmpty() && !card.name.toLowerCase(Locale.ROOT).contains(query)
                    && !card.serverLabel.toLowerCase(Locale.ROOT).contains(query)
                    && !card.author.toLowerCase(Locale.ROOT).contains(query)) continue;
            result.add(card);
        }
        return result;
    }

    private ConfigCardRules configRules() { return new ConfigCardRules(configCards, DashboardController::configsAvailable); }

    private void renderConfigCard(UiRenderer renderer, ConfigCard card, float x, float y) {
        renderCardBackground(renderer, CONFIG_CARD_PREFIX + card.id, x, y, 493.0f, 144.0f, card.selected);
        painter(renderer).roundedRect(x + 16.0f, y + 16.0f, 24.0f, 24.0f, 6.0f, 268435455);
        if (card.serverIcon != null) {
            renderer.drawImage(card.serverIcon, renderX(x + 16.0f), renderY(y + 16.0f),
                    renderSize(24.0f), renderSize(24.0f), renderSize(6.0f),
                    Math.max(0.0f, Math.min(1.0f, openingOpacity * layerOpacity)));
        } else {
            painter(renderer).icon(DashboardIcon.WORLD, x + 21.0f, y + 21.0f, 14.0f, 14.0f, 1.0f, -1);
        }
        painter(renderer).text(FontAsset.INTER_SEMI_BOLD, card.name, x + 48.0f, y + 19.5f, 15.0f, -1);
        float badgeRight = x + 477.0f;
        ConfigCardRules rules = configRules();
        if (rules.hasLocalLoadTarget(card)) {
            UiRect menu = DashboardLayout.configMenuButton(x, y);
            boolean hovered = card.id.equals(contextMenuCardId)
                    || containsPointer(menu.x(), menu.y(), menu.width(), menu.height());
            painter(renderer).icon(DashboardIcon.MORE, menu.x() + 1.0f, menu.y() + 1.0f,
                    20.0f, 20.0f, 1.0f, hovered ? -1 : -2130706433);
            badgeRight = x + 447.0f;
        }
        if (card.selected) {
            painter(renderer).roundedRect(badgeRight - 65.0f, y + 17.0f, 65.0f, 22.0f, 4.0f, 268435455);
            painter(renderer).roundedRect(badgeRight - 57.0f, y + 25.0f, 6.0f, 6.0f, 3.0f, -637534209);
            painter(renderer).text(FontAsset.INTER, ACTIVE_CONFIG_LABEL, badgeRight - 45.0f, y + 21.0f, 12.0f, -637534209);
        } else if (card.publicVisibility) {
            int copies = card.owned ? 0 : rules.copyCount(card);
            String label = card.owned ? OWN_CONFIG_LABEL : copies > 1 ? copies + CONFIG_COPIES_SUFFIX
                    : copies == 1 ? ADDED_CONFIG_LABEL : "";
            if (!label.isEmpty()) {
                float width = painter(renderer).textWidth(FontAsset.INTER, label, 12.0f) + 20.0f;
                painter(renderer).roundedRect(badgeRight - width, y + 17.0f, width, 22.0f, 4.0f, 268435455);
                painter(renderer).text(FontAsset.INTER, label, badgeRight + 10.0f - width, y + 21.0f, 12.0f, -637534209);
            }
        }
        float textRight = x + 477.0f;
        if (!card.publicVisibility && configsAvailable()) textRight = renderConfigPublishingBadge(renderer, card, textRight, y) - 12.0f;
        float textX = x + 16.0f;
        boolean first = true;
        String attribution = card.publicVisibility ? CONFIG_AUTHOR_PREFIX + card.author
                : card.sourceAuthor.isBlank() ? "" : CONFIG_SOURCE_AUTHOR_PREFIX + card.sourceAuthor;
        for (String label : new String[]{card.serverLabel, attribution, card.updatedLabel}) {
            if (label.isBlank()) continue;
            float width = painter(renderer).textWidth(FontAsset.INTER, label, 12.0f);
            if (textX + (first ? 0 : 11) + width > textRight) break;
            if (!first) {
                painter(renderer).roundedRect(textX, y + 59.5f, 3.0f, 3.0f, 1.5f, -2130706433);
                textX += 11.0f;
            }
            painter(renderer).text(FontAsset.INTER, label, textX, y + 54.0f, 12.0f, -2130706433);
            textX += width + 8.0f;
            first = false;
        }
        drawRule(renderer, x + 16.0f, y + 82.0f, 461.0f, 1.0f, -14671840);
        ConfigPrimaryAction primary = rules.primaryAction(card);
        ConfigPrimaryAction secondary = secondaryConfigAction(card);
        renderConfigButton(renderer, card, primary, ConfigCardLayout.primaryButton(x, y, secondary), true);
        if (secondary != null) renderConfigButton(renderer, card, secondary,
                ConfigCardLayout.secondaryButton(x, y, secondary), secondary.isGetUpdate());
    }

    private ConfigPrimaryAction secondaryConfigAction(ConfigCard card) {
        if (!card.publicVisibility) {
            if (card.selected) return ConfigPrimaryAction.SAVE;
            return card.updateAvailable ? ConfigPrimaryAction.GET_UPDATE : null;
        }
        if (card.owned) return ConfigPrimaryAction.UNPUBLISH;
        ConfigCardRules rules = configRules();
        ConfigCard copy = rules.copyCount(card) == 1 ? rules.firstCopy(card) : null;
        return copy != null && copy.updateAvailable ? ConfigPrimaryAction.GET_UPDATE : null;
    }

    private float renderConfigPublishingBadge(UiRenderer renderer, ConfigCard card, float right, float y) {
        String label;
        int dotColor;
        if (card.isPublished() && card.publishedChanges) {
            label = UNPUBLISHED_CHANGES_LABEL;
            dotColor = -18176;
        } else if (card.isPublished()) {
            label = PUBLIC_SEGMENT_LABEL;
            dotColor = 0;
        } else {
            if (!card.updateAvailable) return right + 12.0f;
            label = UPDATE_AVAILABLE_LABEL;
            dotColor = -637534209;
        }
        float width = painter(renderer).textWidth(FontAsset.INTER, label, 11.0f) + 34.0f;
        float x = right - width;
        painter(renderer).roundedRect(x, y + 50.5f, width, 21.0f, 6.0f, 234881023);
        if (dotColor == 0) painter(renderer).icon(DashboardIcon.PUBLIC, x + 8.0f, y + 55.0f, 12.0f, 12.0f, 1.0f, -1);
        else painter(renderer).roundedRect(x + 11.0f, y + 58.0f, 6.0f, 6.0f, 3.0f, dotColor);
        painter(renderer).text(FontAsset.INTER, label, x + 25.0f, y + 54.0f, 11.0f, -1929379841);
        return x;
    }

    private void renderConfigButton(UiRenderer renderer, ConfigCard card, ConfigPrimaryAction action,
                                     UiRect bounds, boolean emphasized) {
        boolean confirmation = isConfirmationActive(card.id + "/" + action.name());
        String status = isConfigStatusActive(card.id) ? statusMessage : "";
        painter(renderer).panel(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 6.0f,
                confirmation ? 654273387 : emphasized ? 452984831 : 184549375, 1.0f, 268435455);
        String label = confirmation ? action.confirmationLabel
                : !status.isEmpty() && action.isSave() ? status : action.label;
        new DashboardWidgetRenderer(painter(renderer)).centeredText(FontAsset.INTER, label,
                bounds.x() + bounds.width() / 2.0f, bounds.y() + 8.0f, 13.0f,
                confirmation ? -38037 : emphasized ? -1 : -1711276033);
    }

    private boolean isConfirmationActive(String id) {
        return id.equals(pendingConfirmationId) && System.currentTimeMillis() - confirmationStartedAtMillis < 3000L;
    }
    private boolean isConfigStatusActive(String id) {
        return id.equals(statusConfigId) && System.currentTimeMillis() - statusStartedAtMillis < 1500L;
    }

    private UiRect configContextMenuBounds(ConfigCard card) {
        float height = configRules().menuActions(card).size() * 30.0f + 8.0f;
        float x = contextMenuAnchor.x() + contextMenuAnchor.width() - 188.0f;
        float y = contextMenuAnchor.y() + contextMenuAnchor.height() + 6.0f;
        if (y + height > 1042.0f) y = contextMenuAnchor.y() - 6.0f - height;
        return new UiRect(x, y, 188.0f, height);
    }

    private void renderConfigContextMenu(UiRenderer renderer) {
        ConfigCard card = page == DashboardPage.CONFIGS ? findConfig(contextMenuCardId) : null;
        if (card == null) return;
        List<ConfigMenuAction> actions = configRules().menuActions(card);
        UiRect bounds = configContextMenuBounds(card);
        float opacity = easedProgress(contextMenuOpenedAtNanos, 140);
        drawShadow(renderer, bounds.x(), bounds.y() + 4.0f, bounds.width(), bounds.height(),
                8.0f, 16.0f, DashboardColors.multiplyAlpha(1711276032, opacity));
        painter(renderer).panel(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 8.0f,
                DashboardColors.multiplyAlpha(-15987700, opacity), 1.0f,
                DashboardColors.multiplyAlpha(-14408668, opacity));
        for (int index = 0; index < actions.size(); index++) {
            ConfigMenuAction action = actions.get(index);
            float y = bounds.y() + 4.0f + index * 30.0f;
            boolean confirmation = isConfirmationActive(card.id + CONFIG_MENU_ACTION_SEPARATOR + action.name());
            boolean hovered = rawContainsPointer(bounds.x() + 4.0f, y, bounds.width() - 8.0f, 30.0f);
            if (confirmation || hovered) painter(renderer).roundedRect(bounds.x() + 4.0f, y,
                    bounds.width() - 8.0f, 30.0f, 5.0f,
                    DashboardColors.multiplyAlpha(confirmation ? 654273387 : 352321535, opacity));
            String label = confirmation ? action.confirmationLabel(card) : action.label;
            int color = !action.destructive && !confirmation ? -637534209 : -38037;
            painter(renderer).text(FontAsset.INTER, label, bounds.x() + 14.0f, y + 8.0f, 13.0f,
                    DashboardColors.multiplyAlpha(color, opacity));
        }
    }

    private float modalProgress() {
        float time = Math.min(1.0f, Math.max(0.0f, (float) (System.nanoTime() - modalOpenedAtNanos) / 2.4e8f));
        return AnimationRegistry.ease(time, TransitionSpec.DURATION_240_MS);
    }

    private void renderConfigModal(UiRenderer renderer) {
        float progress = modalProgress();
        float dimming = easedProgress(modalOpenedAtNanos, 160);
        painter(renderer).roundedRect(0.0f, 0.0f, 1920.0f, 1080.0f, 0.0f,
                DashboardColors.multiplyAlpha(-1946157056, dimming));
        pivotX = 960.0f;
        pivotY = 540.0f;
        animationScale = 0.98f + 0.02f * progress;
        layerOpacity = progress;
        localOffsetY = 8.0f * (1.0f - progress);
        float x = 680.0f;
        float y = 311.0f;
        drawShadow(renderer, x, y + 16.0f, 560.0f, 459.0f, 12.0f, 40.0f, Integer.MIN_VALUE);
        painter(renderer).panel(x, y, 560.0f, 459.0f, 12.0f, -15987700, 1.0f, -14408668);
        boolean publishing = modalMode == ConfigModalMode.PUBLISH || modalMode == ConfigModalMode.UPDATE;
        String title = switch (modalMode) {
            case NEW -> MODAL_NEW_TITLE;
            case EDIT -> MODAL_EDIT_TITLE;
            case COPY -> MODAL_COPY_TITLE;
            case PUBLISH -> MODAL_PUBLISH_TITLE;
            case UPDATE -> MODAL_UPDATE_TITLE;
        };
        painter(renderer).text(FontAsset.INTER_SEMI_BOLD, title, x + 24.0f, y + 24.0f, 18.0f, -1);
        String description = switch (modalMode) {
            case NEW -> MODAL_NEW_DESCRIPTION;
            case EDIT -> modalCard != null && modalCard.selected ? MODAL_EDIT_ACTIVE_DESCRIPTION : MODAL_EDIT_DESCRIPTION;
            case COPY -> MODAL_COPY_DESCRIPTION;
            case PUBLISH -> MODAL_PUBLISH_DESCRIPTION;
            case UPDATE -> MODAL_UPDATE_DESCRIPTION;
        };
        painter(renderer).text(FontAsset.INTER, description, x + 24.0f, y + 49.0f, 13.0f, -2130706433);
        painter(renderer).roundedRect(x + 506.0f, y + 29.0f, 30.0f, 30.0f, 8.0f, 234881023);
        drawGlyph(renderer, 57778, x + 513.0f, y + 36.0f, 16.0f, -2130706433);
        drawRule(renderer, x, y + 84.0f, 560.0f, 1.0f, -14408668);
        painter(renderer).text(FontAsset.INTER, publishing ? MODAL_PUBLIC_NAME_LABEL : MODAL_NAME_LABEL,
                x + 24.0f, y + 105.0f, 12.0f, -1711276033);
        float nameTarget = textFocus == TextInputFocus.MODAL_NAME ? 1.0f : 0.0f;
        float nameFocus = animations.tween(UiId.ROOT.child(MODAL_NAME_ID), FOCUS_PROPERTY,
                nameTarget, nameTarget, TransitionSpec.DURATION_140_MS);
        painter(renderer).panel(x + 24.0f, y + 127.0f, 512.0f, 40.0f, 8.0f, 234881023, 1.0f,
                DashboardColors.interpolate(-14408668, 1509949439, nameFocus));
        painter(renderer).text(FontAsset.INTER, modalName.isEmpty() ? MODAL_NAME_PLACEHOLDER : modalName,
                x + 36.0f, y + 139.5f, 13.0f,
                modalName.isEmpty() ? DashboardColors.interpolate(1308622847, 872415231, nameFocus) : -1);
        if (textFocus == TextInputFocus.MODAL_NAME && caretVisible()) {
            float caretX = x + 36.0f + painter(renderer).textWidth(FontAsset.INTER, modalName, 13.0f);
            drawRule(renderer, caretX, y + 137.0f, 1.5f, 17.0f, -1);
        }
        painter(renderer).text(FontAsset.INTER, MODAL_TYPE_LABEL, x + 24.0f, y + 185.0f, 12.0f, -1711276033);
        renderTypeChoice(renderer, x + 24.0f, y + 207.0f, 251.0f, 44.0f, MODAL_GHOST_LABEL, DashboardIcon.GHOST, modalGhost);
        renderTypeChoice(renderer, x + 285.0f, y + 207.0f, 251.0f, 44.0f, MODAL_RAGE_LABEL, DashboardIcon.DANGER, !modalGhost);
        painter(renderer).text(FontAsset.INTER, MODAL_DESCRIPTION_LABEL, x + 24.0f, y + 269.0f, 12.0f, -1711276033);
        float descriptionTarget = textFocus == TextInputFocus.MODAL_DESCRIPTION ? 1.0f : 0.0f;
        float descriptionFocus = animations.tween(UiId.ROOT.child(MODAL_DESCRIPTION_ID), FOCUS_PROPERTY,
                descriptionTarget, descriptionTarget, TransitionSpec.DURATION_140_MS);
        painter(renderer).panel(x + 24.0f, y + 291.0f, 512.0f, 76.0f, 8.0f, 234881023, 1.0f,
                DashboardColors.interpolate(-14408668, 1509949439, descriptionFocus));
        painter(renderer).text(FontAsset.INTER, modalDescription.isEmpty() ? MODAL_DESCRIPTION_PLACEHOLDER : modalDescription,
                x + 36.0f, y + 302.0f, 13.0f,
                modalDescription.isEmpty() ? DashboardColors.interpolate(1308622847, 872415231, descriptionFocus) : -1);
        drawRule(renderer, x, y + 387.0f, 560.0f, 1.0f, -14408668);
        if (publishing) painter(renderer).text(FontAsset.INTER, MODAL_FRIENDS_NOTICE, x + 24.0f, y + 414.0f, 12.0f, -2130706433);
        painter(renderer).panel(x + 359.0f, y + 404.0f, 79.0f, 35.0f, 8.0f, 150994943, 1.0f, -14408668);
        new DashboardWidgetRenderer(painter(renderer)).centeredText(FontAsset.INTER, MODAL_CANCEL_LABEL,
                x + 398.5f, y + 414.0f, 13.0f, -1275068417);
        painter(renderer).roundedRect(x + 450.0f, y + 404.0f, 86.0f, 35.0f, 8.0f, -335544321);
        String action = switch (modalMode) {
            case NEW -> MODAL_CREATE_LABEL;
            case EDIT -> MODAL_SAVE_LABEL;
            case COPY -> MODAL_ADD_LABEL;
            case PUBLISH -> MODAL_PUBLISH_LABEL;
            case UPDATE -> MODAL_UPDATE_LABEL;
        };
        new DashboardWidgetRenderer(painter(renderer)).centeredText(FontAsset.INTER_SEMI_BOLD, action,
                x + 493.0f, y + 414.0f, 13.0f, -15987700);
        animationScale = 1.0f;
        layerOpacity = 1.0f;
        localOffsetY = 0.0f;
    }

    private void renderTypeChoice(UiRenderer renderer, float x, float y, float width, float height,
                                   String label, DashboardIcon icon, boolean selected) {
        float target = selected ? 1.0f : 0.0f;
        float selection = animations.tween(UiId.ROOT.child(TYPE_CHOICE_ID_PREFIX + label), SEGMENT_SELECTION_PROPERTY,
                target, target, TransitionSpec.DURATION_180_MS);
        painter(renderer).panel(x, y, width, height, 8.0f, DashboardColors.interpolate(234881023, -1250068, selection),
                1.0f, DashboardColors.interpolate(-14408668, -1250068, selection));
        painter(renderer).icon(icon, x + 16.0f, y + 15.0f, 14.0f, 14.0f, 1.0f,
                DashboardColors.interpolate(-1, -15987700, selection));
        painter(renderer).text(FontAsset.INTER, label, x + 42.0f, y + 14.5f, 13.0f,
                DashboardColors.interpolate(-1275068417, -15987700, selection));
    }

    private boolean rawContainsPointer(float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private void drawShadow(UiRenderer renderer, float x, float y, float width, float height,
                             float radius, float blur, int color) {
        renderer.drawShadow(renderX(x), renderY(y), renderSize(width), renderSize(height),
                renderSize(radius), renderSize(blur), DashboardColors.multiplyAlpha(color,
                        Math.max(0.0f, Math.min(1.0f, openingOpacity * layerOpacity))));
    }

    private Feature settingsFeature() { return page == DashboardPage.OVERLAYS ? selectedOverlay : selectedFeature; }

    private UiId settingId(Setting setting) {
        Feature feature = settingsFeature();
        return UiId.ROOT.child(SETTING_ID_SEGMENT).child(FeatureNameAssignments.id(feature))
                .child(feature.settings.indexOf(setting));
    }

    private List<CharSequence> choiceOptions() {
        List<CharSequence> result = new ArrayList<>();
        if (openChoice != null) result.addAll(openChoice.getOptions());
        return result;
    }

    private PanelBounds choicePopupLayout() {
        List<CharSequence> options = choiceOptions();
        float width = Math.max(choiceAnchor.width(), minimumChoiceWidth);
        UiRect anchor = new UiRect(choiceAnchor.x() + choiceAnchor.width() - width,
                choiceAnchor.y(), width, choiceAnchor.height());
        UiRect popup = UiLayoutMath.placePopup(anchor, width, Math.min(220, options.size() * 26 + 6),
                new UiRect(330.0f, 110.0f, 1560.0f, 930.0f), 4.0f);
        return new PanelBounds(choiceAnchor, popup);
    }

    private void layoutSettings() {
        settingRows.clear();
        settingSections.clear();
        if (settingsFeature() == null) return;
        int column = 0;
        float y = settingColumnTop(0);
        for (SettingSection section : collectSettingSections()) {
            int settingIndex = 0;
            while (settingIndex < section.settings().size()) {
                boolean atColumnTop = y <= settingColumnTop(column) + 0.01f;
                if (!atColumnTop && y + remainingSectionHeight(section, settingIndex) > 1040.0f
                        && column < SETTING_COLUMN_X.length - 1) {
                    y = settingColumnTop(++column);
                    continue;
                }
                float sectionTop = y;
                float rowY = sectionTop + 40.0f;
                int count = 0;
                while (settingIndex < section.settings().size()) {
                    Setting setting = section.settings().get(settingIndex);
                    float height = settingHeight(setting);
                    if (count > 0 && rowY + height > 1040.0f) break;
                    settingRows.add(new SettingRow(setting, SETTING_COLUMN_X[column] + 12.0f, rowY, height, column));
                    rowY += height;
                    settingIndex++;
                    count++;
                }
                settingSections.add(new SettingSectionLayout(section.title(), column, sectionTop, rowY - sectionTop));
                y = rowY + 40.0f;
                if (settingIndex >= section.settings().size()) break;
                if (column >= SETTING_COLUMN_X.length - 1) return;
                y = settingColumnTop(++column);
            }
        }
        markContinuedSettingSections();
    }

    private List<SettingSection> collectSettingSections() {
        List<SettingSection> result = new ArrayList<>();
        for (Setting setting : settingsFeature().settings) {
            if (!setting.isVisible()) continue;
            CharSequence group = setting.getTooltip();
            CharSequence title = group.isEmpty() ? DEFAULT_SETTING_SECTION : group;
            SettingSection destination = null;
            for (SettingSection section : result) {
                if (Concat.contentEquals(section.title(), title)) destination = section;
            }
            if (destination == null) {
                destination = new SettingSection(title, new ArrayList<>());
                result.add(destination);
            }
            destination.settings().add(setting);
        }
        return result;
    }

    private float settingColumnTop(int column) {
        if (page == DashboardPage.OVERLAYS) return 452.0f;
        if (selectedFeature != null && !selectedFeature.isKeybindable()) return 110.0f;
        return column == 0 ? 286.0f : 110.0f;
    }

    private float remainingSectionHeight(SettingSection section, int first) {
        float height = 40.0f;
        for (int index = first; index < section.settings().size(); index++) height += settingHeight(section.settings().get(index));
        return height;
    }

    private float settingHeight(Setting setting) {
        if (setting instanceof FloatSetting || setting instanceof IntSetting) return 56.0f;
        if (setting instanceof FloatRangeSetting) return 56.0f;
        if (setting instanceof ColorSetting) return 46.0f;
        if (setting instanceof RegistryListSetting) return 46.0f;
        if (setting instanceof TextSetting) return 76.0f;
        if (setting instanceof HotbarSlotsSetting) return 78.0f;
        if (setting instanceof NamedRangeGroupSetting ranges) return 32.0f + namedRangesHeight(ranges) + 8.0f;
        if (setting instanceof MultiChoiceSetting choices) {
            float header = choices.isSelectionMode() ? 76.0f : 33.0f;
            int count = choices.isSelectionMode() ? choices.getOptions().size() : filteredMultiChoices(choices).size();
            return header + Math.max(1, Math.min(6, count)) * 32.0f + 10.0f;
        }
        if (setting instanceof CustomWidgetSetting custom) return custom.getHeight() + 12.0f;
        return 39.0f;
    }

    private float namedRangesHeight(NamedRangeGroupSetting setting) {
        float height = setting.getRanges().size() * 36.0f;
        return editedNamedRange != null && setting.getRanges().indexOf(editedNamedRange) >= 0 ? height + 72.0f : height;
    }

    private List<Map.Entry<CharSequence, Boolean>> filteredMultiChoices(MultiChoiceSetting setting) {
        List<Map.Entry<CharSequence, Boolean>> result = new ArrayList<>(setting.getOptions().entrySet());
        if (!setting.isSelectionMode()) return result;
        String query = multiChoiceSearches.getOrDefault(settingId(setting), "").toLowerCase(Locale.ROOT);
        if (!query.isEmpty()) result.removeIf(entry -> !Concat.containsLowercase(entry.getKey(), query));
        return result;
    }

    private void markContinuedSettingSections() {
        for (int index = 0; index < settingSections.size(); index++) {
            SettingSectionLayout section = settingSections.get(index);
            boolean duplicate = false;
            for (SettingSectionLayout other : settingSections) {
                if (other != section && Concat.contentEquals(other.title(), section.title())) duplicate = true;
            }
            if (duplicate) settingSections.set(index, new SettingSectionLayout(
                    Concat.sequences(section.title(), CONTINUED_SECTION_SUFFIX), section.column(), section.top(), section.height()));
        }
    }

    private void renderSettingsRows(UiRenderer renderer) {
        if (settingSections.isEmpty()) {
            float y = settingColumnTop(1);
            new DashboardWidgetRenderer(painter(renderer)).section(SETTING_COLUMN_X[1], y, 500.0f, 130.0f, 36.0f, EMPTY_SETTINGS_TITLE);
            painter(renderer).text(FontAsset.INTER, EMPTY_SETTINGS_DESCRIPTION,
                    SETTING_COLUMN_X[1] + 12.0f, y + 48.0f, 13.0f, -2130706433);
            return;
        }
        for (SettingSectionLayout section : settingSections) {
            new DashboardWidgetRenderer(painter(renderer)).section(SETTING_COLUMN_X[section.column()],
                    section.top(), 500.0f, section.height(), 36.0f, section.title());
        }
        for (SettingRow row : settingRows) renderSettingRow(renderer, row);
        renderColorPickerIfPresent(renderer);
        renderRegistryPickerIfPresent(renderer);
    }

    private void renderSettingRow(UiRenderer renderer, SettingRow row) {
        Setting setting = row.setting();
        UiId id = settingId(setting);
        float x = row.x();
        float y = row.y();
        CharSequence label = setting.getLabel();
        if (setting instanceof BooleanSetting toggle) {
            painter(renderer).text(FontAsset.INTER, label, x, y + 8.0f, 13.0f, -1);
            renderToggle(renderer, id, x + 446.0f, y + 6.0f, 30.0f, 19.0f, 9.0f, toggle.getValue());
        } else if (setting instanceof FloatSetting number) {
            renderScalarSetting(renderer, id, label, x, y,
                    normalizedValue(number.getValue(), number.getMinimum(), number.getMaximum()), setting);
        } else if (setting instanceof IntSetting number) {
            renderScalarSetting(renderer, id, label, x, y,
                    normalizedValue(number.getValue(), number.getMinimum(), number.getMaximum()), setting);
        } else if (setting instanceof FloatRangeSetting range) {
            renderRangeSetting(renderer, id, label, x, y, range);
        } else if (setting instanceof ChoiceSetting choice) {
            painter(renderer).text(FontAsset.INTER, label, x, y + 8.0f, 13.0f, -1);
            CharSequence selected = choice.getValue();
            float width = Math.max(42.0f, painter(renderer).textWidth(FontAsset.INTER, selected, 13.0f) + 33.0f);
            float left = x + 476.0f - width;
            renderChoicePill(renderer, left, y + 2.0f, width, 27.0f, selected, choice,
                    !overlapsOtherChoicePopup(choice, left, y + 2.0f, width, 27.0f));
        } else if (setting instanceof MultiChoiceSetting choices) {
            renderMultiChoiceSetting(renderer, id, label, x, y, choices);
        } else if (setting instanceof ColorSetting color) {
            renderColorSetting(renderer, id, label, x, y, color);
        } else if (setting instanceof RegistryListSetting registry) {
            renderRegistrySetting(renderer, id, label, x, y, registry);
        } else if (setting instanceof KeybindSetting key) {
            painter(renderer).text(FontAsset.INTER, label, x, y + 8.0f, 13.0f, -1);
            String keyLabel = settingKeyLabel(key);
            float width = Math.max(36.0f, painter(renderer).textWidth(FontAsset.INTER, settingKeyLabel(key), 13.0f) + 16.0f);
            renderBindPill(renderer, x + 476.0f - width, y + 2.0f, width, 27.0f, keyLabel);
        } else if (setting instanceof TextSetting text) {
            renderTextSetting(renderer, label, x, y, text);
        } else if (setting instanceof CustomWidgetSetting custom) {
            renderCustomSetting(renderer, custom, x, y);
        } else if (setting instanceof HotbarSlotsSetting slots) {
            painter(renderer).text(FontAsset.INTER, label, x, y + 8.0f, 13.0f, -1);
            for (int slot = 0; slot < 9; slot++) {
                boolean selected = slots.isSelected(slot);
                float left = x + slot * 53;
                painter(renderer).panel(left, y + 32.0f, 46.0f, 32.0f, 6.0f,
                        selected ? 268435455 : 234881023, 1.0f, selected ? 1728053247 : -14408668);
                new DashboardWidgetRenderer(painter(renderer)).centeredText(FontAsset.INTER, Integer.toString(slot + 1),
                        left + 23.0f, y + 40.0f, 13.0f, selected ? -1 : -2130706433);
            }
        } else if (setting instanceof NamedRangeGroupSetting ranges) {
            renderNamedRanges(renderer, ranges, id, x, y);
        } else {
            painter(renderer).text(FontAsset.INTER, label, x, y + 8.0f, 13.0f, -1);
            CharSequence unit = setting.getUnit();
            new DashboardWidgetRenderer(painter(renderer)).rightAlignedText(FontAsset.INTER,
                    unit.isEmpty() ? NONEDITABLE_SETTING_LABEL : unit, x + 476.0f, y + 9.0f, 12.0f, -2130706433);
        }
    }

    private static float normalizedValue(float value, float minimum, float maximum) {
        float range = maximum - minimum;
        if (range <= 0.0f) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, (value - minimum) / range));
    }

    private static String formatSettingFloat(float value) {
        return value == Math.round(value) ? Integer.toString(Math.round(value)) : String.format(Locale.ROOT, SETTING_FLOAT_FORMAT, value);
    }

    private boolean editingNumber(Setting setting) { return textFocus == TextInputFocus.SETTING_NUMBER && numericSetting == setting; }
    private boolean editingRange(FloatRangeSetting setting, boolean upper) { return editingNumber(setting) && editingUpperRange == upper; }
    private String rangeValueText(FloatRangeSetting setting, boolean upper) {
        return editingRange(setting, upper) ? numericText : formatSettingFloat(upper ? setting.getUpper() : setting.getLower());
    }

    private CharSequence scalarValueText(Setting setting) {
        if (editingNumber(setting)) return Concat.sequences(numericText, setting.getUnit());
        if (setting instanceof FloatSetting number) {
            CharSequence override = number.getDisplayOverride();
            if (override != null) return override;
            return Concat.sequences(formatSettingFloat(number.getValue()), number.getUnit());
        }
        IntSetting number = (IntSetting) setting;
        return Concat.sequences(Integer.toString(number.getValue()), number.getUnit());
    }

    private void renderScalarSetting(UiRenderer renderer, UiId id, CharSequence label, float x, float y,
                                      float value, Setting setting) {
        boolean editing = editingNumber(setting);
        painter(renderer).text(FontAsset.INTER, label, x, y + 8.0f, 13.0f, -1);
        new DashboardWidgetRenderer(painter(renderer)).rightAlignedText(FontAsset.INTER, scalarValueText(setting),
                x + 476.0f, y + 9.0f, 12.0f, editing ? -1 : -2130706433);
        if (editing && caretVisible()) {
            float unitWidth = painter(renderer).textWidth(FontAsset.INTER, setting.getUnit(), 12.0f);
            painter(renderer).roundedRect(x + 476.0f - unitWidth - 1.0f, y + 9.0f, 1.0f, 14.0f, 0.0f, -1275068417);
        }
        renderSlider(renderer, id, x, y + 33.0f, 476.0f, 7.0f, value);
    }

    private void renderSlider(UiRenderer renderer, UiId id, float x, float y, float width, float height, float value) {
        float visual = animations.spring(id, SLIDER_VISUAL_VALUE, value, value, SpringSpec.CRITICAL_10_HZ);
        visual = Math.max(0.0f, Math.min(1.0f, visual));
        painter(renderer).roundedRect(x, y, width, height, 3.5f, 536870911);
        float thumb = DashboardLayout.sliderThumbCenter(x, width, 16.0f, visual);
        painter(renderer).roundedRect(x, y, thumb - x, height, 3.5f, -4144960);
        painter(renderer).roundedRect(thumb - 8.0f, y - 1.0f, 16.0f, 9.0f, 2.0f, -4144960);
    }

    private float[] rangeTextGeometry(FloatRangeSetting setting, float x) {
        UiRenderer renderer = Echo.uiRenderer;
        float lower = Math.max(editingRange(setting, false) ? 10.0f : 0.0f,
                painter(renderer).textWidth(FontAsset.INTER, rangeValueText(setting, false), 12.0f));
        float upper = Math.max(editingRange(setting, true) ? 10.0f : 0.0f,
                painter(renderer).textWidth(FontAsset.INTER, rangeValueText(setting, true), 12.0f));
        float separator = painter(renderer).textWidth(FontAsset.INTER, RANGE_VALUE_SEPARATOR, 12.0f);
        float unit = painter(renderer).textWidth(FontAsset.INTER, setting.getUnit(), 12.0f);
        float start = x + 476.0f - (lower + separator + upper + unit);
        return new float[]{start, start + lower, start + lower + separator, start + lower + separator + upper, lower, upper};
    }

    private void renderRangeSetting(UiRenderer renderer, UiId id, CharSequence label, float x, float y, FloatRangeSetting range) {
        painter(renderer).text(FontAsset.INTER, label, x, y + 8.0f, 13.0f, -1);
        boolean lowerEditing = editingRange(range, false);
        boolean upperEditing = editingRange(range, true);
        float[] positions = rangeTextGeometry(range, x);
        painter(renderer).text(FontAsset.INTER, rangeValueText(range, false), positions[0], y + 9.0f, 12.0f,
                lowerEditing ? -1 : -2130706433);
        painter(renderer).text(FontAsset.INTER, RANGE_VALUE_SEPARATOR, positions[1], y + 9.0f, 12.0f, -2130706433);
        painter(renderer).text(FontAsset.INTER, rangeValueText(range, true), positions[2], y + 9.0f, 12.0f,
                upperEditing ? -1 : -2130706433);
        painter(renderer).text(FontAsset.INTER, range.getUnit(), positions[3], y + 9.0f, 12.0f, -2130706433);
        if ((lowerEditing || upperEditing) && caretVisible()) painter(renderer).roundedRect(
                (lowerEditing ? positions[1] : positions[3]) - 1.0f, y + 9.0f, 1.0f, 14.0f, 0.0f, -1275068417);
        float lower = normalizedValue(range.getLower(), range.getMinimum(), range.getMaximum());
        float upper = normalizedValue(range.getUpper(), range.getMinimum(), range.getMaximum());
        renderRangeSlider(renderer, id, x, y + 33.0f, 476.0f, 7.0f, lower, upper);
    }

    private void renderRangeSlider(UiRenderer renderer, UiId id, float x, float y, float width, float height,
                                   float lower, float upper) {
        float visualLower = DashboardLayout.clampFraction(animations.spring(id, SLIDER_VISUAL_MINIMUM, lower, lower, SpringSpec.CRITICAL_10_HZ));
        float visualUpper = DashboardLayout.clampFraction(animations.spring(id, SLIDER_VISUAL_MAXIMUM, upper, upper, SpringSpec.CRITICAL_10_HZ));
        painter(renderer).roundedRect(x, y, width, height, 3.5f, 536870911);
        float left = DashboardLayout.sliderThumbCenter(x, width, 16.0f, visualLower);
        float right = DashboardLayout.sliderThumbCenter(x, width, 16.0f, Math.max(visualLower, visualUpper));
        painter(renderer).roundedRect(left, y, Math.max(0.0f, right - left), height, 3.5f, -4144960);
        painter(renderer).roundedRect(left - 8.0f, y - 1.0f, 16.0f, 9.0f, 2.0f, -4144960);
        painter(renderer).roundedRect(right - 8.0f, y - 1.0f, 16.0f, 9.0f, 2.0f, -4144960);
    }

    private String settingKeyLabel(KeybindSetting setting) {
        return capturingBind && bindingSetting == setting ? SETTING_PRESS_KEY_LABEL : setting.getKey().displayLabel();
    }

    private void renderTextSetting(UiRenderer renderer, CharSequence label, float x, float y, TextSetting setting) {
        painter(renderer).text(FontAsset.INTER, label, x, y + 8.0f, 13.0f, -1);
        boolean editing = textFocus == TextInputFocus.SETTING_TEXT && textSetting == setting;
        painter(renderer).panel(x, y + 30.0f, 476.0f, 32.0f, 5.0f, 234881023, 1.0f, editing ? 1728053247 : -14408668);
        String text = editing ? settingText : setting.getText();
        String displayed = text.isEmpty() && !editing ? setting.getPlaceholder() : text;
        renderer.pushScissor(renderX(x), renderY(y + 30.0f), renderSize(476.0f), renderSize(32.0f));
        painter(renderer).text(FontAsset.INTER, displayed, x + 10.0f, y + 39.0f, 13.0f, text.isEmpty() && !editing ? -2130706433 : -1);
        if (editing && caretVisible()) {
            float caretX = x + 10.0f + painter(renderer).textWidth(FontAsset.INTER, text, 13.0f) + 2.0f;
            painter(renderer).roundedRect(caretX, y + 37.0f, 1.0f, 16.0f, 0.0f, -1275068417);
        }
        renderer.popScissor();
    }

    private boolean overlapsOtherChoicePopup(ChoiceSetting setting, float x, float y, float width, float height) {
        if (openChoice == null || openChoice == setting) return false;
        UiRect popup = choicePopupLayout().popup();
        return x < popup.x() + popup.width() && x + width > popup.x() && y < popup.y() + popup.height() && y + height > popup.y();
    }

    private void renderChoicePill(UiRenderer renderer, float x, float y, float width, float height,
                                 CharSequence text, ChoiceSetting setting, boolean chevron) {
        painter(renderer).roundedRect(x, y, width, height, 5.0f, 268435455);
        renderer.pushScissor(renderX(x), renderY(y), renderSize(width), renderSize(height));
        painter(renderer).text(FontAsset.INTER, text, x + 8.0f, y + 6.0f, 13.0f, -2130706433);
        if (chevron) {
            UiId id = settingId(setting).child(CHOICE_CHEVRON_ID);
            float target = openChoice == setting ? 1.0f : 0.0f;
            float up = animations.tween(id, CHOICE_CHEVRON_PROPERTY, target, target, TransitionSpec.DURATION_180_MS);
            float iconX = x + width - 13.0f - 7.5f * 0.5f;
            float iconY = y + height * 0.5f - 4.5f * 0.5f;
            if (up < 0.999f) painter(renderer).icon(DashboardIcon.DROPDOWN_CHEVRON, iconX, iconY, 7.5f, 4.5f, 1.0f - up, -6710887);
            if (up > 0.001f) painter(renderer).icon(DashboardIcon.DROPDOWN_CHEVRON_UP, iconX, iconY, 7.5f, 4.5f, up, -6710887);
        }
        renderer.popScissor();
    }

    private void renderMultiChoiceSetting(UiRenderer renderer, UiId id, CharSequence label,
                                          float x, float y, MultiChoiceSetting setting) {
        painter(renderer).text(FontAsset.INTER, label, x, y + 8.0f, 13.0f, -1);
        if (setting.isSelectionMode()) {
            renderSearchBox(renderer, id.child(MULTI_SEARCH_ID_SEGMENT), x, y + 30.0f, 476.0f,
                    multiChoiceSearches.getOrDefault(settingId(setting), ""),
                    textFocus == TextInputFocus.MULTI_SEARCH && searchedMultiChoice == setting, MULTI_SEARCH_PLACEHOLDER);
        }
        List<Map.Entry<CharSequence, Boolean>> options = filteredMultiChoices(setting);
        float top = y + (setting.isSelectionMode() ? 76.0f : 33.0f);
        int visibleCount = setting.isSelectionMode() ? setting.getOptions().size() : filteredMultiChoices(setting).size();
        float height = Math.max(1, Math.min(6, visibleCount)) * 32.0f;
        float scroll = multiChoiceScroll.getOrDefault(id, 0.0f);
        float totalHeight = options.size() * 32.0f;
        boolean overflowing = totalHeight > height;
        float rowWidth = overflowing ? 466.0f : 476.0f;
        renderer.pushScissor(renderX(x), renderY(top), renderSize(476.0f), renderSize(height));
        for (int index = 0; index < options.size(); index++) {
            float rowY = top + index * 32.0f - scroll;
            if (rowY + 32.0f < top || rowY > top + height) continue;
            Map.Entry<CharSequence, Boolean> option = options.get(index);
            renderOptionRow(renderer, id.child(CHOICE_OPTION_ID_SEGMENT).child(option.getKey()),
                    x, rowY, rowWidth, option.getKey(), null, Boolean.TRUE.equals(option.getValue()));
        }
        renderer.popScissor();
        if (overflowing) {
            float thumbHeight = Math.max(24.0f, height * height / totalHeight);
            float travel = height - thumbHeight;
            float thumbY = top + travel * (scroll / (totalHeight - height));
            painter(renderer).roundedRect(x + 476.0f - 4.0f, top, 3.0f, height, 1.5f, 352321535);
            painter(renderer).roundedRect(x + 476.0f - 5.0f, thumbY, 5.0f, thumbHeight, 2.5f, 1728053247);
        }
        if (options.isEmpty()) painter(renderer).text(FontAsset.INTER, NO_MULTI_MATCHES_LABEL, x, top + 8.0f, 12.0f, -2130706433);
    }

    private void renderSearchBox(UiRenderer renderer, UiId id, float x, float y, float width,
                                  String value, boolean focused, String placeholder) {
        float target = focused ? 1.0f : 0.0f;
        float focus = animations.tween(id, FOCUS_PROPERTY, target, target, TransitionSpec.DURATION_140_MS);
        painter(renderer).panel(x, y, width, 34.0f, 6.0f, 234881023, 1.0f,
                DashboardColors.interpolate(-14408668, 1509949439, focus));
        drawGlyph(renderer, 57681, x + 12.0f, y + 9.0f, 16.0f, 1308622847);
        painter(renderer).text(FontAsset.INTER, value.isEmpty() ? placeholder : value,
                x + 36.0f, y + 9.5f, 13.0f, value.isEmpty() ? 1308622847 : -1);
        if (focused && caretVisible()) {
            float caretX = x + 36.0f + painter(renderer).textWidth(FontAsset.INTER, value, 13.0f);
            drawRule(renderer, caretX, y + 8.0f, 1.5f, 17.0f, -1);
        }
    }

    private void renderOptionRow(UiRenderer renderer, UiId id, float x, float y, float width,
                                  CharSequence label, CharSequence suffix, boolean selected) {
        float target = selected ? 1.0f : 0.0f;
        float fill = animations.tween(id, OPTION_FILL_PROPERTY, target, target, TransitionSpec.DURATION_140_MS);
        float indicator = animations.tween(id, OPTION_INDICATOR_PROPERTY, target, target, TransitionSpec.DURATION_160_MS);
        int color = DashboardColors.multiplyAlpha(268435455, fill);
        if (color >>> 24 != 0) drawPixelAlignedPanel(renderer, x, y, width, 28.0f, 3.0f, color, 0.0f, 0);
        if (indicator > 0.001f) {
            float height = 10.0f * indicator;
            painter(renderer).roundedRect(x + 8.0f, y + 14.0f - height * 0.5f, 3.0f, height, 1.5f,
                    DashboardColors.multiplyAlpha(-4144960, indicator));
        }
        painter(renderer).text(FontAsset.INTER, label, x + 19.0f * fill, y + 7.0f, 13.0f,
                DashboardColors.interpolate(-2130706433, -1, fill));
        if (suffix != null) new DashboardWidgetRenderer(painter(renderer)).rightAlignedText(FontAsset.INTER,
                suffix, x + width - 10.0f, y + 8.0f, 11.0f, -2130706433);
    }

    /** Original condition-row helper, retaining its 31-unit background and centered indicator. */
    private void renderConditionRow(UiRenderer renderer, String label, float x, float y, boolean selected) {
        renderConditionRow(renderer, label, x, y, 476.0f, selected);
    }

    private void renderConditionRow(UiRenderer renderer, String label, float x, float y, float width, boolean selected) {
        UiId id = UiId.ROOT.child(CONDITION_ID_PREFIX + label);
        float target = selected ? 1.0f : 0.0f;
        float fill = animations.tween(id, CONDITION_FILL_PROPERTY, target, target, TransitionSpec.DURATION_140_MS);
        float indicator = animations.tween(id, CONDITION_INDICATOR_PROPERTY, target, target, TransitionSpec.DURATION_160_MS);
        int color = DashboardColors.multiplyAlpha(268435455, fill);
        if (color >>> 24 != 0) painter(renderer).roundedRect(x, y, width, 31.0f, 3.0f, color);
        if (indicator > 0.001f) {
            float height = 10.0f * indicator;
            painter(renderer).roundedRect(x + 8.0f, y + 15.5f - height * 0.5f, 3.0f, height, 1.5f,
                    DashboardColors.multiplyAlpha(-4144960, indicator));
        }
        painter(renderer).text(FontAsset.INTER, label, x + 19.0f * fill, y + 8.0f, 13.0f,
                DashboardColors.interpolate(-2130706433, -1, fill));
    }

    private void renderSlider(UiRenderer renderer, String id, float x, float y, float width, float height, float value) {
        renderSlider(renderer, UiId.ROOT.child(id), x, y, width, height, value);
    }

    private void renderRangeSlider(UiRenderer renderer, String id, float x, float y, float width, float height, float lower, float upper) {
        renderRangeSlider(renderer, UiId.ROOT.child(id), x, y, width, height, lower, upper);
    }

    private void drawPixelAlignedPanel(UiRenderer renderer, float x, float y, float width, float height,
                                       float radius, int fill, float borderWidth, int border) {
        float opacity = Math.max(0.0f, Math.min(1.0f, openingOpacity * layerOpacity));
        renderer.drawPixelAlignedRect(renderX(x), renderY(y), renderSize(width), renderSize(height), renderSize(radius),
                DashboardColors.multiplyAlpha(fill, opacity), renderSize(borderWidth), DashboardColors.multiplyAlpha(border, opacity));
    }

    private void renderCustomSetting(UiRenderer renderer, CustomWidgetSetting setting, float x, float y) {
        float height = setting.getHeight();
        int pages = setting.getColumnCount();
        int selected = Math.floorMod(setting.getLayoutIndex(), pages);
        setting.setLayoutIndex(selected);
        painter(renderer).backdropPanel(x, y, 476.0f, height, 8.0f, -871888888, 1.0f, -14408668);
        renderer.pushScissor(renderX(x), renderY(y), renderSize(476.0f), renderSize(height));
        ItemHudLayout layout = new ItemHudLayout(renderer, renderX(x), renderY(y), renderSize(1.0f),
                476.0f, height, Math.max(0.0f, Math.min(1.0f, openingOpacity * layerOpacity)), selected, mouseX - x, mouseY - y);
        setting.render(layout);
        renderer.popScissor();
        if (pages > 1) {
            UiId id = settingId(setting);
            new DashboardWidgetRenderer(painter(renderer)).centeredText(FontAsset.INTER, selected + 1 + "/" + pages,
                    x + 238.0f, y + 12.0f, 11.0f, -2130706433);
            renderPreviewArrow(renderer, id.child(PREVIEW_PREVIOUS_ID), previewArrowBounds(x, y, height, false), true);
            renderPreviewArrow(renderer, id.child(PREVIEW_NEXT_ID), previewArrowBounds(x, y, height, true), false);
        }
    }

    private static UiRect previewArrowBounds(float x, float y, float height, boolean next) {
        return new UiRect(next ? x + 476.0f - 10.0f - 26.0f : x + 10.0f, y + (height - 26.0f) * 0.5f, 26.0f, 26.0f);
    }

    private void renderPreviewArrow(UiRenderer renderer, UiId id, UiRect bounds, boolean previous) {
        float target = containsPointer(bounds.x(), bounds.y(), bounds.width(), bounds.height()) ? 1.0f : 0.0f;
        float hover = animations.tween(id, CARD_HOVER_PROPERTY, target, target, TransitionSpec.DURATION_140_MS);
        painter(renderer).panel(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 6.0f,
                DashboardColors.interpolate(184549375, 352321535, hover), 1.0f,
                DashboardColors.interpolate(352321535, 1090519039, hover));
        painter(renderer).icon(previous ? DashboardIcon.PREVIEW_PREV : DashboardIcon.PREVIEW_NEXT,
                bounds.x() + 6.5f, bounds.y() + 6.5f, 13.0f, 13.0f, 1.0f,
                DashboardColors.interpolate(-2130706433, -1, hover));
    }

    private void renderColorSetting(UiRenderer renderer, UiId id, CharSequence label, float x, float y, ColorSetting setting) {
        int color = setting.getArgb();
        float target = openColorPicker == setting ? 1.0f : 0.0f;
        float selected = animations.tween(id, SEGMENT_SELECTION_PROPERTY, target, target, TransitionSpec.DURATION_140_MS);
        painter(renderer).text(FontAsset.INTER, label, x, y + 8.0f, 13.0f, -1);
        painter(renderer).panel(x + 412.0f, y, 64.0f, 31.0f, 6.0f,
                DashboardColors.interpolate(234881023, 352321535, selected), 1.0f,
                DashboardColors.interpolate(-14408668, 1728053247, selected));
        painter(renderer).roundedRect(x + 420.0f, y + 7.0f, 17.0f, 17.0f, 4.0f, color);
        painter(renderer).text(FontAsset.INTER, EDIT_SETTING_LABEL, x + 444.0f, y + 8.0f, 11.0f,
                DashboardColors.interpolate(-2130706433, -1275068417, selected));
    }

    private void renderRegistrySetting(UiRenderer renderer, UiId id, CharSequence label, float x, float y, RegistryListSetting setting) {
        float target = openRegistryPicker == setting ? 1.0f : 0.0f;
        float selected = animations.tween(id, SEGMENT_SELECTION_PROPERTY, target, target, TransitionSpec.DURATION_140_MS);
        int count = setting.getSelectedCount();
        painter(renderer).text(FontAsset.INTER, label, x, y + 8.0f, 13.0f, -1);
        painter(renderer).panel(x + 412.0f, y, 64.0f, 31.0f, 6.0f,
                DashboardColors.interpolate(234881023, 352321535, selected), 1.0f,
                DashboardColors.interpolate(-14408668, 1728053247, selected));
        new DashboardWidgetRenderer(painter(renderer)).centeredText(FontAsset.INTER, EDIT_SETTING_LABEL,
                x + 444.0f, y + 9.0f, 11.0f, DashboardColors.interpolate(-2130706433, -1275068417, selected));
        String summary = count == 0 ? ANY_REGISTRY_ENTRY_LABEL : count + SELECTED_REGISTRY_ENTRIES_SUFFIX;
        new DashboardWidgetRenderer(painter(renderer)).rightAlignedText(FontAsset.INTER, summary,
                x + 400.0f, y + 9.0f, 12.0f, -2130706433);
    }

    private int unusedSettingColumn() {
        for (int column = SETTING_COLUMN_X.length - 1; column >= 0; column--) {
            boolean occupied = false;
            for (SettingSectionLayout section : settingSections) if (section.column() == column) occupied = true;
            if (!occupied) return column;
        }
        return -1;
    }

    private List<ColorSetting> visibleColorSettings() {
        List<ColorSetting> result = new ArrayList<>();
        Feature feature = settingsFeature();
        if (feature == null) return result;
        for (Setting setting : feature.settings) {
            if (setting instanceof ColorSetting color && setting.isVisible()) result.add(color);
        }
        return result;
    }

    private List<RegistryListSetting<?>> visibleRegistrySettings() {
        List<RegistryListSetting<?>> result = new ArrayList<>();
        Feature feature = settingsFeature();
        if (feature == null) return result;
        for (Setting setting : feature.settings) {
            if (setting instanceof RegistryListSetting<?> registry && setting.isVisible()) result.add(registry);
        }
        return result;
    }

    private void renderColorPickerIfPresent(UiRenderer renderer) {
        if (openColorPicker == null || unusedSettingColumn() < 0) return;
        renderColorPicker(renderer);
    }

    private void renderColorPicker(UiRenderer renderer) {
        int column = unusedSettingColumn();
        float panelX = SETTING_COLUMN_X[column];
        float x = panelX + 12.0f;
        float top = settingColumnTop(column);
        int color = openColorPicker.getArgb();
        float hue = DashboardColorPicker.hue(color);
        float saturation = DashboardColorPicker.saturation(color);
        float brightness = DashboardColors.brightness(color);
        float alpha = (color >>> 24 & 255) / 255.0f;
        List<ColorSetting> settings = visibleColorSettings();
        float squareY = DashboardLayout.optionGridBottom(top, settings.size());
        new DashboardWidgetRenderer(painter(renderer)).section(panelX, top, 500.0f, squareY - top + 373.0f, 36.0f, COLOR_PICKER_TITLE);
        for (int index = 0; index < settings.size(); index++) {
            ColorSetting setting = settings.get(index);
            renderColorTarget(renderer, x + index % 2 * 244, top + 48.0f + index / 2 * 44,
                    232.0f, setting.getLabel(), setting, setting.getArgb());
        }
        float opacity = Math.max(0.0f, Math.min(1.0f, openingOpacity * layerOpacity));
        renderer.drawSaturationBrightness(renderX(x), renderY(squareY), renderSize(476.0f), renderSize(190.0f), renderSize(5.0f), hue, opacity);
        float handleX = x + saturation * 476.0f;
        float handleY = squareY + (1.0f - brightness) * 190.0f;
        drawShadow(renderer, handleX - 5.0f, handleY - 5.0f, 10.0f, 10.0f, 5.0f, 4.0f, Integer.MIN_VALUE);
        painter(renderer).panel(handleX - 5.0f, handleY - 5.0f, 10.0f, 10.0f, 5.0f, 0, 2.0f, -1);
        float stripsY = squareY + 204.0f;
        painter(renderer).panel(x, stripsY, 24.0f, 24.0f, 5.0f, color, 1.0f, 872415231);
        renderer.drawHueStrip(renderX(x + 36.0f), renderY(stripsY + 2.0f), renderSize(440.0f), renderSize(7.0f), renderSize(3.5f), opacity);
        renderer.drawAlphaStrip(renderX(x + 36.0f), renderY(stripsY + 17.0f), renderSize(440.0f), renderSize(7.0f), renderSize(3.5f),
                (color >> 16 & 255) / 255.0f, (color >> 8 & 255) / 255.0f, (color & 255) / 255.0f, opacity);
        float hueX = DashboardLayout.sliderThumbCenter(x + 36.0f, 440.0f, 9.0f, hue);
        float alphaX = DashboardLayout.sliderThumbCenter(x + 36.0f, 440.0f, 9.0f, alpha);
        drawShadow(renderer, hueX - 4.5f, stripsY - 2.5f, 9.0f, 16.0f, 4.5f, 3.0f, 1711276032);
        painter(renderer).panel(hueX - 4.5f, stripsY - 2.5f, 9.0f, 16.0f, 4.5f, 0, 2.0f, -1);
        drawShadow(renderer, alphaX - 4.5f, stripsY + 12.5f, 9.0f, 16.0f, 4.5f, 3.0f, 1711276032);
        painter(renderer).panel(alphaX - 4.5f, stripsY + 12.5f, 9.0f, 16.0f, 4.5f, 0, 2.0f, -1);
        painter(renderer).panel(x, squareY + 247.0f, 476.0f, 38.0f, 6.0f, -15921907, 1.0f, -14671840);
        String hex = String.format(Locale.ROOT, COLOR_HEX_FORMAT, color >> 16 & 255, color >> 8 & 255, color & 255);
        painter(renderer).text(FontAsset.JETBRAINS_MONO, hex, x + 12.0f, squareY + 258.0f, 13.0f, -1275068417);
        drawRule(renderer, x + 402.0f, squareY + 255.0f, 1.0f, 22.0f, -14671840);
        new DashboardWidgetRenderer(painter(renderer)).rightAlignedText(FontAsset.INTER, Math.round(alpha * 100.0f) + "%",
                x + 464.0f, squareY + 258.5f, 13.0f, -1711276033);
        drawRule(renderer, panelX, squareY + 301.0f, 500.0f, 1.0f, -14671840);
        painter(renderer).text(FontAsset.INTER_SEMI_BOLD, COLOR_PRESETS_LABEL, x, squareY + 315.0f, 10.0f, -2130706433);
        for (int index = 0; index < COLOR_PRESETS.length; index++) {
            painter(renderer).panel(x + index * 30, squareY + 339.0f, 18.0f, 18.0f, 5.0f, COLOR_PRESETS[index], 1.0f, 872415231);
        }
    }

    private void renderColorTarget(UiRenderer renderer, float x, float y, float width,
                                   CharSequence label, ColorSetting setting, int color) {
        float target = openColorPicker == setting ? 1.0f : 0.0f;
        float selected = animations.tween(settingId(setting).child(COLOR_TARGET_ID_SEGMENT), COLOR_TARGET_SELECTION,
                target, target, TransitionSpec.DURATION_180_MS);
        painter(renderer).panel(x, y, width, 32.0f, 6.0f,
                DashboardColors.interpolate(234881023, 268435455, selected), 1.0f,
                DashboardColors.interpolate(-14408668, 1728053247, selected));
        painter(renderer).panel(x + 9.0f, y + 8.0f, 16.0f, 16.0f, 4.0f, color, 1.0f, 872415231);
        painter(renderer).text(FontAsset.INTER, label, x + 34.0f, y + 8.0f, 13.0f,
                DashboardColors.interpolate(-1711276033, -1, selected));
    }

    private void renderRegistryPickerIfPresent(UiRenderer renderer) {
        if (openRegistryPicker == null || unusedSettingColumn() < 0) return;
        renderRegistryPicker(renderer);
    }

    private <T> void refreshRegistryRows(RegistryListSetting<T> setting) {
        String key = System.identityHashCode(setting) + "/" + registrySearch + "/" + setting.getSelectedIds().hashCode();
        if (key.equals(registryCacheKey)) return;
        registryCacheKey = key;
        Registry<T> registry = setting.getRegistry();
        Predicate<T> filter = setting.getEntryFilter();
        Function<T, String> labelProvider = setting.getEntryLabel();
        String query = registrySearch.toLowerCase(Locale.ROOT);
        registryRows.clear();
        for (T entry : registry) {
            if (!filter.test(entry)) continue;
            RegistryKey<T> registryKey = registry.getKey(entry).orElse(null);
            if (registryKey == null) continue;
            String label = labelProvider.apply(entry);
            if (query.isEmpty() || label.toLowerCase(Locale.ROOT).contains(query) || registryKey.getValue().toString().contains(query)) {
                registryRows.add(new RegistryPickerRow(registryKey.getValue(), label));
            }
        }
        registryRows.sort(Comparator.comparing(RegistryPickerRow::label));
        registryGroupStates.clear();
        for (RegistryEntryGroup<T> group : setting.getGroups()) registryGroupStates.add(setting.getGroupState(group));
    }

    private float layoutRegistryGroups(UiRenderer renderer, RegistryListSetting<?> setting, List<UiRect> bounds, float x, float y) {
        float offset = 0.0f;
        float row = 0.0f;
        List<? extends RegistryEntryGroup<?>> groups = setting.getGroups();
        for (int index = 0; index < groups.size(); index++) {
            CharSequence label = groups.get(index).label();
            float width = painter(renderer).textWidth(FontAsset.INTER, label, 12.0f) + 26.0f;
            if (offset > 0.0f && offset + width > 476.0f) {
                offset = 0.0f;
                row++;
            }
            if (bounds != null) bounds.add(new UiRect(x + offset, y + row * 34.0f, width, 26.0f));
            offset += width + 8.0f;
        }
        return (row + 1.0f) * 34.0f;
    }

    private float registryListTop(UiRenderer renderer, RegistryListSetting<?> setting, float top) {
        if (setting.getGroups().isEmpty()) return top + 114.0f;
        return top + 118.0f + layoutRegistryGroups(renderer, setting, null, 0.0f, 0.0f) + 20.0f;
    }

    private void renderRegistryPicker(UiRenderer renderer) {
        RegistryListSetting<?> setting = openRegistryPicker;
        refreshRegistryRows(setting);
        int column = unusedSettingColumn();
        float panelX = SETTING_COLUMN_X[column];
        float x = panelX + 12.0f;
        float top = settingColumnTop(column);
        float listTop = registryListTop(renderer, setting, top);
        float height = listTop - top + 300.0f + 56.0f;
        new DashboardWidgetRenderer(painter(renderer)).section(panelX, top, 500.0f, height, 36.0f, setting.getLabel());
        renderSearchBox(renderer, UiId.ROOT.child(REGISTRY_SEARCH_ID), x, top + 48.0f, 476.0f,
                registrySearch, textFocus == TextInputFocus.PICKER_SEARCH, REGISTRY_SEARCH_PLACEHOLDER);
        if (!setting.getGroups().isEmpty()) {
            painter(renderer).text(FontAsset.INTER_SEMI_BOLD, REGISTRY_GROUPS_LABEL, x, top + 96.0f, 10.0f, -2130706433);
            List<UiRect> bounds = new ArrayList<>();
            layoutRegistryGroups(renderer, setting, bounds, x, top + 114.0f);
            List<? extends RegistryEntryGroup<?>> groups = setting.getGroups();
            for (int index = 0; index < bounds.size(); index++) {
                if (index >= registryGroupStates.size()) break;
                renderRegistryGroup(renderer, bounds.get(index), groups.get(index).label(), registryGroupStates.get(index));
            }
        }
        drawRule(renderer, panelX, listTop - 12.0f, 500.0f, 1.0f, -14671840);
        if (registryRows.isEmpty()) painter(renderer).text(FontAsset.INTER, REGISTRY_NO_MATCHES_LABEL,
                x, listTop + 8.0f, 13.0f, -2130706433);
        renderer.pushScissor(renderX(x), renderY(listTop), renderSize(476.0f), renderSize(300.0f));
        for (int index = 0; index < registryRows.size(); index++) {
            float y = listTop + index * 32.0f - registryScroll;
            if (y + 32.0f < listTop || y > listTop + 300.0f) continue;
            RegistryPickerRow row = registryRows.get(index);
            boolean selected = setting.isIdSelected(row.id());
            renderOptionRow(renderer, UiId.ROOT.child(REGISTRY_ENTRY_ID_PREFIX + row.id()), x, y, 476.0f,
                    row.label(), row.id().getPath(), selected);
        }
        renderer.popScissor();
        float footer = listTop + 300.0f + 16.0f;
        drawRule(renderer, panelX, footer - 12.0f, 500.0f, 1.0f, -14671840);
        int count = setting.getSelectedCount();
        String selection = count == 0 ? REGISTRY_ANY_ALLOWED_LABEL : count + REGISTRY_SELECTION_SUFFIX;
        painter(renderer).text(FontAsset.INTER, selection, x, footer + 8.0f, 12.0f, -2130706433);
        painter(renderer).panel(x + 476.0f - 64.0f, footer, 64.0f, 28.0f, 6.0f, 234881023, 1.0f, -14408668);
        new DashboardWidgetRenderer(painter(renderer)).centeredText(FontAsset.INTER, REGISTRY_CLEAR_LABEL,
                x + 476.0f - 32.0f, footer + 8.0f, 12.0f, count == 0 ? -2130706433 : -1275068417);
    }

    private void renderRegistryGroup(UiRenderer renderer, UiRect bounds, CharSequence label, SelectionState state) {
        boolean all = state == SelectionState.ALL;
        boolean partial = state == SelectionState.PARTIAL;
        painter(renderer).panel(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 6.0f,
                all ? 268435455 : 234881023, 1.0f, all ? 1728053247 : -14408668);
        if (partial) painter(renderer).roundedRect(bounds.x() + 9.0f, bounds.y() + 11.0f, 4.0f, 4.0f, 2.0f, -1275068417);
        painter(renderer).text(FontAsset.INTER, label, bounds.x() + (partial ? 19 : 13), bounds.y() + 7.0f,
                12.0f, all ? -1 : partial ? -1275068417 : -2130706433);
    }

    private void renderNamedRanges(UiRenderer renderer, NamedRangeGroupSetting setting, UiId id, float x, float y) {
        painter(renderer).text(FontAsset.INTER, setting.getLabel(), x, y + 8.0f, 13.0f, -1);
        List<NamedRange> ranges = setting.getRanges();
        float top = y + 32.0f;
        float right = x + 476.0f;
        NamedRange dragging = null;
        float dragY = 0.0f;
        int dragIndex = -1;
        for (int index = 0; index < ranges.size(); index++) {
            NamedRange range = ranges.get(index);
            UiId rowId = id.child(range.getLabel());
            if (draggedNamedRange == range) {
                float offset = animations.snap(rowId, RANGE_DRAG_OFFSET_PROPERTY, draggedRangeY(top, ranges.size()) - top);
                dragging = range;
                dragY = top + offset;
                dragIndex = index;
            } else {
                float target = namedRangeOffset(setting, index);
                float offset = animations.spring(rowId, RANGE_OFFSET_PROPERTY, target, target, SpringSpec.CRITICAL_10_HZ);
                renderNamedRangeRow(renderer, setting, rowId, range, index, x, top + offset, right, false);
            }
        }
        if (dragging != null) renderNamedRangeRow(renderer, setting, id.child(dragging.getLabel()), dragging,
                dragIndex, x, dragY, right, true);
    }

    private float namedRangeOffset(NamedRangeGroupSetting setting, int index) {
        int edited = editedNamedRange == null ? -1 : setting.getRanges().indexOf(editedNamedRange);
        float offset = index * 36.0f;
        return edited >= 0 && index > edited ? offset + 72.0f : offset;
    }

    private float draggedRangeY(float top, int count) {
        float bottom = top + Math.max(0, count - 1) * 36.0f;
        return Math.max(top, Math.min(bottom, mouseY - namedRangeDragOffset));
    }

    private void renderNamedRangeRow(UiRenderer renderer, NamedRangeGroupSetting setting, UiId id, NamedRange range,
                                     int index, float x, float y, float right, boolean dragged) {
        int count = setting.getRanges().size();
        boolean editing = editedNamedRange == range && !dragged;
        float target = editing ? 1.0f : 0.0f;
        float open = animations.tween(id.child(RANGE_EDITOR_ID), RANGE_EDITOR_OPEN_PROPERTY,
                target, target, TransitionSpec.DURATION_160_MS);
        float height = 30.0f + 72.0f * open;
        painter(renderer).panel(x, y, 476.0f, height, 5.0f,
                dragged ? -15987700 : 234881023, 1.0f, dragged ? 1090519039 : -14408668);
        for (int column = 0; column < 2; column++) {
            for (int row = 0; row < 3; row++) {
                painter(renderer).roundedRect(x + 10.0f + column * 4, y + 10.0f + row * 4, 2.0f, 2.0f, 1.0f, 1090519039);
            }
        }
        painter(renderer).text(FontAsset.INTER, Integer.toString(index + 1), x + 24.0f, y + 8.0f, 12.0f, -2130706433);
        painter(renderer).text(FontAsset.INTER, range.getLabel(), x + 40.0f, y + 7.0f, 13.0f, range.isEnabled() ? -1 : -2130706433);
        if (setting.hasBounds()) {
            UiRect chip = namedRangeChipBounds(renderer, setting, range, x, y);
            float litTarget = !setting.coversAll(range) ? 1.0f : 0.0f;
            float lit = animations.tween(id.child(RANGE_CHIP_ID), RANGE_CHIP_LIT_PROPERTY,
                    litTarget, litTarget, TransitionSpec.DURATION_140_MS);
            boolean hovered = containsPointer(chip.x(), chip.y(), chip.width(), chip.height());
            int border = !editing && !hovered ? DashboardColors.interpolate(-14408668, 654311423, lit) : 1090519039;
            painter(renderer).panel(chip.x(), chip.y(), chip.width(), chip.height(), 3.0f,
                    DashboardColors.multiplyAlpha(352321535, lit), 1.0f, border);
            painter(renderer).text(FontAsset.INTER, namedRangeSummary(setting, range), chip.x() + 6.0f, chip.y() + 2.0f, 11.0f,
                    DashboardColors.interpolate(-2130706433, -637534209, lit));
        }
        new DashboardWidgetRenderer(painter(renderer)).smallChevron(DashboardIcon.DROPDOWN_CHEVRON_UP, right - 76.0f, y + 15.0f, index > 0);
        new DashboardWidgetRenderer(painter(renderer)).smallChevron(DashboardIcon.DROPDOWN_CHEVRON, right - 56.0f, y + 15.0f, index < count - 1);
        renderToggle(renderer, id.child(RANGE_TOGGLE_ID), right - 40.0f, y + 5.5f, 30.0f, 19.0f, 9.0f, range.isEnabled());
        if (!(open <= 0.001f)) {
            renderer.pushScissor(renderX(x), renderY(y), renderSize(476.0f), renderSize(height));
            renderNamedRangeEditor(renderer, setting, id, range, x, y + 30.0f, right);
            renderer.popScissor();
        }
    }

    private static String formatNamedRangeFloat(float value) {
        return value == Math.round(value) ? Integer.toString(Math.round(value)) : String.format(Locale.ROOT, NAMED_RANGE_FLOAT_FORMAT, value);
    }

    private String namedRangeSummary(NamedRangeGroupSetting setting, NamedRange range) {
        RangeBounds bounds = setting.getBounds();
        if (setting.coversAll(range)) return ANY_RANGE_LABEL;
        if (range.getUpper() >= bounds.maximum()) return formatNamedRangeFloat(range.getLower()) + "+";
        if (range.getLower() <= bounds.minimum()) return UNDER_RANGE_PREFIX + formatNamedRangeFloat(range.getUpper());
        return formatNamedRangeFloat(range.getLower()) + RANGE_VALUE_SEPARATOR + formatNamedRangeFloat(range.getUpper());
    }

    private UiRect namedRangeChipBounds(UiRenderer renderer, NamedRangeGroupSetting setting, NamedRange range, float x, float y) {
        float left = x + 40.0f + painter(renderer).textWidth(FontAsset.INTER, range.getLabel(), 13.0f) + 8.0f;
        float width = painter(renderer).textWidth(FontAsset.INTER, namedRangeSummary(setting, range), 11.0f) + 12.0f;
        return new UiRect(left, y + 7.0f, width, 16.0f);
    }

    private boolean editingNamedRange(NamedRangeGroupSetting setting, NamedRange range, boolean upper) {
        return editingNumber(setting) && numericNamedRange == range && editingUpperRange == upper;
    }

    private String namedRangeValueText(NamedRangeGroupSetting setting, NamedRange range, boolean upper) {
        if (editingNamedRange(setting, range, upper)) return numericText;
        if (!upper) return formatNamedRangeFloat(range.getLower());
        RangeBounds bounds = setting.getBounds();
        return range.getUpper() >= bounds.maximum() ? formatNamedRangeFloat(bounds.maximum()) + "+" : formatNamedRangeFloat(range.getUpper());
    }

    private float[] namedRangeTextGeometry(NamedRangeGroupSetting setting, NamedRange range, float right) {
        UiRenderer renderer = Echo.uiRenderer;
        float lower = Math.max(editingNamedRange(setting, range, false) ? 10.0f : 0.0f,
                painter(renderer).textWidth(FontAsset.INTER, namedRangeValueText(setting, range, false), 12.0f));
        float upper = Math.max(editingNamedRange(setting, range, true) ? 10.0f : 0.0f,
                painter(renderer).textWidth(FontAsset.INTER, namedRangeValueText(setting, range, true), 12.0f));
        float separator = painter(renderer).textWidth(FontAsset.INTER, RANGE_VALUE_SEPARATOR, 12.0f);
        float unit = painter(renderer).textWidth(FontAsset.INTER, setting.getBounds().unit(), 12.0f);
        float start = right - 10.0f - (lower + separator + upper + unit);
        return new float[]{start, start + lower, start + lower + separator, start + lower + separator + upper, lower, upper};
    }

    private UiRect rangeBandBounds(UiRenderer renderer, RangeBand band, float x, float y) {
        return new UiRect(x, y + 46.0f, painter(renderer).textWidth(FontAsset.INTER, band.label(), 11.0f) + 14.0f, 18.0f);
    }

    private static boolean rangeMatchesBand(NamedRangeGroupSetting setting, NamedRange range, RangeBand band) {
        RangeBounds bounds = setting.getBounds();
        float lower = Math.max(bounds.minimum(), Math.min(bounds.maximum(), band.lower()));
        float upper = Math.max(bounds.minimum(), Math.min(bounds.maximum(), band.upper()));
        return range.getLower() == lower && range.getUpper() == upper;
    }

    private void renderNamedRangeEditor(UiRenderer renderer, NamedRangeGroupSetting setting, UiId id, NamedRange range,
                                       float x, float y, float right) {
        RangeBounds bounds = setting.getBounds();
        painter(renderer).text(FontAsset.INTER, bounds.label(), x + 30.0f, y + 6.0f, 12.0f, -2130706433);
        boolean lowerEditing = editingNamedRange(setting, range, false);
        boolean upperEditing = editingNamedRange(setting, range, true);
        float[] positions = namedRangeTextGeometry(setting, range, right);
        painter(renderer).text(FontAsset.INTER, namedRangeValueText(setting, range, false), positions[0], y + 7.0f, 12.0f,
                lowerEditing ? -1 : -1275068417);
        painter(renderer).text(FontAsset.INTER, RANGE_VALUE_SEPARATOR, positions[1], y + 7.0f, 12.0f, -2130706433);
        painter(renderer).text(FontAsset.INTER, namedRangeValueText(setting, range, true), positions[2], y + 7.0f, 12.0f,
                upperEditing ? -1 : -1275068417);
        painter(renderer).text(FontAsset.INTER, bounds.unit(), positions[3], y + 7.0f, 12.0f, -2130706433);
        if ((lowerEditing || upperEditing) && caretVisible()) painter(renderer).roundedRect(
                (lowerEditing ? positions[1] : positions[3]) - 1.0f, y + 7.0f, 1.0f, 14.0f, 0.0f, -1275068417);
        float lower = normalizedValue(range.getLower(), bounds.minimum(), bounds.maximum());
        float upper = normalizedValue(range.getUpper(), bounds.minimum(), bounds.maximum());
        renderRangeSlider(renderer, id.child(NAMED_RANGE_SLIDER_ID), x + 30.0f, y + 30.0f, 436.0f, 7.0f, lower, upper);
        float bandX = x + 30.0f;
        for (RangeBand band : setting.getBands()) {
            UiRect rectangle = rangeBandBounds(renderer, band, bandX, y);
            boolean selected = rangeMatchesBand(setting, range, band);
            boolean hovered = containsPointer(rectangle.x(), rectangle.y(), rectangle.width(), rectangle.height());
            painter(renderer).panel(rectangle.x(), rectangle.y(), rectangle.width(), rectangle.height(), 3.0f,
                    selected ? 352321535 : 0, 1.0f, !selected && !hovered ? -14408668 : 1090519039);
            painter(renderer).text(FontAsset.INTER, band.label(), rectangle.x() + 7.0f, rectangle.y() + 3.0f,
                    11.0f, selected ? -637534209 : -2130706433);
            bandX += rectangle.width() + 6.0f;
        }
    }

    private void renderFeatureBinding(UiRenderer renderer) {
        new DashboardWidgetRenderer(painter(renderer)).section(330.0f, 110.0f, 500.0f, 152.0f, 36.0f, MAIN_SETTINGS_LABEL);
        painter(renderer).text(FontAsset.INTER, TOGGLE_FEATURE_LABEL, 342.0f, 159.5f, 14.0f, -1);
        renderToggle(renderer, FeatureNameAssignments.id(selectedFeature) + TOGGLE_ID_SUFFIX,
                788.0f, 158.0f, 30.0f, 19.0f, 9.0f, selectedFeature.isEnabled());
        renderFeatureBindKey(renderer, selectedFeature, 195.0f);
        painter(renderer).text(FontAsset.INTER, VISIBLE_FEATURE_LABEL, 342.0f, 231.5f, 14.0f, -1);
        renderToggle(renderer, FeatureNameAssignments.id(selectedFeature) + FEATURE_VISIBLE_SUFFIX,
                788.0f, 230.0f, 30.0f, 19.0f, 9.0f, selectedFeature.isVisible());
    }

    private void renderFeatureBindKey(UiRenderer renderer, Feature feature, float y) {
        painter(renderer).text(FontAsset.INTER, BIND_LABEL, 342.0f, y, 14.0f, -1);
        boolean capturing = capturingBind && bindingFeature == feature;
        String label = capturing ? PRESS_KEY_LABEL : feature.getKey().displayLabel();
        float targetWidth = Math.max(20.0f, painter(renderer).textWidth(FontAsset.INTER, label, 13.0f) + 12.0f);
        UiId id = UiId.ROOT.child(BIND_ID_PREFIX + FeatureNameAssignments.id(feature));
        float previousWidth = featureBindWidths.getOrDefault(feature, 36.0f);
        float width = animations.tween(id, BIND_WIDTH_PROPERTY, previousWidth, targetWidth, TransitionSpec.DURATION_140_MS);
        featureBindWidths.put(feature, width);
        renderBindPill(renderer, 753.0f - width, y - 5.5f, width, 27.0f, label);
        if (capturing) {
            float pulse = 0.55f + 0.45f * (float) Math.sin(System.nanoTime() / 1.0e9 * Math.PI * 2.0);
            painter(renderer).panel(753.0f - width, y - 5.5f, width, 27.0f, 2.0f, 0, 1.0f,
                    DashboardColors.multiplyAlpha(-2130706433, pulse));
        }
        renderFeatureBindMode(renderer, feature, y - 6.0f);
    }

    private void renderBindPill(UiRenderer renderer, float x, float y, float width, float height, String label) {
        painter(renderer).roundedRect(x, y, width, height, 5.0f, 268435455);
        renderer.pushScissor(renderX(x), renderY(y), renderSize(width), renderSize(height));
        new DashboardWidgetRenderer(painter(renderer)).centeredText(FontAsset.INTER, label,
                x + width * 0.5f, y + 6.0f, 13.0f, -2130706433);
        renderer.popScissor();
    }

    private void renderFeatureBindMode(UiRenderer renderer, Feature feature, float y) {
        float target = feature.isHoldMode() ? 1.0f : 0.0f;
        float selection = animations.tween(UiId.ROOT.child(BIND_MODE_ID_PREFIX + FeatureNameAssignments.id(feature) + BIND_MODE_SUFFIX),
                SEGMENT_SELECTION_PROPERTY, target, target, TransitionSpec.DURATION_180_MS);
        painter(renderer).roundedRect(761.0f, y, 57.0f, 28.0f, 4.0f, 234881023);
        painter(renderer).roundedRect(761.0f + 29.0f * selection, y, 28.0f, 28.0f, 4.0f, 268435455);
        painter(renderer).icon(DashboardIcon.BIND_TOGGLE, 769.375f, y + 8.375f, 11.25f, 11.25f, 1.0f,
                DashboardColors.interpolate(-4144960, 1728053247, selection));
        painter(renderer).icon(DashboardIcon.BIND_HOLD, 797.848f, y + 5.938f, 11.162f, 15.5625f, 1.0f,
                DashboardColors.interpolate(-2130706433, -4144960, selection));
    }

    private static int choiceIndex(List<CharSequence> options, CharSequence value) {
        for (int index = 0; index < options.size(); index++) {
            if (Concat.contentEquals(options.get(index), value)) return index;
        }
        return -1;
    }

    /** Original loadService: animated, clipped choice popup with per-row stagger. */
    private void renderSettingPopups(UiRenderer renderer) {
        if (openChoice == null) return;
        List<CharSequence> options = choiceOptions();
        UiRect bounds = choicePopupLayout().popup();
        float x = bounds.x();
        float y = bounds.y();
        float width = bounds.width();
        int selected = choiceIndex(options, openChoice.getValue());
        float height = bounds.height();
        float opacity = easedProgress(choiceOpenedAtNanos, 180);
        drawShadow(renderer, x, y + 4.0f, width, height, 6.0f, 16.0f,
                DashboardColors.multiplyAlpha(1711276032, opacity));
        painter(renderer).roundedRect(x, y, width, height, 6.0f, DashboardColors.multiplyAlpha(-15987700, opacity));
        renderer.pushScissor(renderX(x), renderY(y), renderSize(width), renderSize(height));
        for (int index = 0; index < options.size(); index++) {
            float rowY = y + 3.0f + index * 26 - choiceScroll;
            float shown = easedProgress(choiceOpenedAtNanos + index * 26_000_000L, 160);
            if (shown <= 0.001f) continue;
            boolean hovered = rawContainsPointer(x + 3.0f, rowY, width - 6.0f, 24.0f);
            float target = hovered ? 1.0f : 0.0f;
            float hover = animations.tween(settingId(openChoice).child(CHOICE_OPTION_ID_SEGMENT).child(index),
                    CARD_HOVER_PROPERTY, target, target, TransitionSpec.DURATION_140_MS);
            int fill = index == selected ? 268435455 : DashboardColors.multiplyAlpha(352321535, hover);
            float rowX = x + 3.0f;
            float rowWidth = width - 6.0f;
            float slide = 7.0f * (1.0f - shown);
            renderer.pushScissor(renderX(rowX), renderY(rowY), renderSize(rowWidth), renderSize(24.0f));
            if (fill >>> 24 != 0) painter(renderer).roundedRect(rowX + slide, rowY, rowWidth, 24.0f, 4.0f,
                    DashboardColors.multiplyAlpha(fill, shown));
            painter(renderer).text(index == selected ? FontAsset.INTER_SEMI_BOLD : FontAsset.INTER,
                    options.get(index), x + 10.0f + slide, rowY + 6.0f, 11.0f,
                    DashboardColors.multiplyAlpha(index == selected ? -1 : -2130706433, shown));
            renderer.popScissor();
        }
        renderer.popScissor();
    }

    private void renderSegmentedControl(UiRenderer renderer, String id, float x, float y, float width, float height,
                                        boolean firstSelected, String first, String second) {
        renderSegmentedControl(renderer, id, x, y, width, height, firstSelected, first, second, true);
    }

    private void renderSegmentedControl(UiRenderer renderer, String id, float x, float y, float width, float height,
                                        boolean firstSelected, String first, String second, boolean firstEnabled) {
        renderSegmentedControl(renderer, id, x, y, width, height, firstSelected, first, second, 74.0f, firstEnabled, true);
    }
    private void renderSegmentedControl(UiRenderer renderer, String id, float x, float y, float width, float height,
                                        boolean firstSelected, String first, String second, float firstWidth,
                                        boolean firstEnabled, boolean secondEnabled) {
        float inset = height >= 37.0f ? 4.0f : 3.0f;
        float gap = height >= 37.0f ? 4.0f : 2.0f;
        float secondWidth = width - inset * 2.0f - gap - firstWidth;
        painter(renderer).panel(x, y, width, height, 8.0f,
                height >= 37.0f ? 184549375 : 234881023, height >= 37.0f ? 1.0f : 0.0f, 268435455);
        float target = firstSelected ? 0.0f : 1.0f;
        float selection = animations.tween(UiId.ROOT.child(id), SEGMENT_SELECTION_PROPERTY,
                target, target, TransitionSpec.DURATION_180_MS);
        float selectedX = x + inset + (firstWidth + gap) * selection;
        float selectedWidth = firstWidth + (secondWidth - firstWidth) * selection;
        painter(renderer).roundedRect(selectedX, y + inset, selectedWidth, height - inset * 2.0f, 6.0f, 268435455);
        new DashboardWidgetRenderer(painter(renderer)).centeredText(FontAsset.INTER, first,
                x + inset + firstWidth * 0.5f, y + inset + 7.0f, 13.0f,
                firstEnabled ? DashboardColors.interpolate(-1, 1946157055, selection) : 872415231);
        new DashboardWidgetRenderer(painter(renderer)).centeredText(FontAsset.INTER, second,
                x + inset + firstWidth + gap + secondWidth * 0.5f, y + inset + 7.0f, 13.0f,
                secondEnabled ? DashboardColors.interpolate(1946157055, -1, selection) : 872415231);
    }

    private List<ScriptCard> filteredScripts() {
        String query = scriptSearch.toLowerCase(Locale.ROOT).trim();
        List<ScriptCard> result = new ArrayList<>();
        for (ScriptCard card : scriptCards) {
            if (!publicScripts && card.publicVisibility) continue;
            if (enabledScriptsOnly && !card.enabled) continue;
            if (!query.isEmpty() && !card.name.toLowerCase(Locale.ROOT).contains(query)
                    && !card.author.toLowerCase(Locale.ROOT).contains(query)) continue;
            result.add(card);
        }
        return result;
    }

    private void renderScriptCard(UiRenderer renderer, ScriptCard card, float x, float y, float width, float height) {
        renderCardBackground(renderer, SCRIPT_CARD_PREFIX + card.name, x, y, width, height, card.enabled);
        painter(renderer).roundedRect(x + 16.0f, y + 16.0f, 24.0f, 24.0f, 6.0f, 268435455);
        painter(renderer).icon(DashboardIcon.SCRIPT_CARD, x + 21.0f, y + 21.0f, 14.0f, 14.0f, 1.0f, -1);
        painter(renderer).text(FontAsset.INTER_SEMI_BOLD, card.name, x + 50.0f, y + 20.0f, 14.0f, -1);
        float badgeWidth = card.publicVisibility ? 66.0f : 70.0f;
        float badgeX = x + 477.0f - badgeWidth;
        painter(renderer).roundedRect(badgeX, y + 17.5f, badgeWidth, 21.0f, 6.0f, 234881023);
        painter(renderer).icon(card.publicVisibility ? DashboardIcon.PUBLIC : DashboardIcon.PRIVATE,
                badgeX + 8.0f, y + 22.0f, 12.0f, 12.0f, 1.0f, -1);
        painter(renderer).text(FontAsset.INTER, card.publicVisibility ? SCRIPT_PUBLIC_LABEL : SCRIPT_PRIVATE_LABEL,
                badgeX + 25.0f, y + 21.5f, 11.0f, -1929379841);
        float textX = x + 16.0f;
        String author = BY_AUTHOR_PREFIX + card.author;
        painter(renderer).text(FontAsset.INTER, author, textX, y + 54.0f, 12.0f, -2130706433);
        textX += painter(renderer).textWidth(FontAsset.INTER, author, 12.0f) + 8.0f;
        painter(renderer).roundedRect(textX, y + 59.5f, 3.0f, 3.0f, 1.5f, -2130706433);
        textX += 11.0f;
        painter(renderer).text(FontAsset.INTER, card.typeLabel, textX, y + 54.0f, 12.0f, -2130706433);
        textX += painter(renderer).textWidth(FontAsset.INTER, card.typeLabel, 12.0f) + 8.0f;
        painter(renderer).roundedRect(textX, y + 59.5f, 3.0f, 3.0f, 1.5f, -2130706433);
        textX += 11.0f;
        painter(renderer).text(FontAsset.INTER, card.statusLabel, textX, y + 54.0f, 12.0f, -2130706433);
        drawRule(renderer, x + 16.0f, y + 82.0f, 461.0f, 1.0f, -14671840);
        renderToggle(renderer, SCRIPT_ID_PREFIX + card.name + SCRIPT_TOGGLE_SUFFIX,
                x + 16.0f, y + 102.5f, 34.0f, 20.0f, 16.0f, card.enabled);
        painter(renderer).text(FontAsset.INTER, card.enabled ? SCRIPT_ENABLED_LABEL : SCRIPT_DISABLED_LABEL,
                x + 60.0f, y + 105.0f, 13.0f, card.enabled ? -419430401 : 1946157055);
        painter(renderer).panel(x + 425.0f, y + 97.0f, 52.0f, 31.0f, 8.0f, 184549375, 1.0f, -14408668);
        new DashboardWidgetRenderer(painter(renderer)).centeredText(FontAsset.INTER, EDIT_SCRIPT_LABEL,
                x + 451.0f, y + 105.0f, 13.0f, -1711276033);
    }

    private void renderNewCard(UiRenderer renderer, float x, float y, float width, float height, String label) {
        painter(renderer).roundedRect(x, y, width, height, 10.0f, 100663295);
        for (float offset = 10.0f; offset < width - 10.0f; offset += 16.0f) {
            float dash = Math.min(9.0f, width - 10.0f - offset);
            drawRule(renderer, x + offset, y, dash, 1.0f, 536870911);
            drawRule(renderer, x + offset, y + height - 1.0f, dash, 1.0f, 536870911);
        }
        for (float offset = 10.0f; offset < height - 10.0f; offset += 16.0f) {
            float dash = Math.min(9.0f, height - 10.0f - offset);
            drawRule(renderer, x, y + offset, 1.0f, dash, 536870911);
            drawRule(renderer, x + width - 1.0f, y + offset, 1.0f, dash, 536870911);
        }
        painter(renderer).icon(DashboardIcon.PLUS, x + 234.5f, y + 46.0f, 24.0f, 24.0f, 1.0f, -1);
        new DashboardWidgetRenderer(painter(renderer)).centeredText(FontAsset.INTER, label,
                x + 246.5f, y + 82.0f, 14.0f, -1929379841);
    }

    private List<String> filteredFriends() {
        String query = friendSearch.toLowerCase(Locale.ROOT).trim();
        List<String> result = new ArrayList<>();
        if (Echo.friendManager == null) return result;
        for (String name : Echo.friendManager.names()) {
            if (query.isEmpty() || name.toLowerCase(Locale.ROOT).contains(query)) result.add(name);
        }
        return result;
    }

    private void renderFriendCard(UiRenderer renderer, String name, float x, float y, float width, float height) {
        renderCardBackground(renderer, FRIEND_CARD_PREFIX + name, x, y, width, height, false);
        painter(renderer).roundedRect(x + 16.0f, y + 16.0f, 48.0f, 48.0f, 24.0f, 268435455);
        new DashboardWidgetRenderer(painter(renderer)).centeredText(FontAsset.INTER_SEMI_BOLD,
                name.substring(0, 1).toUpperCase(Locale.ROOT), x + 40.0f, y + 30.0f, 18.0f, -637534209);
        painter(renderer).text(FontAsset.INTER_SEMI_BOLD, name, x + 78.0f, y + 22.0f, 15.0f, -1);
        painter(renderer).text(FontAsset.INTER, FRIEND_LABEL, x + 78.0f, y + 45.0f, 12.0f, -2130706433);
        painter(renderer).panel(x + width - 86.0f, y + 24.0f, 70.0f, 31.0f, 6.0f, 184549375, 1.0f, 268435455);
        new DashboardWidgetRenderer(painter(renderer)).centeredText(FontAsset.INTER, REMOVE_LABEL,
                x + width - 51.0f, y + 32.0f, 12.0f, -1711276033);
    }

    private void renderCardBackground(UiRenderer renderer, String id, float x, float y,
                                       float width, float height, boolean selected) {
        float target = containsPointer(x, y, width, height) ? 1.0f : 0.0f;
        float hover = animations.tween(UiId.ROOT.child(id), CARD_HOVER_PROPERTY,
                target, target, TransitionSpec.DURATION_140_MS);
        painter(renderer).panel(x, y, width, height, 10.0f, 150994943, 1.0f,
                DashboardColors.interpolate(selected ? 536870911 : 268435455, 872415231, hover));
    }

    private void renderPageHeader(UiRenderer renderer, String title, String description,
                                  String placeholder, boolean upload) {
        painter(renderer).text(FontAsset.INTER_SEMI_BOLD, title, 354.0f, 54.0f, 20.0f, -1);
        painter(renderer).text(FontAsset.INTER, description, 354.0f, 81.0f, 13.0f, 1728053247);
        boolean configs = title.equals(CONFIGS_TITLE);
        boolean friends = title.equals(FRIENDS_NAV_LABEL);
        String query = friends ? friendSearch : configs ? configSearch : scriptSearch;
        TextInputFocus destination = friends ? TextInputFocus.FRIEND_SEARCH
                : configs ? TextInputFocus.CONFIG_SEARCH : TextInputFocus.SCRIPT_SEARCH;
        boolean focused = textFocus == destination;
        float x = upload ? 1407.0f : 1606.0f;
        UiId identity = UiId.ROOT.child(friends ? FRIEND_SEARCH_ID : configs ? CONFIG_SEARCH_ID : SCRIPT_SEARCH_ID);
        float target = focused ? 1.0f : 0.0f;
        float focus = animations.tween(identity, FOCUS_PROPERTY, target, target, TransitionSpec.DURATION_140_MS);
        painter(renderer).panel(x, 58.0f, 260.0f, 34.0f, 6.0f, 234881023, 1.0f,
                DashboardColors.interpolate(-14408668, 1509949439, focus));
        drawGlyph(renderer, 57681, x + 12.0f, 67.0f, 16.0f, 1308622847);
        painter(renderer).text(FontAsset.INTER, query.isEmpty() ? placeholder : query,
                x + 36.0f, 67.5f, 13.0f,
                query.isEmpty() ? DashboardColors.interpolate(1308622847, 872415231, focus) : -1);
        if (focused && caretVisible()) {
            float caretX = x + 36.0f + painter(renderer).textWidth(FontAsset.INTER, query, 13.0f);
            drawRule(renderer, caretX, 66.0f, 1.5f, 17.0f, -1);
        }
        if (!configs && !friends) {
            painter(renderer).panel(x + 270.0f, 58.0f, 83.0f, 34.0f, 6.0f, 234881023, 1.0f, -14408668);
            painter(renderer).icon(DashboardIcon.FILTER, x + 284.0f, 67.0f, 16.0f, 16.0f, 1.0f, -1);
            painter(renderer).text(FontAsset.INTER, enabledScriptsOnly ? RESET_FILTER_LABEL : FILTER_LABEL,
                    x + 308.0f, 67.5f, 13.0f, -1275068417);
        }
        if (upload) {
            painter(renderer).panel(x + 363.0f, 58.0f, 96.0f, 34.0f, 6.0f, 234881023, 1.0f, -14408668);
            drawGlyph(renderer, 57758, x + 377.0f, 67.0f, 16.0f, -1275068417);
            painter(renderer).text(FontAsset.INTER, UPLOAD_LABEL, x + 401.0f, 67.5f, 13.0f, -1275068417);
        }
    }

    private void renderOverlayCard(UiRenderer renderer, Feature feature, UiRect bounds, boolean selected) {
        boolean hovered = containsPointer(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        String identity = OVERLAY_ID_PREFIX + FeatureNameAssignments.id(feature);
        float state = hovered ? 1.0f : 0.0f;
        float hover = animations.tween(UiId.ROOT.child(identity), HOVER_PROPERTY,
                state, state, TransitionSpec.DURATION_140_MS);
        int border = selected ? 1728053247 : DashboardColors.interpolate(318767103, 788529151, hover);
        painter(renderer).panel(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 8.0f,
                DashboardColors.multiplyAlpha(352321535, 0.35f + hover * 0.65f), 1.0f, border);
        painter(renderer).text(FontAsset.INTER_SEMI_BOLD, feature.getName(), bounds.x() + 14.0f,
                bounds.y() + 13.0f, 13.0f, -1);
        painter(renderer).text(FontAsset.INTER, feature.getDescription(), bounds.x() + 14.0f,
                bounds.y() + 35.0f, 11.5f, -2130706433);
        UiRect toggle = new UiRect(bounds.x() + bounds.width() - 44.0f, bounds.y() + 22.0f, 30.0f, 19.0f);
        renderToggle(renderer, identity + TOGGLE_ID_SUFFIX, toggle.x(), toggle.y(),
                toggle.width(), toggle.height(), 9.0f, feature.isEnabled());
    }

    private void renderModuleSearch(UiRenderer renderer) {
        boolean focused = textFocus == TextInputFocus.MODULE_SEARCH;
        float state = focused ? 1.0f : 0.0f;
        float focus = animations.tween(UiId.ROOT.child(MODULE_SEARCH_FIELD_ID), FOCUS_PROPERTY,
                state, state, TransitionSpec.DURATION_140_MS);
        painter(renderer).panel(342.0f, 40.0f, 1536.0f, 32.0f, 6.0f, 352321535, 1.0f,
                DashboardColors.interpolate(-13619152, 1509949439, focus));
        drawRule(renderer, 343.0f, 41.0f, 1534.0f, 1.0f, 1073741824);
        drawGlyph(renderer, 57681, 354.0f, 48.0f, 16.0f, 1308622847);
        boolean empty = moduleSearch.isEmpty();
        painter(renderer).text(FontAsset.INTER, empty ? MODULE_SEARCH_PLACEHOLDER : moduleSearch,
                378.0f, 48.5f, 13.0f, empty ? 1308622847 : -1);
        if (focused && caretVisible()) {
            float x = 378.0f + painter(renderer).textWidth(FontAsset.INTER, moduleSearch, 13.0f);
            drawRule(renderer, x, 47.0f, 1.5f, 17.0f, -1);
        }
        if (!empty) {
            int results = 0;
            for (Category category : FeatureNameAssignments.categories()) results += filteredFeatures(category).size();
            CharSequence label = results + (results == 1 ? SINGLE_RESULT : MULTIPLE_RESULTS);
            new DashboardWidgetRenderer(painter(renderer)).rightAlignedText(FontAsset.INTER, label,
                    1866.0f, 48.5f, 13.0f, -2130706433);
        }
    }

    private static boolean caretVisible() { return (System.currentTimeMillis() / 500L & 1L) == 0L; }
    private void renderToggle(UiRenderer renderer, String identity, float x, float y, float width,
                              float height, float thumbSize, boolean enabled) {
        renderToggle(renderer, UiId.ROOT.child(identity), x, y, width, height, thumbSize, enabled);
    }
    private void renderToggle(UiRenderer renderer, UiId identity, float x, float y, float width,
                              float height, float thumbSize, boolean enabled) {
        float target = enabled ? 1.0f : 0.0f;
        float position = animations.spring(identity, TOGGLE_POSITION_PROPERTY, target, target, SpringSpec.CRITICAL_4_5_HZ);
        float velocity = Math.abs(animations.velocity(identity, TOGGLE_VELOCITY_PROPERTY));
        float stretch = thumbSize <= 9.01f ? Math.min(4.0f, velocity * 0.32f) : 0.0f;
        float thumbWidth = thumbSize + stretch;
        float padding = (height - thumbSize) * 0.5f;
        float left = x + padding + thumbSize * 0.5f;
        float right = x + width - padding - thumbSize * 0.5f;
        float center = left + (right - left) * position;
        painter(renderer).roundedRect(x, y, width, height, height * 0.5f,
                DashboardColors.interpolate(536870911, 1308622847, position));
        painter(renderer).roundedRect(center - thumbWidth * 0.5f, y + (height - thumbSize) * 0.5f,
                thumbWidth, thumbSize, thumbSize * 0.5f,
                DashboardColors.interpolate(-2130706433, -4144960, position));
    }

    private void drawGlyph(UiRenderer renderer, int glyph, float x, float y, float size, int color) {
        renderer.drawText(FontAsset.LUCIDE, Character.toString(glyph), renderX(x), renderY(y), renderSize(size),
                DashboardColors.multiplyAlpha(color, Math.max(0.0f, Math.min(1.0f, openingOpacity * layerOpacity))));
    }

    private boolean isModuleSearchActive() { return !moduleSearch.trim().isEmpty(); }
    private List<Feature> filteredFeatures(Category category) {
        String query = moduleSearch.trim();
        List<Feature> result = new ArrayList<>();
        for (Feature feature : FeatureNameAssignments.features(category, true)) {
            if (Concat.containsLowercase(feature.getName(), query)
                    || Concat.containsLowercase(feature.getDescription(), query)) result.add(feature);
        }
        return result;
    }

    private static DashboardIcon categoryIcon(Category category) {
        return switch (category) {
            case COMBAT -> DashboardIcon.COMBAT;
            case RENDER -> DashboardIcon.VISUALS;
            case MOVEMENT -> DashboardIcon.LEGIT;
            case WORLD -> DashboardIcon.PLANET;
            case UTILITY -> DashboardIcon.UTILITY;
            case INTERNALS -> DashboardIcon.MANAGER;
        };
    }
    private CharSequence moduleHeaderName() { return selectedFeature == null ? MODULES_LABEL : selectedFeature.getName(); }
    private CharSequence moduleHeaderDescription() {
        if (selectedFeature == null) return "";
        CharSequence hovered = hoveredSettingDescription();
        return hovered.isEmpty() ? selectedFeature.getDescription() : hovered;
    }
    private CharSequence hoveredSettingDescription() {
        for (SettingRow row : settingRows) {
            if (containsPointer(row.x(), row.y(), 476.0f, row.height())) return row.setting().getDescription();
        }
        return "";
    }
    private boolean containsPointer(float x, float y, float width, float height) {
        if (modalOpen || openChoice != null || contextMenuCardId != null || closing
                || !(easedProgress(pageChangedAtNanos, 220) >= 0.98f)) return false;
        return mouseX >= x && mouseY >= y && mouseX <= x + width && mouseY <= y + height;
    }
    private void drawRule(UiRenderer renderer, float x, float y, float width, float height, int color) {
        renderer.drawRoundedRect(renderX(x), renderY(y), renderSize(width), Math.max(0.5f, renderSize(height)),
                0.0f, DashboardColors.multiplyAlpha(color, Math.max(0.0f, Math.min(1.0f, openingOpacity * layerOpacity))));
    }
    private float selectedSidebarIndicatorY() {
        for (FeatureRow row : sidebarRows) if (row.feature() == selectedFeature) return row.y() + 13.0f;
        return sidebarIndicatorY;
    }
    private float progress(long startedAt, int durationMillis) {
        if (startedAt == 0L || durationMillis <= 0) return 1.0f;
        float multiplier = animations.durationScale();
        if (multiplier <= 0.0f) return 1.0f;
        return Math.max(0.0f, Math.min(1.0f,
                (float) (System.nanoTime() - startedAt) / (durationMillis * multiplier * 1_000_000.0f)));
    }
    private float easedProgress(long startedAt, int durationMillis) {
        return AnimationRegistry.ease(progress(startedAt, durationMillis), TransitionSpec.easeOut(durationMillis / 1000.0f));
    }

    private float renderX(float x) {
        float center = viewportLeft + 1920.0f * viewportScale * 0.5f;
        float animated = pivotX + (x - pivotX) * animationScale;
        return center + (animated - 960.0f + localOffsetX) * viewportScale * zoom;
    }
    private float renderY(float y) {
        float center = viewportTop + 1080.0f * viewportScale * 0.5f;
        float animated = pivotY + (y - pivotY) * animationScale + localOffsetY;
        return center + (animated - 540.0f) * viewportScale * zoom + verticalOffset * viewportScale;
    }
    private float renderSize(float size) { return size * viewportScale * zoom * animationScale; }
    private float inputX(float x) {
        float center = viewportLeft + 1920.0f * viewportScale * 0.5f;
        return 960.0f + (x - center) / Math.max(viewportScale * zoom, 1.0e-4f);
    }
    private float inputY(float y) {
        float center = viewportTop + 1080.0f * viewportScale * 0.5f + verticalOffset * viewportScale;
        return 540.0f + (y - center) / Math.max(viewportScale * zoom, 1.0e-4f);
    }
    private boolean capturesOverlayInput() { return closing || progress(openedAtNanos, 220) < 1.0f; }

    private void processImmediateInput() {
        mouseX = ui.mouseX();
        mouseY = ui.mouseY();
        boolean escape = ui.input().keyEvents().stream().anyMatch(event -> event.keyCode() == InputKey.ESCAPE.code());
        if (escape) {
            processQueuedKeyboard();
            return;
        }
        if (capturingBind) {
            for (int button = 0; button < UiInputState.INPUT_COUNT; button++) {
                if (ui.input().wasPressed(button)) {
                    capturingBind = !assignCapturedBinding(InputKey.fromMouseCode(button));
                    return;
                }
            }
        }
        if (modalOpen) {
            handleModalInput();
            processQueuedKeyboard();
            return;
        }
        if (openChoice != null) {
            processChoicePopupInput();
            return;
        }
        if (handleContextMenuInput()) {
            processQueuedKeyboard();
            return;
        }
        boolean sidebarHovered = mouseY >= 140.0f && mouseY < 140.0f + DashboardLayout.availableContentHeight()
                && mouseX >= 30.0f && mouseX <= 300.0f;
        if (sidebarHovered && Math.abs(ui.input().verticalScroll()) > 0.0) {
            float maximum = Math.max(0.0f, sidebarContentHeight - DashboardLayout.availableContentHeight() + 40.0f);
            sidebarScroll = Math.max(0.0f, Math.min(maximum, sidebarScroll - (float) ui.input().verticalScroll() * 40.0f));
        }
        if (sidebarHovered) {
            for (FeatureRow row : sidebarRows) {
                if (row.feature() == null) {
                    UiId id = UiId.ROOT.child(SIDEBAR_CATEGORY_INPUT_ID).child(row.category().name);
                    if (ui.interact(id, new UiRect(42.0f, row.y(), 246.0f, 40.0f)).clicked()) {
                        expandedCategory = expandedCategory == row.category() ? null : row.category();
                        changePage(DashboardPage.MODULE);
                    }
                } else if (!(row.visibility() <= 0.5f)) {
                    String id = SIDEBAR_MODULE_INPUT_PREFIX + FeatureNameAssignments.id(row.feature());
                    if (ui.interact(id, new UiRect(51.0f, row.y(), 237.0f, 32.0f)).clicked()) {
                        expandedCategory = row.category();
                        selectFeature(row.feature());
                        setModuleSearch("");
                        processQueuedKeyboard();
                        return;
                    }
                }
            }
        }
        if (ui.interact(SIDEBAR_CONFIGS_INPUT_ID, new UiRect(42.0f, sidebarFooterY, 246.0f, 40.0f)).clicked()) {
            copiesSourceId = null;
            if (Echo.featureConfig != null) Echo.featureConfig.saveAsync(AUTOSAVE_CONFIG_ID);
            requestRefresh();
            changePage(DashboardPage.CONFIGS);
        }
        if (ui.interact(SIDEBAR_FRIENDS_INPUT_ID, new UiRect(42.0f, sidebarFooterY + 52.0f, 246.0f, 40.0f)).clicked()) {
            changePage(DashboardPage.FRIENDS);
        }
        if (ui.interact(SIDEBAR_OVERLAYS_INPUT_ID, new UiRect(42.0f, sidebarFooterY + 104.0f, 246.0f, 40.0f)).clicked()) {
            changePage(DashboardPage.OVERLAYS);
        }
        if (ui.interact(SIDEBAR_HUD_EDITOR_INPUT_ID, new UiRect(42.0f, sidebarFooterY + 156.0f, 246.0f, 40.0f)).clicked()) {
            MinecraftRenderState.setScreen(new HudEditorScreen());
            return;
        }
        if (easedProgress(pageChangedAtNanos, 220) >= 0.98f) {
            ui.pushClip(new UiRect(330.0f, 30.0f, 1560.0f, 1020.0f));
            switch (page) {
                case MODULE -> handleModuleInput();
                case OVERLAYS -> handleOverlayInput();
                case CONFIGS -> handleConfigsInput();
                case FRIENDS -> handleFriendsInput();
                case SCRIPTS -> handleScriptsInput();
                case EDITOR -> handleEditorInput();
            }
            ui.popClip();
        }
        processQueuedKeyboard();
    }

    private void processChoicePopupInput() {
        List<CharSequence> options = choiceOptions();
        UiRect bounds = choicePopupLayout().popup();
        int last = Math.max(0, options.size() - 1);
        float lastOpacity = easedProgress(choiceOpenedAtNanos + last * 26_000_000L, 160);
        if (bounds.contains(mouseX, mouseY)) {
            if (lastOpacity > 0.75f && Math.abs(ui.input().verticalScroll()) > 0.0) {
                float maximum = Math.max(0.0f, options.size() * 26 + 6 - bounds.height());
                choiceScroll = Math.max(0.0f, Math.min(maximum, choiceScroll - (float) ui.input().verticalScroll() * 22.0f));
            }
            ui.pushClip(bounds);
            for (int index = 0; index < options.size(); index++) {
                UiRect row = new UiRect(bounds.x() + 3.0f, bounds.y() + 3.0f + index * 26 - choiceScroll,
                        bounds.width() - 6.0f, 24.0f);
                float opacity = easedProgress(choiceOpenedAtNanos + index * 26_000_000L, 160);
                if (opacity > 0.75f && ui.interact(settingId(openChoice).child(CHOICE_OPTION_ID_SEGMENT).child(index), row).clicked()) {
                    openChoice.setValue(options.get(index));
                    openChoice = null;
                    ui.popClip();
                    processQueuedKeyboard();
                    return;
                }
            }
            ui.popClip();
            processQueuedKeyboard();
        } else {
            if (ui.input().wasPressed(InputKey.MOUSE_LEFT.code())) openChoice = null;
            processQueuedKeyboard();
        }
    }

    private boolean assignCapturedBinding(InputKey key) {
        if (bindingSetting != null) {
            bindingSetting.setKey(key);
            return true;
        }
        if (bindingFeature == null) return true;
        return FeatureNameAssignments.assignBinding(bindingFeature, key) != NameAssignmentResult.RESERVED;
    }

    private void selectFeature(Feature feature) {
        selectedFeature = feature;
        openColorPicker = null;
        openRegistryPicker = null;
        changePage(DashboardPage.MODULE);
    }

    private void setModuleSearch(String value) {
        moduleSearch = value;
        sidebarScroll = 0.0f;
        if (value.isEmpty()) textFocus = TextInputFocus.NONE;
    }

    private void changePage(DashboardPage next) {
        int moduleIndex = selectedFeatureIndex();
        int index = switch (next) {
            case MODULE -> moduleIndex;
            case OVERLAYS -> 20004;
            case CONFIGS -> 20000;
            case FRIENDS -> 20003;
            case SCRIPTS -> 20001;
            case EDITOR -> 20002;
        };
        if (next == page && index == keyboardNavigationIndex) return;
        pageTransitionDirection = Integer.compare(index, keyboardNavigationIndex);
        if (pageTransitionDirection == 0) pageTransitionDirection = 1;
        keyboardNavigationIndex = index;
        page = next;
        pageChangedAtNanos = System.nanoTime();
        if (next != DashboardPage.MODULE) moduleSearch = "";
        if (next == DashboardPage.EDITOR) editorPulseStartedNanos = pageChangedAtNanos;
        textFocus = TextInputFocus.NONE;
        openChoice = null;
        capturingBind = false;
    }

    private void processQueuedKeyboard() {
        suppressCharacters = false;
        for (KeyStroke event : ui.input().keyEvents()) processKeyStroke(event.keyCode(), event.scanCode(), event.modifiers());
        if (!suppressCharacters) {
            for (int character : ui.input().characters()) processCharacter(character);
        }
    }

    private boolean processCharacter(int character) {
        if (character < 32 || character == 127) return false;
        if (textFocus == TextInputFocus.NONE && page == DashboardPage.MODULE && !modalOpen && openChoice == null && !capturingBind) {
            textFocus = TextInputFocus.MODULE_SEARCH;
        }
        if (textFocus == TextInputFocus.NONE) return false;
        if (textFocus == TextInputFocus.EDITOR) {
            editor.insert(Character.toString(character));
            return true;
        }
        if (textFocus == TextInputFocus.SETTING_NUMBER) {
            boolean accepted = character >= '0' && character <= '9'
                    || character == '.' && numericText.indexOf('.') < 0
                    && (numericSetting instanceof FloatSetting || numericSetting instanceof FloatRangeSetting || numericSetting instanceof NamedRangeGroupSetting)
                    || character == '-' && numericText.isEmpty();
            if (accepted && numericText.length() < 12) numericText += (char) character;
            return true;
        }
        String text = focusedText();
        int limit = textFocus == TextInputFocus.SETTING_TEXT && textSetting != null ? textSetting.getMaximumLength()
                : textFocus == TextInputFocus.MODAL_DESCRIPTION ? 72 : textFocus == TextInputFocus.MODAL_NAME ? 28 : 42;
        if (text.codePointCount(0, text.length()) >= limit) return true;
        setFocusedText(text + Character.toString(character));
        return true;
    }

    private boolean processKeyStroke(int key, int scanCode, int modifiers) {
        if (capturingBind) {
            if (key == InputKey.ESCAPE.code() || key == InputKey.DELETE.code() || key == InputKey.BACKSPACE.code()) {
                assignCapturedBinding(InputKey.NONE);
                capturingBind = false;
            } else capturingBind = !assignCapturedBinding(InputKey.fromCode(key));
            suppressCharacters = true;
            return true;
        }
        if (key == InputKey.ESCAPE.code()) {
            if (modalOpen) { closeConfigModal(); return true; }
            if (openChoice != null) { openChoice = null; return true; }
            if (contextMenuCardId != null) { closeContextMenu(); return true; }
            if (textFocus != TextInputFocus.NONE) {
                if (textFocus == TextInputFocus.MODULE_SEARCH) moduleSearch = "";
                textFocus = TextInputFocus.NONE;
                return true;
            }
            if (page == DashboardPage.EDITOR) changePage(DashboardPage.SCRIPTS);
            else beginClosing();
            return true;
        }
        if (textFocus == TextInputFocus.EDITOR) return handleEditorKey(key, modifiers);
        if (textFocus == TextInputFocus.NONE) return false;
        String text = focusedText();
        if (key == InputKey.BACKSPACE.code() && !text.isEmpty()) {
            boolean word = (modifiers & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SUPER)) != 0;
            int end = word ? previousInputWord(text) : text.offsetByCodePoints(text.length(), -1);
            setFocusedText(text.substring(0, end));
            return true;
        }
        if (key != InputKey.ENTER.code() && key != InputKey.KEYPAD_ENTER.code()) return false;
        if (modalOpen) commitConfigModal();
        else if (textFocus == TextInputFocus.FRIEND_NAME) addFriend();
        else if (textFocus == TextInputFocus.SETTING_NUMBER) commitNumericInput();
        else if (textFocus == TextInputFocus.MODULE_SEARCH) {
            Feature feature = firstSearchResult();
            if (feature != null) {
                expandedCategory = feature.getCategory();
                selectFeature(feature);
            }
            setModuleSearch("");
        } else textFocus = TextInputFocus.NONE;
        return true;
    }

    private static int previousInputWord(String text) {
        int index = text.length();
        while (index > 0 && Character.isWhitespace(text.charAt(index - 1))) index--;
        while (index > 0 && !Character.isWhitespace(text.charAt(index - 1))) index--;
        return index;
    }

    private String focusedText() {
        return switch (textFocus) {
            case SETTING_TEXT -> settingText;
            case SETTING_NUMBER -> numericText;
            case CONFIG_SEARCH -> configSearch;
            case SCRIPT_SEARCH -> scriptSearch;
            case FRIEND_SEARCH -> friendSearch;
            case FRIEND_NAME -> friendName;
            case PICKER_SEARCH -> registrySearch;
            case MULTI_SEARCH -> searchedMultiChoice == null ? "" : multiChoiceSearches.getOrDefault(settingId(searchedMultiChoice), "");
            case MODULE_SEARCH -> moduleSearch;
            case MODAL_NAME -> modalName;
            case MODAL_DESCRIPTION -> modalDescription;
            default -> "";
        };
    }

    private void setFocusedText(String text) {
        switch (textFocus) {
            case SETTING_TEXT -> {
                settingText = text;
                if (textSetting != null) textSetting.setText(text);
            }
            case SETTING_NUMBER -> numericText = text;
            case CONFIG_SEARCH -> configSearch = text;
            case SCRIPT_SEARCH -> scriptSearch = text;
            case FRIEND_SEARCH -> friendSearch = text;
            case FRIEND_NAME -> friendName = text;
            case PICKER_SEARCH -> { registrySearch = text; registryScroll = 0.0f; }
            case MULTI_SEARCH -> {
                if (searchedMultiChoice != null) {
                    multiChoiceSearches.put(settingId(searchedMultiChoice), text);
                    multiChoiceScroll.remove(settingId(searchedMultiChoice));
                }
            }
            case MODULE_SEARCH -> setModuleSearch(text);
            case MODAL_NAME -> modalName = text;
            case MODAL_DESCRIPTION -> modalDescription = text;
        }
    }

    private Feature firstSearchResult() {
        for (Category category : FeatureNameAssignments.categories()) {
            List<Feature> features = filteredFeatures(category);
            if (!features.isEmpty()) return features.getFirst();
        }
        return null;
    }

    private void beginClosing() {
        if (closing) return;
        closing = true;
        closingAtNanos = System.nanoTime();
        ui.setEnabled(false);
        capturingBind = false;
        selectingEditorText = false;
    }

    private void commitNumericInput() {
        Setting setting = numericSetting;
        NamedRange range = numericNamedRange;
        String text = numericText.trim();
        boolean upper = editingUpperRange;
        textFocus = TextInputFocus.NONE;
        numericSetting = null;
        numericNamedRange = null;
        numericText = "";
        if (setting == null || text.isEmpty()) return;
        try {
            float value = Float.parseFloat(text);
            if (setting instanceof FloatSetting number) number.setValue(value);
            else if (setting instanceof IntSetting number) number.setValue(Math.round(value));
            else if (setting instanceof FloatRangeSetting number) {
                float step = number.getStep();
                float snapped = step > 0.0f ? Math.round(value / step) * step : value;
                if (upper) number.setUpper(number.isOutOfRangeAllowed() ? snapped : Math.clamp(snapped, number.getLower(), number.getMaximum()));
                else number.setLower(number.isOutOfRangeAllowed() ? snapped : Math.clamp(snapped, number.getMinimum(), number.getUpper()));
            } else if (setting instanceof NamedRangeGroupSetting group) setNamedRangeEndpoint(group, range, value, upper);
        } catch (NumberFormatException ignored) {
            // The original dismisses the edit before parsing and ignores only this exception type.
        }
    }

    private void setNamedRangeEndpoint(NamedRangeGroupSetting setting, NamedRange range, float value, boolean upper) {
        RangeBounds bounds = setting.getBounds();
        if (bounds == null || range == null) return;
        float step = bounds.step();
        float snapped = step > 0.0f ? Math.round(value / step) * step : value;
        if (upper) setting.setRange(range, range.getLower(), Math.max(snapped, range.getLower()));
        else setting.setRange(range, Math.min(snapped, range.getUpper()), range.getUpper());
    }

    private boolean handleEditorKey(int key, int modifiers) {
        boolean control = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        InputKey input = InputKey.fromCode(key);
        if (control) {
            switch (input) {
                case S -> { saveEditorScript(); return true; }
                case R -> { compileEditorScript(); return true; }
                case A -> {
                    editor.setSelectionAnchor(0);
                    editor.setCaret(editor.text().length());
                    editor.revealCaret();
                    return true;
                }
                case C -> { editor.copySelection(); return true; }
                case X -> {
                    if (editor.hasSelection()) { editor.copySelection(); editor.deleteSelection(); }
                    return true;
                }
                case V -> {
                    String clipboard = MinecraftClient.getInstance().keyboard.getClipboard();
                    editor.insert(clipboard == null ? "" : clipboard.replace(CLIPBOARD_WINDOWS_NEWLINE, "\n").replace('\r', '\n'));
                    return true;
                }
            }
        }
        switch (input) {
            case BACKSPACE -> editor.deleteBackward(control);
            case DELETE -> editor.deleteForward(control);
            case LEFT -> editor.moveHorizontal(-1, shift, control);
            case RIGHT -> editor.moveHorizontal(1, shift, control);
            case HOME -> {
                editor.prepareSelection(shift);
                editor.setCaret(control ? 0 : editor.lineStart(editor.caret()));
                editor.revealCaret();
            }
            case END -> {
                editor.prepareSelection(shift);
                editor.setCaret(control ? editor.text().length() : editor.lineEnd(editor.caret()));
                editor.revealCaret();
            }
            case UP -> { editor.prepareSelection(shift); editor.moveVertical(-1); }
            case DOWN -> { editor.prepareSelection(shift); editor.moveVertical(1); }
            case ENTER, KEYPAD_ENTER -> editor.insert("\n");
            case TAB -> editor.insert(EDITOR_TAB_TEXT);
            default -> { return false; }
        }
        return true;
    }

    private void handleEditorInput() {
        UiRect code = new UiRect(386.0f, 96.0f, 1490.0f, 844.0f);
        if (textFocus == TextInputFocus.EDITOR && ui.input().wasPressed(InputKey.MOUSE_LEFT.code())
                && !code.contains(mouseX, mouseY)) textFocus = TextInputFocus.NONE;
        if (ui.interact(EDITOR_BACK_INPUT_ID, new UiRect(354.0f, 47.0f, 32.0f, 32.0f)).clicked()) {
            changePage(DashboardPage.SCRIPTS);
            return;
        }
        if (ui.interact(EDITOR_IMPORT_INPUT_ID, new UiRect(1610.0f, 46.0f, 93.0f, 34.0f)).clicked()) {
            editor.replaceText(DEFAULT_SCRIPT);
            editor.setCaret(editor.text().length());
            editor.clearSelection();
            editor.setSaved(false);
            editorPulseStartedNanos = System.nanoTime();
        }
        if (ui.interact(EDITOR_SAVE_INPUT_ID, new UiRect(1713.0f, 46.5f, 62.0f, 33.0f)).clicked()) saveEditorScript();
        if (ui.interact(EDITOR_COMPILE_INPUT_ID, new UiRect(1785.0f, 46.5f, 81.0f, 33.0f)).clicked()) compileEditorScript();
        var interaction = ui.interact(EDITOR_CODE_INPUT_ID, code);
        if (interaction.pressed()) {
            textFocus = TextInputFocus.EDITOR;
            editor.placeCaret(mouseX, mouseY);
            editor.setSelectionAnchor(editor.caret());
            selectingEditorText = true;
            editor.resetAutoScroll();
        }
        if (interaction.held() && selectingEditorText) {
            editor.autoScroll(mouseY, deltaSeconds);
            editor.placeCaret(mouseX, mouseY);
            editor.revealCaret();
        }
        if (interaction.released()) {
            editor.placeCaret(mouseX, mouseY);
            selectingEditorText = false;
            editor.resetAutoScroll();
            if (editor.selectionAnchor() == editor.caret()) editor.clearSelection();
        }
        if (Math.abs(ui.input().verticalScroll()) > 0.0 && new UiRect(330.0f, 96.0f, 1560.0f, 844.0f).contains(mouseX, mouseY)) {
            int count = editor.lines().length;
            int maximum = Math.max(0, count - DashboardLayout.visibleEditorLines());
            editor.setFirstVisibleLine(Math.max(0, Math.min(maximum,
                    editor.firstVisibleLine() - (int) Math.signum(ui.input().verticalScroll()) * 3)));
        }
        int count = editor.lines().length;
        if (count > DashboardLayout.visibleEditorLines()) {
            float thumbHeight = Math.max(40.0f, 828.0f * DashboardLayout.visibleEditorLines() / count);
            float range = count - DashboardLayout.visibleEditorLines();
            float y = 104.0f + (828.0f - thumbHeight) * editor.displayedFirstLine() / range;
            var scrollbar = ui.interact(EDITOR_SCROLLBAR_INPUT_ID, new UiRect(1878.0f, y - 4.0f, 13.0f, thumbHeight + 8.0f));
            if (!scrollbar.pressed() && !scrollbar.held()) return;
            float fraction = (mouseY - 104.0f - thumbHeight * 0.5f) / (828.0f - thumbHeight);
            editor.setFirstVisibleLine(Math.round(Math.max(0.0f, Math.min(1.0f, fraction)) * range));
        }
    }

    private void handleScriptsInput() {
        if (textFocus == TextInputFocus.SCRIPT_SEARCH && ui.input().wasPressed(InputKey.MOUSE_LEFT.code())
                && !new UiRect(1407.0f, 58.0f, 260.0f, 34.0f).contains(mouseX, mouseY)) textFocus = TextInputFocus.NONE;
        if (ui.interact(SCRIPT_PUBLIC_INPUT_ID, new UiRect(354.0f, 112.0f, 82.0f, 37.0f)).clicked()) publicScripts = true;
        if (ui.interact(SCRIPT_PRIVATE_INPUT_ID, new UiRect(436.0f, 112.0f, 82.0f, 37.0f)).clicked()) publicScripts = false;
        if (ui.interact(SCRIPT_SEARCH_INPUT_ID, new UiRect(1407.0f, 58.0f, 260.0f, 34.0f)).clicked()) textFocus = TextInputFocus.SCRIPT_SEARCH;
        if (ui.interact(SCRIPT_FILTER_INPUT_ID, new UiRect(1677.0f, 58.0f, 83.0f, 34.0f)).clicked()) enabledScriptsOnly = !enabledScriptsOnly;
        ui.interact(SCRIPT_UPLOAD_INPUT_ID, new UiRect(1770.0f, 58.0f, 96.0f, 34.0f));
        List<ScriptCard> cards = filteredScripts();
        int count = Math.min(cards.size(), 6);
        for (int index = 0; index < count; index++) {
            float x = 354 + index % 3 * 509;
            float y = 165 + index / 3 * 160;
            ScriptCard card = cards.get(index);
            if (ui.interact(SCRIPT_INPUT_PREFIX + card.name + TOGGLE_ID_SUFFIX, new UiRect(x + 16.0f, y + 97.0f, 110.0f, 31.0f)).clicked()) {
                Feature loaded = ScriptFiles.loadedScript(card.name);
                if (loaded == null) {
                    ScriptFiles.reloadWithDiagnostics(card.name);
                    loaded = ScriptFiles.loadedScript(card.name);
                }
                if (loaded != null) loaded.toggle();
                card.enabled = loaded != null && loaded.isEnabled();
            }
            if (ui.interact(SCRIPT_INPUT_PREFIX + card.name + SCRIPT_EDIT_SUFFIX, new UiRect(x + 425.0f, y + 97.0f, 52.0f, 31.0f)).clicked()) {
                openScript(scriptCards.indexOf(card));
            }
        }
        float x = 354 + count % 3 * 509;
        float y = 165 + count / 3 * 160;
        if (ui.interact(NEW_SCRIPT_INPUT_ID, new UiRect(x, y, 493.0f, 144.0f)).clicked()) openScript(-1);
    }

    private void openScript(int index) {
        selectedScriptIndex = index;
        editor.replaceText(index >= 0 ? ScriptFiles.readSource(scriptCards.get(index).name) : DEFAULT_SCRIPT);
        editor.setCaret(index >= 0 ? 0 : editor.text().length());
        editor.clearSelection();
        selectingEditorText = false;
        editor.resetAutoScroll();
        editor.setFirstVisibleLine(0);
        editor.setSaved(index >= 0);
        editorOutput.clear();
        if (index >= 0) {
            Feature loaded = ScriptFiles.loadedScript(scriptCards.get(index).name);
            editorOutput.add(loaded == null ? SCRIPT_NOT_LOADED_MESSAGE : SCRIPT_LOADED_PREFIX + loaded.getName());
        } else editorOutput.add(NEW_SCRIPT_MESSAGE);
        changePage(DashboardPage.EDITOR);
    }

    private void refreshScriptCards() {
        scriptCards.clear();
        for (String filename : ScriptFiles.listFilenames()) {
            Feature feature = ScriptFiles.loadedScript(filename);
            scriptCards.add(new ScriptCard(filename, LOCAL_SCRIPT_AUTHOR, LOCAL_SCRIPT_LANGUAGE,
                    ScriptFiles.describeFile(filename), publicScripts, feature != null && feature.isEnabled(), ScriptFiles.readSource(filename)));
        }
    }

    private String unusedScriptFilename() {
        List<String> filenames = ScriptFiles.listFilenames();
        int suffix = 1;
        while (true) {
            String filename = suffix == 1 ? UNTITLED_SCRIPT_FILENAME : UNTITLED_SCRIPT_PREFIX + suffix + SCRIPT_FILE_EXTENSION;
            if (!filenames.contains(filename)) return filename;
            suffix++;
        }
    }

    private int scriptIndex(String filename) {
        for (int index = 0; index < scriptCards.size(); index++) if (scriptCards.get(index).name.equals(filename)) return index;
        return -1;
    }

    private void saveEditorScript() {
        String filename = selectedScriptIndex < 0 ? unusedScriptFilename() : scriptCards.get(selectedScriptIndex).name;
        editorOutput.clear();
        if (!ScriptFiles.writeSource(filename, editor.text())) {
            editorOutput.add(SCRIPT_WRITE_FAILURE_PREFIX + filename);
        } else {
            refreshScriptCards();
            selectedScriptIndex = scriptIndex(filename);
            editor.setSaved(true);
            editorOutput.add(SCRIPT_SAVED_PREFIX + filename);
            editorPulseStartedNanos = System.nanoTime();
        }
    }

    private void compileEditorScript() {
        saveEditorScript();
        if (selectedScriptIndex >= 0) {
            String filename = scriptCards.get(selectedScriptIndex).name;
            List<String> diagnostics = ScriptFiles.reloadWithDiagnostics(filename);
            refreshScriptCards();
            selectedScriptIndex = scriptIndex(filename);
            editorOutput.addAll(diagnostics);
            editorPulseStartedNanos = System.nanoTime();
        }
    }

    private void processSettingsInput() {
        for (SettingRow row : settingRows) {
            Setting setting = row.setting();
            UiId id = settingId(setting);
            float x = row.x();
            float y = row.y();
            if (setting instanceof BooleanSetting toggle) {
                if (ui.interact(id, new UiRect(x + 446.0f, y + 6.0f, 30.0f, 19.0f)).clicked()) toggle.setValue(!toggle.getValue());
            } else if (setting instanceof FloatSetting number) {
                if (handleScalarNumberInput(setting, x, y)) continue;
                var drag = ui.interact(id, new UiRect(x, y + 23.0f, 476.0f, 28.0f));
                if (drag.pressed() || drag.held()) {
                    float fraction = DashboardLayout.sliderValue(mouseX, x, 476.0f, 16.0f);
                    number.setValue(sliderSettingValue(fraction, number.getMinimum(), number.getMaximum(), number.getStep()));
                }
            } else if (setting instanceof IntSetting number) {
                if (handleScalarNumberInput(setting, x, y)) continue;
                var drag = ui.interact(id, new UiRect(x, y + 23.0f, 476.0f, 28.0f));
                if (drag.pressed() || drag.held()) {
                    float fraction = DashboardLayout.sliderValue(mouseX, x, 476.0f, 16.0f);
                    number.setValue(Math.round(sliderSettingValue(fraction, number.getMinimum(), number.getMaximum(), 1.0f)));
                }
            } else if (setting instanceof FloatRangeSetting range) {
                if (handleRangeNumberInput(range, x, y)) continue;
                var drag = ui.interact(id, new UiRect(x, y + 23.0f, 476.0f, 28.0f));
                float fraction = DashboardLayout.sliderValue(mouseX, x, 476.0f, 16.0f);
                if (drag.pressed()) {
                    float lower = normalizedValue(range.getLower(), range.getMinimum(), range.getMaximum());
                    float upper = normalizedValue(range.getUpper(), range.getMinimum(), range.getMaximum());
                    draggingUpperRange = Math.abs(fraction - upper) <= Math.abs(fraction - lower);
                    draggedFloatRange = range;
                }
                if ((drag.pressed() || drag.held()) && draggedFloatRange == range) {
                    float value = sliderSettingValue(fraction, range.getMinimum(), range.getMaximum(), range.getStep());
                    if (draggingUpperRange) {
                        if (value < range.getLower()) {
                            range.setUpper(range.getLower());
                            range.setLower(value);
                            draggingUpperRange = false;
                        } else range.setUpper(value);
                    } else {
                        if (value > range.getUpper()) {
                            range.setLower(range.getUpper());
                            range.setUpper(value);
                            draggingUpperRange = true;
                        } else range.setLower(value);
                    }
                }
            } else if (setting instanceof ChoiceSetting choice) {
                float width = Math.max(42.0f, painter(Echo.uiRenderer).textWidth(FontAsset.INTER, choice.getValue(), 13.0f) + 33.0f);
                UiRect bounds = new UiRect(x + 476.0f - width, y + 2.0f, width, 27.0f);
                if (ui.interact(id, bounds).clicked()) toggleChoicePopup(choice, new UiRect(x + 476.0f - width, y + 2.0f, width, 27.0f));
            } else if (setting instanceof MultiChoiceSetting choices) {
                handleMultiChoiceInput(choices, id, x, y);
            } else if (setting instanceof ColorSetting color) {
                if (ui.interact(id, new UiRect(x + 412.0f, y, 64.0f, 31.0f)).clicked()) {
                    openColorPicker = openColorPicker == color ? null : color;
                    if (openColorPicker != null) openRegistryPicker = null;
                }
            } else if (setting instanceof RegistryListSetting<?> registry) {
                if (ui.interact(id, new UiRect(x + 412.0f, y, 64.0f, 31.0f)).clicked()) {
                    openRegistryPicker = openRegistryPicker == registry ? null : registry;
                    registrySearch = "";
                    registryScroll = 0.0f;
                    if (openRegistryPicker != null) {
                        openColorPicker = null;
                        if (textFocus == TextInputFocus.PICKER_SEARCH) textFocus = TextInputFocus.NONE;
                    }
                }
            } else if (setting instanceof KeybindSetting key) {
                float width = Math.max(36.0f, painter(Echo.uiRenderer).textWidth(FontAsset.INTER, settingKeyLabel(key), 13.0f) + 16.0f);
                if (ui.interact(id, new UiRect(x + 476.0f - width, y + 2.0f, width, 27.0f)).clicked()) {
                    bindingSetting = key;
                    bindingFeature = null;
                    capturingBind = true;
                    textFocus = TextInputFocus.NONE;
                }
            } else if (setting instanceof TextSetting text) {
                if (ui.interact(id, new UiRect(x, y + 30.0f, 476.0f, 32.0f)).clicked()) {
                    textFocus = TextInputFocus.SETTING_TEXT;
                    textSetting = text;
                    settingText = text.getText();
                }
            } else if (setting instanceof HotbarSlotsSetting slots) {
                for (int slot = 0; slot < 9; slot++) {
                    if (ui.interact(id.child(slot), new UiRect(x + slot * 53, y + 32.0f, 46.0f, 32.0f)).clicked()) slots.toggle(slot);
                }
            } else if (setting instanceof NamedRangeGroupSetting ranges) {
                handleNamedRangesInput(ranges, id, x, y);
            } else if (setting instanceof CustomWidgetSetting custom && custom.getColumnCount() > 1) {
                float height = custom.getHeight();
                if (ui.interact(id.child(PREVIEW_PREVIOUS_INPUT_ID), previewArrowBounds(x, y, height, false)).clicked()) {
                    custom.setLayoutIndex(custom.getLayoutIndex() - 1);
                }
                if (ui.interact(id.child(PREVIEW_NEXT_INPUT_ID), previewArrowBounds(x, y, height, true)).clicked()) {
                    custom.setLayoutIndex(custom.getLayoutIndex() + 1);
                }
            }
        }
        if (openColorPicker != null && unusedSettingColumn() >= 0) handleColorPickerInput();
        if (openRegistryPicker != null && unusedSettingColumn() >= 0) handleRegistryPickerInput();
    }

    private static float sliderSettingValue(float fraction, float minimum, float maximum, float step) {
        return UiLayoutMath.snapAndClamp(minimum + fraction * (maximum - minimum), minimum, maximum, step);
    }

    private boolean handleScalarNumberInput(Setting setting, float x, float y) {
        float width = Math.max(24.0f, painter(Echo.uiRenderer).textWidth(FontAsset.INTER, scalarValueText(setting), 12.0f));
        UiRect bounds = new UiRect(x + 476.0f - width - 6.0f, y + 5.0f, width + 12.0f, 17.0f);
        boolean clicked = ui.interact(settingId(setting).child(NUMERIC_VALUE_INPUT_ID), bounds).clicked();
        if (editingNumber(setting)) {
            if (ui.input().wasPressed(InputKey.MOUSE_LEFT.code()) && !bounds.contains(mouseX, mouseY)) commitNumericInput();
            return clicked;
        }
        if (clicked) {
            textFocus = TextInputFocus.SETTING_NUMBER;
            numericSetting = setting;
            numericText = "";
        }
        return clicked;
    }

    private UiRect rangeNumberBounds(FloatRangeSetting setting, float x, float y, boolean upper) {
        float[] positions = rangeTextGeometry(setting, x);
        float left = upper ? positions[2] : positions[0];
        float width = upper ? positions[5] : positions[4];
        return new UiRect(left - 4.0f, y + 5.0f, width + 8.0f, 17.0f);
    }

    private boolean handleRangeNumberInput(FloatRangeSetting setting, float x, float y) {
        UiRect lower = rangeNumberBounds(setting, x, y, false);
        UiRect upper = rangeNumberBounds(setting, x, y, true);
        UiId id = settingId(setting);
        boolean upperClicked = ui.interact(id.child(RANGE_MAX_INPUT_ID), upper).clicked();
        boolean lowerClicked = !upperClicked && ui.interact(id.child(RANGE_MIN_INPUT_ID), lower).clicked();
        boolean clicked = lowerClicked || upperClicked;
        boolean editing = editingNumber(setting);
        if (editing && ui.input().wasPressed(InputKey.MOUSE_LEFT.code())
                && !lower.contains(mouseX, mouseY) && !upper.contains(mouseX, mouseY)) {
            commitNumericInput();
            return clicked;
        }
        if (!clicked) return editing;
        if (editing && editingUpperRange == upperClicked) return true;
        if (editing) commitNumericInput();
        textFocus = TextInputFocus.SETTING_NUMBER;
        numericSetting = setting;
        editingUpperRange = upperClicked;
        numericText = "";
        return true;
    }

    private void toggleChoicePopup(ChoiceSetting setting, UiRect bounds) {
        if (openChoice == setting) { openChoice = null; return; }
        openChoice = setting;
        choiceAnchor = bounds;
        choiceOpenedAtNanos = System.nanoTime();
        choiceScroll = 0.0f;
        UiRenderer renderer = Echo.uiRenderer;
        List<CharSequence> options = choiceOptions();
        float width = 0.0f;
        for (CharSequence option : options) width = Math.max(width, painter(renderer).textWidth(FontAsset.INTER, option, 11.0f));
        minimumChoiceWidth = Math.max(38.0f, width + 20.0f);
    }

    private static UiRect clippedOptionBounds(float x, float y, float width, float top, float height) {
        float first = Math.max(y, top);
        float last = Math.min(y + 28.0f, top + height);
        return last - first < 4.0f ? null : new UiRect(x, first, width, last - first);
    }

    private void handleMultiChoiceInput(MultiChoiceSetting setting, UiId id, float x, float y) {
        if (setting.isSelectionMode() && ui.interact(id.child(MULTI_SEARCH_INPUT_ID), new UiRect(x, y + 30.0f, 476.0f, 34.0f)).clicked()) {
            textFocus = TextInputFocus.MULTI_SEARCH;
            searchedMultiChoice = setting;
        }
        List<Map.Entry<CharSequence, Boolean>> options = filteredMultiChoices(setting);
        float top = y + (setting.isSelectionMode() ? 76.0f : 33.0f);
        int count = setting.isSelectionMode() ? setting.getOptions().size() : filteredMultiChoices(setting).size();
        float height = Math.max(1, Math.min(6, count)) * 32.0f;
        float scroll = multiChoiceScroll.getOrDefault(id, 0.0f);
        boolean hovered = mouseX >= x && mouseX <= x + 476.0f && mouseY >= top && mouseY <= top + height;
        if (hovered && Math.abs(ui.input().verticalScroll()) > 0.0) {
            float maximum = Math.max(0.0f, options.size() * 32.0f - height);
            scroll = Math.max(0.0f, Math.min(maximum, scroll - (float) ui.input().verticalScroll() * 32.0f));
            multiChoiceScroll.put(id, scroll);
        }
        if (hovered) {
            for (int index = 0; index < options.size(); index++) {
                UiRect bounds = clippedOptionBounds(x, top + index * 32.0f - scroll, 476.0f, top, height);
                if (bounds == null) continue;
                CharSequence option = options.get(index).getKey();
                if (ui.interact(id.child(MULTI_OPTION_INPUT_ID).child(option), bounds).clicked()) setting.toggle(option);
            }
        }
    }

    private void handleColorPickerInput() {
        int column = unusedSettingColumn();
        float x = SETTING_COLUMN_X[column] + 12.0f;
        float top = settingColumnTop(column);
        List<ColorSetting> settings = visibleColorSettings();
        float squareY = DashboardLayout.optionGridBottom(top, settings.size());
        for (int index = 0; index < settings.size(); index++) {
            if (ui.interact(COLOR_TARGET_INPUT_PREFIX + index,
                    new UiRect(x + index % 2 * 244, top + 48.0f + index / 2 * 44, 232.0f, 32.0f)).clicked()) {
                openColorPicker = settings.get(index);
                return;
            }
        }
        var square = ui.interact(COLOR_SQUARE_INPUT_ID, new UiRect(x, squareY, 476.0f, 190.0f));
        if (square.pressed() || square.held()) {
            float saturation = Math.max(0.0f, Math.min(1.0f, (mouseX - x) / 476.0f));
            float brightness = 1.0f - Math.max(0.0f, Math.min(1.0f, (mouseY - squareY) / 190.0f));
            setColorSaturationBrightness(saturation, brightness);
        }
        float stripsY = squareY + 204.0f;
        var hue = ui.interact(COLOR_HUE_INPUT_ID, new UiRect(x + 31.0f, stripsY - 2.0f, 450.0f, 16.0f));
        if (hue.pressed() || hue.held()) setColorHue(DashboardLayout.sliderValue(mouseX, x + 36.0f, 440.0f, 9.0f));
        var alpha = ui.interact(COLOR_ALPHA_INPUT_ID, new UiRect(x + 31.0f, stripsY + 13.0f, 450.0f, 16.0f));
        if (alpha.pressed() || alpha.held()) {
            openColorPicker.setAlpha(Math.round(DashboardLayout.sliderValue(mouseX, x + 36.0f, 440.0f, 9.0f) * 255.0f));
        }
        for (int index = 0; index < COLOR_PRESETS.length; index++) {
            if (ui.interact(COLOR_PRESET_INPUT_PREFIX + index, new UiRect(x + index * 30 - 4.0f, squareY + 335.0f, 26.0f, 26.0f)).clicked()) {
                openColorPicker.setRgb(COLOR_PRESETS[index] & 0x00ffffff);
            }
        }
    }

    private void setColorSaturationBrightness(float saturation, float brightness) {
        int color = openColorPicker.getArgb();
        ColorSetting setting = openColorPicker;
        setting.setRgb(DashboardColorPicker.argb(DashboardColorPicker.hue(color), saturation, brightness, 1.0f) & 0x00ffffff);
    }

    private void setColorHue(float hue) {
        int color = openColorPicker.getArgb();
        ColorSetting setting = openColorPicker;
        setting.setRgb(DashboardColorPicker.argb(hue, DashboardColorPicker.saturation(color), DashboardColors.brightness(color), 1.0f) & 0x00ffffff);
    }

    private void handleRegistryPickerInput() {
        RegistryListSetting<?> setting = openRegistryPicker;
        refreshRegistryRows(setting);
        int column = unusedSettingColumn();
        float x = SETTING_COLUMN_X[column] + 12.0f;
        float top = settingColumnTop(column);
        float listTop = registryListTop(Echo.uiRenderer, setting, top);
        if (ui.interact(REGISTRY_SEARCH_INPUT_ID, new UiRect(x, top + 48.0f, 476.0f, 34.0f)).clicked()) {
            textFocus = TextInputFocus.PICKER_SEARCH;
        }
        if (!setting.getGroups().isEmpty()) {
            List<UiRect> rectangles = new ArrayList<>();
            layoutRegistryGroups(Echo.uiRenderer, setting, rectangles, x, top + 114.0f);
            setting.getGroups();
            for (int index = 0; index < rectangles.size(); index++) {
                if (index >= registryGroupStates.size()) break;
                if (ui.interact(REGISTRY_GROUP_INPUT_PREFIX + index, rectangles.get(index)).clicked()) {
                    setRegistryGroupSelected(setting, index, registryGroupStates.get(index) != SelectionState.ALL);
                }
            }
        }
        boolean hovered = mouseX >= x && mouseX <= x + 476.0f && mouseY >= listTop && mouseY <= listTop + 300.0f;
        if (hovered && Math.abs(ui.input().verticalScroll()) > 0.0) {
            float maximum = Math.max(0.0f, registryRows.size() * 32.0f - 300.0f);
            registryScroll = Math.max(0.0f, Math.min(maximum, registryScroll - (float) ui.input().verticalScroll() * 40.0f));
        }
        if (hovered) {
            for (int index = 0; index < registryRows.size(); index++) {
                float y = listTop + index * 32.0f - registryScroll;
                UiRect bounds = clippedOptionBounds(x, y, 476.0f, listTop, 300.0f);
                if (bounds == null) continue;
                RegistryPickerRow row = registryRows.get(index);
                if (ui.interact(REGISTRY_ENTRY_INPUT_PREFIX + row.id(), bounds).clicked()) {
                    setting.setIdSelected(row.id(), !setting.isIdSelected(row.id()));
                }
            }
        }
        float footer = listTop + 300.0f + 16.0f;
        if (ui.interact(REGISTRY_CLEAR_INPUT_ID, new UiRect(x + 476.0f - 64.0f, footer, 64.0f, 28.0f)).clicked()) {
            setting.replaceSelectedIds(List.of());
        }
    }

    private static <T> void setRegistryGroupSelected(RegistryListSetting<T> setting, int index, boolean selected) {
        setting.setGroupSelected(setting.getGroups().get(index), selected);
    }

    private void handleNamedRangesInput(NamedRangeGroupSetting setting, UiId id, float x, float y) {
        List<NamedRange> ranges = setting.getRanges();
        float top = y + 32.0f;
        float right = x + 476.0f;
        for (int index = 0; index < ranges.size(); index++) {
            NamedRange range = ranges.get(index);
            UiId rowId = id.child(range.getLabel());
            float rowY = top + namedRangeOffset(setting, index);
            if (ui.interact(rowId.child(NAMED_TOGGLE_INPUT_ID), new UiRect(right - 40.0f, rowY + 5.5f, 30.0f, 19.0f)).clicked()) {
                setting.toggleRange(index);
            }
            if (setting.hasBounds() && ui.interact(rowId.child(NAMED_CHIP_INPUT_ID), namedRangeChipBounds(Echo.uiRenderer, setting, range, x, rowY)).clicked()) {
                boolean open = editedNamedRange != range;
                closeNamedRangeEditor(setting);
                if (open) editedNamedRange = range;
                return;
            }
            if (ui.interact(rowId.child(NAMED_UP_INPUT_ID), new UiRect(right - 85.0f, rowY + 6.0f, 18.0f, 18.0f)).clicked()) {
                setting.moveRange(index, index - 1);
                return;
            }
            if (ui.interact(rowId.child(NAMED_DOWN_INPUT_ID), new UiRect(right - 65.0f, rowY + 6.0f, 18.0f, 18.0f)).clicked()) {
                setting.moveRange(index, index + 1);
                return;
            }
            if (editedNamedRange == range && setting.hasBounds()) handleNamedRangeEditorInput(setting, rowId, range, x, rowY + 30.0f);
            var drag = ui.interact(rowId, new UiRect(x, rowY, 386.0f, 30.0f));
            if (drag.pressed()) {
                draggedNamedRange = range;
                closeNamedRangeEditor(setting);
                namedRangeDragOffset = mouseY - rowY;
            }
            if (draggedNamedRange == range) {
                if (!drag.held()) draggedNamedRange = null;
                else {
                    int destination = Math.round((draggedRangeY(top, ranges.size()) - top) / 36.0f);
                    if (destination != index) { setting.moveRange(index, destination); return; }
                }
            }
        }
    }

    private void closeNamedRangeEditor(NamedRangeGroupSetting setting) {
        if (textFocus == TextInputFocus.SETTING_NUMBER && numericSetting == setting) commitNumericInput();
        editedNamedRange = null;
    }

    private void handleNamedRangeEditorInput(NamedRangeGroupSetting setting, UiId id, NamedRange range, float x, float y) {
        if (handleNamedRangeNumberInput(setting, id, range, x + 476.0f, y)) return;
        RangeBounds bounds = setting.getBounds();
        float left = x + 30.0f;
        var drag = ui.interact(id.child(NAMED_SLIDER_INPUT_ID), new UiRect(left, y + 20.0f, 436.0f, 26.0f));
        float fraction = DashboardLayout.sliderValue(mouseX, left, 436.0f, 16.0f);
        if (drag.pressed()) {
            float lower = normalizedValue(range.getLower(), bounds.minimum(), bounds.maximum());
            float upper = normalizedValue(range.getUpper(), bounds.minimum(), bounds.maximum());
            draggingUpperNamedRange = lower == upper ? fraction >= upper : Math.abs(fraction - upper) <= Math.abs(fraction - lower);
        }
        if (drag.pressed() || drag.held()) {
            float value = sliderSettingValue(fraction, bounds.minimum(), bounds.maximum(), bounds.step());
            if (draggingUpperNamedRange) setting.setRange(range, range.getLower(), Math.max(value, range.getLower()));
            else setting.setRange(range, Math.min(value, range.getUpper()), range.getUpper());
        }
        float bandX = x + 30.0f;
        for (RangeBand band : setting.getBands()) {
            UiRect rectangle = rangeBandBounds(Echo.uiRenderer, band, bandX, y);
            if (ui.interact(id.child(NAMED_PRESET_INPUT_ID).child(band.label()), rectangle).clicked()) setting.setRange(range, band.lower(), band.upper());
            bandX += rectangle.width() + 6.0f;
        }
    }

    private UiRect namedRangeNumberBounds(NamedRangeGroupSetting setting, NamedRange range, float right, float y, boolean upper) {
        float[] positions = namedRangeTextGeometry(setting, range, right);
        float x = upper ? positions[2] : positions[0];
        float width = upper ? positions[5] : positions[4];
        return new UiRect(x - 4.0f, y + 3.0f, width + 8.0f, 17.0f);
    }

    private boolean handleNamedRangeNumberInput(NamedRangeGroupSetting setting, UiId id, NamedRange range, float right, float y) {
        UiRect lower = namedRangeNumberBounds(setting, range, right, y, false);
        UiRect upper = namedRangeNumberBounds(setting, range, right, y, true);
        boolean upperClicked = ui.interact(id.child(NAMED_MAX_INPUT_ID), upper).clicked();
        boolean lowerClicked = !upperClicked && ui.interact(id.child(NAMED_MIN_INPUT_ID), lower).clicked();
        boolean clicked = lowerClicked || upperClicked;
        boolean editing = editingNumber(setting) && numericNamedRange == range;
        if (editing && ui.input().wasPressed(InputKey.MOUSE_LEFT.code()) && !lower.contains(mouseX, mouseY) && !upper.contains(mouseX, mouseY)) {
            commitNumericInput();
            return clicked;
        }
        if (!clicked) return editing;
        if (editing && editingUpperRange == upperClicked) return true;
        if (textFocus == TextInputFocus.SETTING_NUMBER) commitNumericInput();
        textFocus = TextInputFocus.SETTING_NUMBER;
        numericSetting = setting;
        numericNamedRange = range;
        editingUpperRange = upperClicked;
        numericText = "";
        return true;
    }

    private void handleFriendsInput() {
        if (ui.input().wasPressed(InputKey.MOUSE_LEFT.code())) {
            if (textFocus == TextInputFocus.FRIEND_SEARCH && !rawContainsPointer(1606.0f, 58.0f, 260.0f, 34.0f)) {
                textFocus = TextInputFocus.NONE;
            }
            if (textFocus == TextInputFocus.FRIEND_NAME && !rawContainsPointer(354.0f, 112.0f, 380.0f, 37.0f)) {
                textFocus = TextInputFocus.NONE;
            }
        }
        if (ui.interact(FRIEND_SEARCH_INPUT_ID, new UiRect(1606.0f, 58.0f, 260.0f, 34.0f)).clicked()) {
            textFocus = TextInputFocus.FRIEND_SEARCH;
        }
        if (ui.interact(FRIEND_NAME_INPUT_ID, new UiRect(354.0f, 112.0f, 380.0f, 37.0f)).clicked()) {
            textFocus = TextInputFocus.FRIEND_NAME;
        }
        if (ui.interact(FRIEND_ADD_INPUT_ID, new UiRect(742.0f, 112.0f, 86.0f, 37.0f)).clicked()) addFriend();
        List<String> friends = filteredFriends();
        int count = Math.min(friends.size(), 12);
        for (int index = 0; index < count; index++) {
            String name = friends.get(index);
            float x = 354 + index % 4 * 383;
            float y = 171 + index / 4 * 96;
            if (ui.interact(FRIEND_INPUT_PREFIX + name + FRIEND_REMOVE_SUFFIX,
                    new UiRect(x + 281.0f, y + 24.0f, 70.0f, 31.0f)).clicked()) {
                if (Echo.friendManager != null) Echo.friendManager.remove(name);
                return;
            }
        }
    }

    private boolean handleContextMenuInput() {
        if (contextMenuCardId == null) return false;
        ConfigCard card = page == DashboardPage.CONFIGS ? findConfig(contextMenuCardId) : null;
        if (card == null) {
            closeContextMenu();
            return false;
        }
        UiRect bounds = configContextMenuBounds(card);
        if (!bounds.contains(mouseX, mouseY)) {
            if (ui.input().wasPressed(InputKey.MOUSE_LEFT.code())) closeContextMenu();
            return true;
        }
        List<ConfigMenuAction> actions = configRules().menuActions(card);
        for (int index = 0; index < actions.size(); index++) {
            ConfigMenuAction action = actions.get(index);
            UiRect row = new UiRect(bounds.x() + 4.0f, bounds.y() + 4.0f + index * 30.0f,
                    bounds.width() - 8.0f, 30.0f);
            if (!ui.interact(CONFIG_MENU_INPUT_PREFIX + action.name(), row).clicked()) continue;
            switch (action) {
                case EDIT -> { closeContextMenu(); openConfigModal(ConfigModalMode.EDIT, card); }
                case ADD_COPY -> { closeContextMenu(); openConfigModal(ConfigModalMode.COPY, card); }
                case PUBLISH -> { closeContextMenu(); openConfigModal(ConfigModalMode.PUBLISH, card); }
                case UPDATE_PUBLIC -> { closeContextMenu(); openConfigModal(ConfigModalMode.UPDATE, card); }
                case GET_UPDATE -> {
                    if (confirmConfigAction(card.id + CONFIG_MENU_INPUT_SEPARATOR + action.name())) {
                        closeContextMenu();
                        applyConfigUpdate(card);
                    }
                }
                case UNPUBLISH -> {
                    if (confirmConfigAction(card.id + CONFIG_MENU_INPUT_SEPARATOR + action.name())) {
                        closeContextMenu();
                        unpublishConfig(card.publishedId);
                    }
                }
                case DELETE -> {
                    if (confirmConfigAction(card.id + CONFIG_MENU_INPUT_SEPARATOR + action.name())) {
                        closeContextMenu();
                        deleteConfig(card);
                    }
                }
            }
            return true;
        }
        return true;
    }

    private boolean confirmConfigAction(String id) {
        if (isConfirmationActive(id)) {
            pendingConfirmationId = null;
            return true;
        }
        pendingConfirmationId = id;
        confirmationStartedAtMillis = System.currentTimeMillis();
        return false;
    }

    private void openConfigModal(ConfigModalMode mode, ConfigCard card) {
        modalMode = mode;
        modalCard = card;
        modalOpen = true;
        modalOpenedAtNanos = System.nanoTime();
        textFocus = TextInputFocus.MODAL_NAME;
        ConfigCard details = mode == ConfigModalMode.UPDATE && findConfig(card.publishedId) != null
                ? findConfig(card.publishedId) : card;
        modalName = details == null ? "" : mode == ConfigModalMode.COPY ? uniqueCopyName(details.name) : details.name;
        modalDescription = details == null ? "" : details.description;
        modalGhost = details == null || !RAGE_TYPE_ID.equalsIgnoreCase(details.type);
    }

    private String uniqueCopyName(String name) {
        String candidate = name;
        int suffix = 2;
        while (configRules().localNameExists(candidate)) candidate = name + COPY_NUMBER_PREFIX + suffix++ + ")";
        return candidate;
    }

    private UiRect animatedModalBounds() {
        float progress = modalProgress();
        float scale = 0.98f + 0.02f * progress;
        float slide = 8.0f * (1.0f - progress);
        return new UiRect(960.0f - 280.0f * scale, 540.0f - 229.0f * scale + slide, 560.0f * scale, 459.0f * scale);
    }

    private void handleModalInput() {
        UiRect modal = new UiRect(680.0f, 311.0f, 560.0f, 459.0f);
        if (ui.input().wasPressed(InputKey.MOUSE_LEFT.code()) && !animatedModalBounds().contains(mouseX, mouseY)) {
            closeConfigModal();
            return;
        }
        if (modalProgress() < 0.65f) return;
        ui.pushClip(modal);
        boolean onName = new UiRect(704.0f, 438.0f, 512.0f, 40.0f).contains(mouseX, mouseY);
        boolean onDescription = new UiRect(704.0f, 602.0f, 512.0f, 76.0f).contains(mouseX, mouseY);
        if (ui.input().wasPressed(InputKey.MOUSE_LEFT.code()) && !onName && !onDescription) textFocus = TextInputFocus.NONE;
        if (ui.interact(MODAL_CLOSE_INPUT_ID, new UiRect(1186.0f, 340.0f, 30.0f, 30.0f)).clicked()
                || ui.interact(MODAL_CANCEL_INPUT_ID, new UiRect(1039.0f, 715.0f, 79.0f, 35.0f)).clicked()) {
            closeConfigModal();
        } else if (ui.interact(MODAL_COMMIT_INPUT_ID, new UiRect(1130.0f, 715.0f, 86.0f, 35.0f)).clicked()) {
            commitConfigModal();
        } else if (ui.interact(MODAL_NAME_INPUT_ID, new UiRect(704.0f, 438.0f, 512.0f, 40.0f)).clicked()) {
            textFocus = TextInputFocus.MODAL_NAME;
        } else if (ui.interact(MODAL_GHOST_INPUT_ID, new UiRect(704.0f, 518.0f, 251.0f, 44.0f)).clicked()) {
            modalGhost = true;
        } else if (ui.interact(MODAL_RAGE_INPUT_ID, new UiRect(965.0f, 518.0f, 251.0f, 44.0f)).clicked()) {
            modalGhost = false;
        } else if (ui.interact(MODAL_DESCRIPTION_INPUT_ID, new UiRect(704.0f, 602.0f, 512.0f, 76.0f)).clicked()) {
            textFocus = TextInputFocus.MODAL_DESCRIPTION;
        }
        ui.popClip();
    }

    private void applyConfigUpdate(ConfigCard card) {
        if (Echo.featureConfig == null) return;
        ConfigMetadata updated = Echo.featureConfig.selectAndApplyPreferred(card.id, card.sourceId);
        if (updated != null) acceptSelectedConfig(updated.id());
    }

    private void acceptSelectedConfig(String id) {
        activeConfigId = id;
        for (ConfigCard card : configCards) card.selected = card.id.equals(activeConfigId);
        refreshData();
    }

    private void unpublishConfig(String id) {
        if (Echo.featureConfig != null && Echo.featureConfig.delete(id)) requestRefresh();
    }

    private void deleteConfig(ConfigCard card) {
        if (Echo.featureConfig != null && Echo.featureConfig.deleteById(card.id)) {
            configCards.remove(card);
            if (card.selected) refreshData();
            requestRefresh();
        }
    }

    private void commitConfigModal() {
        String name = modalName.trim().isEmpty() ? UNTITLED_CONFIG_NAME : modalName.trim();
        String type = modalGhost ? MODAL_GHOST_TYPE_ID : MODAL_RAGE_TYPE_ID;
        ConfigCard card = modalCard;
        if (Echo.featureConfig != null) {
            switch (modalMode) {
                case NEW -> {
                    ConfigMetadata created = Echo.featureConfig.create(name, modalDescription, type);
                    if (created != null) activeConfigId = created.id();
                }
                case EDIT -> {
                    ConfigMetadata edited = card == null ? null
                            : Echo.featureConfig.updatePrivate(card.id, name, modalDescription, type, card.selected);
                    if (edited != null && card.selected) activeConfigId = edited.id();
                }
                case COPY -> {
                    ConfigMetadata copy = card == null ? null
                            : Echo.featureConfig.updateMetadataAndApply(card.id, name, modalDescription, type);
                    if (copy != null) acceptSelectedConfig(copy.id());
                }
                case PUBLISH, UPDATE -> {
                    if (card != null) Echo.featureConfig.updatePublic(card.id, name, modalDescription, type, card.selected);
                }
            }
        }
        requestRefresh();
        closeConfigModal();
    }

    private void loadConfig(String id, String name) {
        if (Echo.featureConfig == null) return;
        ConfigMetadata selected = Echo.featureConfig.selectById(id);
        if (selected != null) {
            activeConfigId = selected.id();
            for (ConfigCard card : configCards) card.selected = card.id.equals(activeConfigId);
            refreshData();
            requestRefresh();
        }
    }

    private void saveConfig(ConfigCard card) {
        if (Echo.featureConfig != null
                && Echo.featureConfig.updatePrivate(card.id, card.name, card.description, card.type, true) != null) {
            showConfigStatus(card.id, CONFIG_SAVED_STATUS);
            requestRefresh();
        }
    }

    private void performConfigAction(ConfigCard card, ConfigPrimaryAction action) {
        switch (action) {
            case LOAD, LOAD_OWN, ADD -> loadConfig(card.id, card.name);
            case LOAD_COPY -> {
                ConfigCard copy = configRules().firstCopy(card);
                if (copy != null) loadConfig(copy.id, copy.name);
            }
            case SHOW_COPIES -> {
                selectConfigBrowseMode(false);
                copiesSourceId = card.id;
                copiesSourceName = card.name;
                configScroll = 0.0f;
            }
            case SAVE -> saveConfig(card);
            case GET_UPDATE -> {
                if (!confirmConfigAction(card.id + "/" + action.name())) return;
                ConfigCard copy = card.publicVisibility ? configRules().firstCopy(card) : card;
                if (copy != null) applyConfigUpdate(copy);
            }
            case UNPUBLISH -> {
                if (confirmConfigAction(card.id + "/" + action.name())) unpublishConfig(card.id);
            }
        }
    }

    private void selectConfigBrowseMode(boolean browse) {
        copiesSourceId = null;
        if (browsePublicConfigs == browse) return;
        browsePublicConfigs = browse;
        configScroll = 0.0f;
        pendingConfirmationId = null;
    }

    private void handleConfigsInput() {
        if (textFocus == TextInputFocus.CONFIG_SEARCH && ui.input().wasPressed(InputKey.MOUSE_LEFT.code())
                && !new UiRect(1606.0f, 58.0f, 260.0f, 34.0f).contains(mouseX, mouseY)) {
            textFocus = TextInputFocus.NONE;
        }
        if (ui.interact(CONFIGS_YOURS_INPUT_ID, new UiRect(354.0f, 112.0f, 110.0f, 37.0f)).clicked()) {
            selectConfigBrowseMode(false);
        }
        if (configsAvailable() && ui.interact(CONFIGS_BROWSE_INPUT_ID, new UiRect(464.0f, 112.0f, 80.0f, 37.0f)).clicked()) {
            selectConfigBrowseMode(true);
        }
        if (ui.interact(CONFIG_SEARCH_INPUT_ID, new UiRect(1606.0f, 58.0f, 260.0f, 34.0f)).clicked()) {
            textFocus = TextInputFocus.CONFIG_SEARCH;
        }
        if (copiesSourceId != null && ui.interact(CONFIG_SOURCE_FILTER_INPUT_ID,
                new UiRect(556.0f, 112.0f, copiesFilterWidth, 37.0f)).clicked()) {
            copiesSourceId = null;
            configScroll = 0.0f;
        }
        List<ConfigCard> cards = filteredConfigs();
        UiRect viewport = new UiRect(330.0f, 157.0f, 1560.0f, 885.0f);
        if (viewport.contains(mouseX, mouseY) && Math.abs(ui.input().verticalScroll()) > 0.0) {
            float maximum = maximumConfigScroll(cards.size());
            configScroll = Math.max(0.0f, Math.min(maximum, configScroll - (float) ui.input().verticalScroll() * 60.0f));
        }
        boolean overConfirmation = false;
        ui.pushClip(viewport);
        for (int index = 0; index < cards.size(); index++) {
            float x = 354 + index % 3 * 509;
            float y = configCardY(index);
            if (!configCardVisible(y)) continue;
            ConfigCard card = cards.get(index);
            ConfigPrimaryAction pending = secondaryConfigAction(card);
            overConfirmation |= pending != null && isConfirmationActive(card.id + "/" + pending.name())
                    && ConfigCardLayout.secondaryButton(x, y, pending).contains(mouseX, mouseY);
            if (configRules().hasLocalLoadTarget(card)
                    && ui.interact(CONFIG_MORE_INPUT_PREFIX + card.id + CONFIG_MORE_SUFFIX,
                    DashboardLayout.configMenuButton(x, y)).clicked()) {
                openContextMenu(card, DashboardLayout.configMenuButton(x, y));
                ui.popClip();
                return;
            }
            ConfigPrimaryAction primary = configRules().primaryAction(card);
            ConfigPrimaryAction secondary = secondaryConfigAction(card);
            if (ui.interact(CONFIG_ACTION_INPUT_PREFIX + card.id + CONFIG_PRIMARY_SUFFIX,
                    ConfigCardLayout.primaryButton(x, y, secondary)).clicked()) {
                performConfigAction(card, primary);
                ui.popClip();
                return;
            }
            if (secondary != null && ui.interact(CONFIG_ACTION_INPUT_PREFIX + card.id + CONFIG_SECONDARY_SUFFIX,
                    ConfigCardLayout.secondaryButton(x, y, secondary)).clicked()) {
                performConfigAction(card, secondary);
                ui.popClip();
                return;
            }
        }
        if (canShowNewConfigCard()) {
            int index = cards.size();
            float y = configCardY(index);
            if (configCardVisible(y) && ui.interact(NEW_CONFIG_INPUT_ID,
                    new UiRect(354 + index % 3 * 509, configCardY(index), 493.0f, 144.0f)).clicked()) {
                openConfigModal(ConfigModalMode.NEW, null);
            }
        }
        ui.popClip();
        if (!overConfirmation && ui.input().wasPressed(InputKey.MOUSE_LEFT.code())) pendingConfirmationId = null;
    }

    private void handleModuleInput() {
        UiRect header = new UiRect(330.0f, 30.0f, 1560.0f, 52.0f);
        if (textFocus == TextInputFocus.MODULE_SEARCH && ui.input().wasPressed(InputKey.MOUSE_LEFT.code())
                && !header.contains(mouseX, mouseY)) textFocus = TextInputFocus.NONE;
        if (ui.interact(MODULE_SEARCH_ID, header).clicked()) textFocus = TextInputFocus.MODULE_SEARCH;
        if (selectedFeature != null) {
            handleFeatureBindingInput(selectedFeature);
            processSettingsInput();
        }
    }

    private void handleOverlayInput() {
        List<Feature> overlays = FeatureNameAssignments.overlays();
        for (int index = 0; index < overlays.size(); index++) {
            Feature feature = overlays.get(index);
            UiRect card = DashboardLayout.moduleCard(index);
            UiRect toggle = new UiRect(card.x() + card.width() - 44.0f, card.y() + 22.0f, 30.0f, 19.0f);
            if (ui.interact(OVERLAY_ID_PREFIX + FeatureNameAssignments.id(feature) + TOGGLE_ID_SUFFIX, toggle).clicked()) {
                feature.toggle();
                return;
            }
            if (ui.input().wasPressed(InputKey.MOUSE_RIGHT.code()) && card.contains(mouseX, mouseY)) {
                selectedOverlay = selectedOverlay == feature ? null : feature;
                openColorPicker = null;
                openRegistryPicker = null;
                textFocus = TextInputFocus.NONE;
                return;
            }
        }
        if (selectedOverlay != null) processSettingsInput();
    }

    private void handleFeatureBindingInput(Feature feature) {
        if (!feature.isKeybindable()) return;
        String id = FeatureNameAssignments.id(feature);
        if (ui.interact(id + TOGGLE_ID_SUFFIX, new UiRect(788.0f, 158.0f, 30.0f, 19.0f)).clicked()) feature.toggle();
        float width = featureBindWidths.getOrDefault(feature, 36.0f);
        if (ui.interact(id + FEATURE_BIND_INPUT_SUFFIX, new UiRect(753.0f - width, 189.5f, width, 27.0f)).clicked()) {
            bindingFeature = feature;
            bindingSetting = null;
            capturingBind = true;
            textFocus = TextInputFocus.NONE;
        }
        if (ui.interact(id + FEATURE_TOGGLE_MODE_INPUT_SUFFIX, new UiRect(761.0f, 189.0f, 28.0f, 28.0f)).clicked()) {
            FeatureNameAssignments.setBindMode(feature, BindMode.TOGGLE);
        }
        if (ui.interact(id + FEATURE_HOLD_MODE_INPUT_SUFFIX, new UiRect(790.0f, 189.0f, 28.0f, 28.0f)).clicked()) {
            FeatureNameAssignments.setBindMode(feature, BindMode.HOLD);
        }
        if (ui.interact(id + FEATURE_VISIBLE_INPUT_SUFFIX, new UiRect(788.0f, 230.0f, 30.0f, 19.0f)).clicked()) {
            feature.setVisible(!feature.isVisible());
        }
    }

    private void addFriend() {
        String name = friendName.trim();
        if (!name.isEmpty() && Echo.friendManager != null) {
            Echo.friendManager.add(name);
            friendName = "";
        }
    }

    public void mouseMoved(double x, double y) {
        mouseX = inputX((float) x);
        mouseY = inputY((float) y);
        UiInputState input = ui.input();
        float screenY = canvas.toScreenY(mouseY);
        float screenX = canvas.toScreenX(mouseX);
        input.setPointer(screenX, screenY);
    }
    public boolean mouseClicked(double x, double y, int button) {
        mouseMoved(x, y); ui.input().setDown(button, true);
        return capturesOverlayInput() || modalOpen || canvas.contains(mouseX, mouseY);
    }
    public boolean mouseReleased(double x, double y, int button) {
        mouseMoved(x, y); ui.input().setDown(button, false);
        return capturesOverlayInput() || modalOpen || canvas.contains(mouseX, mouseY);
    }
    public boolean mouseDragged(double x, double y, int button) {
        mouseMoved(x, y); return ui.input().isDown(button);
    }
    public boolean mouseScrolled(double x, double y, double verticalAmount) {
        mouseMoved(x, y); ui.input().addScroll(0.0, verticalAmount);
        return capturesOverlayInput() || modalOpen || canvas.contains(mouseX, mouseY);
    }
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        ui.input().addKeyEvent(key, scanCode, modifiers); return true;
    }
    public boolean charTyped(int character) { ui.input().addCharacter(character); return true; }
    private void closeConfigModal() { modalOpen = false; modalCard = null; textFocus = TextInputFocus.NONE; }
    private void closeContextMenu() { contextMenuCardId = null; pendingConfirmationId = null; }
    private void showConfigStatus(String id, String message) {
        statusConfigId = id; statusMessage = message; statusStartedAtMillis = System.currentTimeMillis();
    }
    private void openContextMenu(ConfigCard card, UiRect anchor) {
        contextMenuCardId = card.id; contextMenuAnchor = anchor;
        contextMenuOpenedAtNanos = System.nanoTime(); pendingConfirmationId = null;
    }
    private void preloadIcon(float scale, DashboardIcon icon, float width, float height) {
        svgTextures.getOrSchedule(icon.path, Math.max(1, Math.round(width * scale)),
                Math.max(1, Math.round(height * scale)), icon.colorMode);
    }
}
