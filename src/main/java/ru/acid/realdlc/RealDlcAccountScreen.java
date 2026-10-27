package ru.acid.realdlc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Stores nickname presets locally. Online-mode identity remains bound to the signed-in Microsoft account. */
public final class RealDlcAccountScreen extends Screen {
    private final List<String> profiles = new ArrayList<>();
    private EditBox nickname;
    private String error = "";
    private final Path file = Minecraft.getInstance().gameDirectory.toPath().resolve("realdlc-nicknames.txt");
    private static String activeNickname = "";
    private static boolean nicknameLoaded = false;

    public RealDlcAccountScreen() {
        super(Component.literal("RealDLC — профили"));
        loadProfiles();
    }

    public static String displayName() {
        if (!nicknameLoaded) {
            nicknameLoaded = true;
            Path saved = Minecraft.getInstance().gameDirectory.toPath().resolve("realdlc-nicknames.txt");
            try {
                if (Files.exists(saved)) {
                    for (String line : Files.readAllLines(saved, StandardCharsets.UTF_8)) {
                        if (line.startsWith("*") && line.length() > 1) {
                            String nick = line.substring(1).trim();
                            if (nick.matches("[A-Za-z0-9_]{1,16}")) {
                                activeNickname = nick;
                                break;
                            }
                        }
                    }
                }
            } catch (IOException ignored) {
                // Fall back to the signed-in account name if no local selection is available.
            }
        }
        String nick = activeNickname;
        if (nick != null && !nick.isBlank()) return nick;
        return Minecraft.getInstance().getUser().getName();
    }

    private void loadProfiles() {
        nicknameLoaded = true;
        try {
            if (Files.exists(file)) {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    String value = line.trim();
                    if (value.startsWith("*")) {
                        activeNickname = value.substring(1);
                        value = activeNickname;
                    }
                    if (!value.isBlank() && value.matches("[A-Za-z0-9_]{1,16}") && !profiles.contains(value)) profiles.add(value);
                }
            }
        } catch (IOException ignored) {
            error = "Не удалось прочитать профили";
        }
    }

    private void saveProfiles() {
        try {
            List<String> lines = new ArrayList<>();
            for (String profile : profiles) lines.add(profile.equals(activeNickname) ? "*" + profile : profile);
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            error = "Не удалось сохранить профили";
        }
    }

    @Override
    protected void init() {
        int pw = Math.min(540, width - 30);
        int x = (width-pw)/2;
        nickname = new EditBox(font, x+30, height/2 - 112, pw-60, 30, Component.literal("Никнейм"));
        nickname.setMaxLength(16);
        nickname.setHint(Component.literal("Латинские буквы, цифры, _"));
        nickname.setBordered(false);
        nickname.setTextColor(0xFFFFFFFF);
        addRenderableWidget(nickname);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        UiDraw.drawBackground(g, width, height, System.currentTimeMillis()/45);
        int pw = Math.min(540, width - 30), ph = Math.min(440, height-24);
        int x = (width-pw)/2, y = (height-ph)/2;
        UiDraw.panel(g, x, y, pw, ph, 26, true);
        g.drawCenteredString(font, "ПРОФИЛЬ И НИКНЕЙМЫ", width/2, y+22, 0xFFFFFFFF);
        g.drawCenteredString(font, "Аккаунт Minecraft: " + Minecraft.getInstance().getUser().getName(), width/2, y+43, 0xACDDEBFA);
        UiDraw.rounded(g, x+20, y+68, pw-40, 40, 13, 0x562B4258);
        g.drawString(font, "ДОБАВИТЬ ЛОКАЛЬНЫЙ ПРОФИЛЬ", x+28, y+57, 0xBBD9E8F5, false);

        int listY = y+124;
        int rowH = 36;
        int visible = Math.min(6, Math.max(0, (y+ph-102-listY)/rowH));
        for (int i = 0; i < visible && i < profiles.size(); i++) {
            String value = profiles.get(i);
            int ry = listY + i*rowH;
            boolean selected = value.equals(activeNickname);
            UiDraw.rounded(g, x+22, ry, pw-44, 31, 10, selected ? 0x7D4F9AC0 : 0x302D4256);
            g.drawString(font, value, x+34, ry+11, 0xFFFFFFFF, false);
            g.drawString(font, selected ? "АКТИВЕН" : "ВЫБРАТЬ", x+pw-112, ry+11, selected ? 0xFF9EDFFF : 0xB9FFFFFF, false);
        }
        if (profiles.isEmpty()) g.drawCenteredString(font, "Пока нет сохранённых профилей", width/2, listY+16, 0xBFFFFFFF);
        g.drawCenteredString(font, "Онлайн-серверы используют ник авторизованного Microsoft-аккаунта.", width/2, y+ph-61, 0xA8FFFFFF);
        g.drawCenteredString(font, "Смена аккаунта требует отдельного официального входа.", width/2, y+ph-46, 0xA8FFFFFF);
        drawButton(g, x+pw/2-60, y+ph-34, 120, 26, "ГОТОВО", mx, my, true);
        if (!error.isBlank()) g.drawCenteredString(font, error, width/2, y+ph-80, 0xFFFFC3A7);
        super.render(g, mx, my, partialTick);
    }

    private void drawButton(GuiGraphics g, int x, int y, int w, int h, String label, int mx, int my, boolean primary) {
        boolean hover = mx>=x && mx<=x+w && my>=y && my<=y+h;
        UiDraw.rounded(g, x, y, w, h, 10, hover ? 0x73FFFFFF : (primary ? 0x4DFFFFFF : 0x25FFFFFF));
        g.drawCenteredString(font, label, x+w/2, y+8, 0xFFFFFFFF);
    }

    private void addProfile() {
        String value = nickname.getValue().trim();
        if (!value.matches("[A-Za-z0-9_]{1,16}")) {
            error = "Нужно 1–16 латинских букв, цифр или _";
            return;
        }
        if (!profiles.contains(value)) profiles.add(value);
        activeNickname = value;
        saveProfiles();
        nickname.setValue("");
        error = "Профиль выбран для оформления меню";
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        int pw = Math.min(540, width-30), ph = Math.min(440, height-24);
        int x = (width-pw)/2, y = (height-ph)/2;
        if (event.x() >= x+20 && event.x() <= x+pw-20 && event.y() >= y+68 && event.y() <= y+108) {
            addProfile();
            return true;
        }
        int listY = y+124, rowH = 36;
        int visible = Math.min(6, Math.max(0, (y+ph-102-listY)/rowH));
        for (int i = 0; i < visible && i < profiles.size(); i++) {
            int ry = listY+i*rowH;
            if (event.x() >= x+22 && event.x() <= x+pw-22 && event.y() >= ry && event.y() <= ry+31) {
                activeNickname = profiles.get(i);
                saveProfiles();
                error = "Выбран профиль: " + activeNickname;
                return true;
            }
        }
        if (event.y() >= y+ph-38 && event.x() >= width/2-65 && event.x() <= width/2+65) {
            minecraft.setScreen(new RealDlcHomeScreen());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            minecraft.setScreen(new RealDlcHomeScreen());
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ENTER) {
            addProfile();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public boolean isPauseScreen() { return true; }
}
