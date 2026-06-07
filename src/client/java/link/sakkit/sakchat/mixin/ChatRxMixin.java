package link.sakkit.sakchat.mixin;

import link.sakkit.sakchat.config.ConfigManager;
import link.sakkit.sakchat.SakChatClient;
import link.sakkit.sakchat.engine.AsyncTasksEngine;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import net.minecraft.client.multiplayer.ServerData;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Mixin(value = ChatComponent.class, priority = 1500)
public class ChatRxMixin {

	@ModifyVariable(method = "addMessage", at = @At("HEAD"), argsOnly = true)

	private Component sakHandleMessage(Component message) {
		if (ConfigManager.config.status.modules.getOrDefault("record-chat", false) && !ConfigManager.config.status.disabled) {
			saveChatLine(message);
		}

		if (ConfigManager.config == null || ConfigManager.config.status.disabled) {
			return message;
		} else if (!ConfigManager.config.status.modules.getOrDefault("mute-chat", false)) {
			return SakChatClient.processIncoming(message);
		} else {
			Minecraft.getInstance().gui.getChat().clearMessages(false);
			SakChatClient.processIncoming(message);
			return Component.empty();
		}
	}
	private static void saveChatLine(Component line) {
		Minecraft client = Minecraft.getInstance();

		if (client.level == null) return;

		String contextFolder = resolveContextFolder(client);
		String date = LocalDate.now().toString();

		AsyncTasksEngine.runAsync(() -> {
			try {

				LocalTime time = LocalTime.now();
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
				String formattedTime = "[" + time.format(formatter) + "] ";

				Path folder = FabricLoader.getInstance().getGameDir()
						.resolve("sakchat")
						.resolve("records")
						.resolve(contextFolder);
				Path file = folder.resolve(date + ".txt");


				Files.createDirectories(folder);
				Files.writeString(file, formattedTime + line.getString() + "\n",
						StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			} catch (Exception e) {
				ConfigManager.LOGGER.error("[SAKChat] Failed to save chat line!", e);
				throw new RuntimeException(e);
			}
		});
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
		} catch (Throwable ignored) {}

		try {
			Method m = Minecraft.class.getMethod("getSingleplayerServer");
			Object integrated = m.invoke(client);
			if (integrated != null) {
				try {
					Method worldPathMethod = integrated.getClass().getMethod("getWorldPath", Class.forName("net.minecraft.world.level.storage.LevelResource"));
				} catch (Throwable ignored) {}

				try {
					Method getLevelData = integrated.getClass().getMethod("getWorldData");
					Object worldData = getLevelData.invoke(integrated);
					if (worldData != null) {
						String name = tryReadStringFieldOrMethod(worldData, "getLevelName", "levelName", "getName", "name");
						if (name != null && !name.isEmpty()) {
							return "singleplayer/" + sanitize(name);
						}
					}
				} catch (Throwable ignored) {}
			}
		} catch (Throwable ignored) {}

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
				} catch (Throwable ignored) { }
			}

			if (levelData != null) {
				String name = tryReadStringFieldOrMethod(levelData, "getLevelName", "levelName", "getName", "name", "getDisplayName", "displayName");
				if (name != null && !name.isEmpty()) {
					return "singleplayer/" + sanitize(name);
				}
			}
		} catch (Throwable ignored) {}

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
				} catch (Throwable ignored) { }
			}

			if (seed != null) {
				String dim = safeDimensionString(client);
				String id = "singleplayer_seed_" + Long.toUnsignedString(seed) + "_" + dim;
				return "singleplayer/" + sanitize(id);
			}
		} catch (Throwable ignored) { }

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
		} catch (Throwable ignored) { }

		return "singleplayer";
	}

	private static String tryReadStringFieldOrMethod(Object obj, String... names) {
		for (String n : names) {
			try {
				Method m = obj.getClass().getMethod(n);
				Object r = m.invoke(obj);
				if (r instanceof String) return (String) r;
			} catch (Throwable ignored) { }
			try {
				Field f = obj.getClass().getDeclaredField(n);
				f.setAccessible(true);
				Object r = f.get(obj);
				if (r instanceof String) return (String) r;
			} catch (Throwable ignored) { }
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
				} catch (Throwable ignored) { }
			}
			if (dim != null) return dim.toString().replace(":", "_");
		} catch (Throwable ignored) { }
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
