package ru.acid.realdlc;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** A deliberately empty glass panel. The blur is applied by Minecraft's GUI render pipeline. */
public final class RealDlcScreen extends Screen {
    public RealDlcScreen() {
        super(Component.literal("RealDLC"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blurBeforeThisStratum();
        // 15% black overlay, then a very transparent rounded pane.
        graphics.fill(0, 0, width, height, 0x26000000);
        int panelWidth = Math.min(760, width - 40);
        int panelHeight = Math.min(430, height - 40);
        panelWidth = Math.max(220, panelWidth);
        panelHeight = Math.max(160, panelHeight);
        int x = (width - panelWidth) / 2;
        int y = (height - panelHeight) / 2;

        UiDraw.rounded(graphics, x, y, panelWidth, panelHeight, 34, 0x40FFFFFF);
        UiDraw.rounded(graphics, x + 1, y + 1, panelWidth - 2, panelHeight - 2, 33, 0x14DCEFFF);
        // Soft edge and tiny upper reflection. No labels or controls are intentionally added.
        UiDraw.rounded(graphics, x + 14, y + 1, panelWidth - 28, 1, 1, 0x32FFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT || event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
