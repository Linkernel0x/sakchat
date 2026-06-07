package link.sakkit.sakchat.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class SakConfig {

    public List<TriggerEntry> triggers = new ArrayList<>();

    public List<BlacklistEntry> blacklist = new ArrayList<>();

    public SakStatus status = new SakStatus();

    public List<ShortcutsEntry> shortcuts = new ArrayList<>();

    public List<Map.Entry<Pattern, String>> compiledBlacklist;
    public Map<String, Pattern> compiledTriggers;

    public SakConfig() {
        this.triggers = new ArrayList<>();
        this.blacklist = new ArrayList<>();
        this.status = new SakStatus();
        this.shortcuts = new ArrayList<>();
    }
}
