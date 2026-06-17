package link.sakkit.sakchat.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class SakChatConfig {

    public List<TriggerEntry> triggers;

    public ConfigEntry config;

    public List<ShortcutsEntry> shortcuts;

    public transient Map<String, Pattern> compiledTriggers;

    public SakChatConfig() {
        this.triggers = new ArrayList<>();
        this.config = new ConfigEntry();
        this.shortcuts = new ArrayList<>();
    }
}
