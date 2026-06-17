package link.sakkit.sakchat.helpers;

import link.sakkit.sakchat.config.ConfigManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Unique;
import com.google.gson.JsonElement;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ChatLogger {

    @Unique
    public static void saveChatLine(Component line) {
        Minecraft client = Minecraft.getInstance();

        if (client.level == null) return;

        String contextFolder = resolveContextFolder(client);
        String date = LocalDate.now().toString();

        AsyncTasksWorker.runAsync(() -> {
            try {
                var serializationContext = client.level.registryAccess()
                        .createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);

                LocalTime time = LocalTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
                String formattedTime = "[" + time.format(formatter) + "] ";

                Path folder = FabricLoader.getInstance().getGameDir()
                        .resolve("sakchat")
                        .resolve("records")
                        .resolve(contextFolder);

                Path txtFile = folder.resolve(date + ".txt");
                Path jsonFile = folder.resolve(date + ".jsonl");

                Component newLine = Component.literal("").append(Component.literal(formattedTime).withStyle(ChatFormatting.ITALIC, ChatFormatting.GOLD)).append(line);

                JsonElement jsonElement = ComponentSerialization.CODEC
                        .encodeStart(serializationContext, newLine)
                        .getOrThrow(err -> new RuntimeException("Error: " + err));

                Files.createDirectories(folder);

                Files.writeString(txtFile, formattedTime + line.getString() + "\n",
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);

                Files.writeString(jsonFile, jsonElement + "\n",
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            } catch (Exception e) {
                ConfigManager.LOGGER.error("[SAKChat] Failed to save chat line!", e);
                throw new RuntimeException(e);
            }
        });
    }

    @Unique
    public static Component loadChatComponents(Path jsonPath) {
        MutableComponent text = Component.empty();
        Minecraft client = Minecraft.getInstance();

        try {
            if (!Files.exists(jsonPath)) {
                return Component.literal(jsonPath.getFileName().toString() + " ").append(Component.translatable("sakchat.notFound")).withStyle(ChatFormatting.RED);
            }

            if (client.level == null) {
                return Component.translatable("sakchat.command.failed");
            }

            var serializationContext = client.level.registryAccess()
                    .createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);

            List<String> jsonList = Files.readAllLines(jsonPath);

            for (int i = 0; i < jsonList.size(); i++) {
                String json = jsonList.get(i).trim();
                if (json.isBlank()) continue;

                JsonElement jsonElement = com.google.gson.JsonParser.parseString(json);

                Component line = ComponentSerialization.CODEC
                        .parse(serializationContext, jsonElement)
                        .getOrThrow(err -> new RuntimeException("Error: " + err));

                text.append(line);
                if (i < jsonList.size() - 1) {
                    text.append("\n");
                }
            }
        } catch (Exception e) {
            ConfigManager.LOGGER.warn("[SAKChat] Failed to read chat components from JSON!", e);
            return Component.translatable("sakchat.command.failed").withStyle(ChatFormatting.RED);
        }

        return text;
    }


    private static String resolveContextFolder(Minecraft client) {
        try {
            ServerData data = client.getCurrentServer();
            if (data != null) {
                if (data.isRealm()) {
                    return "realms/" + sanitize(data.name);
                }
                String id = data.ip.replace(":", "_").replace(".", "_");
                return "multiplayer/" + sanitize(id);
            }
        } catch (Throwable ignored) {
        }

        try {
            Method m = Minecraft.class.getMethod("getSingleplayerServer");
            Object integrated = m.invoke(client);
            if (integrated != null) {
                try {
                    Method worldPathMethod = integrated.getClass().getMethod("getWorldPath", Class.forName("net.minecraft.world.level.storage.LevelResource"));
                } catch (Throwable ignored) {
                }

                try {
                    Method getLevelData = integrated.getClass().getMethod("getWorldData");
                    Object worldData = getLevelData.invoke(integrated);
                    if (worldData != null) {
                        String name = tryReadStringFieldOrMethod(worldData, "getLevelName", "levelName", "getName", "name");
                        if (name != null && !name.isEmpty()) {
                            return "singleplayer/" + sanitize(name);
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            Object levelData = null;
            try {
                Method m = client.level.getClass().getMethod("getLevelData");
                levelData = m.invoke(client.level);
            } catch (Throwable t) {
                try {
                    Field f = client.level.getClass().getDeclaredField("levelData");
                    f.setAccessible(true);
                    levelData = f.get(client.level);
                } catch (Throwable ignored) {
                }
            }

            if (levelData != null) {
                String name = tryReadStringFieldOrMethod(levelData, "getLevelName", "levelName", "getName", "name", "getDisplayName", "displayName");
                if (name != null && !name.isEmpty()) {
                    return "singleplayer/" + sanitize(name);
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            Long seed = null;
            try {
                Method m = client.level.getClass().getMethod("getSeed");
                Object s = m.invoke(client.level);
                if (s instanceof Long) seed = (Long) s;
            } catch (Throwable t) {
                try {
                    Field f = client.level.getClass().getDeclaredField("seed");
                    f.setAccessible(true);
                    Object s = f.get(client.level);
                    if (s instanceof Long) seed = (Long) s;
                } catch (Throwable ignored) {
                }
            }

            if (seed != null) {
                String dim = safeDimensionString(client);
                String id = "singleplayer_seed_" + Long.toUnsignedString(seed) + "_" + dim;
                return "singleplayer/" + sanitize(id);
            }
        } catch (Throwable ignored) {
        }

        try {
            Path base = FabricLoader.getInstance().getGameDir().resolve("sakchat").resolve("records");
            Files.createDirectories(base);
            Path marker = base.resolve("singleplayer_id.txt");
            if (Files.exists(marker)) {
                String existing = Files.readString(marker).trim();
                if (!existing.isEmpty()) return "singleplayer/" + sanitize(existing);
            } else {
                String generated = "singleplayer_" + stableRandomId();
                Files.writeString(marker, generated, StandardOpenOption.CREATE);
                return "singleplayer/" + sanitize(generated);
            }
        } catch (Throwable ignored) {
        }

        return "singleplayer";
    }

    private static String tryReadStringFieldOrMethod(Object obj, String... names) {
        for (String n : names) {
            try {
                Method m = obj.getClass().getMethod(n);
                Object r = m.invoke(obj);
                if (r instanceof String) return (String) r;
            } catch (Throwable ignored) {
            }
            try {
                Field f = obj.getClass().getDeclaredField(n);
                f.setAccessible(true);
                Object r = f.get(obj);
                if (r instanceof String) return (String) r;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static String safeDimensionString(Minecraft client) {
        try {
            Object dim = null;
            try {
                Method m = client.level.getClass().getMethod("dimension");
                dim = m.invoke(client.level);
            } catch (Throwable t) {
                try {
                    Method m2 = client.level.getClass().getMethod("getDimension");
                    dim = m2.invoke(client.level);
                } catch (Throwable ignored) {
                }
            }
            if (dim != null) return dim.toString().replace(":", "_");
        } catch (Throwable ignored) {
        }
        return "overworld";
    }

    private static String stableRandomId() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            long t = System.currentTimeMillis();
            md.update(Long.toString(t).getBytes());
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6 && i < digest.length; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return Long.toHexString(System.currentTimeMillis());
        }
    }

    private static String sanitize(String input) {
        if (input == null) return "unknown";
        String s = input.trim().toLowerCase(Locale.ROOT);
        return s.replaceAll("[^a-z0-9._-]", "_");
    }

}
