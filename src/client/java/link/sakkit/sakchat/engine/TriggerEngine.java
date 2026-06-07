package link.sakkit.sakchat.engine;

import link.sakkit.sakchat.config.ConfigManager;
import link.sakkit.sakchat.config.TriggerEntry;
import link.sakkit.sakchat.helpers.ActionExecutor;
import net.minecraft.network.chat.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class TriggerEngine {
    private static final Map<String, Long> cooldownMap = new HashMap<>();

    public static Component process(Component currentComp, String cleanForMatch) {
        if (!ConfigManager.config.status.disabled && ConfigManager.config.status.modes.getOrDefault("triggerlist", true)) {
            long currentTime = System.currentTimeMillis();

            for (TriggerEntry trigger : ConfigManager.config.triggers) {
                if (trigger.enabled && isMatch(cleanForMatch, trigger)) {
                    long lastExecution = cooldownMap.getOrDefault(trigger.id, 0L);
                    if (currentTime - lastExecution >= trigger.cooldownMs) {
                        cooldownMap.put(trigger.id, currentTime);

                        return ActionExecutor.execute(currentComp, trigger);
                    }
                }
            }
        }
        return currentComp;
    }

    public static boolean isMatch(String message, TriggerEntry trigger) {
        Map<String, Pattern> compiledTriggers = ConfigManager.config.compiledTriggers;

        if (compiledTriggers == null || !compiledTriggers.containsKey(trigger.id)) {
            return false;
        }

        return compiledTriggers.get(trigger.id).matcher(message).find();
    }
}
