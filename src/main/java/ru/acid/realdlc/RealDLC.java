package ru.acid.realdlc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;

/** Client-only launcher integration. */
public final class RealDLC implements ClientModInitializer {
    private static boolean wasInWorld;

    private static boolean connectionOverlayActive;
    private static long connectionOverlayStartedAt;
    private static long worldReadyAt = -1L;
    private static final long WORLD_REVEAL_MILLIS = 560L;
    private static final long MAX_CONNECTION_OVERLAY_MILLIS = 90_000L;
    private static final int WALLPAPER_WIDTH = 1920;
    private static final int WALLPAPER_HEIGHT = 1080;

    @Override
    public void onInitializeClient() {
        // Keep the last frame of the zoom transition over the vanilla connection screen.
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) ->
                ScreenEvents.afterRender(screen).register((screen1, graphics, mouseX, mouseY, tickDelta) -> {
                    if (!connectionOverlayActive || isOverlayExcludedScreen(screen1)) {
                        return;
                    }
                    drawConnectionOverlay(graphics,
                            client.getWindow().getGuiScaledWidth(),
                            client.getWindow().getGuiScaledHeight());
                }));

        // Once the loading screen disappears, fade the same frame over the newly loaded world.
        HudRenderCallback.EVENT.register((graphics, tickCounter) -> {
            Minecraft client = Minecraft.getInstance();
            if (connectionOverlayActive && client.screen == null) {
                drawConnectionOverlay(graphics,
                        client.getWindow().getGuiScaledWidth(),
                        client.getWindow().getGuiScaledHeight());
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            updateConnectionOverlay(client);

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

    static void beginConnectionOverlay() {
        connectionOverlayActive = true;
        connectionOverlayStartedAt = System.currentTimeMillis();
        worldReadyAt = -1L;
    }

    private static void updateConnectionOverlay(Minecraft client) {
        if (!connectionOverlayActive) return;

        long now = System.currentTimeMillis();
        if (isOverlayExcludedScreen(client.screen)) {
            connectionOverlayActive = false;
            worldReadyAt = -1L;
            return;
        }

        if (client.player != null && client.level != null) {
            if (worldReadyAt < 0L) worldReadyAt = now;
            if (now - worldReadyAt >= WORLD_REVEAL_MILLIS) {
                connectionOverlayActive = false;
                worldReadyAt = -1L;
            }
        } else if (now - connectionOverlayStartedAt >= MAX_CONNECTION_OVERLAY_MILLIS) {
            // Do not hide an error screen forever if a server never responds.
            connectionOverlayActive = false;
            worldReadyAt = -1L;
        }
    }


    /**
     * The connection wallpaper belongs only over vanilla connection/loading screens.
     * Never let it render on top of RealDLC's own menus, even if a connection exits
     * through an unusual callback path.
     */
    private static boolean isOverlayExcludedScreen(Screen screen) {
        return screen instanceof RealDlcTransitionScreen
                || screen instanceof DisconnectedScreen
                || screen instanceof TitleScreen
                || screen instanceof RealDlcHomeScreen
                || screen instanceof RealDlcWelcomeScreen
                || screen instanceof RealDlcAccountScreen
                || screen instanceof RealDlcAddServerScreen;
    }

    private static void drawConnectionOverlay(GuiGraphics graphics, int width, int height) {
        if (!connectionOverlayActive || width <= 0 || height <= 0) return;

        long now = System.currentTimeMillis();
        int alpha = 255;
        if (worldReadyAt >= 0L) {
            float progress = Math.min(1.0F, (now - worldReadyAt) / (float) WORLD_REVEAL_MILLIS);
            // Smoothstep prevents a visible snap when the actual world is revealed.
            float eased = progress * progress * (3.0F - 2.0F * progress);
            alpha = Math.max(0, Math.min(255, Math.round(255.0F * (1.0F - eased))));
            if (alpha == 0) return;
        }

        int tint = (alpha << 24) | 0x00FFFFFF;
        graphics.blit(RenderPipelines.GUI_TEXTURED, UiDraw.WALLPAPER,
                0, 0, 0.0F, 0.0F, width, height,
                WALLPAPER_WIDTH, WALLPAPER_HEIGHT,
                WALLPAPER_WIDTH, WALLPAPER_HEIGHT, tint);

        // Keep the transition's first frame clean; the status softly appears after it has settled.
        long elapsed = now - connectionOverlayStartedAt;
        if (elapsed > 180L) {
            float messageProgress = Math.min(1.0F, (elapsed - 180L) / 260.0F);
            int textAlpha = Math.max(0, Math.min(alpha,
                    Math.round(220.0F * messageProgress * (alpha / 255.0F))));
            int color = (textAlpha << 24) | 0x00FFFFFF;
            UiDraw.centered(graphics, Minecraft.getInstance().font,
                    "ПОДКЛЮЧАЕМСЯ К МИРУ", width / 2, height - 54, color);

            int dotAlpha = Math.max(0, Math.min(alpha,
                    Math.round(200.0F * messageProgress * (alpha / 255.0F))));
            int dotColor = (dotAlpha << 24) | 0x00C9EEFF;
            int pulse = (int) ((now / 220L) % 3L);
            for (int i = 0; i < 3; i++) {
                int dotSize = i == pulse ? 5 : 3;
                int dx = width / 2 - 15 + i * 12;
                int dy = height - 35 - dotSize / 2;
                graphics.fill(dx, dy, dx + dotSize, dy + dotSize, dotColor);
            }
        }
    }
}
