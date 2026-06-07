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

            if (trigger.actions.highlight) {
                return applyHighlight(message, trigger);
            }

            if (trigger.actions.shortcut && ConfigManager.config.status.modules.getOrDefault("shortcuts", true)) {
                runShortcut(trigger);
            }
        }
        return message;
    }

    public static Component applyHighlight(Component original, TriggerEntry trigger) {
        if (trigger.appearance == null) return original;

        MutableComponent highlighted = original.copy();
        var style = highlighted.getStyle().withBold(trigger.appearance.bold);

        if (trigger.appearance.highlightColor != null && !trigger.appearance.highlightColor.isEmpty()) {
            try {
                style = style.withColor(TextColor.parseColor(trigger.appearance.highlightColor).getOrThrow());
            } catch (Exception ignored) {}
        }

        return highlighted.setStyle(style);
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
            ConfigManager.LOGGER.warn("[SAKChat] Critical failure in ActionExecutor: " + soundConfig.id, e);
        }
    }

    private static SoundEvent extractSoundEvent(Object obj) {
        if (obj == null) return null;
        if (obj instanceof SoundEvent) return (SoundEvent) obj;

        if (obj instanceof Optional) {
            Optional<?> opt = (Optional<?>) obj;
            return opt.isPresent() ? extractSoundEvent(opt.get()) : null;
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

    private static void runShortcut(TriggerEntry trigger) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.connection.sendCommand("/sc " +  trigger.shortcut.id + trigger.shortcut.params);
        }
    }
}
