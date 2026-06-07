package link.sakkit.sakchat;

import link.sakkit.sakchat.config.ConfigManager;
import link.sakkit.sakchat.engine.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.network.chat.Component;

import java.util.regex.Pattern;
//import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;


public class SakChatClient implements ClientModInitializer {

	private static boolean firstWorld = true;
	@Override
	public void onInitializeClient() {
		ConfigManager.LOGGER.info("SAKChat: Initializing...");
		ConfigManager.LOGGER.info("Loading configuration files...");
		ConfigManager.loadConfig();
		ConfigManager.LOGGER.info("Registering commands...");
		CommandsEngine.registerAll();
		InputEngine.register();

		if (ConfigManager.config.status.updatesCheck) {
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
		String rawText = message.getString();
		String filteredText = BlacklistEngine.process(rawText);

		Component currentComp = filteredText.equals(rawText) ? message :
				Component.literal(filteredText).setStyle(message.getStyle());

		String cleanForMatch = COLOR_PATTERN.matcher(filteredText).replaceAll("");
		//long currentTime = System.currentTimeMillis();

		currentComp = TriggerEngine.process(currentComp, cleanForMatch);
		return currentComp;
	}

	public static String processOutgoing(String message) {
		String currentMessage = ReplaceEngine.process(message);

		return currentMessage;
	}
}
