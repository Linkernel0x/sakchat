package link.sakkit.sakchat.ui;

import link.sakkit.sakchat.config.ConfigManager;
import link.sakkit.sakchat.config.ShortcutsEntry;
import link.sakkit.sakchat.config.TriggerEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.ModifierKeyCode;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

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
                                ConfigManager.configs.config.disabled
                        )
                        .setDefaultValue(false)
                        .setTooltip(Component.translatable("sakchat.config.general.disabled.tooltip"))
                        .setSaveConsumer(v -> ConfigManager.configs.config.disabled = v)
                        .build()
        );

        general.addEntry(
                eb.startBooleanToggle(
                                Component.translatable("sakchat.config.general.check_updates"),
                                ConfigManager.configs.config.updatesCheck
                        )
                        .setDefaultValue(true)
                        .setSaveConsumer(v -> ConfigManager.configs.config.updatesCheck = v)
                        .build()
        );

        if (ConfigManager.configs.config.devMode) {
            general.addEntry(
                    eb.startBooleanToggle(
                                    Component.translatable("sakchat.config.general.debug"),
                                    ConfigManager.configs.config.debugMode
                            )
                            .setDefaultValue(false)
                            .setSaveConsumer(v -> ConfigManager.configs.config.debugMode = v)
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
                                ConfigManager.configs.config.modes.getOrDefault("triggers", true)
                        )
                        .setDefaultValue(true)
                        .setSaveConsumer(v -> ConfigManager.configs.config.modes.put("triggers", v))
                        .build()
        );

        modules.addEntry(
                eb.startBooleanToggle(
                                Component.translatable("sakchat.config.modules.blacklist"),
                                ConfigManager.configs.config.modes.getOrDefault("blacklist", true)
                        )
                        .setDefaultValue(true)
                        .setSaveConsumer(v -> ConfigManager.configs.config.modes.put("blacklist", v))
                        .build()
        );

        modules.addEntry(
                eb.startBooleanToggle(
                                Component.translatable("sakchat.config.modules.shortcuts"),
                                ConfigManager.configs.config.modes.getOrDefault("shortcuts", true)
                        )
                        .setDefaultValue(true)
                        .setSaveConsumer(v -> ConfigManager.configs.config.modes.put("shortcuts", v))
                        .build()
        );

        modules.addEntry(
                eb.startBooleanToggle(
                                Component.translatable("sakchat.config.modules.record-chat"),
                                ConfigManager.configs.config.modules.getOrDefault("record-chat", true)
                        )
                        .setDefaultValue(true)
                        .setTooltip(Component.translatable("sakchat.config.modules.record-chat.tooltip"))
                        .setSaveConsumer(v -> ConfigManager.configs.config.modules.put("record-chat", v))
                        .build()
        );



        modules.addEntry(
                eb.startBooleanToggle(
                            Component.translatable("sakchat.config.modules.anti-doxxing"),
                            ConfigManager.configs.config.modules.getOrDefault("anti-doxxing", false)
                        )
                        .setDefaultValue(false)
                        .setTooltip(Component.translatable("sakchat.config.modules.anti-doxxing.tooltip"))
                        .setSaveConsumer(v -> ConfigManager.configs.config.modules.put("anti-doxxing", v))
                        .build()
        );

        modules.addEntry(
                eb.startBooleanToggle(
                                Component.translatable("sakchat.config.modules.anti-leak"),
                                ConfigManager.configs.config.modules.getOrDefault("anti-leak", true)
                        )
                        .setDefaultValue(true)
                        .setTooltip(Component.translatable("sakchat.config.modules.anti-leak.tooltip"))
                        .setSaveConsumer(v -> ConfigManager.configs.config.modules.put("anti-leak", v))
                        .build()
        );

        modules.addEntry(
                eb.startBooleanToggle(
                                Component.translatable("sakchat.config.modules.streamer-mode"),
                                ConfigManager.configs.config.modules.getOrDefault("streamer-mode", false)
                        )
                        .setDefaultValue(false)
                        .setTooltip(Component.translatable("sakchat.config.modules.streamer-mode.tooltip"))
                        .setSaveConsumer(v -> ConfigManager.configs.config.modules.put("streamer-mode", v))
                        .build()
        );

        modules.addEntry(
                eb.startBooleanToggle(
                                Component.translatable("sakchat.config.modules.mute-chat"),
                                ConfigManager.configs.config.modules.getOrDefault("mute-chat", false)
                        )
                        .setDefaultValue(false)
                        .setTooltip(Component.translatable("sakchat.config.modules.mute-chat.tooltip"))
                        .setSaveConsumer(v -> ConfigManager.configs.config.modules.put("mute-chat", v))
                        .build()
        );

        modules.addEntry(
                eb.startBooleanToggle(
                                Component.translatable("sakchat.config.modules.placeholders"),
                                ConfigManager.configs.config.modules.getOrDefault("placeholders", true)
                        )
                        .setDefaultValue(true)
                        .setTooltip(Component.translatable("sakchat.config.modules.placeholders.tooltip"))
                        .setSaveConsumer(v -> ConfigManager.configs.config.modules.put("placeholders", v))
                        .build()
        );

        modules.addEntry(
                eb.startBooleanToggle(
                                Component.translatable("sakchat.config.modules.translations"),
                                ConfigManager.configs.config.modules.getOrDefault("translations", false)
                        )
                        .setDefaultValue(false)
                        .setSaveConsumer(v -> ConfigManager.configs.config.modules.put("translations", v))
                        .build()
        );


        SubCategoryBuilder language = eb.startSubCategory(
                Component.translatable("sakchat.config.modules.translations")
        );

        language.add(
                eb.startStrField(
                                Component.translatable("sakchat.config.translations.lang"),
                                ConfigManager.configs.config.translations.lang
                        )
                        .setSaveConsumer(v -> ConfigManager.configs.config.translations.lang = v)
                        .build()
        );

        String[] apiProviders = new String[]{"GOOGLE", "CUSTOM", "CUSTOM+GOOGLE"};

        language.add(
                eb.startSelector(
                                Component.translatable("sakchat.config.translations.provider"),
                                apiProviders,
                                ConfigManager.configs.config.translations.apiProvider
                        )
                        .setDefaultValue("GOOGLE")
                        .setSaveConsumer(newValue -> ConfigManager.configs.config.translations.apiProvider = newValue)
                        .build()
        );

        language.add(
                eb.startStrField(
                                Component.translatable("sakchat.config.translations.custom"),
                                ConfigManager.configs.config.translations.custom
                        )
                        .setSaveConsumer(v -> ConfigManager.configs.config.translations.custom = v)
                        .setTooltip(Component.translatable("sakchat.config.translations.custom.tooltip"))
                        .build()
        );

        String[] apiType = new String[]{"TXT", "JSON"};

        language.add(
                eb.startSelector(
                                Component.translatable("sakchat.config.translations.customType"),
                                apiType,
                                ConfigManager.configs.config.translations.customType
                        )
                        .setDefaultValue("JSON")
                        .setSaveConsumer(newValue -> ConfigManager.configs.config.translations.customType = newValue)
                        .build()
        );

        language.add(
                eb.startStrField(
                                Component.translatable("sakchat.config.translations.customKey"),
                                ConfigManager.configs.config.translations.customKey
                        )
                        .setSaveConsumer(v -> ConfigManager.configs.config.translations.customKey = v)
                        .build()
        );

        modules.addEntry(language.build());

    }

    // ------------------------------------------------------------
    // TRIGGERS
    // ------------------------------------------------------------
    private static void addTriggersCategory(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(
                Component.translatable("sakchat.config.category.triggers_list")
        );

        category.addEntry(
                eb.startBooleanToggle(Component.translatable("sakchat.config.entry.add_trigger"), false)
                        .setDefaultValue(false)
                        .setTooltip(Component.translatable("sakchat.config.reloadRequired"))
                        .setSaveConsumer(v -> {
                            if (v) {
                                TriggerEntry t = new TriggerEntry();
                                t.id = "trigger_" + System.currentTimeMillis();
                                ConfigManager.configs.triggers.add(t);
                            }
                        })
                        .build()
        );

        for (TriggerEntry trigger : ConfigManager.configs.triggers) {

            SubCategoryBuilder sub = eb.startSubCategory(
                    Component.literal("ID: " + trigger.id).withColor(parseHex("93C0A0"))
            );

            sub.add(
                    eb.startBooleanToggle(Component.translatable("sakchat.config.entry.remove_trigger"), false)
                            .setDefaultValue(false)
                            .setTooltip(Component.translatable("sakchat.config.reloadRequired"))
                            .setSaveConsumer(v -> {
                                if (v) {
                                    ConfigManager.configs.triggers.remove(trigger);
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
                            .setDefaultValue(true)
                            .setSaveConsumer(v -> trigger.enabled = v)
                            .build()
            );

            final boolean[] liveRegexState = new boolean[1];

            sub.add(
                    eb.startBooleanToggle(
                                    Component.translatable("sakchat.config.entry.use_regex"),
                                    trigger.regex
                            )
                            .setSaveConsumer(v -> trigger.regex = v)
                            .setDefaultValue(false)
                            .setTooltip(Component.translatable("sakchat.config.entry.use_regex.tootltip"))
                            .setYesNoTextSupplier(isTrue -> {
                                liveRegexState[0] = isTrue;
                                return isTrue ? Component.translatable("text.cloth-config.boolean.value.true")
                                        : Component.translatable("text.cloth-config.boolean.value.false");
                            })
                            .build()
            );

            // Pattern
            sub.add(
                    eb.startStrField(
                                    Component.translatable("sakchat.config.entry.pattern"),
                                    trigger.pattern
                            )
                            .setErrorSupplier(val -> liveRegexState[0] ? validateRegex(val) : Optional.empty())
                            .setSaveConsumer(v -> trigger.pattern = v)
                            .build()
            );

            // Cooldown
            sub.add(
                    eb.startLongSlider(
                                    Component.translatable("sakchat.config.entry.cooldown"),
                                    trigger.cooldownMs / 100,
                                    0,
                                    30000
                            )
                            .setDefaultValue(1)
                            .setTooltip(Component.translatable("sakchat.config.entry.cooldown.tooltip").withStyle(ChatFormatting.RED))
                            .setSaveConsumer(v -> trigger.cooldownMs = v * 100)
                            .build()
            );

            // Sound toggle
            sub.add(
                    eb.startBooleanToggle(
                                    Component.translatable("sakchat.config.entry.sound_enabled"),
                                    trigger.actions.sound
                            )
                            .setDefaultValue(true)
                            .setSaveConsumer(v -> trigger.actions.sound = v)
                            .build()
            );

            sub.add(
                    eb.startDropdownMenu(
                                    Component.translatable("sakchat.config.entry.sound_id"),
                                    DropdownMenuBuilder.TopCellElementBuilder.of(
                                            trigger.sound.id,
                                            stringValue -> stringValue
                                    )
                            )
                            .setSelections(BuiltInRegistries.SOUND_EVENT.keySet().stream()
                                    .map(Identifier::toString)
                                    .sorted()
                                    .toList())
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

            // Force Color
            sub.add(
                    eb.startBooleanToggle(
                                    Component.translatable("sakchat.config.entry.highlight.forceColor"),
                                    trigger.appearance.forceColor
                            )
                            .setSaveConsumer(v -> trigger.appearance.forceColor = v)
                            .setTooltip(Component.translatable("sakchat.config.entry.replacement.warning").withStyle(ChatFormatting.RED))
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
                                    trigger.shortcut.params
                            )
                            .setSaveConsumer(v -> trigger.shortcut.params = v)
                            .build()
            );

            sub.add(
                    eb.startBooleanToggle(
                                    Component.translatable( "sakchat.config.modules.blacklist"),
                                    trigger.actions.blacklist
                            )
                            .setSaveConsumer(v -> trigger.actions.blacklist = v)
                            .setTooltip(Component.translatable("sakchat.config.entry.blacklist.tooltip").append(Component.translatable("sakchat.config.entry.replacement.warning").withStyle(ChatFormatting.RED)))
                            .build()
            );

            sub.add(
                    eb.startBooleanToggle(
                                    Component.translatable( "sakchat.config.entry.blacklist.aggressive"),
                                    trigger.blacklist.aggressive
                            )
                            .setSaveConsumer(v -> trigger.blacklist.aggressive = v)
                            .setTooltip(Component.translatable("sakchat.config.entry.blacklist.aggressive.tooltip"))
                            .build()
            );

            sub.add(
                    eb.startStrField(
                                    Component.translatable("sakchat.config.entry.replacement"),
                                    trigger.blacklist.replacement

                            )
                            .setSaveConsumer(v -> trigger.blacklist.replacement = v)
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

        category.addEntry(
                eb.startBooleanToggle(Component.translatable("sakchat.config.entry.add_shortcut"), false)
                        .setDefaultValue(false)
                        .setTooltip(Component.translatable("sakchat.config.reloadRequired"))
                        .setSaveConsumer(v -> {
                            if (v) {
                                ShortcutsEntry b = new ShortcutsEntry();
                                b.id = "shortcut_" + System.currentTimeMillis();
                                ConfigManager.configs.shortcuts.add(b);
                            }
                        })
                        .build()
        );

        for (ShortcutsEntry entry : ConfigManager.configs.shortcuts) {

            entry.prepareForGui();

            SubCategoryBuilder sub = eb.startSubCategory(
                    Component.literal("ID: " + entry.id)
            );

            sub.add(
                    eb.startBooleanToggle(Component.translatable("sakchat.config.entry.remove_shortcut"), false)
                            .setTooltip(Component.translatable("sakchat.config.reloadRequired"))
                            .setDefaultValue(false)
                            .setSaveConsumer(v -> {
                                if (v) {
                                    ConfigManager.configs.shortcuts.remove(entry);
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

            sub.add(
                    eb.startBooleanToggle(
                                    Component.translatable("sakchat.config.entry.enabled"),
                                    entry.enabled
                            )
                            .setSaveConsumer(v -> entry.enabled = v)
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
                            .setTooltip()
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

            // bound key
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

    public static int parseHex(String hex) {
        try {
            return Integer.parseInt(hex.replace("#", ""), 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }
}
