package link.sakkit.sakchat.config;

public class BlacklistEntry {
    public String id;
    public String pattern;
    public boolean regex;
    public boolean caseSensitive;
    public String replacement;

    public BlacklistEntry() {
        this.id = "";
        this.pattern = "";
        this.regex = false;
        this.caseSensitive = false;
        this.replacement = "***";
    }
}
