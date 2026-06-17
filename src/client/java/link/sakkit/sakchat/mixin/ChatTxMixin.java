package link.sakkit.sakchat.mixin;

import link.sakkit.sakchat.config.ConfigManager;
import link.sakkit.sakchat.SakChatClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.regex.Pattern;

import static link.sakkit.sakchat.config.ConfigManager.ANTI_LEAK_RULES;

@Mixin(value = ClientPacketListener.class, priority = 1500)
public class ChatTxMixin {

    @Unique
    private static final ThreadLocal<Boolean> sakchat$sendingModified = ThreadLocal.withInitial(() -> false);

    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void sakchat$onSendChat(String content, CallbackInfo ci) {
        if (sakchat$sendingModified.get()) {
            return;
        }

        if (ConfigManager.configs.config.modules.getOrDefault("mute-chat", false)) {
            ci.cancel();
            return;
        }

        String modified = content;

        if (ConfigManager.configs.config.modules.getOrDefault("anti-leak", true)) {
            if (!modified.startsWith("#leak#")) {
                for (Map.Entry<Pattern, String> rule : ANTI_LEAK_RULES) {
                    try {
                        Pattern p = rule.getKey();
                        String reason = rule.getValue();

                        if (p.matcher(modified).find()) {
                            ci.cancel();
                            Component error = (Component.translatable("sakchat.sakchat.brackets").withStyle(ChatFormatting.DARK_AQUA)).append(Component.literal(" "))
                                    .append(Component.translatable("sakchat.main.blockedLeak", Component.literal(reason).withStyle(ChatFormatting.RED), Component.literal("#leak#").withStyle(ChatFormatting.RED)).withStyle(ChatFormatting.DARK_RED));
                            if (Minecraft.getInstance().player != null) {
                                Minecraft.getInstance().player.sendSystemMessage(error);
                            }
                            return;
                        }
                    } catch (Exception e) {
                        ConfigManager.debugLog("Invalid anti-leak regex: " + e.getMessage());
                    }
                }
            } else {
                modified = modified.substring(6);
            }
        }

        modified = SakChatClient.processOutgoing(modified);

        if (!modified.equals(content)) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                try {
                    sakchat$sendingModified.set(true);
                    mc.player.connection.sendChat(modified);
                } finally {
                    sakchat$sendingModified.set(false);
                }
            }
            ci.cancel();
        }
    }
}
