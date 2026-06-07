package link.sakkit.sakchat.engine;

import link.sakkit.sakchat.config.ConfigManager;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class BlacklistEngine {

    private static final List<Map.Entry<Pattern, String>> ANTI_DOXXING_RULES = List.of(
            // Email
            Map.entry(Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"), "§c[Email]§r"),
            // phone number
            Map.entry(Pattern.compile("\\d{3}-\\d{6,7}"), "§c[Phone]§r"),
            // IPv4
            Map.entry(Pattern.compile("\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b"), "§c[IPv4]§r"),
            // IPv6
            Map.entry(Pattern.compile("\\b(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}\\b"), "§c[IPv6]§r"),
            // Cords
            Map.entry(Pattern.compile("(?i)(?:(?:X:\\s*)?(?<X1>-?\\d+(?:[.,]\\d+)?)(?:\\s*,\\s*|\\s+)(?:Y:\\s*)?(?<Y1>-?\\d+(?:[.,]\\d+)?)(?:\\s*,\\s*|\\s+)(?:Z:\\s*)?(?<Z1>-?\\d+(?:[.,]\\d+)?))|(?:(?<A1>[XYZ]):\\s*(?<V1>-?\\d+(?:[.,]\\d+)?)(?:\\s*,\\s*|\\s+)(?<A2>[XYZ]):\\s*(?<V2>-?\\d+(?:[.,]\\d+)?)(?:\\s*,\\s*|\\s+)(?<A3>[XYZ]):\\s*(?<V3>-?\\d+(?:[.,]\\d+)?))"), "§c[Cords]§r")

    );

    public static String process(String originalText) {
        String filteredText = originalText;

        //---------------------
        // STREAMER MODE
        //---------------------

        if (ConfigManager.config.status.modules.getOrDefault("streamer-mode", false)) {
            String username = Minecraft.getInstance().getUser().getName();
            filteredText = filteredText.replaceAll("(?i)" + Pattern.quote(username), Matcher.quoteReplacement("§2[You]§r"));

        }

        //---------------------
        // ANTI DOXXING
        //---------------------

        if (ConfigManager.config.status.modules.getOrDefault("anti-doxxing", false)) {
            for (Map.Entry<Pattern, String> rule : ANTI_DOXXING_RULES) {
                try {
                    Pattern p = rule.getKey();
                    String replacement = rule.getValue();

                    filteredText = p.matcher(filteredText).replaceAll(Matcher.quoteReplacement(replacement));
                } catch (Exception e) {
                    ConfigManager.debugLog("Invalid anti-doxxing regex: " + e.getMessage());
                }
            }
        }

        //---------------------
        // BLACKLIST
        //---------------------

        if (ConfigManager.config.status.modes.getOrDefault("blacklist", false)) {

            for (Map.Entry<Pattern, String> entry : ConfigManager.config.compiledBlacklist) {
                Pattern p = entry.getKey();
                String replacement = entry.getValue();

                filteredText = p.matcher(filteredText).replaceAll(Matcher.quoteReplacement(replacement));
            }
        }

        return filteredText;
    }
}
