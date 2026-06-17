package link.sakkit.sakchat.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import link.sakkit.sakchat.helpers.AsyncTasksWorker;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigManager {
    public static final String MOD_ID = "sakchat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Path: .minecraft/config/sakchat/

    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("sakchat");
    private static final File TRIGGER_FILE = CONFIG_PATH.resolve("triggers.json").toFile();
    private static final File CONFIG_FILE = CONFIG_PATH.resolve("config.json").toFile();
    private static final File SHORTCUTS_FILE = CONFIG_PATH.resolve("shortcuts.json").toFile();
    public static final String UPDATE_URL = "https://gist.githubusercontent.com/Linkernel0x/dbf09d70ae440a5ee11693c0b6a2d09d/raw/dd0d82650303824793e589d2d75725c6376417ab/version.json";
    public static final String CURRENT_VERSION;

    static {
        CURRENT_VERSION = FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("0.0.0-UNKNOWN");
    }

    public static SakChatConfig configs = new SakChatConfig();

    public static void loadConfig() {
        if (!CONFIG_PATH.toFile().exists()) {
            CONFIG_PATH.toFile().mkdirs();
        }

        AsyncTasksWorker.runAsync(() -> {
            configs.triggers = loadTriggers();
            configs.config = loadConfigFile();
            configs.shortcuts = loadShortcuts();
            configs.compiledTriggers = compileTriggers();
        });
    }

    public static void checkUpdates() {
        debugLog("current version:" + CURRENT_VERSION);
        HttpURLConnection conn = null;
        Minecraft client = Minecraft.getInstance();
        try {
            URL url = URI.create(UPDATE_URL).toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);

            if (conn.getResponseCode() == 200) {
                InputStreamReader reader = new InputStreamReader(conn.getInputStream());
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                reader.close();

                String latestVersion = json.get("latest_version").getAsString();
                String message = json.get("note").getAsString();

                if (!CURRENT_VERSION.equals(latestVersion)) {
                    SystemToast.add(
                            client.getToastManager(),
                            SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                            Component.translatable("sakchat.sakchat").append(Component.literal(" - ")).append(Component.translatable("sakchat.command.check.available.toast", latestVersion)),
                            Component.literal(message)
                    );
                    if (client.player != null) {
                        client.player.sendSystemMessage(Component.literal("").append(Component.translatable("sakchat.sakchat").withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_AQUA)).append(Component.literal(" - ")).append(Component.translatable("sakchat.command.check.available", latestVersion, message).withStyle(ChatFormatting.GREEN)));
                    }

                    LOGGER.info("[SakChat] A new update is available! Update from {} to {}", CURRENT_VERSION, latestVersion);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[SakChat] Could not check for updates or message: {}", e.getMessage());
            if (client.player != null) {
                client.player.sendSystemMessage(Component.literal("").append(Component.translatable("sakchat.sakchat").withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_AQUA)).append(Component.literal(" - ")).append(Component.translatable("sakchat.command.check.failed", CURRENT_VERSION).withStyle(ChatFormatting.RED)));
            }
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static List<TriggerEntry> loadTriggers() {
        if (!TRIGGER_FILE.exists()) {
            debugLog("No triggers.json found, creating a default one.");
            saveTriggers();
            return new ArrayList<>();
        }

        try (FileReader reader = new FileReader(TRIGGER_FILE)) {
            TriggerEntry[] loaded = GSON.fromJson(reader, TriggerEntry[].class);
            debugLog("Successfully loaded triggers.json");
            return loaded != null ? new ArrayList<>(List.of(loaded)) : new ArrayList<>();
        } catch (Exception e) {
            LOGGER.error("Failed to load triggers.json", e);
            return new ArrayList<>();
        }
    }

    private static ConfigEntry loadConfigFile() {
        if (!CONFIG_FILE.exists()) {
            debugLog("No configEntry.json found, creating a default one.");
            saveConfigFile();
            return new ConfigEntry();
        }
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            ConfigEntry loaded = GSON.fromJson(reader, ConfigEntry.class);
            debugLog("Successfully loaded config.json");
            return loaded != null ? loaded : new ConfigEntry();
        } catch (Exception e) {
            LOGGER.error("Failed to load config.json", e);
            return new ConfigEntry();
        }
    }

    private static List<ShortcutsEntry> loadShortcuts() {
        if (!SHORTCUTS_FILE.exists()) {
            debugLog("No shortcuts.json found, creating a default one.");
            saveShortcuts();
            return new ArrayList<>();
        }

        try (FileReader reader = new FileReader(SHORTCUTS_FILE)) {
            ShortcutsEntry[] loaded = GSON.fromJson(reader, ShortcutsEntry[].class);
            debugLog("Successfully loaded shortcuts.json");
            return loaded != null ? new ArrayList<>(List.of(loaded)) : new ArrayList<>();
        } catch (Exception e) {
            LOGGER.error("Failed to load shortcuts.json", e);
            return new ArrayList<>();
        }
    }

    public static Map<String, Pattern> compileTriggers() {
        Map<String, Pattern> compiledTriggers = new HashMap<>();

        for (TriggerEntry trigger : ConfigManager.configs.triggers) {
            if (trigger.pattern == null || trigger.pattern.isEmpty()) continue;

            try {
                int flags = trigger.caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
                Pattern p;

                if (trigger.regex) {
                    p = Pattern.compile(trigger.pattern, flags);
                } else {
                    p = Pattern.compile(Pattern.quote(trigger.pattern), flags);
                }

                compiledTriggers.put(trigger.id, p);

            } catch (Exception e) {
                ConfigManager.debugLog("Regex Error compiling trigger " + trigger.id);
            }
        }
        return compiledTriggers;
    }

    public static void debugLog(String message) {
        if (configs != null && configs.config.debugMode) {
            LOGGER.info("[SAKChat] {}", message);
        }
    }

    public static final List<Map.Entry<Pattern, String>> ANTI_LEAK_RULES = List.of(
            Map.entry(Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"), "[Email]"),
            Map.entry(Pattern.compile("\\d{3}-\\d{6,7}"), "[Phone]"),
            Map.entry(Pattern.compile("\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b"), "[IPv4]"),
            Map.entry(Pattern.compile("\\b(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}\\b"), "[IPv6]"),
            //Map.entry(Pattern.compile("(?i)(?:(?:X:\\s*)?(?<X1>-?\\d+(?:[.,]\\d+)?)(?:\\s*,\\s*|\\s+)(?:Y:\\s*)?(?<Y1>-?\\d+(?:[.,]\\d+)?)(?:\\s*,\\s*|\\s+)(?:Z:\\s*)?(?<Z1>-?\\d+(?:[.,]\\d+)?))|(?:(?<A1>[XYZ]):\\s*(?<V1>-?\\d+(?:[.,]\\d+)?)(?:\\s*,\\s*|\\s+)(?<A2>[XYZ]):\\s*(?<V2>-?\\d+(?:[.,]\\d+)?)(?:\\s*,\\s*|\\s+)(?<A3>[XYZ]):\\s*(?<V3>-?\\d+(?:[.,]\\d+)?))"), "§c[Cords]§r")
            Map.entry(Pattern.compile("(?i)(?:[xyz]:\\s*)?-?\\d+(?:[.,]\\d+)?(?:\\s*,\\s*|\\s+)(?:[xyz]:\\s*)?-?\\d+(?:[.,]\\d+)?(?:\\s*,\\s*|\\s+)(?:[xyz]:\\s*)?-?\\d+(?:[.,]\\d+)?"), "[Cords]")
    );

    public static void saveConfig() {
        AsyncTasksWorker.runAsync(() -> {
            saveTriggers();
            saveConfigFile();
            saveShortcuts();
            loadConfig();
        });
    }

    public static void saveTriggers() {
        try (FileWriter writer = new FileWriter(TRIGGER_FILE)) {
            GSON.toJson(configs.triggers, writer);
            debugLog("Successfully saved triggers.json");
        } catch (Exception e) {
            LOGGER.error("Failed to save triggers.json", e);
        }
    }

    public static void saveConfigFile() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(configs.config, writer);
            debugLog("Successfully saved configEntry.json");
        } catch (Exception e) {
            LOGGER.error("Failed to save configEntry.json", e);
        }
    }

    public static void saveShortcuts() {
        try (FileWriter writer = new FileWriter(SHORTCUTS_FILE)) {
            GSON.toJson(configs.shortcuts, writer);
            debugLog("Successfully saved shortcuts.json");
        } catch (Exception e) {
            LOGGER.error("Failed to save shortcuts.json", e);
        }
    }
}
