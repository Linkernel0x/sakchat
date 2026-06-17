package link.sakkit.sakchat.helpers;

import link.sakkit.sakchat.config.TriggerEntry;
import link.sakkit.sakchat.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

import net.minecraft.client.gui.components.toasts.SystemToast;

public class ActionExecutor {
    public static Component execute(Component message, TriggerEntry trigger) {
        if (trigger.actions != null) {
            if (trigger.actions.sound) {
                playSound(trigger.sound);
            }

            if (trigger.actions.hudPopup) {
                displayHud(trigger);
            }

            if (trigger.actions.toast) {
                displayToast(trigger);
            }

            if (trigger.actions.shortcut && ConfigManager.configs.config.modes.getOrDefault("shortcuts", true)) {
                runShortcut(trigger);
            }

            if (trigger.actions.blacklist && ConfigManager.configs.config.modes.getOrDefault("blacklist", true)) {
                message = Component.literal(processBlacklist(message, trigger));
            }

            if (trigger.actions.highlight) {
                return applyHighlight(message, trigger);
            }
        }
        return message;
    }



    private static void playSound(TriggerEntry.SoundConfig soundConfig) {
        if (soundConfig == null || soundConfig.id == null) return;

        Minecraft client = Minecraft.getInstance();

        try {
            Identifier soundId = Identifier.tryParse(soundConfig.id);
            if (soundId == null) return;

            SoundEvent soundEvent = null;

            try {
                Object direct = BuiltInRegistries.SOUND_EVENT.get(soundId);
                soundEvent = extractSoundEvent(direct);
            } catch (Throwable ignored) {
                //
            }

            if (soundEvent == null) {
                try {
                    Optional<?> opt = BuiltInRegistries.SOUND_EVENT.getOptional(soundId);
                    if (opt.isPresent()) {
                        soundEvent = extractSoundEvent(opt.get());
                    }
                } catch (Throwable ignored) { }
            }

            if (soundEvent == null) {
                ConfigManager.debugLog("Sound not found in registry: " + soundConfig.id);
                return;
            }

            final SoundEvent toPlay = soundEvent;
            client.execute(() -> {
                if (client.player != null) {
                    client.player.playSound(toPlay, soundConfig.volume, soundConfig.pitch);
                    ConfigManager.debugLog("Successfully played: " + soundConfig.id);
                }
            });

        } catch (Exception e) {
            ConfigManager.LOGGER.warn("[SAKChat] Critical failure in ActionExecutor: {}", soundConfig.id, e);
        }
    }

    private static SoundEvent extractSoundEvent(Object obj) {
        if (obj == null) return null;
        if (obj instanceof SoundEvent) return (SoundEvent) obj;

        if (obj instanceof Optional<?> opt) {
            return opt.map(ActionExecutor::extractSoundEvent).orElse(null);
        }

        try {
            Method valueMethod = obj.getClass().getMethod("value");
            Object val = valueMethod.invoke(obj);
            if (val instanceof SoundEvent) return (SoundEvent) val;
        } catch (Exception ignored) {
            try {
                Method getMethod = obj.getClass().getMethod("get");
                Object val = getMethod.invoke(obj);
                if (val instanceof SoundEvent) return (SoundEvent) val;
            } catch (Exception ignored2) { }
        }
        return null;
    }

    private static void displayHud(TriggerEntry trigger) {
        Minecraft client = Minecraft.getInstance();

        String text = "§3§l[SAKChat]§r " + trigger.id + ": '" + trigger.pattern + "'";
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendOverlayMessage(Component.literal(text));
            }
        });
    }

    private static void displayToast(TriggerEntry trigger) {
        Minecraft client = Minecraft.getInstance();
        SystemToast.add(
                client.getToastManager(),
                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                Component.literal("SAKChat - trigger match"),
                Component.literal(trigger.id + ": " + trigger.pattern)
        );
    }

    public static Component applyHighlight(Component message, TriggerEntry trigger) {
        int rgb = TextColor.parseColor(trigger.appearance.highlightColor)
                .getOrThrow()
                .getValue();

        MutableComponent prefix = Component.literal("[" + trigger.id + "] ➢ ")
                .withStyle(style -> style.withColor(rgb).withBold(true));
        MutableComponent suffix = Component.literal(" ⬅")
                .withStyle(style -> style.withColor(rgb).withBold(true));

        if (trigger.appearance.forceColor) {
            message = Component.literal(message.getString());
        }
        return prefix.append(message).append(suffix);
    }


    private static void runShortcut(TriggerEntry trigger) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            if (trigger.shortcut.params.isEmpty()) {
                client.player.connection.sendCommand("sc " + trigger.shortcut.id);
            } else {
                client.player.connection.sendCommand("sc " + trigger.shortcut.id + " " + trigger.shortcut.params);
            }
        }
    }

    public static String processBlacklist(Component originalText, TriggerEntry trigger) {
        String filteredText = originalText.getString();

        if (!trigger.blacklist.aggressive) {

            Pattern compiled = ConfigManager.configs.compiledTriggers.get(trigger.id);

            if (compiled != null) {
                filteredText = compiled.matcher(filteredText)
                        .replaceAll(Matcher.quoteReplacement(trigger.blacklist.replacement));
                return filteredText;
            }

            if (!trigger.regex) {
                filteredText = filteredText.replace(trigger.pattern, trigger.blacklist.replacement);
            }
        } else {
            filteredText = trigger.blacklist.replacement;
        }

        return filteredText;
    }

}
