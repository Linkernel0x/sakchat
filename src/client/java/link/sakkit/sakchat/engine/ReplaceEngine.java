package link.sakkit.sakchat.engine;

import link.sakkit.sakchat.config.ConfigManager;
import link.sakkit.sakchat.config.PlaceholderRegistry;

public class ReplaceEngine {

    public static String process(String originalMessage) {
        String editedMessage = originalMessage;

        //---------------------
        // PLACEHOLDERS
        //---------------------

        if (ConfigManager.configs.config.modules.getOrDefault("placeholders", true)) {
            for (var entry : PlaceholderRegistry.PLACEHOLDERS.entrySet()) {
                String key = "{" + entry.getKey() + "}";
                if (editedMessage.contains(key)) {
                    editedMessage = editedMessage.replace(key, entry.getValue().get());
                }
            }

            // PLACEHOLDERS - shortcuts
            if (ConfigManager.configs.config.modules.getOrDefault("shortcuts", true)) {
                for (var shortcut : ConfigManager.configs.shortcuts) {
                    if (shortcut.placeholding && shortcut.enabled) {
                        String key = "{" + shortcut.placeholder + "}";
                        if (editedMessage.contains(key)) {
                            editedMessage = editedMessage.replace(key, shortcut.shortcut);
                        }
                    }
                }
            }
        }

        return editedMessage;
    }
}