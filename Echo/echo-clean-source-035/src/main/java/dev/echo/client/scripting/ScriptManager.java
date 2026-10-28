package dev.echo.client.scripting;

import dev.echo.client.Echo;
import dev.echo.client.events.scripting.ScriptLoadedEvent;
import dev.echo.client.events.scripting.ScriptUnloadedEvent;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mozilla.javascript.Scriptable;

/**
 * Structured script loading and feature-list integration candidate.
 * The application bootstrap must supply the original scripts-directory value;
 * it is not guessed here. Event-name reconciliation is in interface-links.json.
 */
public class ScriptManager {
    private final Path directory;
    private final ScriptEngine engine;
    private final Map<String, ScriptFeature> loadedScripts;

    /** Retains the script engine and explicitly bound directory without validation. */
    public ScriptManager(ScriptEngine engine, Path directory) {
        this.directory = directory;
        loadedScripts = new HashMap<>();
        this.engine = engine;
    }

    /** Loads matching directory entries in the directory stream's original order. */
    public void loadAll() {
        ensureDirectory();
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(directory, "*.js")) {
            for (Path path : paths) load(path);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        if (!loadedScripts.isEmpty()) {
            ScriptEngine.LOGGER.info("Loaded {} script module(s)", loadedScripts.size());
        }
    }

    /** Unloads first, then loads the resolved path only if it exists. */
    public void reload(String filename) {
        unload(filename);
        Path path = directory.resolve(filename);
        if (Files.exists(path)) load(path);
    }

    /** Removes the map entry before disabling and removing the feature from lists. */
    public void unload(String filename) {
        ScriptFeature feature = loadedScripts.remove(filename);
        if (feature != null) {
            feature.setEnabled(false);
            Echo.featureManager.removeFeatureFromLists(feature);
            new ScriptUnloadedEvent(feature, filename).post();
        }
    }

    /** Lists matching filenames without sorting, copying, or normalizing their spelling. */
    public List<String> listFilenames() {
        ensureDirectory();
        ArrayList<String> result = new ArrayList<>();
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(directory, "*.js")) {
            for (Path path : paths) result.add(path.getFileName().toString());
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return result;
    }

    private void load(Path path) {
        String filename = path.getFileName().toString();
        try {
            String source = Files.readString(path);
            Scriptable scope = engine.run(source, filename);
            if (scope == null) return;
            ScriptDefinition definition = ScriptDefinitionParser.parse(scope, filename);
            if (definition == null) return;
            ScriptFeature feature = new ScriptFeature(definition, engine);
            feature.initSettings();
            Echo.featureManager.addFeatureToLists(feature);
            loadedScripts.put(filename, feature);
            new ScriptLoadedEvent(feature, filename).post();
            ScriptEngine.LOGGER.info("Loaded '{}' as '{}'", filename, definition.name());
        } catch (Exception exception) {
            ScriptErrorReporter.report(ScriptEngine.LOGGER, filename, "failed to load", exception);
        }
    }

    private void ensureDirectory() {
        try {
            if (!Files.exists(directory)) Files.createDirectories(directory);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    /** Returns the actual mutable backing map, as the original accessor does. */
    public Map<String, ScriptFeature> loadedScripts() { return loadedScripts; }
}
