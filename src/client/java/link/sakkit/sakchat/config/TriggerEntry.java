package link.sakkit.sakchat.config;

import java.util.regex.Pattern;

public class TriggerEntry {
    //private static final Pattern HEX_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$");

    public String id;
    public boolean enabled;
    public String pattern;
    public boolean regex;
    public boolean caseSensitive;
    public long cooldownMs;

    public ActionsConfig actions;
    public AppearanceConfig appearance;
    public SoundConfig sound;
    public ShortcutConfig shortcut;

    public TriggerEntry() {
        this.id = "";
        this.enabled = true;
        this.pattern = "";
        this.regex = false;
        this.caseSensitive = false;
        this.cooldownMs = 1000;

        this.actions = new ActionsConfig();
        this.appearance = new AppearanceConfig();
        this.sound = new SoundConfig();
        this.shortcut = new ShortcutConfig();
    }

    /*public void ensureDefaults() {
        if (this.id == null) {
            this.id = "trigger_" + System.currentTimeMillis(); // ID di fallback se manca
        }

        if (this.pattern == null) this.pattern = "";

        if (this.cooldownMs < 0) this.cooldownMs = 1000;

        if (this.helpers == null) this.helpers = new ActionsConfig();
        if (this.appearance == null) this.appearance = new AppearanceConfig();
        if (this.sound == null) this.sound = new SoundConfig();
        if (this.shortcut == null) this.shortcut = new ShortcutConfig();

        if (this.sound.id == null || this.sound.id.trim().isEmpty()) {
            this.sound.id = "minecraft:entity.experience_orb.pickup";
        }
        if (this.sound.volume < 0 || this.sound.volume > 1) this.sound.volume = 1.0f;
        if (this.sound.pitch < 0 || this.sound.pitch > 2) this.sound.pitch = 1.0f;

        if (this.appearance.highlightColor == null || !HEX_PATTERN.matcher(this.appearance.highlightColor).matches()) {
            this.appearance.highlightColor = "#FFFFFF";
        }

        if (this.appearance.backgroundColor == null || !HEX_PATTERN.matcher(this.appearance.backgroundColor).matches()) {
            this.appearance.backgroundColor = "#00000000";
        }

        if (this.shortcut.id == null) this.shortcut.id = "";
        if (this.shortcut.params == null) this.shortcut.params = "";
    }*/

    public static class ActionsConfig {
        public boolean sound;
        public boolean hudPopup;
        public boolean highlight;
        public boolean toast;
        public boolean shortcut;

        public ActionsConfig() {
            this.sound = false;
            this.hudPopup = false;
            this.highlight = false;
            this.toast = false;
            this.shortcut = false;
        }
    }

    public static class AppearanceConfig {
        public String highlightColor;
        public String backgroundColor;
        public boolean bold;

        public AppearanceConfig() {
            this.highlightColor = "#FFFFFF";
            //this.backgroundColor = "#00000000";
            this.bold = false;
        }
    }

    public static class SoundConfig {
        public String id;
        public float volume;
        public float pitch;

        public SoundConfig() {
            this.id = "minecraft:entity.experience_orb.pickup";
            this.volume = 1.0f;
            this.pitch = 1.0f;
        }
    }

    public static class ShortcutConfig {
        public String id;
        public String params;
        public ShortcutConfig() {
            this.id = "";
            this.params = "";
        }
    }
}