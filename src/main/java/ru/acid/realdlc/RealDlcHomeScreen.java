package ru.acid.realdlc;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class RealDlcHomeScreen extends Screen {
    public static int wallpaper = 0;
    private int age;

    public RealDlcHomeScreen() {
        super(Component.literal("RealDLC"));
    }

    @Override
    public void tick() {
        age = Math.min(40, age + 1);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        UiDraw.drawScene(g, width, height, wallpaper);
        g.fillGradient(0, 0, width, height, 0x17061018, 0x54040A12);
        UiDraw.drawClock(g, font, width, Math.max(34, height / 7));
        int spacing = Math.min(98, Math.max(68, (width - 100) / 3));
        int cy = (int)(height * 0.60);
        int start = width / 2 - spacing * 3 / 2;
        int size = 66;
        int reveal = Math.min(255, age * 11);
        int iconColor = (reveal << 24) | 0x00FFFFFF;
        for (int i = 0; i < 4; i++) {
            int cx = start + i * spacing;
            boolean hover = Math.abs(mouseX - cx) <= size / 2 + 3 && Math.abs(mouseY - cy) <= size / 2 + 3;
            UiDraw.glassButton(g, cx - size / 2, cy - size / 2, size, hover);
            switch (i) {
                case 0 -> UiDraw.multiplayerIcon(g, cx, cy, 1, iconColor);
                case 1 -> UiDraw.personIcon(g, cx, cy, 1, iconColor);
                case 2 -> UiDraw.accountIcon(g, cx, cy, 1, iconColor);
                case 3 -> UiDraw.powerIcon(g, cx, cy, 1, iconColor);
            }
        }
        g.drawCenteredString(font, "RealDLC", width / 2, height - 24, 0x75FFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return true;
        double mx = event.x(), my = event.y();
        int spacing = Math.min(98, Math.max(68, (width - 100) / 3));
        int cy = (int)(height * 0.60);
        int start = width / 2 - spacing * 3 / 2;
        int size = 66;
        for (int i = 0; i < 4; i++) {
            int cx = start + i * spacing;
            if (Math.abs(mx - cx) <= size / 2 + 7 && Math.abs(my - cy) <= size / 2 + 7) {
                switch (i) {
                    case 0 -> minecraft.setScreen(new JoinMultiplayerScreen(this));
                    case 1 -> minecraft.setScreen(new SelectWorldScreen(this));
                    case 2 -> minecraft.setScreen(new RealDlcAccountScreen());
                    case 3 -> minecraft.stop();
                }
                return true;
            }
        }
        return true;
    }

    @Override
    public void onClose() {
        // Keep the custom launcher open; Escape acts as a no-op on this screen.
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
