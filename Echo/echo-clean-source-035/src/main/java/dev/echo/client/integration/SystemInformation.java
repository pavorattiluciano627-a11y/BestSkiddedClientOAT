package dev.echo.client.integration;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.echo.client.Echo;
import dev.echo.client.features.internals.ClickGUIFeature;
import dev.echo.client.input.InputKey;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;

/**
 * Once-only client diagnostics assembled as section/name/value triples.
 * Conditional source candidate: exact literal and old-link applicability domains
 * remain in the provider receipt, without a universal shared-cache claim.
 */
public final class SystemInformation {
    private static final int DEFAULT_VALUE_LIMIT = 380;
    private static boolean sent;

    private SystemInformation() {
    }

    /** Collects graphics/client state on the calling thread and hardware state on a daemon. */
    public static void send() {
        if (sent || Echo.eventsDisabled) return;
        sent = true;
        List<String> clientRows = new ArrayList<>();
        collectGraphics(clientRows);
        collectDisplay(clientRows);
        collectClient(clientRows);
        Thread worker = new Thread(() -> {
            List<String> rows = new ArrayList<>();
            collectSystem(rows);
            rows.addAll(clientRows);
            try {
                OfflineRuntimeSession.shared().call("client_info", rows.toArray(String[]::new));
            } catch (Throwable failure) {
                // Original command submission deliberately discards a failed report.
                return;
            }
        }, "echo-client-info");
        worker.setDaemon(true);
        worker.start();
    }

    private static void collectSystem(List<String> rows) {
        append(rows, "System", "OS", () -> System.getProperty("os.name") + " "
                + System.getProperty("os.version") + " (" + System.getProperty("os.arch") + ")");
        try {
            HardwareAbstractionLayer hardware = new SystemInfo().getHardware();
            CentralProcessor processor = hardware.getProcessor();
            append(rows, "System", "CPU", () -> processor.getProcessorIdentifier().getName().trim()
                    + " · " + processor.getPhysicalProcessorCount() + "C/" + processor.getLogicalProcessorCount() + "T");
            append(rows, "System", "RAM", () -> gibibytes(hardware.getMemory().getTotal()));
            List<GraphicsCard> cards = hardware.getGraphicsCards();
            for (int index = 0; index < cards.size(); index++) {
                GraphicsCard card = cards.get(index);
                String label = cards.size() == 1 ? "GPU" : "GPU " + (index + 1);
                append(rows, "System", label, () -> card.getName()
                        + (card.getVRam() > 0L ? " · " + gibibytes(card.getVRam()) : "")
                        + driverSuffix(card.getVersionInfo()));
            }
        } catch (Throwable failure) {
            append(rows, "System", "CPU", () -> Runtime.getRuntime().availableProcessors() + " threads");
        }
        append(rows, "System", "Java", () -> System.getProperty("java.version") + " ("
                + System.getProperty("java.vendor") + ")");
        append(rows, "System", "Heap", () -> gibibytes(Runtime.getRuntime().maxMemory()));
        append(rows, "System", "JVM flags", () -> ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .filter(argument -> argument.startsWith("-Xmx") || argument.startsWith("-Xms") || argument.startsWith("-XX:"))
                .collect(Collectors.joining(" ")));
    }

    private static void collectGraphics(List<String> rows) {
        try {
            GpuDevice device = RenderSystem.getDevice();
            append(rows, "Graphics", "API", () -> device.getDeviceInfo().backendName());
            append(rows, "Graphics", "Renderer", () -> device.getDeviceInfo().name());
            append(rows, "Graphics", "Vendor", () -> device.getDeviceInfo().vendorName());
            append(rows, "Graphics", "Driver", () -> device.getDeviceInfo().driverInfo());
        } catch (Throwable failure) {
            // The stream-proof row remains meaningful even if the device query fails.
        }
        append(rows, "Graphics", "Stream proof", () -> Echo.externalRendererActive ? "On" : "Off");
    }

    private static void collectDisplay(List<String> rows) {
        Minecraft client = Minecraft.getInstance();
        Window window = client.getWindow();
        append(rows, "Display", "Framebuffer", () -> DisplaySettingsDiagnostics.framebufferSize(window));
        append(rows, "Display", "Window", () -> DisplaySettingsDiagnostics.windowSize(window));
        append(rows, "Display", "GUI scale", () -> DisplaySettingsDiagnostics.scaleFactor(window));
        append(rows, "Display", "Fullscreen", () -> DisplaySettingsDiagnostics.fullscreen(client));
        append(rows, "Display", "VSync", () -> DisplaySettingsDiagnostics.verticalSync(client));
        append(rows, "Display", "FPS limit", () -> DisplaySettingsDiagnostics.maximumFps(client));
        append(rows, "Display", "Render distance", () -> DisplaySettingsDiagnostics.viewDistance(client));
        append(rows, "Display", "Graphics preset", () -> DisplaySettingsDiagnostics.graphicsPreset(client));
    }

    private static void collectClient(List<String> rows) {
        append(rows, "Client", "ClickGUI key", () -> {
            InputKey key = Echo.featureManager.findFeature(ClickGUIFeature.class).getKey();
            return key == null ? "NONE" : key.toString();
        });
        append(rows, "Client", "Language", () -> Minecraft.getInstance().options.languageCode);
        append(rows, "Client", "Class loader", DisplaySettingsDiagnostics::minecraftClassLoader);
        append(rows, "Client", "Mods", SystemInformation::modSummary, Integer.MAX_VALUE);
    }

    private static void append(List<String> rows, String section, String name, Callable<String> value) {
        append(rows, section, name, value, DEFAULT_VALUE_LIMIT);
    }

    private static void append(List<String> rows, String section, String name, Callable<String> value, int maximumLength) {
        try {
            String text = value.call();
            if (text == null || text.isBlank()) return;
            rows.add(section);
            rows.add(name);
            rows.add(text.length() > maximumLength ? text.substring(0, maximumLength - 1) + "…" : text);
        } catch (Throwable failure) {
            return;
        }
    }

    private static String gibibytes(long bytes) {
        return String.format(Locale.ROOT, "%.1f GB", (double) bytes / 1.073741824E9);
    }

    private static String driverSuffix(String version) {
        return version == null || version.isBlank() ? "" : " · " + version.replace("DriverVersion=", "driver ").trim();
    }

    private static String modSummary() {
        List<String> mods = new ArrayList<>();
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            String id = mod.getMetadata().getId();
            if (mod.getContainingMod().isPresent() || id.equals("java") || id.equals("minecraft")
                    || id.equals("fabricloader") || id.equals("mixinextras")) continue;
            mods.add(id + " " + mod.getMetadata().getVersion().getFriendlyString());
        }
        mods.sort(String.CASE_INSENSITIVE_ORDER);
        return mods.size() + ": " + String.join(", ", mods);
    }
}
