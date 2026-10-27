package ru.acid.realdlc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerSkin;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** PS-style launcher: custom tabs and a moving horizontal carousel of saves or servers. */
public final class RealDlcHomeScreen extends Screen {
    private boolean multiplayer;
    private int selectedIndex;
    private int age;
    private double carouselOffset;
    private double targetOffset;
    private String notice = "";
    private boolean cachedMode = false;
    private int cachedAt = -1000;
    private List<Card> cachedCards = new ArrayList<>();
    private static ResourceLocation skinTexture;
    private static boolean skinRequested;

    private record Card(String title, String subtitle, String target, boolean add) {}

    public RealDlcHomeScreen() {
        this(false);
    }

    public RealDlcHomeScreen(boolean openMultiplayer) {
        super(Component.literal("RealDLC"));
        this.multiplayer = openMultiplayer;
        requestSkin();
    }

    private void requestSkin() {
        if (skinRequested) return;
        skinRequested = true;
        Minecraft client = Minecraft.getInstance();
        client.getSkinManager().get(client.getGameProfile()).thenAccept((Optional<PlayerSkin> skin) ->
                skin.ifPresent(value -> client.execute(() -> skinTexture = value.body().texturePath())));
    }

    private List<Card> cards() {
        if (cachedMode == multiplayer && age - cachedAt < 20 && !cachedCards.isEmpty()) return cachedCards;
        List<Card> result = new ArrayList<>();
        result.add(new Card(multiplayer ? "Добавить сервер" : "Создать мир", multiplayer ? "Имя и адрес" : "Новый мир", "", true));
        if (multiplayer) {
            try {
                ServerList list = new ServerList(Minecraft.getInstance());
                list.load();
                for (int i = 0; i < list.size(); i++) {
                    ServerData data = list.get(i);
                    result.add(new Card(data.name, data.ip, data.ip, false));
                }
            } catch (RuntimeException ex) {
                notice = "Не удалось прочитать список серверов";
            }
        } else {
            Path saves = Minecraft.getInstance().gameDirectory.toPath().resolve("saves");
            try {
                if (Files.isDirectory(saves)) {
                    try (var stream = Files.list(saves)) {
                        stream.filter(Files::isDirectory)
                                .filter(p -> Files.isRegularFile(p.resolve("level.dat")))
                                .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                                .forEach(p -> result.add(new Card(p.getFileName().toString(), "Локальный мир", p.getFileName().toString(), false)));
                    }
                }
            } catch (IOException ex) {
                notice = "Не удалось прочитать папку миров";
            }
        }
        cachedMode = multiplayer;
        cachedAt = age;
        cachedCards = result;
        return result;
    }

    @Override
    public void tick() {
        age++;
        carouselOffset += (targetOffset - carouselOffset) * 0.22;
        if (Math.abs(targetOffset - carouselOffset) < 0.015) carouselOffset = targetOffset;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        UiDraw.drawBackground(g, width, height, age);
        float reveal = Math.min(1F, (age + partialTick) / 18F);
        g.fill(0, 0, width, height, (int)(0x30000000 * reveal));

        drawTopBar(g, mouseX, mouseY);
        int headingY = Math.max(95, height / 4);
        g.drawString(font, multiplayer ? "МНОГОПОЛЬЗОВАТЕЛЬСКАЯ ИГРА" : "ОДИНОЧНАЯ ИГРА",
                54, headingY - 24, 0xEFFFFFFF, false);
        g.drawString(font, multiplayer ? "Выберите сервер и подключитесь" : "Выберите мир, чтобы продолжить",
                54, headingY - 7, 0xA9D5E3F4, false);

        List<Card> items = cards();
        int cardW = Math.max(106, Math.min(144, width / 7));
        int cardH = Math.max(146, Math.min(184, height / 2));
        int step = cardW + 14;
        int top = Math.min(height - cardH - 70, headingY + 18);
        int startX = 48;
        int visible = Math.min(items.size(), Math.max(1, (width - 64) / step + 2));
        for (int i = 0; i < items.size(); i++) {
            double relative = i - carouselOffset;
            int x = startX + (int)Math.round(relative * step);
            if (x > width + 10 || x + cardW < -10) continue;
            boolean active = i == selectedIndex;
            int y = top + (active ? -4 : 0);
            Card card = items.get(i);
            boolean hover = mouseX >= x && mouseX <= x + cardW && mouseY >= y && mouseY <= y + cardH;
            UiDraw.drawCardArtwork(g, x, y, cardW, cardH, !multiplayer, active ? 0xFF294D68 : 0xFF1C2A3B, age + i * 13, active);
            if (card.add) {
                UiDraw.plusIcon(g, x + cardW/2, y + cardH/2 - 3, Math.min(cardW, cardH)/4, 0xFFF3FAFF);
            }
            UiDraw.drawLabel(g, font, card.title, x + cardW/2, y + cardH - 38, cardW - 12, active ? 0xFFFFFFFF : 0xD7FFFFFF);
            UiDraw.drawLabel(g, font, card.subtitle, x + cardW/2, y + cardH - 23, cardW - 12, 0x91D9E8F5);
            if (hover) {
                UiDraw.rounded(g, x + 7, y + cardH - 5, cardW - 14, 2, 1, 0xB5A9E9FF);
            }
        }

        int navY = top + cardH + 18;
        drawPill(g, 54, navY, 34, 28, "‹", mouseX, mouseY, selectedIndex > 0);
        drawPill(g, 96, navY, 34, 28, "›", mouseX, mouseY, selectedIndex < items.size() - 1);
        String hint = multiplayer ? "ENTER — подключиться     •     Нажмите +, чтобы добавить сервер"
                                  : "ENTER — открыть мир     •     Нажмите +, чтобы открыть список миров";
        g.drawString(font, hint, 148, navY + 9, 0xBFFFFFFF, false);
        if (items.size() > 1) {
            String count = String.format("%02d / %02d", selectedIndex + 1, items.size());
            g.drawString(font, count, width - 110, navY + 9, 0xA7FFFFFF, false);
        }
        if (!notice.isBlank()) g.drawCenteredString(font, notice, width/2, height - 29, 0xFFFFD4AE);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawTopBar(GuiGraphics g, int mx, int my) {
        int tabY = 24;
        drawTab(g, 26, tabY, 150, 34, "ОДИНОЧНАЯ ИГРА", !multiplayer, mx, my);
        drawTab(g, 184, tabY, 190, 34, "МНОГОПОЛЬЗОВАТЕЛЬСКАЯ", multiplayer, mx, my);

        int right = width - 26;
        String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        int timeW = font.width(time);
        g.drawString(font, time, right - timeW, 37, 0xF3FFFFFF, false);
        int nameW = Math.min(126, font.width(RealDlcAccountScreen.displayName()));
        int nameX = right - timeW - nameW - 42;
        g.drawString(font, RealDlcAccountScreen.displayName(), nameX, 37, 0xDFFFFFFF, false);
        int avatarSize = 26;
        int avatarX = nameX - avatarSize - 10;
        int avatarY = 28;
        boolean hover = mx >= avatarX - 5 && mx <= avatarX + avatarSize + 5 && my >= avatarY - 5 && my <= avatarY + avatarSize + 5;
        UiDraw.drawAvatar(g, avatarX, avatarY, avatarSize, skinTexture);
        if (hover) UiDraw.rounded(g, avatarX - 4, avatarY - 4, avatarSize + 8, avatarSize + 8, 9, 0x24FFFFFF);
    }

    private void drawTab(GuiGraphics g, int x, int y, int w, int h, String label, boolean active, int mx, int my) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        UiDraw.rounded(g, x, y, w, h, 12, active ? 0xB52D6682 : hover ? 0x5AFFFFFF : 0x1CFFFFFF);
        UiDraw.rounded(g, x + 1, y + 1, w - 2, h - 2, 11, active ? 0xCC142A3A : 0x80111B28);
        g.drawCenteredString(font, label, x + w/2, y + 13, active ? 0xFFFFFFFF : 0xBFDDEAF4);
        if (active) UiDraw.rounded(g, x + 15, y + h - 4, w - 30, 2, 1, 0xFF8CE1FF);
    }

    private void drawPill(GuiGraphics g, int x, int y, int w, int h, String label, int mx, int my, boolean enabled) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        UiDraw.rounded(g, x, y, w, h, 10, hover ? 0x68FFFFFF : 0x27FFFFFF);
        g.drawCenteredString(font, label, x + w/2, y + 8, enabled ? 0xFFFFFFFF : 0x55FFFFFF);
    }

    private int cardTop() {
        int headingY = Math.max(95, height / 4);
        int cardH = Math.max(146, Math.min(184, height / 2));
        return Math.min(height - cardH - 70, headingY + 18);
    }

    private int cardWidth() { return Math.max(106, Math.min(144, width / 7)); }
    private int cardHeight() { return Math.max(146, Math.min(184, height / 2)); }
    private int cardStep() { return cardWidth() + 14; }

    private void activateSelected() {
        List<Card> items = cards();
        if (items.isEmpty() || selectedIndex < 0 || selectedIndex >= items.size()) return;
        Card card = items.get(selectedIndex);
        if (card.add) {
            if (multiplayer) minecraft.setScreen(new RealDlcAddServerScreen());
            else minecraft.setScreen(new SelectWorldScreen(this));
            return;
        }
        if (multiplayer) {
            ServerList list = new ServerList(Minecraft.getInstance());
            list.load();
            int idx = selectedIndex - 1;
            if (idx >= 0 && idx < list.size()) {
                ServerData server = list.get(idx);
                ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(server.ip), server, false, null);
            }
        } else {
            minecraft.createWorldOpenFlows().openWorld(card.target, () -> minecraft.setScreen(this));
        }
    }

    private void select(int index, int count) {
        selectedIndex = Math.max(0, Math.min(index, Math.max(0, count - 1)));
        targetOffset = Math.max(0, selectedIndex - 1);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        double mx = event.x(), my = event.y();
        if (my >= 24 && my <= 58) {
            if (mx >= 26 && mx <= 176) { multiplayer = false; selectedIndex = 0; targetOffset = carouselOffset = 0; cachedAt = -1000; notice = ""; return true; }
            if (mx >= 184 && mx <= 374) { multiplayer = true; selectedIndex = 0; targetOffset = carouselOffset = 0; cachedAt = -1000; notice = ""; return true; }
        }
        int right = width - 26;
        String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        int timeW = font.width(time);
        int nameW = Math.min(126, font.width(RealDlcAccountScreen.displayName()));
        int avatarX = right - timeW - nameW - 42 - 26 - 10;
        if (mx >= avatarX - 5 && mx <= avatarX + 31 && my >= 22 && my <= 60) {
            minecraft.setScreen(new RealDlcAccountScreen());
            return true;
        }

        List<Card> items = cards();
        int cardW = cardWidth(), cardH = cardHeight(), top = cardTop(), step = cardStep(), startX = 48;
        for (int i = 0; i < items.size(); i++) {
            int x = startX + (int)Math.round((i - carouselOffset) * step);
            int y = top + (i == selectedIndex ? -4 : 0);
            if (mx >= x && mx <= x + cardW && my >= y && my <= y + cardH) {
                if (items.get(i).add) {
                    select(i, items.size());
                    activateSelected();
                } else if (i == selectedIndex && doubleClick) {
                    activateSelected();
                } else {
                    select(i, items.size());
                    notice = "";
                }
                return true;
            }
        }
        int navY = top + cardH + 18;
        if (my >= navY && my <= navY + 28) {
            if (mx >= 54 && mx <= 88) select(selectedIndex - 1, items.size());
            if (mx >= 96 && mx <= 130) select(selectedIndex + 1, items.size());
            if (selectedIndex == 0 && mx >= 54 && mx <= 88) notice = "";
            return true;
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_LEFT) { select(selectedIndex - 1, cards().size()); return true; }
        if (key == GLFW.GLFW_KEY_RIGHT) { select(selectedIndex + 1, cards().size()); return true; }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { activateSelected(); return true; }
        if (key == GLFW.GLFW_KEY_1) { multiplayer = false; selectedIndex = 0; targetOffset = carouselOffset = 0; cachedAt = -1000; return true; }
        if (key == GLFW.GLFW_KEY_2) { multiplayer = true; selectedIndex = 0; targetOffset = carouselOffset = 0; cachedAt = -1000; return true; }
        if (key == GLFW.GLFW_KEY_ESCAPE) return true;
        return super.keyPressed(event);
    }

    @Override
    public void onClose() { /* The launcher remains open; Exit is the explicit quit action. */ }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public boolean isPauseScreen() { return true; }
}
