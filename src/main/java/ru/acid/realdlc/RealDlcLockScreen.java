package ru.acid.realdlc;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class RealDlcLockScreen extends Screen {
    private boolean wallpaperPicker;

    public RealDlcLockScreen() {
        super(Component.literal("RealDLC Lock Screen"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        UiDraw.drawScene(g, width, height, RealDlcHomeScreen.wallpaper);
        g.fillGradient(0, 0, width, height, 0x15000000, 0x41000000);
        UiDraw.drawClock(g, font, width, Math.max(44, height / 5 - 22));

        int buttonSize = 54;
        int bx = width - buttonSize - 28;
        int by = 28;
        UiDraw.glassButton(g, bx, by, buttonSize, mouseX >= bx && mouseX <= bx + buttonSize && mouseY >= by && mouseY <= by + buttonSize);
        UiDraw.layersIcon(g, bx + buttonSize / 2, by + buttonSize / 2, 1, 0xFFFFFFFF);

        g.drawCenteredString(font, "ПРОВЕДИТЕ ВВЕРХ ИЛИ НАЖМИТЕ, ЧТОБЫ ОТКРЫТЬ", width / 2, height - 42, 0xBFFFFFFF);

        if (wallpaperPicker) drawWallpaperPicker(g, mouseX, mouseY);
    }

    private void drawWallpaperPicker(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(0, 0, width, height, 0x92030A12);
        int cardW = Math.min(180, Math.max(110, width / 5));
        int cardH = Math.min(120, Math.max(80, height / 4));
        int gap = 16;
        int total = cardW * 4 + gap * 3;
        int startX = (width - total) / 2;
        int top = (height - cardH) / 2;
        g.drawCenteredString(font, "ВЫБОР ФОНА", width / 2, top - 35, 0xFFFFFFFF);
        String[] names = {"Сумеречное озеро", "Аметистовая долина", "Холодные пики", "Закат в горах"};
        for (int i = 0; i < 4; i++) {
            int x = startX + i * (cardW + gap);
            UiDraw.rounded(g, x - 2, top - 2, cardW + 4, cardH + 4, 15,
                    RealDlcHomeScreen.wallpaper == i ? 0xE6FFFFFF : 0x60FFFFFF);
            UiDraw.rounded(g, x, top, cardW, cardH, 13, 0xFF101A27);
            int[][] colors = {
                {0xFF0A2030, 0xFF327A89}, {0xFF171034, 0xFF5C4C91},
                {0xFF10263D, 0xFF4D8491}, {0xFF381B33, 0xFFB77468}
            };
            g.fillGradient(x + 2, top + 2, x + cardW - 2, top + cardH - 2, colors[i][0], colors[i][1]);
            UiDraw.circle(g, x + cardW / 2, top + cardH / 2 - 7, Math.max(10, cardH / 8), 0x80FFFFFF);
            UiDraw.polygon(g, 0xB01D3642,
                    x + 2, top + cardH * 2 / 3, x + cardW / 3, top + cardH / 3,
                    x + cardW * 2 / 3, top + cardH * 3 / 5, x + cardW - 2, top + cardH / 2,
                    x + cardW - 2, top + cardH - 2, x + 2, top + cardH - 2);
            g.drawCenteredString(font, names[i], x + cardW / 2, top + cardH + 10, 0xFFFFFFFF);
        }
        g.drawCenteredString(font, "Нажмите на карточку, чтобы применить фон", width / 2, top + cardH + 39, 0xBFFFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return true;
        double mx = event.x(), my = event.y();
        int size = 54, bx = width - size - 28, by = 28;
        if (!wallpaperPicker && mx >= bx && mx <= bx + size && my >= by && my <= by + size) {
            wallpaperPicker = true;
            return true;
        }
        if (wallpaperPicker) {
            int cardW = Math.min(180, Math.max(110, width / 5));
            int cardH = Math.min(120, Math.max(80, height / 4));
            int gap = 16;
            int startX = (width - (cardW * 4 + gap * 3)) / 2;
            int top = (height - cardH) / 2;
            for (int i = 0; i < 4; i++) {
                int x = startX + i * (cardW + gap);
                if (mx >= x - 2 && mx <= x + cardW + 2 && my >= top - 2 && my <= top + cardH + 2) {
                    RealDlcHomeScreen.wallpaper = i;
                    wallpaperPicker = false;
                    return true;
                }
            }
            wallpaperPicker = false;
            return true;
        }
        minecraft.setScreen(new RealDlcHomeScreen());
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (wallpaperPicker) wallpaperPicker = false;
            return true;
        }
        return super.keyPressed(event);
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
