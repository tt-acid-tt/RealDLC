package ru.acid.realdlc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screens.TitleScreen;

/** Client-only launcher integration. No in-world hotkey is registered. */
public final class RealDLC implements ClientModInitializer {
    private static boolean wasInWorld;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.level != null) {
                wasInWorld = true;
                return;
            }

            if (client.screen instanceof TitleScreen) {
                if (wasInWorld) {
                    wasInWorld = false;
                    client.setScreen(new RealDlcTransitionScreen(true, null));
                } else {
                    client.setScreen(new RealDlcWelcomeScreen());
                }
            }
        });
    }
}
