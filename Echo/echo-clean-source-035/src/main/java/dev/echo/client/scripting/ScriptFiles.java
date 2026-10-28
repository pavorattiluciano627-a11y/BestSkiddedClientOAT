package dev.echo.client.scripting;

import dev.echo.client.screens.DashboardScreen;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Original dashboard script-file facade, including its disabled direct APIs.
 * The recovery receipt records the selected caller/key and shared-cache domains.
 */
public final class ScriptFiles {
    public static final boolean SCRIPTING_ENABLED = false;
    private static final Path DIRECTORY = Path.of(new String("Echo"), new String("scripts"));

    // Named, lazy original string slots preserve shared non-interned results.
    private static String disabledMessage;
    private static String failedPrefix;
    private static String parseErrorMessage;
    private static String loadedPrefix;
    private static String nameSeparator;
    private static String enabledMessage;
    private static String registeredMessage;

    private ScriptFiles() {}

    /** The shipped saveGroup() body is exactly ACONST_NULL; ARETURN. */
    public static ScriptManager unavailableManager() {
        return null;
    }

    public static List<String> listFilenames() {
        boolean stateClear = DashboardScreen.isSharedControlStateClear();
        ScriptManager manager = unavailableManager();
        if (!stateClear && manager == null) {
            return List.of();
        }
        return manager.listFilenames();
    }

    public static ScriptFeature loadedScript(String filename) {
        boolean state = DashboardScreen.getSharedControlState();
        ScriptManager manager = unavailableManager();
        if (!state) {
            // The original alternate branch casts the manager itself, not its map.
            return (ScriptFeature) (Object) manager;
        }
        return manager == null ? null : manager.loadedScripts().get(filename);
    }

    /** Original getResult(String) returns the constant empty string without I/O. */
    public static String readSource(String filename) {
        return "";
    }

    /** Original registerSnapshot(String,String) returns false without touching either argument. */
    public static boolean writeSource(String filename, String source) {
        return false;
    }

    public static boolean deleteScript(String filename) {
        boolean state = DashboardScreen.getSharedControlState();
        ScriptManager manager = unavailableManager();
        if (state && manager == null) {
            return false;
        }
        try {
            if (state) {
                manager.unload(filename);
            }
            return Files.deleteIfExists(DIRECTORY.resolve(filename));
        } catch (IOException exception) {
            return false;
        }
    }

    /** Original getEvent(String) returns the same empty-string constant as getResult. */
    public static String describeFile(String filename) {
        return "";
    }

    public static List<String> reloadWithDiagnostics(String filename) {
        boolean state = DashboardScreen.getSharedControlState();
        ArrayList<String> diagnostics = new ArrayList<>();
        ScriptManager manager = unavailableManager();
        if (state) {
            if (manager == null) {
                diagnostics.add(disabledMessage());
                return diagnostics;
            }
            manager.unload(filename);
            manager.reload(filename);
        }
        ScriptFeature feature = manager.loadedScripts().get(filename);
        if (state) {
            if (feature == null) {
                diagnostics.add(failedPrefix() + filename);
                diagnostics.add(parseErrorMessage());
                return diagnostics;
            }
            String name = String.valueOf(feature.getName());
            diagnostics.add(loadedPrefix() + filename + nameSeparator() + name);
        }
        diagnostics.add(feature.isEnabled() ? enabledMessage() : registeredMessage());
        return diagnostics;
    }

    private static String disabledMessage() {
        String value = disabledMessage;
        if (value == null) disabledMessage = value = new String("scripts are disabled");
        return value;
    }

    private static String failedPrefix() {
        String value = failedPrefix;
        if (value == null) failedPrefix = value = new String("failed to load ");
        return value;
    }

    private static String parseErrorMessage() {
        String value = parseErrorMessage;
        if (value == null) parseErrorMessage = value = new String("see the log for the parse error");
        return value;
    }

    private static String loadedPrefix() {
        String value = loadedPrefix;
        if (value == null) loadedPrefix = value = new String("loaded ");
        return value;
    }

    private static String nameSeparator() {
        String value = nameSeparator;
        if (value == null) nameSeparator = value = new String(" as ");
        return value;
    }

    private static String enabledMessage() {
        String value = enabledMessage;
        if (value == null) enabledMessage = value = new String("module enabled");
        return value;
    }

    private static String registeredMessage() {
        String value = registeredMessage;
        if (value == null) registeredMessage = value = new String("module registered, currently off");
        return value;
    }
}
