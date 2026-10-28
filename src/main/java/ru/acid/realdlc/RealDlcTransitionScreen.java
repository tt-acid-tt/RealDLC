package ru.acid.realdlc;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Cinematic zoom transition used before opening a world/server and when returning to the launcher. */
public final class RealDlcTransitionScreen extends Screen {
    private final boolean returning;
    private final Runnable action;
    private int age;
    private boolean actionStarted;

    public RealDlcTransitionScreen(boolean returning, Runnable action) {
        super(Component.literal("RealDLC Transition"));
        this.returning = returning;
        this.action = action;
    }

    @Override
    public void tick() {
        age++;
        if (age >= 33 && !actionStarted) {
            actionStarted = true;
            if (returning) {
                minecraft.setScreen(new RealDlcHomeScreen(RealDlcHomeScreen.lastMultiplayer()));
            } else if (action != null) {
                // Continue the exact final frame over the connection and world-loading screens.
                RealDLC.beginConnectionOverlay();
                action.run();
            } else {
                minecraft.setScreen(new RealDlcHomeScreen());
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float linear = Math.min(1F, (age + partialTick) / 32F);
        float eased = linear * linear * (3F - 2F * linear);
        UiDraw.background(g, width, height, System.currentTimeMillis());
        g.fillGradient(0, 0, width, height, 0x9908111C, 0x74030A12);
        float startW = Math.min(168F, width * 0.22F);
        float startH = Math.min(210F, height * 0.42F);
        float endW = width;
        float endH = height;
        float f = returning ? (1F - eased) : eased;
        float cardW = startW + (endW - startW) * f;
        float cardH = startH + (endH - startH) * f;
        int cw = Math.max(1, (int)cardW), ch = Math.max(1, (int)cardH);
        int x = (width - cw) / 2, y = (height - ch) / 2;
        g.blit(UiDraw.WALLPAPER, x, y, x + cw, y + ch, 0F, 1F, 0F, 1F);
        if (linear < 0.78F) {
            String message = returning ? "ВОЗВРАЩАЕМСЯ" : "ЗАПУСКАЕМ";
            UiDraw.centered(g, font, message, width / 2, height - 54, 0xEFFFFFFF);
        }
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return true; }
}
