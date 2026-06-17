package link.sakkit.sakchat.config;

import java.util.HashMap;
import java.util.Map;

public class ConfigEntry {
    public boolean disabled = false;
    public Map<String, Boolean> modes = new HashMap<>();
    public Map<String, Boolean> modules = new HashMap<>();
    public boolean updatesCheck = true;
    public boolean devMode = false;
    public boolean debugMode = false;

    public Translations translations = new Translations();

    public static class Translations {
        public String lang = "";
        public String apiProvider = "";
        public String custom = "";
        public String customType = "";
        public String customKey = "";
    }
    public ConfigEntry() {
        modes.put("triggers", true);
        modes.put("blacklist", true);
        modes.put("shortcuts", true);
        modules.put("anti-doxxing", false);
        modules.put("record-chat", true);
        modules.put("mute-chat", false);
        modules.put("streamer-mode", false);
        modules.put("placeholders", true);
        modules.put("anti-leak", true);
        modules.put("translations", false);
    }
}
