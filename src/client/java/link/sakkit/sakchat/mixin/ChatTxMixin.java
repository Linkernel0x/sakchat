package link.sakkit.sakchat.mixin;

import link.sakkit.sakchat.config.ConfigManager;
import link.sakkit.sakchat.SakChatClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Mixin(value = ClientPacketListener.class, priority = 1500)
public class ChatTxMixin {

    private static final List<Map.Entry<Pattern, String>> ANTI_LEAK_RULES = List.of(
            Map.entry(Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"), "§c[Email]§r"),
            Map.entry(Pattern.compile("\\d{3}-\\d{6,7}"), "§c[Phone]§r"),
            Map.entry(Pattern.compile("\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b"), "§c[IPv4]§r"),
            Map.entry(Pattern.compile("\\b(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}\\b"), "§c[IPv6]§r"),
            Map.entry(Pattern.compile("(?i)(?:(?:X:\\s*)?(?<X1>-?\\d+(?:[.,]\\d+)?)(?:\\s*,\\s*|\\s+)(?:Y:\\s*)?(?<Y1>-?\\d+(?:[.,]\\d+)?)(?:\\s*,\\s*|\\s+)(?:Z:\\s*)?(?<Z1>-?\\d+(?:[.,]\\d+)?))|(?:(?<A1>[XYZ]):\\s*(?<V1>-?\\d+(?:[.,]\\d+)?)(?:\\s*,\\s*|\\s+)(?<A2>[XYZ]):\\s*(?<V2>-?\\d+(?:[.,]\\d+)?)(?:\\s*,\\s*|\\s+)(?<A3>[XYZ]):\\s*(?<V3>-?\\d+(?:[.,]\\d+)?))"), "§c[Cords]§r")
    );

    private static final ThreadLocal<Boolean> sakchat$sendingModified = ThreadLocal.withInitial(() -> false);

    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void sakchat$onSendChat(String message, CallbackInfo ci) {
        // Se il messaggio è già quello modificato da noi, non fare nulla e lascialo passare
        if (sakchat$sendingModified.get()) {
            return;
        }

        if (ConfigManager.config.status.modules.getOrDefault("mute-chat", false)) {
            ci.cancel();
            return;
        }

        String modified = message;

        if (ConfigManager.config.status.modules.getOrDefault("anti-leak", true)) {
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

        if (!modified.equals(message)) {
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
