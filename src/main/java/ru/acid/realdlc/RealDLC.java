package ru.acid.realdlc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import org.lwjgl.glfw.GLFW;

/** RealDLC client entry point. The old title and in-world overlay are replaced by the new flow. */
public final class RealDLC implements ClientModInitializer {
    private static boolean rightShiftWasDown;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.screen instanceof TitleScreen) {
                client.setScreen(new RealDlcWelcomeScreen());
                return;
            }

            long window = client.getWindow().handle();
            boolean down = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
            if (down && !rightShiftWasDown && client.player != null && client.screen == null) {
                client.setScreen(new RealDlcHomeScreen());
            }
            rightShiftWasDown = down;
        });
    }
}
