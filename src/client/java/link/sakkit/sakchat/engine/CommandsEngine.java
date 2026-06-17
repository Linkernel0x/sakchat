package link.sakkit.sakchat.engine;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import link.sakkit.sakchat.config.*;
import link.sakkit.sakchat.helpers.ChatLogger;
import link.sakkit.sakchat.ui.DisplayerGui;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import link.sakkit.sakchat.ui.ConfigScreenBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CommandsEngine {

    public static void registerAll() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            shortcuts.register(dispatcher);
            SakChatCommand.register(dispatcher);
        });
    }

    private static class shortcuts {

        public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
            dispatcher.register(ClientCommands.literal("sc")
                    .then(ClientCommands.argument("id", StringArgumentType.string())
                            .suggests((context, builder) -> {
                                String input = builder.getRemaining().toLowerCase();
                                for (ShortcutsEntry entry : ConfigManager.configs.shortcuts) {
                                    if (entry.id.toLowerCase().startsWith(input)) {
                                        builder.suggest(entry.id);
                                    }
                                }
                                return builder.buildFuture();
                            })
                            .executes(context -> executeShortcut(context, StringArgumentType.getString(context, "id"), ""))
                            .then(ClientCommands.argument("args", StringArgumentType.greedyString())
                                    .executes(context -> executeShortcut(context, StringArgumentType.getString(context, "id"), StringArgumentType.getString(context, "args")))
                            )
                    )
            );
        }
    }

    //private static final Pattern SC_PATTERN = Pattern.compile("([^\"]\\S*|\"[^\"]*\")\\s*");
    private static final Pattern SC_PATTERN = Pattern.compile("([^\\s\"]+|\"[^\"]*\")\\s*");

    private static int executeShortcut(com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> context, String id, String rawArgs) {
        try {
            ShortcutsEntry shortcut = null;
            for (ShortcutsEntry entry : ConfigManager.configs.shortcuts) {
                if (entry.id.equals(id)) {
                    shortcut = entry;
                    break;
                }
            }

            if (shortcut == null || shortcut.shortcut == null || !shortcut.enabled) {
                ConfigManager.LOGGER.info("Shortcut ID not found or disabled: {}", id);
                Component error = Component.translatable("sakchat.command.failed")
                        .append(Component.literal(": '" + id + "' ").withStyle(ChatFormatting.ITALIC, ChatFormatting.BOLD))
                        .append(Component.translatable("sakchat.command.shortcut.notFound"))
                        .withStyle(ChatFormatting.RED);
                context.getSource().sendFeedback(error);
                return 1;
            }

            List<String> argsList = new ArrayList<>();
            Matcher m = SC_PATTERN.matcher(rawArgs);
            while (m.find()) {
                String arg = m.group(1);
                if (arg.startsWith("'") && arg.endsWith("'")) {
                    arg = arg.substring(1, arg.length() - 1);
                }
                argsList.add(arg);
            }

            String resultText = shortcut.shortcut;
            int argIndex = 0;

            while (resultText.contains("%") && argIndex < argsList.size()) {
                resultText = resultText.replaceFirst("%", Matcher.quoteReplacement(argsList.get(argIndex)));
                argIndex++;
            }

            if (argIndex < argsList.size()) {
                StringBuilder remaining = new StringBuilder();
                for (int i = argIndex; i < argsList.size(); i++) {
                    if (!remaining.isEmpty()) remaining.append(" ");
                    remaining.append(argsList.get(i));
                }
                if (!resultText.endsWith(" ") && !remaining.isEmpty()) {
                    resultText += " ";
                }
                resultText += remaining.toString();
            }

            if (ConfigManager.configs.config.modules.getOrDefault("placeholders", true)) {
                for (var entry : PlaceholderRegistry.PLACEHOLDERS.entrySet()) {
                    String key = "{" + entry.getKey() + "}";
                    if (resultText.contains(key)) {
                        resultText = resultText.replace(key, entry.getValue().get());
                    }
                }
            }

            try {
                if (shortcut.isCommand) {
                    context.getSource().getClient().player.connection.sendCommand(resultText);
                } else {
                    context.getSource().getClient().player.connection.sendChat(resultText);
                }
            } catch (Exception e) {
                ConfigManager.LOGGER.warn("Failed to send message/command: {} Error: {}", resultText, e);
            }

        } catch (Exception e) {
            ConfigManager.LOGGER.warn("Failed to execute shortcut {} due to {}", id, e.getMessage());
        }
        return 1;
    }
    private static final class SakChatCommand {

        //private SakChatCommand() {}

        // Example module list (kept for help and suggestions). Real module flags are in ConfigManager.configs.config.modules
        private static final Map<String, String> KNOWN_MODULES = Map.of(
                "triggers", "Trigger system",
                "shortcuts", "Shortcuts system",
                "placeholder", "Placeholder engine",
                "gui", "Configuration GUI"
        );


        public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
            dispatcher.register(ClientCommands.literal("sakchat")

                    // help
                    .then(ClientCommands.literal("help")
                            .executes(ctx -> {
                                sendHeader(ctx.getSource(), Component.translatable("sakchat.command.help.header").getString());

                                String[][] helpEntries = {
                                        { "sakchat.command.help.cmd.help", "sakchat.command.help.desc.help" },
                                        { "sakchat.command.help.cmd.gui", "sakchat.command.help.desc.gui" },
                                        { "sakchat.command.help.cmd.reload", "sakchat.command.help.desc.reload" },
                                        { "sakchat.command.help.cmd.check", "sakchat.command.help.desc.check" },
                                        { "sakchat.command.help.cmd.save", "sakchat.command.help.desc.save" },
                                        { "sakchat.command.help.cmd.configEntry", "sakchat.command.help.desc.configEntry" },
                                        { "sakchat.command.help.cmd.modes.list", "sakchat.command.help.desc.modes.list" },
                                        { "sakchat.command.help.cmd.modes.toggle", "sakchat.command.help.desc.modes.toggle" },
                                        { "sakchat.command.help.cmd.modules.list", "sakchat.command.help.desc.modules.list" },
                                        { "sakchat.command.help.cmd.modules.toggle", "sakchat.command.help.desc.modules.toggle" },
                                        { "sakchat.command.help.cmd.triggers.list", "sakchat.command.help.desc.triggers.list" },
                                        { "sakchat.command.help.cmd.triggers.info", "sakchat.command.help.desc.triggers.info" },
                                        { "sakchat.command.help.cmd.triggers.toggle", "sakchat.command.help.desc.triggers.toggle" },
                                        { "sakchat.command.help.cmd.triggers.remove", "sakchat.command.help.desc.triggers.remove" },
                                        { "sakchat.command.help.cmd.shortcuts.list", "sakchat.command.help.desc.shortcuts.list" },
                                        { "sakchat.command.help.cmd.shortcuts.toggleplace", "sakchat.command.help.desc.shortcuts.toggleplace" },
                                        { "sakchat.command.help.cmd.shortcuts.remove", "sakchat.command.help.desc.shortcuts.remove" }
                                };

                                for (String[] entry : helpEntries) {
                                    Component cmdComponent = Component.translatable(entry[0]).withStyle(ChatFormatting.WHITE);
                                    Component descComponent = Component.translatable(entry[1]).withStyle(ChatFormatting.YELLOW);

                                    ctx.getSource().sendFeedback(
                                            Component.translatable("sakchat.command.help.layout", cmdComponent, descComponent)
                                                    .withStyle(ChatFormatting.GRAY)
                                    );
                                }

                                return 1;
                            })
                    )


                    // gui: open config screen
                    .then(ClientCommands.literal("gui")
                            .executes(context -> {
                                Minecraft client = context.getSource().getClient();
                                client.execute(() -> {
                                    Screen configScreen = ConfigScreenBuilder.build(null);
                                    client.setScreen(configScreen);
                                });
                                context.getSource().sendFeedback(Component.translatable("sakchat.command.gui.processing").withStyle(ChatFormatting.GREEN));
                                return 1;
                            })
                    )

                    // reload
                    .then(ClientCommands.literal("reload")
                            .requires(source -> ConfigManager.configs.config.devMode)
                            .executes(ctx -> {
                                if (ConfigManager.configs.config.devMode) {
                                    ConfigManager.loadConfig();
                                    ctx.getSource().sendFeedback(Component.translatable("sakchat.command.reload.processing").withStyle(ChatFormatting.GREEN));
                                } else {
                                    ctx.getSource().sendFeedback(Component.translatable("sakchat.command.notAllowed").withStyle(ChatFormatting.RED));
                                }
                                return 1;
                            })
                    )

                    .then(ClientCommands.literal("display")
                            .executes(context -> {
                                Minecraft mc = Minecraft.getInstance();
                                if (mc.level == null) {
                                    return 0;
                                }

                                String contextFolder;
                                try {
                                    java.lang.reflect.Method resolveMethod = ChatLogger.class.getDeclaredMethod("resolveContextFolder", Minecraft.class);
                                    resolveMethod.setAccessible(true);
                                    contextFolder = (String) resolveMethod.invoke(null, mc);
                                } catch (Exception e) {
                                    contextFolder = "singleplayer";
                                }

                                String date = java.time.LocalDate.now().toString();
                                Path baseDir = FabricLoader.getInstance().getGameDir().resolve("sakchat").resolve("records").toAbsolutePath().normalize();
                                Path fileTarget = baseDir.resolve(contextFolder).resolve(date + ".jsonl").toAbsolutePath().normalize();

                                if (!Files.exists(fileTarget) || !Files.isRegularFile(fileTarget)) {
                                    return 0;
                                }

                                mc.execute(() -> {
                                    Component content = ChatLogger.loadChatComponents(fileTarget);
                                    Component title = Component.literal("Log: " + fileTarget.getFileName().toString());
                                    mc.setScreen(new DisplayerGui(title, content));
                                });

                                return 1;
                            })
                            .then(ClientCommands.argument("path", StringArgumentType.greedyString())
                                    .executes(context -> {
                                        String inputPath = StringArgumentType.getString(context, "path");

                                        Path baseDir = FabricLoader.getInstance().getGameDir().resolve("sakchat").resolve("records").toAbsolutePath().normalize();
                                        Path fileTarget = baseDir.resolve(inputPath).toAbsolutePath().normalize();

                                        if (!fileTarget.startsWith(baseDir)) {
                                            return 0;
                                        }

                                        String stringPath = fileTarget.toString();
                                        if (stringPath.endsWith(".txt")) {
                                            fileTarget = Path.of(stringPath.substring(0, stringPath.length() - 4) + ".jsonl");
                                        } else if (!stringPath.endsWith(".jsonl")) {
                                            fileTarget = Path.of(stringPath + ".jsonl");
                                        }

                                        if (!Files.exists(fileTarget) || !Files.isRegularFile(fileTarget)) {
                                            return 0;
                                        }

                                        Path finalFileTarget = fileTarget;
                                        Minecraft mc = Minecraft.getInstance();

                                        mc.execute(() -> {
                                            Component content = ChatLogger.loadChatComponents(finalFileTarget);
                                            Component title = Component.literal("Log: " + finalFileTarget.getFileName().toString())
                                                    .withStyle(style -> style
                                                    .withHoverEvent(new HoverEvent.ShowText(Component.literal(finalFileTarget.toString())))
                                            );
                                            mc.setScreen(new DisplayerGui(title, content));
                                        });

                                        return 1;
                                    })
                            )
                    )


                    // check
                    .then(ClientCommands.literal("check")
                            .executes(ctx -> {
                                ctx.getSource().sendFeedback(Component.translatable("sakchat.command.check.processing").withStyle(ChatFormatting.GREEN));
                                ConfigManager.checkUpdates();
                                return 1;
                            })
                    )

                    // save
                    .then(ClientCommands.literal("save")
                            .executes(ctx -> {
                                ConfigManager.saveConfig();
                                ctx.getSource().sendFeedback(Component.translatable("sakchat.command.save.processing").withStyle(ChatFormatting.GREEN));
                                return 1;
                            })
                    )

                    // config
                    .then(ClientCommands.literal("config")
                            .executes(ctx -> {
                                sendHeader(ctx.getSource(), Component.translatable("sakchat.command.status.title").getString());
                                if (ConfigManager.configs == null) {
                                    ctx.getSource().sendFeedback(Component.translatable("sakchat.command.status.configNotLoaded").withStyle(ChatFormatting.RED));
                                    return 1;
                                }

                                var modes = ConfigManager.configs.config.modes;
                                var modules = ConfigManager.configs.config.modules;

                                ctx.getSource().sendFeedback(Component.literal(" ").append(Component.translatable("sakchat.command.status.modesTitle").withStyle(ChatFormatting.AQUA)));
                                if (modes == null || modes.isEmpty()) {
                                    ctx.getSource().sendFeedback(Component.literal("  ").withStyle(ChatFormatting.GRAY).append(Component.literal("- ")).append(Component.translatable("sakchat.command.status.noModes").withStyle(ChatFormatting.WHITE)));
                                } else {
                                    modes.forEach((k, v) -> {
                                        ChatFormatting color = v ? ChatFormatting.GREEN : ChatFormatting.RED;

                                        ctx.getSource().sendFeedback(Component.literal("  ").withStyle(ChatFormatting.GRAY)
                                                .append(Component.literal("- "))
                                                .append(Component.literal(k).withStyle(ChatFormatting.WHITE))
                                                .append(Component.literal(" = ").withStyle(ChatFormatting.GRAY))
                                                .append(Component.literal(String.valueOf(v)).withStyle(color)));
                                    });
                                }

                                ctx.getSource().sendFeedback(Component.literal(" ").append(Component.translatable("sakchat.command.status.modulesTitle").withStyle(ChatFormatting.AQUA)));
                                if (modules == null || modules.isEmpty()) {
                                    ctx.getSource().sendFeedback(Component.literal("  ").withStyle(ChatFormatting.GRAY).append(Component.literal("- ")).append(Component.translatable("sakchat.command.status.noModes").withStyle(ChatFormatting.WHITE)));
                                } else {
                                    modules.forEach((k, v) -> {
                                        ChatFormatting color = v ? ChatFormatting.GREEN : ChatFormatting.RED;

                                        ctx.getSource().sendFeedback(Component.literal("  ").withStyle(ChatFormatting.GRAY)
                                                .append(Component.literal("- "))
                                                .append(Component.literal(k).withStyle(ChatFormatting.WHITE))
                                                .append(Component.literal(" = ").withStyle(ChatFormatting.GRAY))
                                                .append(Component.literal(String.valueOf(v)).withStyle(color)));
                                    });
                                }

                                return 1;
                            })

                    )

                    // modes subtree
                    .then(ClientCommands.literal("modes")
                            .then(ClientCommands.literal("list")
                                    .executes(ctx -> {
                                        var modes = ConfigManager.configs.config.modes;
                                        sendHeader(ctx.getSource(), Component.translatable("sakchat.command.modes.title").getString());

                                        if (modes == null || modes.isEmpty()) {
                                            ctx.getSource().sendFeedback(Component.translatable("sakchat.command.status.noModes").withStyle(ChatFormatting.RED));
                                            return 1;
                                        }

                                        modes.forEach((k, v) -> {
                                            ChatFormatting color = v ? ChatFormatting.GREEN : ChatFormatting.RED;
                                            Component entryValue = Component.literal(String.valueOf(v)).withStyle(color);
                                            Component entryKey = Component.literal(k).withStyle(ChatFormatting.WHITE);

                                            ctx.getSource().sendFeedback(Component.translatable("sakchat.command.list.entry", entryKey, entryValue)
                                                    .withStyle(ChatFormatting.GRAY));
                                        });
                                        return 1;
                                    })
                            )

                            .then(ClientCommands.literal("toggle")
                                    .then(ClientCommands.argument("mode", StringArgumentType.word())
                                            .suggests((ctx, builder) -> {
                                                var modes = ConfigManager.configs.config.modes;
                                                return SharedSuggestionProvider.suggest(modes == null ? List.of() : modes.keySet(), builder);
                                            })
                                            .then(ClientCommands.argument("value", BoolArgumentType.bool())
                                                    .executes(ctx -> {
                                                        String mode = StringArgumentType.getString(ctx, "mode");
                                                        boolean value = BoolArgumentType.getBool(ctx, "value");

                                                        if (ConfigManager.configs.config.modes == null || !ConfigManager.configs.config.modes.containsKey(mode)) {
                                                            ctx.getSource().sendFeedback(Component.translatable("sakchat.command.modes.notFound", mode).withStyle(ChatFormatting.RED));
                                                            return 0;
                                                        }

                                                        ConfigManager.configs.config.modes.put(mode, value);
                                                        ConfigManager.saveConfigFile();

                                                        ChatFormatting color = value ? ChatFormatting.GREEN : ChatFormatting.RED;

                                                        Component modeComponent = Component.literal(mode).withStyle(ChatFormatting.AQUA);
                                                        Component valueComponent = Component.literal(String.valueOf(value)).withStyle(color);

                                                        ctx.getSource().sendFeedback(Component.translatable("sakchat.command.modes.set.success", modeComponent, valueComponent));
                                                        return 1;
                                                    })
                                            )
                                    )
                            )
                    )

                    // modules subtree
                    .then(ClientCommands.literal("modules")
                            .then(ClientCommands.literal("list")
                                    .executes(ctx -> {
                                        sendHeader(ctx.getSource(), Component.translatable("sakchat.command.modules.title").getString());
                                        var modules = ConfigManager.configs.config.modules;

                                        if (modules == null || modules.isEmpty()) {
                                            KNOWN_MODULES.forEach((k, v) -> {
                                                Component entryKey = Component.literal(k).withStyle(ChatFormatting.WHITE);
                                                Component entryValue = Component.literal(v).withStyle(ChatFormatting.YELLOW);

                                                ctx.getSource().sendFeedback(Component.translatable("sakchat.command.list.entry", entryKey, entryValue)
                                                        .withStyle(ChatFormatting.GRAY));
                                            });
                                            return 1;
                                        }

                                        modules.forEach((k, v) -> {
                                            boolean isTrue = Boolean.parseBoolean(String.valueOf(v));
                                            ChatFormatting color = isTrue ? ChatFormatting.GREEN : ChatFormatting.RED;

                                            Component entryKey = Component.literal(k).withStyle(ChatFormatting.WHITE);
                                            Component entryValue = Component.literal(String.valueOf(v)).withStyle(color);

                                            ctx.getSource().sendFeedback(Component.translatable("sakchat.command.list.entry", entryKey, entryValue)
                                                    .withStyle(ChatFormatting.GRAY));
                                        });
                                        return 1;
                                    })
                            )


                            .then(ClientCommands.literal("toggle")
                                    .then(ClientCommands.argument("module", StringArgumentType.word())
                                            .suggests((ctx, builder) -> {
                                                var modules = ConfigManager.configs.config.modules;
                                                if (modules == null) return SharedSuggestionProvider.suggest(KNOWN_MODULES.keySet(), builder);
                                                return SharedSuggestionProvider.suggest(modules.keySet(), builder);
                                            })
                                            .then(ClientCommands.argument("value", BoolArgumentType.bool())
                                                    .executes(ctx -> {
                                                        String module = StringArgumentType.getString(ctx, "module");
                                                        boolean value = BoolArgumentType.getBool(ctx, "value");

                                                        if (ConfigManager.configs.config.modules == null || !ConfigManager.configs.config.modules.containsKey(module)) {
                                                            ctx.getSource().sendFeedback(Component.translatable("sakchat.command.modules.notFound", module).withStyle(ChatFormatting.RED));
                                                            return 0;
                                                        }

                                                        ConfigManager.configs.config.modules.put(module, value);
                                                        ConfigManager.saveConfigFile();

                                                        ChatFormatting color = value ? ChatFormatting.GREEN : ChatFormatting.RED;
                                                        Component moduleComponent = Component.literal(module).withStyle(ChatFormatting.AQUA);
                                                        Component valueComponent = Component.literal(String.valueOf(value)).withStyle(color);

                                                        ctx.getSource().sendFeedback(Component.translatable("sakchat.command.modules.set.success", moduleComponent, valueComponent));
                                                        return 1;
                                                    })
                                            )
                                    )
                            )
                    )

                    .then(ClientCommands.literal("triggers")
                            .then(ClientCommands.literal("list")
                                    .executes(ctx -> {
                                        List<TriggerEntry> triggers = ConfigManager.configs.triggers;
                                        sendHeader(ctx.getSource(), Component.translatable("sakchat.command.triggers.title").getString());

                                        if (triggers == null || triggers.isEmpty()) {
                                            ctx.getSource().sendFeedback(Component.translatable("sakchat.command.triggers.noTriggers").withStyle(ChatFormatting.RED));
                                            return 1;
                                        }

                                        for (TriggerEntry t : triggers) {
                                            ChatFormatting statusColor = t.enabled ? ChatFormatting.GREEN : ChatFormatting.RED;
                                            Component statusComponent = Component.translatable(t.enabled ? "sakchat.enabled" : "sakchat.disabled").withStyle(statusColor);

                                            Component idComponent = Component.literal(t.id).withStyle(ChatFormatting.WHITE);
                                            Component patternComponent = Component.literal(t.pattern).withStyle(ChatFormatting.YELLOW);

                                            // %1$s = ID, %2$s = ConfigEntry (Enabled/Disabled), %3$s = Pattern
                                            ctx.getSource().sendFeedback(Component.translatable("sakchat.command.triggers.list.entry", idComponent, statusComponent, patternComponent)
                                                    .withStyle(ChatFormatting.GRAY));
                                        }
                                        return 1;
                                    })
                            )

                            .then(ClientCommands.literal("info")
                                    .then(ClientCommands.argument("id", StringArgumentType.word())
                                            .suggests((ctx, builder) -> {
                                                if (ConfigManager.configs.triggers == null) return SharedSuggestionProvider.suggest(List.of(), builder);
                                                List<String> ids = ConfigManager.configs.triggers.stream().map(tr -> tr.id).collect(Collectors.toList());
                                                return SharedSuggestionProvider.suggest(ids, builder);
                                            })
                                            .executes(ctx -> {
                                                String id = StringArgumentType.getString(ctx, "id");
                                                TriggerEntry t = findTriggerById(id);

                                                if (t == null) {
                                                    ctx.getSource().sendFeedback(Component.translatable("sakchat.command.triggers.notFound", Component.literal(id).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.RED));
                                                    return 0;
                                                }

                                                sendHeader(ctx.getSource(), Component.translatable("sakchat.command.triggers.info.title", id).getString());

                                                ctx.getSource().sendFeedback(Component.translatable("sakchat.command.triggers.info.pattern", Component.literal(t.pattern).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GRAY));

                                                Component statusVal = Component.translatable(t.enabled ? "sakchat.enabled" : "sakchat.disabled").withStyle(t.enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
                                                ctx.getSource().sendFeedback(Component.translatable("sakchat.command.triggers.info.enabled", statusVal).withStyle(ChatFormatting.GRAY));

                                                Component regexVal = Component.literal(String.valueOf(t.regex)).withStyle(t.regex ? ChatFormatting.GREEN : ChatFormatting.RED);
                                                Component csVal = Component.literal(String.valueOf(t.caseSensitive)).withStyle(t.caseSensitive ? ChatFormatting.GREEN : ChatFormatting.RED);
                                                ctx.getSource().sendFeedback(Component.translatable("sakchat.command.triggers.info.options", regexVal, csVal).withStyle(ChatFormatting.GRAY));

                                                if (t.actions != null) {
                                                    Component soundAct = Component.literal(String.valueOf(t.actions.sound)).withStyle(t.actions.sound ? ChatFormatting.GREEN : ChatFormatting.RED);
                                                    Component hudAct = Component.literal(String.valueOf(t.actions.hudPopup)).withStyle(t.actions.hudPopup ? ChatFormatting.GREEN : ChatFormatting.RED);
                                                    Component highAct = Component.literal(String.valueOf(t.actions.highlight)).withStyle(t.actions.highlight ? ChatFormatting.GREEN : ChatFormatting.RED);
                                                    Component toastAct = Component.literal(String.valueOf(t.actions.toast)).withStyle(t.actions.toast ? ChatFormatting.GREEN : ChatFormatting.RED);
                                                    ctx.getSource().sendFeedback(Component.translatable("sakchat.command.triggers.info.actions", soundAct, hudAct, highAct, toastAct).withStyle(ChatFormatting.GRAY));
                                                }

                                                if (t.appearance != null) {
                                                    Component colorVal = Component.literal(t.appearance.highlightColor).withStyle(ChatFormatting.WHITE);
                                                    //Component bgVal = Component.literal(t.appearance.backgroundColor != null ? t.appearance.backgroundColor : "none").withStyle(ChatFormatting.WHITE);
                                                    ctx.getSource().sendFeedback(Component.translatable("sakchat.command.triggers.info.appearance", colorVal).withStyle(ChatFormatting.GRAY));
                                                }

                                                if (t.sound != null) {
                                                    Component soundId = Component.literal(t.sound.id).withStyle(ChatFormatting.WHITE);
                                                    Component volVal = Component.literal(String.valueOf(t.sound.volume)).withStyle(ChatFormatting.YELLOW);
                                                    Component pitchVal = Component.literal(String.valueOf(t.sound.pitch)).withStyle(ChatFormatting.YELLOW);
                                                    ctx.getSource().sendFeedback(Component.translatable("sakchat.command.triggers.info.sound", soundId, volVal, pitchVal).withStyle(ChatFormatting.GRAY));
                                                }
                                                return 1;
                                            })
                                    )
                            )

                            .then(ClientCommands.literal("toggle")
                                    .then(ClientCommands.argument("id", StringArgumentType.word())
                                            .suggests((ctx, builder) -> {
                                                if (ConfigManager.configs.triggers == null) return SharedSuggestionProvider.suggest(List.of(), builder);
                                                List<String> ids = ConfigManager.configs.triggers.stream().map(tr -> tr.id).collect(Collectors.toList());
                                                return SharedSuggestionProvider.suggest(ids, builder);
                                            })
                                            .executes(ctx -> {
                                                String id = StringArgumentType.getString(ctx, "id");
                                                TriggerEntry entry = findTriggerById(id);

                                                if (entry == null) {
                                                    ctx.getSource().sendFeedback(Component.translatable("sakchat.command.triggers.notFound", Component.literal(id).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.RED));
                                                    return 0;
                                                }

                                                entry.enabled = !entry.enabled;
                                                ConfigManager.saveTriggers();

                                                Component idComponent = Component.literal(id).withStyle(ChatFormatting.AQUA);
                                                Component stateComponent = Component.translatable(entry.enabled ? "sakchat.enabled" : "sakchat.disabled").withStyle(entry.enabled ? ChatFormatting.GREEN : ChatFormatting.RED);

                                                ctx.getSource().sendFeedback(Component.translatable("sakchat.command.triggers.toggle.success", idComponent, stateComponent));
                                                return 1;
                                            })
                                    )
                            )

                            .then(ClientCommands.literal("remove")
                                    .then(ClientCommands.argument("id", StringArgumentType.word())
                                            .suggests((ctx, builder) -> {
                                                if (ConfigManager.configs.triggers == null) return SharedSuggestionProvider.suggest(List.of(), builder);
                                                List<String> ids = ConfigManager.configs.triggers.stream().map(tr -> tr.id).collect(Collectors.toList());
                                                return SharedSuggestionProvider.suggest(ids, builder);
                                            })
                                            .executes(ctx -> {
                                                String id = StringArgumentType.getString(ctx, "id");

                                                if (ConfigManager.configs.triggers == null) {
                                                    ctx.getSource().sendFeedback(Component.translatable("sakchat.command.triggers.notFound", Component.literal(id).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.RED));
                                                    return 0;
                                                }

                                                boolean removed = ConfigManager.configs.triggers.removeIf(t -> t.id.equals(id));
                                                if (!removed) {
                                                    ctx.getSource().sendFeedback(Component.translatable("sakchat.command.triggers.notFound", Component.literal(id).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.RED));
                                                    return 0;
                                                }
                                                ConfigManager.saveTriggers();
                                                ctx.getSource().sendFeedback(Component.translatable("sakchat.command.triggers.remove.success", Component.literal(id).withStyle(ChatFormatting.WHITE)));
                                                return 1;
                                            })
                                    )
                            )
                    )

                    // shortcuts subtree (list, toggleplace, remove)
                    .then(ClientCommands.literal("shortcuts")
                            .then(ClientCommands.literal("list")
                                    .executes(ctx -> {
                                        List<ShortcutsEntry> list = ConfigManager.configs.shortcuts;
                                        sendHeader(ctx.getSource(), Component.translatable("sakchat.command.shortcuts.title").getString());

                                        if (list == null || list.isEmpty()) {
                                            ctx.getSource().sendFeedback(Component.translatable("sakchat.command.shortcuts.noEntries").withStyle(ChatFormatting.GRAY));
                                            return 1;
                                        }

                                        for (ShortcutsEntry s : list) {
                                            Component idComponent = Component.literal(s.id).withStyle(ChatFormatting.WHITE);
                                            Component shortcutComponent = Component.literal(s.shortcut != null ? s.shortcut : "").withStyle(ChatFormatting.YELLOW);

                                            Component isCommandComponent = Component.literal(String.valueOf(s.isCommand))
                                                    .withStyle(s.isCommand ? ChatFormatting.GREEN : ChatFormatting.RED);
                                            Component placeholdingComponent = Component.literal(String.valueOf(s.placeholding))
                                                    .withStyle(s.placeholding ? ChatFormatting.GREEN : ChatFormatting.RED);

                                            // %1$s = ID, %2$s = Shortcut, %3$s = isCommand, %4$s = placeholding
                                            MutableComponent entry = Component.translatable("sakchat.command.shortcuts.list.entry",
                                                    idComponent, shortcutComponent, isCommandComponent, placeholdingComponent);

                                            // Se il placeholding è attivo, iniettiamo il pezzo di traduzione aggiuntivo per il placeholder
                                            if (s.placeholding) {
                                                Component placeholderVal = Component.literal(s.placeholder != null ? s.placeholder : "").withStyle(ChatFormatting.WHITE);
                                                entry.append(Component.translatable("sakchat.command.shortcuts.list.entry.placeholder", placeholderVal));
                                            }

                                            ctx.getSource().sendFeedback(entry.withStyle(ChatFormatting.GRAY));
                                        }
                                        return 1;
                                    })
                            )

                            .then(ClientCommands.literal("toggleplace")
                                    .then(ClientCommands.argument("id", StringArgumentType.word())
                                            .suggests((ctx, builder) -> {
                                                if (ConfigManager.configs.shortcuts == null) return SharedSuggestionProvider.suggest(List.of(), builder);
                                                List<String> ids = ConfigManager.configs.shortcuts.stream().map(s -> s.id).collect(Collectors.toList());
                                                return SharedSuggestionProvider.suggest(ids, builder);
                                            })
                                            .executes(ctx -> {
                                                String id = StringArgumentType.getString(ctx, "id");
                                                ShortcutsEntry entry = findShortcutById(id);

                                                if (entry == null) {
                                                    ctx.getSource().sendFeedback(Component.translatable("sakchat.command.shortcuts.notFound", Component.literal(id).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.RED));
                                                    return 0;
                                                }

                                                entry.placeholding = !entry.placeholding;
                                                ConfigManager.saveShortcuts();

                                                Component idComponent = Component.literal(id).withStyle(ChatFormatting.AQUA);
                                                Component stateComponent = Component.literal(String.valueOf(entry.placeholding))
                                                        .withStyle(entry.placeholding ? ChatFormatting.GREEN : ChatFormatting.RED);

                                                ctx.getSource().sendFeedback(Component.translatable("sakchat.command.shortcuts.toggleplace.success", idComponent, stateComponent));
                                                return 1;
                                            })
                                    )
                            )

                            .then(ClientCommands.literal("remove")
                                    .then(ClientCommands.argument("id", StringArgumentType.word())
                                            .suggests((ctx, builder) -> {
                                                if (ConfigManager.configs.shortcuts == null) return SharedSuggestionProvider.suggest(List.of(), builder);
                                                List<String> ids = ConfigManager.configs.shortcuts.stream().map(s -> s.id).collect(Collectors.toList());
                                                return SharedSuggestionProvider.suggest(ids, builder);
                                            })
                                            .executes(ctx -> {
                                                String id = StringArgumentType.getString(ctx, "id");

                                                if (ConfigManager.configs.shortcuts == null) {
                                                    ctx.getSource().sendFeedback(Component.translatable("sakchat.command.shortcuts.notFound", Component.literal(id).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.RED));
                                                    return 0;
                                                }

                                                boolean removed = ConfigManager.configs.shortcuts.removeIf(s -> s.id.equals(id));
                                                if (!removed) {
                                                    ctx.getSource().sendFeedback(Component.translatable("sakchat.command.shortcuts.notFound", Component.literal(id).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.RED));
                                                    return 0;
                                                }

                                                ConfigManager.saveShortcuts();
                                                ctx.getSource().sendFeedback(Component.translatable("sakchat.command.shortcuts.remove.success", Component.literal(id).withStyle(ChatFormatting.WHITE)));
                                                return 1;
                                            })
                                    )
                            )
                    )
            );

        }

        // Helpers

        private static void sendHeader(FabricClientCommandSource source, String title) {
            source.sendFeedback(Component.literal("§6§l=== " + title + " ==="));
        }

        private static TriggerEntry findTriggerById(String id) {
            if (ConfigManager.configs == null || ConfigManager.configs.triggers == null) return null;
            return ConfigManager.configs.triggers.stream().filter(t -> t.id.equals(id)).findFirst().orElse(null);
        }

        private static ShortcutsEntry findShortcutById(String id) {
            if (ConfigManager.configs == null || ConfigManager.configs.shortcuts == null) return null;
            return ConfigManager.configs.shortcuts.stream().filter(s -> s.id.equals(id)).findFirst().orElse(null);
        }
    }


}
