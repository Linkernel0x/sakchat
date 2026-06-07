package link.sakkit.sakchat.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import link.sakkit.sakchat.engine.AsyncTasksEngine;
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
    private static final File BLACKLIST_FILE = CONFIG_PATH.resolve("blacklist.json").toFile();
    private static final File STATUS_FILE = CONFIG_PATH.resolve("status.json").toFile();
    private static final File SHORTCUTS_FILE = CONFIG_PATH.resolve("shortcuts.json").toFile();
    public static final String UPDATE_URL = "https://gist.githubusercontent.com/Linkernel0x/dbf09d70ae440a5ee11693c0b6a2d09d/raw/dd0d82650303824793e589d2d75725c6376417ab/version.json";
    public static final String CURRENT_VERSION;

    static {
        CURRENT_VERSION = FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("0.0.0-UNKNOWN");
    }

    public static SakConfig config = new SakConfig();

    public static void loadConfig() {
        if (!CONFIG_PATH.toFile().exists()) {
            CONFIG_PATH.toFile().mkdirs();
        }

        AsyncTasksEngine.runAsync(() -> {
            config.triggers = loadTriggers();
            config.blacklist = loadBlacklist();
            config.status = loadStatus();
            config.shortcuts = loadShortcuts();
            config.compiledBlacklist = compileBlacklist();
            config.compiledTriggers = compileTriggers();
        });
    }

    public static void checkUpdates() {
        debugLog("current version:" + CURRENT_VERSION);
        HttpURLConnection conn = null;
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
                Minecraft client = Minecraft.getInstance();

                if (!CURRENT_VERSION.equals(latestVersion)) {
                    SystemToast.add(
                            client.getToastManager(),
                            SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                            Component.translatable("sakchat.sakchat").append(Component.literal(" - ")).append(Component.translatable("sakchat.command.check.available.toast", latestVersion)).withStyle(ChatFormatting.GREEN),
                            Component.literal(message)
                    );
                    client.player.sendSystemMessage(Component.literal("").append(Component.translatable("sakchat.sakchat").withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_AQUA)).append(Component.literal(" - ")).append(Component.translatable("sakchat.command.check.available", latestVersion, message).withStyle(ChatFormatting.GREEN)));

                    LOGGER.info("[SakChat] A new update is available! Update from " + CURRENT_VERSION + " to " + latestVersion);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[SakChat] Could not check for updates or message: " + e.getMessage());
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

    private static List<BlacklistEntry> loadBlacklist() {
        if (!BLACKLIST_FILE.exists()) {
            debugLog("No blacklist.json found, creating a default one.");
            saveBlacklist();
            return new ArrayList<>();
        }

        try (FileReader reader = new FileReader(BLACKLIST_FILE)) {
            BlacklistEntry[] loaded = GSON.fromJson(reader, BlacklistEntry[].class);
            debugLog("Successfully loaded blacklist.json");
            return loaded != null ? new ArrayList<>(List.of(loaded)) : new ArrayList<>();
        } catch (Exception e) {
            LOGGER.error("Failed to load blacklist.json", e);
            return new ArrayList<>();
        }
    }

    private static SakStatus loadStatus() {
        if (!STATUS_FILE.exists()) {
            debugLog("No status.json found, creating a default one.");
            saveStatus();
            return new SakStatus();
        }
        try (FileReader reader = new FileReader(STATUS_FILE)) {
            SakStatus loaded = GSON.fromJson(reader, SakStatus.class);
            debugLog("Successfully loaded status.json");
            return loaded != null ? loaded : new SakStatus();
        } catch (Exception e) {
            LOGGER.error("Failed to load status.json", e);
            return new SakStatus();
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

    public static List<Map.Entry<Pattern, String>> compileBlacklist() {
        List<Map.Entry<Pattern, String>> compiledBlacklist = new ArrayList<>();

        for (BlacklistEntry entry : ConfigManager.config.blacklist) {
            if (entry.pattern == null || entry.pattern.isEmpty()) continue;

            try {
                int flags = entry.caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
                Pattern p;

                if (entry.regex) {
                    p = Pattern.compile(entry.pattern, flags);
                } else {
                    p = Pattern.compile(Pattern.quote(entry.pattern), flags);
                }

                String replacement = entry.replacement != null ? entry.replacement : "***";
                compiledBlacklist.add(new AbstractMap.SimpleEntry<>(p, replacement));

            } catch (Exception e) {
                debugLog("Invalid blacklist pattern: " + entry.pattern + " -> " + e.getMessage());
            }
        }
        return compiledBlacklist;
    }

    public static Map<String, Pattern> compileTriggers() {
        Map<String, Pattern> compiledTriggers = new HashMap<>();

        for (TriggerEntry trigger : ConfigManager.config.triggers) {
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
        if (config != null && config.status.debugMode) {
            LOGGER.info("[SAKChat] " + message);
        }
    }

    public static void saveConfig() {
        AsyncTasksEngine.runAsync(() -> {
            saveTriggers();
            saveBlacklist();
            saveStatus();
            saveShortcuts();
            loadConfig();
        });
    }

    public static void saveTriggers() {
        try (FileWriter writer = new FileWriter(TRIGGER_FILE)) {
            GSON.toJson(config.triggers, writer);
            debugLog("Successfully saved triggers.json");
        } catch (Exception e) {
            LOGGER.error("Failed to save triggers.json", e);
        }
    }

    public static void saveBlacklist() {
        try (FileWriter writer = new FileWriter(BLACKLIST_FILE)) {
            GSON.toJson(config.blacklist, writer);
            debugLog("Successfully saved blacklist.json");
        } catch (Exception e) {
            LOGGER.error("Failed to save blacklist.json", e);
        }
    }

    public static void saveStatus() {
        try (FileWriter writer = new FileWriter(STATUS_FILE)) {
            GSON.toJson(config.status, writer);
            debugLog("Successfully saved status.json");
        } catch (Exception e) {
            LOGGER.error("Failed to save status.json", e);
        }
    }

    public static void saveShortcuts() {
        try (FileWriter writer = new FileWriter(SHORTCUTS_FILE)) {
            GSON.toJson(config.shortcuts, writer);
            debugLog("Successfully saved shortcuts.json");
        } catch (Exception e) {
            LOGGER.error("Failed to save shortcuts.json", e);
        }
    }
}
