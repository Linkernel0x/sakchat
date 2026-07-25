package link.sakkit.sakchat.mixin;

import link.sakkit.sakchat.config.ConfigManager;
import link.sakkit.sakchat.SakChatClient;
import link.sakkit.sakchat.helpers.AsyncTasksWorker;
import link.sakkit.sakchat.helpers.ChatLogger;
import link.sakkit.sakchat.helpers.TranslationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChatComponent.class, priority = 500)
public abstract class ChatRxMixin {

	@Unique
	private static final ThreadLocal<Boolean> sakchat$processingIncoming = ThreadLocal.withInitial(() -> false);

	@Shadow
	private void addMessage(Component contents, MessageSignature signature, GuiMessageSource source, GuiMessageTag tag) {

	}

	@Inject(method = "addMessage", at = @At("HEAD"), cancellable = true)
	private void sakchat$onReceiveChat(Component contents, MessageSignature signature, GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci) {

		final Component originalComp = contents;

		if (sakchat$processingIncoming.get()) {
			return;
		}

		if (ConfigManager.configs != null && ConfigManager.configs.config.modules.getOrDefault("record-chat", true) && !ConfigManager.configs.config.disabled) {
			ChatLogger.saveChatLine(contents);
		}

		if (ConfigManager.configs == null || ConfigManager.configs.config.disabled) {
			return;
		}

		if (ConfigManager.configs.config.modules.getOrDefault("mute-chat", false)) {
			Minecraft.getInstance().gui.hud.getChat().clearMessages(false);
			SakChatClient.processIncoming(contents);
			ci.cancel();
			return;
		}

		Component modified = SakChatClient.processIncoming(contents);

		if (ConfigManager.configs.config.modules.getOrDefault("translations", false)) {
			ci.cancel();

            String originalText = modified.getString();
			String targetLang = ConfigManager.configs.config.translations.lang;

			final MessageSignature finalSignature = signature;
			final GuiMessageSource finalSource = source;
			final GuiMessageTag finalTag = tag;

			AsyncTasksWorker.runAsync(() -> {
				Component translatedResult;
				try {
					String translatedString = TranslationHelper.translate(originalText, targetLang).join();

					translatedResult = Component.literal(translatedString).withStyle(ChatFormatting.WHITE);
				} catch (Exception e) {
					translatedResult = Component.literal(originalText);
				}

				final Component finalTranslatedResult = Component.literal("")
						.append(Component.literal("[⏎] ")
								.withStyle(ChatFormatting.DARK_AQUA)
								.withStyle(style -> style
										.withHoverEvent(new HoverEvent.ShowText(originalComp))
								))
						.append(translatedResult);

				Minecraft.getInstance().execute(() -> {
					try {
						sakchat$processingIncoming.set(true);

						addMessage(finalTranslatedResult, finalSignature, finalSource, finalTag);
					} finally {
						sakchat$processingIncoming.set(false);
					}
				});
			});

        }

		if (!modified.equals(contents) && !ConfigManager.configs.config.modules.getOrDefault("translations", false)) {
			try {
				sakchat$processingIncoming.set(true);

				contents = Component.literal("")
						.append(Component.literal("[⏎] ")
								.withStyle(ChatFormatting.DARK_AQUA)
								.withStyle(style -> style
										.withHoverEvent(new HoverEvent.ShowText(originalComp))
								))
						.append(modified);

				addMessage(contents, signature, source, tag);

			} finally {
				sakchat$processingIncoming.set(false);
			}
			ci.cancel();
		}
	}
}