package ru.acid.realdlc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Custom rounded add-server dialog; saved entries use Minecraft's own server list format. */
public final class RealDlcAddServerScreen extends Screen {
    private EditBox name;
    private EditBox address;
    private String error = "";
    private int age;

    public RealDlcAddServerScreen() {
        super(Component.literal("Добавить сервер"));
    }

    @Override
    protected void init() {
        int pw = Math.min(520, width - 36);
        int x = (width - pw) / 2;
        int y = height / 2;
        name = new EditBox(font, x + 34, y - 52, pw - 68, 30, Component.literal("Название"));
        name.setMaxLength(64);
        name.setHint(Component.literal("Например, Друзья"));
        name.setBordered(false);
        name.setTextColor(0xFFFFFFFF);
        address = new EditBox(font, x + 34, y + 12, pw - 68, 30, Component.literal("IP-адрес"));
        address.setMaxLength(255);
        address.setHint(Component.literal("play.example.net:25565"));
        address.setBordered(false);
        address.setTextColor(0xFFFFFFFF);
        addRenderableWidget(name);
        addRenderableWidget(address);
        setInitialFocus(name);
    }

    @Override
    public void tick() {
        age++;
        super.tick();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        UiDraw.drawBackground(g, width, height, age);
        int pw = Math.min(520, width - 36), ph = 292;
        int x = (width - pw)/2, y = (height - ph)/2;
        UiDraw.panel(g, x, y, pw, ph, 25, true);
        g.drawCenteredString(font, "НОВЫЙ СЕРВЕР", width/2, y + 22, 0xFFFFFFFF);
        g.drawString(font, "НАЗВАНИЕ", x + 34, y + 50, 0xBBD9E8F5, false);
        UiDraw.rounded(g, x + 24, y + 65, pw - 48, 38, 12, 0x552D465B);
        g.drawString(font, "АДРЕС СЕРВЕРА", x + 34, y + 115, 0xBBD9E8F5, false);
        UiDraw.rounded(g, x + 24, y + 130, pw - 48, 38, 12, 0x552D465B);
        g.drawString(font, "Пример: play.example.net или 123.45.67.89:25565", x + 32, y + 176, 0x75FFFFFF, false);
        drawButton(g, x + pw - 182, y + ph - 53, 150, 34, "СОХРАНИТЬ", mx, my, true);
        drawButton(g, x + 32, y + ph - 53, 104, 34, "НАЗАД", mx, my, false);
        if (!error.isEmpty()) g.drawCenteredString(font, error, width/2, y + ph - 69, 0xFFFFC2A5);
        super.render(g, mx, my, partialTick);
    }

    private void drawButton(GuiGraphics g, int x, int y, int w, int h, String label, int mx, int my, boolean primary) {
        boolean hover = mx >= x && mx <= x+w && my >= y && my <= y+h;
        UiDraw.rounded(g, x, y, w, h, 12, primary ? (hover ? 0xC66AD6F2 : 0xA94EADD0) : (hover ? 0x65FFFFFF : 0x25FFFFFF));
        g.drawCenteredString(font, label, x+w/2, y+12, primary ? 0xFF07131D : 0xEFFFFFFF);
    }

    private void saveServer() {
        String serverName = name.getValue().trim();
        String host = address.getValue().trim();
        if (serverName.isBlank()) serverName = host;
        if (serverName.isBlank() || host.isBlank()) {
            error = "Заполните название и адрес сервера";
            return;
        }
        if (host.contains(" ") || host.length() > 255) {
            error = "Проверьте адрес сервера";
            return;
        }
        ServerList servers = new ServerList(Minecraft.getInstance());
        servers.load();
        for (int i = 0; i < servers.size(); i++) {
            if (servers.get(i).ip.equalsIgnoreCase(host)) {
                error = "Такой адрес уже есть в списке";
                return;
            }
        }
        servers.add(new ServerData(serverName, host, ServerData.Type.OTHER), false);
        servers.save();
        minecraft.setScreen(new RealDlcHomeScreen(true));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        int pw = Math.min(520, width - 36), ph = 292, x = (width-pw)/2, y = (height-ph)/2;
        int bx = x + pw - 182, by = y + ph - 53;
        if (event.x() >= bx && event.x() <= bx+150 && event.y() >= by && event.y() <= by+34) {
            saveServer();
            return true;
        }
        if (event.x() >= x+32 && event.x() <= x+136 && event.y() >= by && event.y() <= by+34) {
            minecraft.setScreen(new RealDlcHomeScreen(true));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            minecraft.setScreen(new RealDlcHomeScreen(true));
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ENTER && !name.isFocused()) {
            saveServer();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public boolean isPauseScreen() { return true; }
}
