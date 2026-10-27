package ru.acid.realdlc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Brief welcome transition shown after returning to the title/menu entry point. */
public final class RealDlcWelcomeScreen extends Screen {
    private int age;

    public RealDlcWelcomeScreen() {
        super(Component.literal("RealDLC"));
    }

    @Override
    public void tick() {
        age++;
        if (age >= 54) {
            minecraft.setScreen(new RealDlcHomeScreen());
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        UiDraw.drawBackground(g, width, height, age);
        float elapsed = age + partialTick;
        float fadeIn = Math.min(1.0F, elapsed / 10.0F);
        float fadeOut = elapsed < 34 ? 1.0F : Math.max(0.0F, (54.0F - elapsed) / 20.0F);
        int a = (int)(255 * fadeIn * fadeOut);
        String nickname = RealDlcAccountScreen.displayName();
        g.pose().pushMatrix();
        g.pose().translate(width / 2.0F, height / 2.0F - 12.0F);
        float scale = 1.0F + Math.min(0.07F, elapsed * 0.0015F);
        g.pose().scale(scale, scale);
        g.drawCenteredString(font, Component.literal("С возвращением"), 0, 0, (a << 24) | 0x00FFFFFF);
        g.pose().popMatrix();
        g.drawCenteredString(font, Component.literal(nickname), width / 2, height / 2 + 24,
                ((int)(a * 0.80F) << 24) | 0x00CDEBFF);
        UiDraw.rounded(g, width/2 - 34, height/2 + 60, 68, 2, 1, (a << 24) | 0x0084D9FF);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (age > 14) minecraft.setScreen(new RealDlcHomeScreen());
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public boolean isPauseScreen() { return true; }
}
