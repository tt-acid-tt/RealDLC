package ru.acid.realdlc;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class RealDlcWelcomeScreen extends Screen {
    private int age;

    public RealDlcWelcomeScreen() {
        super(Component.literal("RealDLC"));
    }

    @Override
    public void tick() {
        age++;
        if (age >= 56) minecraft.setScreen(new RealDlcHomeScreen());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long t = System.currentTimeMillis();
        UiDraw.background(g, width, height, t);
        float ageF = age + partialTick;
        float fadeIn = Math.min(1F, ageF / 12F);
        float fadeOut = ageF < 36 ? 1F : Math.max(0F, (56F - ageF) / 20F);
        int alpha = (int)(255 * fadeIn * fadeOut);
        g.pose().pushMatrix();
        g.pose().translate(width / 2F, height / 2F - 18F);
        float scale = 0.92F + 0.08F * fadeIn;
        g.pose().scale(scale, scale);
        UiDraw.centered(g, font, "С возвращением", 0, 0, (alpha << 24) | 0x00FFFFFF);
        g.pose().popMatrix();
        UiDraw.centered(g, font, RealDlcAccountScreen.displayName(), width / 2, height / 2 + 25,
                ((int)(alpha * 0.86F) << 24) | 0x00D6F0FF);
        g.fill(width / 2 - 28, height / 2 + 57, width / 2 + 28, height / 2 + 59,
                (alpha << 24) | 0x009BE7FF);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (age > 12) minecraft.setScreen(new RealDlcHomeScreen());
        return true;
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return true; }
}
