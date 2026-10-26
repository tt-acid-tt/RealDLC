package ru.acid.realdlc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.lwjgl.glfw.GLFW;

public final class RealDLC implements ClientModInitializer {
    private static boolean rightShiftWasDown;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Replace the title screen locally. This does not send packets or require server-side installation.
            if (client.screen instanceof TitleScreen) {
                client.setScreen(new RealDlcLockScreen());
                return;
            }

            long handle = client.getWindow().getHandle();
            boolean down = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
            if (down && !rightShiftWasDown && client.player != null && client.screen == null) {
                client.setScreen(new RealDlcScreen());
            }
            rightShiftWasDown = down;
        });
    }

    static Minecraft client() {
        return Minecraft.getInstance();
    }
}
