package ru.acid.realdlc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** PlayStation-style launcher with a smooth horizontal world/server carousel. */
public final class RealDlcHomeScreen extends Screen {
    private boolean multiplayer;
    private int selectedIndex;
    private int age;
    private double carouselOffset;
    private double targetOffset;
    private float contentFade = 1.0F;
    private String notice = "";
    private boolean cachedMode;
    private long cachedAt = -1000;
    private List<Card> cachedCards = new ArrayList<>();
    private static Identifier skinTexture;
    private static boolean skinRequested;
    private static boolean lastMultiplayer;

    private record Card(String title, String subtitle, String target, int sourceIndex, boolean add) {}

    public RealDlcHomeScreen() { this(lastMultiplayer); }

    public RealDlcHomeScreen(boolean openMultiplayer) {
        super(Component.literal("RealDLC"));
        this.multiplayer = openMultiplayer;
        lastMultiplayer = openMultiplayer;
        requestSkin();
    }

    public static boolean lastMultiplayer() { return lastMultiplayer; }

    private void requestSkin() {
        if (skinRequested) return;
        skinRequested = true;
        Minecraft client = Minecraft.getInstance();
        client.getSkinManager().get(client.getGameProfile()).thenAccept((Optional<PlayerSkin> skin) ->
                skin.ifPresent(value -> client.execute(() -> skinTexture = value.body().texturePath())));
    }

    private List<Card> cards() {
        if (cachedMode == multiplayer && System.currentTimeMillis() - cachedAt < 900 && !cachedCards.isEmpty()) return cachedCards;
        List<Card> result = new ArrayList<>();
        if (multiplayer) {
            try {
                ServerList list = new ServerList(Minecraft.getInstance());
                list.load();
                for (int i = 0; i < list.size(); i++) {
                    ServerData data = list.get(i);
                    result.add(new Card(data.name, data.ip, data.ip, i, false));
                }
            } catch (RuntimeException ex) {
                notice = "Не удалось прочитать список серверов";
            }
            result.add(new Card("Добавить сервер", "Название и IP", "", -1, true));
        } else {
            Path saves = Minecraft.getInstance().gameDirectory.toPath().resolve("saves");
            try {
                if (Files.isDirectory(saves)) {
                    try (var stream = Files.list(saves)) {
                        stream.filter(Files::isDirectory)
                                .filter(p -> Files.isRegularFile(p.resolve("level.dat")))
                                .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                                .forEach(p -> result.add(new Card(p.getFileName().toString(), "Локальный мир", p.getFileName().toString(), -1, false)));
                    }
                }
            } catch (IOException ex) {
                notice = "Не удалось прочитать список миров";
            }
            result.add(new Card("Создать мир", "Настройки мира", "", -1, true));
        }
        cachedMode = multiplayer;
        cachedAt = System.currentTimeMillis();
        cachedCards = result;
        selectedIndex = Math.max(0, Math.min(selectedIndex, result.size() - 1));
        targetOffset = Math.max(0, Math.min(targetOffset, Math.max(0, result.size() - 1)));
        return result;
    }

    @Override
    public void tick() {
        age++;
        // Background image and card animations are driven in render() at display refresh rate.
        cards();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long time = System.currentTimeMillis();
        UiDraw.background(g, width, height, time);
        contentFade += (1.0F - contentFade) * 0.16F;
        carouselOffset += (targetOffset - carouselOffset) * 0.22;
        if (Math.abs(targetOffset - carouselOffset) < 0.008) carouselOffset = targetOffset;

        UiDraw.centered(g, font, multiplayer ? "МНОГОПОЛЬЗОВАТЕЛЬСКАЯ ИГРА" : "ОДИНОЧНАЯ ИГРА",
                54 + (multiplayer ? 135 : 0), 86, 0xF4FFFFFF);
        UiDraw.text(g, font, multiplayer ? "Выберите сервер и подключитесь" : "Выберите мир, чтобы продолжить",
                54, 106, ((int)(0xC8 * contentFade) << 24) | 0x00E5F3FF);
        drawTopBar(g, mouseX, mouseY);

        List<Card> items = cards();
        int cardW = Math.max(110, Math.min(158, width / 7));
        int cardH = Math.max(150, Math.min(202, height / 2));
        int step = cardW + 16;
        int top = Math.min(height - cardH - 74, Math.max(128, height / 3));
        int startX = 44;
        int visibleSlots = Math.max(1, (width - 84) / step);
        if (items.size() <= visibleSlots) {
            // Center the full row; the + tile remains the rightmost card.
            startX = Math.max(44, (width - (items.size() * step - 16)) / 2);
        }

        for (int i = 0; i < items.size(); i++) {
            double relative = i - carouselOffset;
            int x = startX + (int)Math.round(relative * step);
            if (x > width + 20 || x + cardW < -20) continue;
            boolean active = i == selectedIndex;
            int scaleBoost = active ? 3 : 0;
            int y = top - scaleBoost;
            Card card = items.get(i);
            UiDraw.card(g, font, x, y, cardW, cardH, i, !multiplayer, card.add, active,
                    mouseX, mouseY);
            if (contentFade < 0.99F) g.fill(x + 3, y + 3, x + cardW - 3, y + cardH - 3, ((int)((1.0F-contentFade)*72) << 24) | 0x00030A12);
            UiDraw.label(g, font, card.title, x + cardW / 2, y + cardH - 35, cardW - 18, 0xFFFFFFFF);
            UiDraw.label(g, font, card.subtitle, x + cardW / 2, y + cardH - 19, cardW - 18, 0xA9D9E8F5);
        }

        int navY = top + cardH + 18;
        pill(g, 48, navY, 38, 30, "‹", mouseX, mouseY, selectedIndex > 0);
        pill(g, 94, navY, 38, 30, "›", mouseX, mouseY, selectedIndex < items.size() - 1);
        UiDraw.text(g, font, multiplayer ? "ENTER — подключиться   •   + — добавить сервер"
                                         : "ENTER — открыть мир   •   + — создать мир",
                148, navY + 10, 0xBDFFFFFF);
        if (items.size() > 1) {
            String count = String.format("%02d / %02d", selectedIndex + 1, items.size());
            UiDraw.text(g, font, count, width - 94, navY + 10, 0xD1FFFFFF);
        }
        if (!notice.isBlank()) UiDraw.centered(g, font, notice, width / 2, height - 28, 0xFFFFE0C1);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawTopBar(GuiGraphics g, int mx, int my) {
        int tabY = 22;
        tab(g, 26, tabY, 150, 36, "ОДИНОЧНАЯ ИГРА", !multiplayer, mx, my);
        tab(g, 184, tabY, 204, 36, "МНОГОПОЛЬЗОВАТЕЛЬСКАЯ", multiplayer, mx, my);
        int right = width - 26;
        String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        int timeW = font.width(time);
        UiDraw.text(g, font, time, right - timeW, 35, 0xF3FFFFFF);
        String name = RealDlcAccountScreen.displayName();
        int nameW = Math.min(130, font.width(name));
        int nameX = right - timeW - nameW - 42;
        UiDraw.text(g, font, name, nameX, 35, 0xEFFFFFFF);
        int size = 26, ax = nameX - size - 10, ay = 27;
        boolean hover = mx >= ax - 4 && mx <= ax + size + 4 && my >= ay - 4 && my <= ay + size + 4;
        UiDraw.avatar(g, ax, ay, size, skinTexture);
        if (hover) UiDraw.panel(g, ax - 4, ay - 4, size + 8, size + 8, 12, true);
    }

    private void tab(GuiGraphics g, int x, int y, int w, int h, String label, boolean active, int mx, int my) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        UiDraw.panel(g, x, y, w, h, 12, active || hover);
        if (active) g.fill(x + 14, y + h - 3, x + w - 14, y + h - 1, 0xFFD6F4FF);
        UiDraw.centered(g, font, label, x + w / 2, y + 13, active ? 0xFFFFFFFF : 0xCDE4F2FF);
    }

    private void pill(GuiGraphics g, int x, int y, int w, int h, String value, int mx, int my, boolean enabled) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        UiDraw.panel(g, x, y, w, h, 10, hover);
        UiDraw.centered(g, font, value, x + w / 2, y + 8, enabled ? 0xFFFFFFFF : 0x55FFFFFF);
    }

    private int cardTop() { return Math.min(height - cardHeight() - 74, Math.max(128, height / 3)); }
    private int cardWidth() { return Math.max(110, Math.min(158, width / 7)); }
    private int cardHeight() { return Math.max(150, Math.min(202, height / 2)); }
    private int cardStep() { return cardWidth() + 16; }

    private int cardStartX(List<Card> items) {
        int slots = Math.max(1, (width - 84) / cardStep());
        if (items.size() <= slots && !items.isEmpty())
            return Math.max(44, (width - (items.size() * cardStep() - 16)) / 2);
        return 44;
    }

    private void activateSelected() {
        List<Card> items = cards();
        if (items.isEmpty() || selectedIndex < 0 || selectedIndex >= items.size()) return;
        Card card = items.get(selectedIndex);
        if (card.add) {
            if (multiplayer) minecraft.setScreen(new RealDlcAddServerScreen());
            else CreateWorldScreen.openFresh(minecraft, () -> minecraft.setScreen(this));
            return;
        }
        if (multiplayer) {
            ServerList list = new ServerList(Minecraft.getInstance());
            list.load();
            int idx = card.sourceIndex;
            if (idx >= 0 && idx < list.size()) {
                ServerData server = list.get(idx);
                minecraft.setScreen(new RealDlcTransitionScreen(false, () ->
                    ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(server.ip), server, false, null)));
            }
        } else {
            minecraft.setScreen(new RealDlcTransitionScreen(false, () ->
                    minecraft.createWorldOpenFlows().openWorld(card.target, () -> minecraft.setScreen(this))));
        }
    }

    private void select(int index, int count) {
        selectedIndex = Math.max(0, Math.min(index, Math.max(0, count - 1)));
        lastMultiplayer = multiplayer;
        int visible = Math.max(1, (width - 84) / cardStep());
        targetOffset = Math.max(0, Math.min(selectedIndex - visible + 1, Math.max(0, count - visible)));
        notice = "";
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        double mx = event.x(), my = event.y();
        if (my >= 22 && my <= 58) {
            if (mx >= 26 && mx <= 176) {
                if (multiplayer) { multiplayer = false; selectedIndex = 0; targetOffset = carouselOffset = 0; cachedAt = -1000; contentFade = 0.0F; lastMultiplayer = false; }
                return true;
            }
            if (mx >= 184 && mx <= 388) {
                if (!multiplayer) { multiplayer = true; selectedIndex = 0; targetOffset = carouselOffset = 0; cachedAt = -1000; contentFade = 0.0F; lastMultiplayer = true; }
                return true;
            }
        }

        int right = width - 26;
        String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        int timeW = font.width(time);
        String name = RealDlcAccountScreen.displayName();
        int nameW = Math.min(130, font.width(name));
        int avatarX = right - timeW - nameW - 42 - 26 - 10;
        if (mx >= avatarX - 6 && mx <= avatarX + 32 && my >= 20 && my <= 60) {
            minecraft.setScreen(new RealDlcAccountScreen());
            return true;
        }

        List<Card> items = cards();
        int cardW = cardWidth(), cardH = cardHeight(), top = cardTop(), step = cardStep();
        int startX = cardStartX(items);
        for (int i = 0; i < items.size(); i++) {
            int x = startX + (int)Math.round((i - carouselOffset) * step);
            int y = top - (i == selectedIndex ? 3 : 0);
            if (mx >= x && mx <= x + cardW && my >= y && my <= y + cardH) {
                select(i, items.size());
                activateSelected();
                return true;
            }
        }
        int navY = top + cardH + 18;
        if (my >= navY && my <= navY + 30) {
            if (mx >= 48 && mx <= 86) select(selectedIndex - 1, items.size());
            if (mx >= 94 && mx <= 132) select(selectedIndex + 1, items.size());
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
        if (key == GLFW.GLFW_KEY_1) { multiplayer = false; selectedIndex = 0; targetOffset = carouselOffset = 0; cachedAt = -1000; lastMultiplayer = false; return true; }
        if (key == GLFW.GLFW_KEY_2) { multiplayer = true; selectedIndex = 0; targetOffset = carouselOffset = 0; cachedAt = -1000; lastMultiplayer = true; return true; }
        if (key == GLFW.GLFW_KEY_ESCAPE) return true;
        return super.keyPressed(event);
    }

    @Override public void onClose() { }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return true; }
}
