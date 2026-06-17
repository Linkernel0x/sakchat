package link.sakkit.sakchat;

import link.sakkit.sakchat.config.ConfigManager;
import link.sakkit.sakchat.engine.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static link.sakkit.sakchat.config.ConfigManager.ANTI_LEAK_RULES;

public class SakChatClient implements ClientModInitializer {

	private static boolean firstWorld = true;
	@Override
	public void onInitializeClient() {
		ConfigManager.LOGGER.info("SAKChat: Initializing...");
		ConfigManager.LOGGER.info("Loading configuration files...");
		ConfigManager.loadConfig();
		ConfigManager.LOGGER.info("Registering commands...");
		CommandsEngine.registerAll();
		ConfigManager.LOGGER.info("Registering keybinds...");
		InputEngine.register();

		if (ConfigManager.configs.config.updatesCheck) {
			ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
				if (firstWorld) {
					ConfigManager.checkUpdates();
					firstWorld = false;
				}
			});
		}
	}

	private static final Pattern COLOR_PATTERN = Pattern.compile("§[0-9a-fk-orZz]");
	public static Component processIncoming(Component message) {
		String cleanForMatch = COLOR_PATTERN.matcher(message.getString()).replaceAll("");

		Component filteredText = message;

		//---------------------
		// STREAMER MODE
		//---------------------
		if (ConfigManager.configs.config.modules.getOrDefault("streamer-mode", false)) {
			String username = Minecraft.getInstance().getUser().getName();

			Component streamerReplacement = Component.translatable("sakchat.config.modules.streamer-mode.replacement")
					.withStyle(net.minecraft.ChatFormatting.DARK_GREEN);

			filteredText = replaceRegexWithComponent(filteredText, "(?i)" + Pattern.quote(username), streamerReplacement);
		}

		//---------------------
		// ANTI DOXXING
		//---------------------
		if (ConfigManager.configs.config.modules.getOrDefault("anti-doxxing", false)) {
			for (Map.Entry<Pattern, String> rule : ANTI_LEAK_RULES) {
				try {
					Pattern p = rule.getKey();
					String replacement = rule.getValue();

					Component replacementComponent = Component.literal(replacement)
							.withStyle(net.minecraft.ChatFormatting.RED);

					filteredText = replaceRegexWithComponent(filteredText, p.pattern(), replacementComponent);
				} catch (Exception e) {
					ConfigManager.debugLog("Invalid anti-doxxing regex match: " + e.getMessage());
				}
			}
		}

		//---------------------
		// TRIGGER ENGINE & RETURN
		//---------------------
		if (message.equals(filteredText)) {
			return TriggerEngine.process(message, cleanForMatch);
		} else {
			return TriggerEngine.process(filteredText, cleanForMatch);
		}
	}


	public static Component replaceRegexWithComponent(Component component, String regex, Component replacement) {
		Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		String plainText = component.getString();
		MutableComponent result;

		if (pattern.matcher(plainText).find()) {
			result = Component.empty();
			Matcher matcher = pattern.matcher(plainText);
			int lastIdx = 0;

			while (matcher.find()) {
				if (matcher.start() > lastIdx) {
					result.append(Component.literal(plainText.substring(lastIdx, matcher.start())).withStyle(component.getStyle()));
				}

				result.append(replacement.copy());
				lastIdx = matcher.end();
			}

			if (lastIdx < plainText.length()) {
				result.append(Component.literal(plainText.substring(lastIdx)).withStyle(component.getStyle()));
			}
		} else {
			result = component.copy();
		}

		return result;
	}

	public static String processOutgoing(String message) {

        return ReplaceEngine.process(message);
	}
}
