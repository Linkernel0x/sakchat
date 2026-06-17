package link.sakkit.sakchat.engine;

import com.mojang.blaze3d.platform.InputConstants;
import link.sakkit.sakchat.config.ConfigManager;
import link.sakkit.sakchat.config.ShortcutsEntry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.lwjgl.glfw.GLFW;

public class InputEngine {

    private static int lastPressedKey = -1;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            
            if (client.player == null || client.screen != null) {
                lastPressedKey = -1;
                return;
            }

            
            long windowHandle = client.getWindow().handle();
            boolean anyKeyPressed = false;

            if (ConfigManager.configs != null && ConfigManager.configs.shortcuts != null) {
                for (ShortcutsEntry s : ConfigManager.configs.shortcuts) {
                    if (s.key == null || s.key.equals("key.keyboard.unknown")) continue;

                    InputConstants.Key mojangKey = InputConstants.getKey(s.key);
                    int keyCode = mojangKey.getValue();

                    
                    if (GLFW.glfwGetKey(windowHandle, keyCode) == GLFW.GLFW_PRESS) {
                        anyKeyPressed = true;

                        
                        if (lastPressedKey != keyCode) {
                            lastPressedKey = keyCode;

                            if (s.keyBinding) {
                                String command = "sc " + s.id;
                                client.player.connection.sendCommand(command);
                            }
                        }
                        break; 
                    }
                }
            }
            
            if (!anyKeyPressed) {
                lastPressedKey = -1;
            }
        });
    }
}
