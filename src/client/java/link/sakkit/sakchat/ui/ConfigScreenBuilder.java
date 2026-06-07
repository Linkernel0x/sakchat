package link.sakkit.sakchat.ui;

import link.sakkit.sakchat.config.BlacklistEntry;
import link.sakkit.sakchat.config.ConfigManager;
import link.sakkit.sakchat.config.ShortcutsEntry;
import link.sakkit.sakchat.config.TriggerEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.ModifierKeyCode;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ConfigScreenBuilder {

    public static Screen build(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("sakchat.config.title"))
                .setSavingRunnable(ConfigManager::saveConfig);

        ConfigEntryBuilder eb = builder.entryBuilder();

        addGeneralCategory(builder, eb);
        addModulesCategory(builder, eb);
        addTriggersCategory(builder, eb);
        addBlacklistCategory(builder, eb);
        addShortcutsCategory(builder, eb);

        return builder.build();
    }

    // ------------------------------------------------------------
    // GENERAL
    // ------------------------------------------------------------
    private static void addGeneralCategory(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory general = builder.getOrCreateCategory(
                Component.translatable("sakchat.config.category.general")
        );

        general.addEntry(
                eb.startBooleanToggle(
                                Component.translatable("sakchat.config.general.disabled"),
                                ConfigManager.config.status.disabled
                        )
                        .setDefaultValue(false)
                        .setTooltip(Component.translatable("sakchat.config.general.disabled.tooltip"))
                        .setSaveConsumer(v -> ConfigManager.config.status.disabled = v)
                        .build()
        );

        general.addEntry(
                eb.startBooleanToggle(
                                Component.translatable("sakchat.config.general.check_updates"),
                                ConfigManager.config.status.updatesCheck
                        )
                        .setDefaultValue(true)
                        .setSaveConsumer(v -> ConfigManager.config.status.updatesCheck = v)
                        .build()
        );

        if (ConfigManager.config.status.devMode) {
            general.addEntry(
                    eb.startBooleanToggle(
                                    Component.translatable("sakchat.config.general.debug"),
                                    ConfigManager.config.status.debugMode
                            )
                            .setDefaultValue(false)
                            .setSaveConsumer(v -> ConfigManager.config.status.debugMode = v)
                            .build()
            );
        }
    }

    // ------------------------------------------------------------
    // MODULES
    // ------------------------------------------------------------
    private static void addModulesCategory(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory modules = builder.getOrCreateCategory(
                Component.translatable("sakchat.config.category.modules")
        );

        modules.addEntry(
                eb.startBooleanToggle(
                                Component.translatable("sakchat.config.modules.triggers"),
                                ConfigManager.config.status.modes.getOrDefault("triggerlist", true)
                        )
                        .setDefaultValue(true)
                        .setSaveConsumer(v -> ConfigManager.config.status.modes.put("triggerlist", v))
                        .build()
        );

        modules.addEntry(
                eb.startBooleanToggle(
                                Component.translatable("sakchat.config.modules.blacklist"),
                                ConfigManager.config.status.modes.getOrDefault("blacklist", true)
                        )
                        .setDefaultValue(true)
                        .setSaveConsumer(v -> ConfigManager.config.status.modes.put("blacklist", v))
                        .build()
        );

        modules.addEntry(
                eb.startBooleanToggle(
                                Component.translatable("sakchat.config.modules.shortcuts"),
                                ConfigManager.config.status.modes.getOrDefault("shortcuts", true)
                        )
                        .setDefaultValue(true)
                        .setSaveConsumer(v -> ConfigManager.config.status.modes.put("shortcuts", v))
                        .build()
        );

        modules.addEntry(
                eb.startBooleanToggle(
                            Component.translatable("sakchat.config.modules.anti-doxxing"),
                            ConfigManager.config.status.modules.getOrDefault("anti-doxxing", false)
                        )
                        .setDefaultValue(false)
                        .setTooltip(Component.translatable("sakchat.config.modules.anti-doxxing.tooltip"))
                        .setSaveConsumer(v -> ConfigManager.config.status.modules.put("anti-doxxing", v))
                        .build()
        );

        modules.addEntry(
                eb.startBooleanToggle(
                                Component.translatable("sakchat.config.modules.anti-leak"),
                                ConfigManager.config.status.modules.getOrDefault("anti-leak", true)
                        )
                        .setDefaultValue(true)
                        .setTooltip(Component.translatable("sakchat.config.modules.anti-leak.tooltip"))
                        .setSaveConsumer(v -> ConfigManager.config.status.modules.put("anti-leak", v))
                        .build()
        );

        modules.addEntry(
                eb.startBooleanToggle(
                                Component.translatable("sakchat.config.modules.streamer-mode"),
                                ConfigManager.config.status.modules.getOrDefault("streamer-mode", false)
                        )
                        .setDefaultValue(false)
                        .setTooltip(Component.translatable("sakchat.config.modules.streamer-mode.tooltip"))
                        .setSaveConsumer(v -> ConfigManager.config.status.modules.put("streamer-mode", v))
                        .build()
        );

        modules.addEntry(
                eb.startBooleanToggle(
                                Component.translatable("sakchat.config.modules.record-chat"),
                                ConfigManager.config.status.modules.getOrDefault("record-chat", false)
                        )
                        .setDefaultValue(false)
                        .setTooltip(Component.translatable("sakchat.config.modules.record-chat.tooltip"))
                        .setSaveConsumer(v -> ConfigManager.config.status.modules.put("record-chat", v))
                        .build()
        );

        modules.addEntry(
                eb.startBooleanToggle(
                                Component.translatable("sakchat.config.modules.mute-chat"),
                                ConfigManager.config.status.modules.getOrDefault("mute-chat", false)
                        )
                        .setDefaultValue(false)
                        .setTooltip(Component.translatable("sakchat.config.modules.mute-chat.tooltip"))
                        .setSaveConsumer(v -> ConfigManager.config.status.modules.put("mute-chat", v))
                        .build()
        );

        modules.addEntry(
                eb.startBooleanToggle(
                                Component.translatable("sakchat.config.modules.placeholders"),
                                ConfigManager.config.status.modules.getOrDefault("placeholders", false)
                        )
                        .setDefaultValue(false)
                        .setTooltip(Component.translatable("sakchat.config.modules.placeholders.tooltip"))
                        .setSaveConsumer(v -> ConfigManager.config.status.modules.put("placeholders", v))
                        .build()
        );
    }

    // ------------------------------------------------------------
    // TRIGGERS
    // ------------------------------------------------------------
    private static void addTriggersCategory(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(
                Component.translatable("sakchat.config.category.triggers_list")
        );

        // -------------------------
        // Pulsante AGGIUNGI
        // -------------------------
        category.addEntry(
                eb.startBooleanToggle(Component.translatable("sakchat.config.entry.add_trigger"), false)
                        .setDefaultValue(false)
                        .setTooltip(Component.translatable("sakchat.config.reloadRequired"))
                        .setSaveConsumer(v -> {
                            if (v) {
                                TriggerEntry t = new TriggerEntry();
                                t.id = "trigger_" + System.currentTimeMillis();
                                ConfigManager.config.triggers.add(t);
                            }
                        })
                        .build()
        );

        // -------------------------
        // Lista trigger esistenti
        // -------------------------
        for (TriggerEntry trigger : ConfigManager.config.triggers) {

            SubCategoryBuilder sub = eb.startSubCategory(
                    Component.literal("ID: " + trigger.id)
            );

            // Pulsante ELIMINA
            sub.add(
                    eb.startBooleanToggle(Component.translatable("sakchat.config.entry.remove_trigger"), false)
                            .setDefaultValue(false)
                            .setTooltip(Component.translatable("sakchat.config.reloadRequired"))
                            .setSaveConsumer(v -> {
                                if (v) {
                                    ConfigManager.config.triggers.remove(trigger);
                                }
                            })
                            .build()
            );

            // ID
            sub.add(
                    eb.startStrField(
                                    Component.translatable("sakchat.config.entry.id"),
                                    trigger.id
                            )
                            .setSaveConsumer(v -> trigger.id = v)
                            .build()
            );

            // Enabled
            sub.add(
                    eb.startBooleanToggle(
                                    Component.translatable("sakchat.config.entry.enabled"),
                                    trigger.enabled
                            )
                            .setSaveConsumer(v -> trigger.enabled = v)
                            .build()
            );

            // Pattern
            sub.add(
                    eb.startStrField(
                                    Component.translatable("sakchat.config.entry.pattern"),
                                    trigger.pattern
                            )
                            .setErrorSupplier(val -> trigger.regex ? validateRegex(val) : Optional.empty())
                            .setSaveConsumer(v -> trigger.pattern = v)
                            .build()
            );

            // Regex toggle
            sub.add(
                    eb.startBooleanToggle(
                                    Component.translatable("sakchat.config.entry.use_regex"),
                                    trigger.regex
                            )
                            .setSaveConsumer(v -> trigger.regex = v)
                            .build()
            );

            // Cooldown
            sub.add(
                    eb.startLongSlider(
                                    Component.translatable("sakchat.config.entry.cooldown"),
                                    trigger.cooldownMs,
                                    0,
                                    300000
                            )
                            .setDefaultValue(1000)
                            .setSaveConsumer(v -> trigger.cooldownMs = v)
                            .build()
            );

            // Sound toggle
            sub.add(
                    eb.startBooleanToggle(
                                    Component.translatable("sakchat.config.entry.sound_enabled"),
                                    trigger.actions.sound
                            )
                            .setSaveConsumer(v -> trigger.actions.sound = v)
                            .build()
            );

            // Sound ID
            sub.add(
                    eb.startStrField(
                                    Component.translatable("sakchat.config.entry.sound_id"),
                                    trigger.sound.id
                            )
                            .setSaveConsumer(v -> trigger.sound.id = v)
                            .build()
            );

            // Volume
            sub.add(
                    eb.startFloatField(
                                    Component.translatable("sakchat.config.entry.volume"),
                                    trigger.sound.volume
                            )
                            .setMin(0f).setMax(1f)
                            .setSaveConsumer(v -> trigger.sound.volume = v)
                            .build()
            );

            // Pitch
            sub.add(
                    eb.startFloatField(
                                    Component.translatable("sakchat.config.entry.pitch"),
                                    trigger.sound.pitch
                            )
                            .setMin(0f).setMax(2f)
                            .setSaveConsumer(v -> trigger.sound.pitch = v)
                            .build()
            );

            // HUD Popup
            sub.add(
                    eb.startBooleanToggle(
                                    Component.translatable("sakchat.config.entry.hudPopup_enabled"),
                                    trigger.actions.hudPopup
                            )
                            .setSaveConsumer(v -> trigger.actions.hudPopup = v)
                            .build()
            );

            sub.add(
                    eb.startBooleanToggle(
                                    Component.translatable("sakchat.config.entry.toast_enabled"),
                                    trigger.actions.toast
                            )
                            .setSaveConsumer(v -> trigger.actions.toast = v)
                            .build()
            );

            // Highlight toggle
            sub.add(
                    eb.startBooleanToggle(
                                    Component.translatable("sakchat.config.entry.highlight_enabled"),
                                    trigger.actions.highlight
                            )
                            .setSaveConsumer(v -> trigger.actions.highlight = v)
                            .build()
            );

            // Color picker
            int colorInt = parseHex(trigger.appearance.highlightColor);
            sub.add(
                    eb.startColorField(
                                    Component.translatable("sakchat.config.entry.color"),
                                    colorInt
                            )
                            .setSaveConsumer(v -> trigger.appearance.highlightColor = String.format("#%06X", (0xFFFFFF & v)))
                            .build()
            );

            sub.add(
                    eb.startBooleanToggle(
                            Component.translatable( "sakchat.config.modules.shortcuts"),
                            trigger.actions.shortcut
                            )
                            .setSaveConsumer(v -> trigger.actions.shortcut = v)
                            .build()
            );

            sub.add(
                    eb.startStrField(
                                    Component.translatable( "sakchat.config.shortcut.id"),
                                    trigger.shortcut.id
                            )
                            .setSaveConsumer(v -> trigger.shortcut.id = v)
                            .build()
            );

            sub.add(
                    eb.startStrField(
                                    Component.translatable( "sakchat.config.entry.params"),
                                    trigger.shortcut.id
                            )
                            .setSaveConsumer(v -> trigger.shortcut.id = v)
                            .build()
            );

            category.addEntry(sub.build());
        }
    }

    // ------------------------------------------------------------
    // BLACKLIST
    // ------------------------------------------------------------
    private static void addShortcutsCategory(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(
                Component.translatable("sakchat.config.category.shortcuts_list")
        );

        // -------------------------
        // Pulsante AGGIUNGI
        // -------------------------
        category.addEntry(
                eb.startBooleanToggle(Component.translatable("sakchat.config.entry.add_shortcut"), false)
                        .setDefaultValue(false)
                        .setTooltip(Component.translatable("sakchat.config.reloadRequired"))
                        .setSaveConsumer(v -> {
                            if (v) {
                                ShortcutsEntry b = new ShortcutsEntry();
                                b.id = "shortcut_" + System.currentTimeMillis();
                                ConfigManager.config.shortcuts.add(b);
                            }
                        })
                        .build()
        );

        // -------------------------
        // Lista blacklist
        // -------------------------
        for (ShortcutsEntry entry : ConfigManager.config.shortcuts) {

            entry.prepareForGui();

            SubCategoryBuilder sub = eb.startSubCategory(
                    Component.literal("ID: " + entry.id)
            );

            // Pulsante ELIMINA
            sub.add(
                    eb.startBooleanToggle(Component.translatable("sakchat.config.entry.remove_shortcut"), false)
                            .setTooltip(Component.translatable("sakchat.config.reloadRequired"))
                            .setDefaultValue(false)
                            .setSaveConsumer(v -> {
                                if (v) {
                                    ConfigManager.config.shortcuts.remove(entry);
                                }
                            })
                            .build()
            );

            // ID
            sub.add(
                    eb.startStrField(
                                    Component.translatable("sakchat.config.shortcut.id"),
                                    entry.id
                            )
                            .setSaveConsumer(v -> entry.id = v)
                            .setErrorSupplier(string -> {
                                if (string.contains(" ") || string.contains("\"")) {
                                    return Optional.of(Component.translatable("sakchat.config.notAllowedChar"));
                                }
                                return Optional.empty();
                            })
                            .build()
            );

            // Pattern
            sub.add(
                    eb.startStrField(
                                    Component.translatable("sakchat.config.entry.shortcut"),
                                    entry.shortcut
                            )
                            .setSaveConsumer(v -> entry.shortcut = v)
                            .setTooltip(Component.translatable("sakchat.config.entry.shortcut.tooltip"))
                            .build()
            );

            // isCommand
            sub.add(
                    eb.startBooleanToggle(
                                    Component.translatable("sakchat.config.entry.isCommand"),
                                    entry.isCommand
                            )
                            .setSaveConsumer(v -> entry.isCommand = v)
                            .build()
            );

            // placeholding
            sub.add(
                    eb.startBooleanToggle(
                                    Component.translatable("sakchat.config.entry.placeholding"),
                                    entry.placeholding
                            )
                            .setSaveConsumer(v -> entry.placeholding = v)
                            .build()
            );

            // keyBinding
            sub.add(
                    eb.startBooleanToggle(
                                    Component.translatable("sakchat.config.entry.keyBinding"),
                                    entry.keyBinding
                            )
                            .setSaveConsumer(v -> entry.keyBinding = v)
                            .build()
            );

            // binded key
            sub.add(
                    eb.startModifierKeyCodeField(
                                    Component.translatable("sakchat.config.entry.key"),
                                    entry.clothKey
                            )
                            .setDefaultValue(ModifierKeyCode.unknown())
                            .setModifierSaveConsumer(entry::saveFromGui)
                            .build()
            );

            category.addEntry(sub.build());
        }
    }

    private static void addBlacklistCategory(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(
                Component.translatable("sakchat.config.category.blacklist_list")
        );

        // -------------------------
        // Pulsante AGGIUNGI
        // -------------------------
        category.addEntry(
                eb.startBooleanToggle(Component.translatable("sakchat.config.entry.add_blacklist"), false)
                        .setDefaultValue(false)
                        .setTooltip(Component.translatable("sakchat.config.reloadRequired"))
                        .setSaveConsumer(v -> {
                            if (v) {
                                BlacklistEntry b = new BlacklistEntry();
                                b.id = "blacklist_" + System.currentTimeMillis();
                                b.pattern = "";
                                b.replacement = "***";
                                ConfigManager.config.blacklist.add(b);
                            }
                        })
                        .build()
        );

        // -------------------------
        // Lista blacklist
        // -------------------------
        for (BlacklistEntry entry : ConfigManager.config.blacklist) {

            SubCategoryBuilder sub = eb.startSubCategory(
                    Component.literal("ID: " + entry.id)
            );

            // Pulsante ELIMINA
            sub.add(
                    eb.startBooleanToggle(Component.translatable("sakchat.config.entry.remove_blacklist"), false)
                            .setDefaultValue(false)
                            .setTooltip(Component.translatable("sakchat.config.reloadRequired"))
                            .setSaveConsumer(v -> {
                                if (v) {
                                    ConfigManager.config.blacklist.remove(entry);
                                }
                            })
                            .build()
            );

            // ID
            sub.add(
                    eb.startStrField(
                                    Component.translatable("sakchat.config.entry.id"),
                                    entry.id
                            )
                            .setSaveConsumer(v -> entry.id = v)
                            .build()
            );

            // Pattern
            sub.add(
                    eb.startStrField(
                                    Component.translatable("sakchat.config.entry.pattern"),
                                    entry.pattern
                            )
                            .setErrorSupplier(ConfigScreenBuilder::validateRegex)
                            .setSaveConsumer(v -> entry.pattern = v)
                            .build()
            );

            // Replacement
            sub.add(
                    eb.startStrField(
                                    Component.translatable("sakchat.config.entry.replacement"),
                                    entry.replacement
                            )
                            .setSaveConsumer(v -> entry.replacement = v)
                            .build()
            );

            category.addEntry(sub.build());
        }
    }

    // ------------------------------------------------------------
    // UTILS
    // ------------------------------------------------------------
    private static Optional<Component> validateRegex(String regex) {
        try {
            Pattern.compile(regex);
            return Optional.empty();
        } catch (PatternSyntaxException e) {
            return Optional.of(Component.translatable("sakchat.config.error.invalid_regex"));
        }
    }

    private static int parseHex(String hex) {
        try {
            return Integer.parseInt(hex.replace("#", ""), 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }
}
