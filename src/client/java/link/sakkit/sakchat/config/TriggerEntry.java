package link.sakkit.sakchat.config;

public class TriggerEntry {
    public String id;
    public boolean enabled;
    public String pattern;
    public boolean regex;
    public boolean caseSensitive;
    public long cooldownMs;

    public ActionsConfig actions;
    public BlacklistConfig blacklist;
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
        this.blacklist = new BlacklistConfig();
        this.appearance = new AppearanceConfig();
        this.sound = new SoundConfig();
        this.shortcut = new ShortcutConfig();
    }

    public static class ActionsConfig {
        public boolean sound;
        public boolean hudPopup;
        public boolean highlight;
        public boolean toast;
        public boolean shortcut;
        public boolean blacklist;

        public ActionsConfig() {
            this.sound = false;
            this.hudPopup = false;
            this.highlight = false;
            this.toast = false;
            this.shortcut = false;
            this.blacklist = false;
        }
    }

    public static class BlacklistConfig {
        public String replacement;
        public boolean aggressive;

        public BlacklistConfig() {
            this.replacement = "";
            this.aggressive = false;
        }
    }

    public static class AppearanceConfig {
        public String highlightColor;
        //public String backgroundColor;
        public boolean forceColor;

        public AppearanceConfig() {
            this.highlightColor = "#FFFFFF";
            //this.backgroundColor = "#00000000";
            this.forceColor = false;
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