package link.sakkit.sakchat.config;

import com.mojang.blaze3d.platform.InputConstants;
import me.shedaniel.clothconfig2.api.ModifierKeyCode;
import me.shedaniel.clothconfig2.api.Modifier;

public class ShortcutsEntry {
    public String id;
    public boolean enabled;
    public String shortcut;
    public boolean isCommand;
    public boolean placeholding;
    public String placeholder;
    public boolean keyBinding;
    
    public String key;
    
    public transient ModifierKeyCode clothKey;

    public ShortcutsEntry() {
        this.id = "";
        this.enabled = true;
        this.shortcut = "";
        this.isCommand = false;
        this.placeholding = false;
        this.placeholder = "";

        this.keyBinding = false;
        
        this.key = "key.keyboard.unknown";
        this.clothKey = ModifierKeyCode.unknown();
    }

    public void prepareForGui() {
        InputConstants.Key mojangKey = InputConstants.getKey(this.key);
        this.clothKey = ModifierKeyCode.of(mojangKey, Modifier.none());
    }

    public void saveFromGui(ModifierKeyCode newValue) {
        this.clothKey = newValue;
        if (newValue != null && newValue.getKeyCode() != null) {
            this.key = newValue.getKeyCode().getName();
        }
    }
}
