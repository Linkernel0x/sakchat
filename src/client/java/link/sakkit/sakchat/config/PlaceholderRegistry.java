package link.sakkit.sakchat.config;

import net.minecraft.client.Minecraft;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class PlaceholderRegistry {

    private static final Minecraft mc = Minecraft.getInstance();

    public static final Map<String, Supplier<String>> PLACEHOLDERS = new HashMap<>();

    static {

        // ---------------------------
        // PLAYER
        // ---------------------------
        PLACEHOLDERS.put("player_name", () -> mc.player != null ? mc.player.getName().getString() : "null");
        PLACEHOLDERS.put("uuid", () -> mc.player != null ? mc.player.getUUID().toString() : "null");
        PLACEHOLDERS.put("health", () -> mc.player != null ? String.valueOf(mc.player.getHealth()) : "0");
        PLACEHOLDERS.put("hearts", () -> mc.player != null ? String.valueOf(mc.player.getHealth() / 2.0f) : "0");
        PLACEHOLDERS.put("max_health", () -> mc.player != null ? String.valueOf(mc.player.getMaxHealth()) : "0");
        PLACEHOLDERS.put("hunger", () -> mc.player != null ? String.valueOf(mc.player.getFoodData().getFoodLevel()) : "0");
        PLACEHOLDERS.put("saturation", () -> mc.player != null ? String.valueOf(mc.player.getFoodData().getSaturationLevel()) : "0");
        PLACEHOLDERS.put("armor", () -> mc.player != null ? String.valueOf(mc.player.getArmorValue()) : "0");
        PLACEHOLDERS.put("air", () -> mc.player != null ? String.valueOf(mc.player.getAirSupply()) : "0");
        PLACEHOLDERS.put("xp_level", () -> mc.player != null ? String.valueOf(mc.player.experienceLevel) : "0");
        PLACEHOLDERS.put("xp_progress", () -> mc.player != null ? String.valueOf(mc.player.experienceProgress) : "0");
        PLACEHOLDERS.put("gamemode", () -> mc.player != null ? mc.player.gameMode().getName() : "null");

        // ---------------------------
        // CORDS & MOVEMENT
        // ---------------------------
        PLACEHOLDERS.put("x", () -> mc.player != null ? String.valueOf((int) mc.player.getX()) : "0");
        PLACEHOLDERS.put("y", () -> mc.player != null ? String.valueOf((int) mc.player.getY()) : "0");
        PLACEHOLDERS.put("z", () -> mc.player != null ? String.valueOf((int) mc.player.getZ()) : "0");
        PLACEHOLDERS.put("yaw", () -> mc.player != null ? String.valueOf(mc.player.getYRot()) : "0");
        PLACEHOLDERS.put("pitch", () -> mc.player != null ? String.valueOf(mc.player.getXRot()) : "0");
        PLACEHOLDERS.put("speed", () -> mc.player != null ? String.format("%.2f", mc.player.getDeltaMovement().length()) : "0");
        PLACEHOLDERS.put("direction", () -> mc.player != null ? mc.player.getDirection().getName() : "null");

        // ---------------------------
        // WORLD
        // ---------------------------
        PLACEHOLDERS.put("world", () -> mc.level != null ? mc.level.dimension().toString() : "null");
        PLACEHOLDERS.put("biome", () -> {
            if (mc.level == null || mc.player == null) return "null";
            return mc.level.getBiome(mc.player.blockPosition()).unwrapKey().get().toString();
        });
        //PLACEHOLDERS.put("daytime", () -> mc.level != null ? String.valueOf(mc.level.dayTime()) : "0");
        PLACEHOLDERS.put("light", () -> {
            if (mc.level == null || mc.player == null) return "0";
            return String.valueOf(mc.level.getMaxLocalRawBrightness(mc.player.blockPosition()));
        });
        PLACEHOLDERS.put("time", () -> LocalTime.now().toString());

        // ---------------------------
        // SERVER
        // ---------------------------
        PLACEHOLDERS.put("server_ip", () -> mc.getCurrentServer() != null ? mc.getCurrentServer().ip : "singleplayer");
        PLACEHOLDERS.put("server_name", () -> mc.getCurrentServer() != null ? mc.getCurrentServer().name : "singleplayer");
        PLACEHOLDERS.put("ping", () -> {
            var info = mc.player.connection.getPlayerInfo(mc.player.getUUID());
            return info != null ? String.valueOf(info.getLatency()) : "0";
        });

        // ---------------------------
        // CLIENT PERFORMANCE
        // ---------------------------
        PLACEHOLDERS.put("fps", () -> String.valueOf(mc.getFps()));
        PLACEHOLDERS.put("vsync", () -> String.valueOf(mc.options.enableVsync().get()));
        //PLACEHOLDERS.put("graphics", () -> mc.options.graphicsMode().get().toString());
        PLACEHOLDERS.put("render_distance", () -> String.valueOf(mc.options.renderDistance().get()));

        // ---------------------------
        // PLAYER STATUS
        // ---------------------------
        PLACEHOLDERS.put("is_sneaking", () -> mc.player != null && mc.player.isShiftKeyDown() ? "true" : "false");
        PLACEHOLDERS.put("is_sprinting", () -> mc.player != null && mc.player.isSprinting() ? "true" : "false");
        PLACEHOLDERS.put("is_flying", () -> mc.player != null && mc.player.getAbilities().flying ? "true" : "false");
        PLACEHOLDERS.put("is_swimming", () -> mc.player != null && mc.player.isSwimming() ? "true" : "false");
        PLACEHOLDERS.put("is_in_water", () -> mc.player != null && mc.player.isInWater() ? "true" : "false");
        PLACEHOLDERS.put("is_in_lava", () -> mc.player != null && mc.player.isInLava() ? "true" : "false");

        // ---------------------------
        // INVENTORY
        // ---------------------------
        PLACEHOLDERS.put("item_main", () -> mc.player != null ? mc.player.getMainHandItem().getDisplayName().getString() : "null");
        PLACEHOLDERS.put("item_off", () -> mc.player != null ? mc.player.getOffhandItem().getDisplayName().getString() : "null");
        PLACEHOLDERS.put("item_count", () -> mc.player != null ? String.valueOf(mc.player.getMainHandItem().getCount()) : "0");
        //PLACEHOLDERS.put("inventory", () -> mc.player != null ? String.valueOf(mc.player.getInventory()) : "0");

        // ---------------------------
        // SYSTEM / PC
        // ---------------------------
        PLACEHOLDERS.put("os_name", () -> System.getProperty("os.name"));
        PLACEHOLDERS.put("java_version", () -> System.getProperty("java.version"));
        PLACEHOLDERS.put("ram_used", () -> {
            long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            return String.valueOf(used / (1024 * 1024)) + "MB";
        });
    }
}
