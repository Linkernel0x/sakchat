package link.sakkit.sakchat.config;

import java.util.HashMap;
import java.util.Map;

public class SakStatus {
    public boolean disabled = false;
    public Map<String, Boolean> modes = new HashMap<>();
    public Map<String, Boolean> modules = new HashMap<>();
    public boolean updatesCheck = true;
    public boolean devMode = false;
    public boolean debugMode = false;
    public SakStatus() {
        modes.put("triggerlist", true);
        modes.put("blacklist", true);
        modes.put("shortcuts", true);
        modules.put("anti-doxxing", false);
        modules.put("record-chat", true);
        modules.put("mute-chat", false);
        modules.put("streamer-mode", false);
        modules.put("placeholders", true);
        modules.put("anti-leak", true);
    }
}
