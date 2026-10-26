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

public final class RealDlcAccountScreen extends Screen {
    private final List<String> profiles = new ArrayList<>();
    private EditBox nickname;
    private String selectedProfile = "";
    private String notice = "";
    private Path profileFile;

    public RealDlcAccountScreen() {
        super(Component.literal("RealDLC Profiles"));
        profileFile = Minecraft.getInstance().gameDirectory.toPath().resolve("realdlc-profiles.txt");
        loadProfiles();
    }

    private void loadProfiles() {
        try {
            if (Files.exists(profileFile)) {
                for (String line : Files.readAllLines(profileFile, StandardCharsets.UTF_8)) {
                    String value = line.trim();
                    if (!value.isEmpty() && value.length() <= 16 && !profiles.contains(value)) profiles.add(value);
                }
            }
        } catch (IOException ignored) {
            notice = "Не удалось прочитать локальные профили";
        }
    }

    private void saveProfiles() {
        try {
            Files.write(profileFile, profiles, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            notice = "Не удалось сохранить профили";
        }
    }

    @Override
    protected void init() {
        int cx = width / 2;
        nickname = new EditBox(font, cx - 186, height / 2 - 112, 300, 28, Component.literal("Никнейм"));
        nickname.setMaxLength(16);
        nickname.setHint(Component.literal("Введите никнейм"));
        nickname.setBordered(false);
        nickname.setTextColor(0xFFFFFFFF);
        addRenderableWidget(nickname);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        UiDraw.drawScene(g, width, height, RealDlcHomeScreen.wallpaper);
        g.fillGradient(0, 0, width, height, 0x24040A12, 0x8C040810);
        int pw = Math.min(490, width - 32);
        int ph = Math.min(430, height - 30);
        pw = Math.max(300, pw);
        ph = Math.max(300, ph);
        int x = (width - pw) / 2;
        int y = (height - ph) / 2;
        UiDraw.rounded(g, x, y, pw, ph, 28, 0x3BFFFFFF);
        UiDraw.rounded(g, x + 1, y + 1, pw - 2, ph - 2, 27, 0xB8121C29);
        g.drawCenteredString(font, "ПРОФИЛИ REALDLC", width / 2, y + 25, 0xFFFFFFFF);

        UiDraw.rounded(g, width / 2 - 194, height / 2 - 116, 316, 36, 12, 0x40203040);
        UiDraw.rounded(g, width / 2 + 126, height / 2 - 116, 70, 36, 12, 0x52FFFFFF);
        g.drawCenteredString(font, "+ ДОБАВИТЬ", width / 2 + 161, height / 2 - 105, 0xFFFFFFFF);

        int rowY = height / 2 - 66;
        int visible = Math.min(6, profiles.size());
        for (int i = 0; i < visible; i++) {
            int ry = rowY + i * 35;
            boolean selected = profiles.get(i).equals(selectedProfile);
            UiDraw.rounded(g, width / 2 - 194, ry, 390, 30, 10, selected ? 0x5FFFFFFF : 0x24FFFFFF);
            g.drawString(font, profiles.get(i), width / 2 - 178, ry + 10, 0xFFFFFFFF);
            UiDraw.rounded(g, width / 2 + 158, ry + 2, 32, 26, 8, 0x30FFFFFF);
            g.drawCenteredString(font, "×", width / 2 + 174, ry + 8, 0xFFFFFFFF);
        }
        if (profiles.isEmpty()) {
            g.drawCenteredString(font, "Добавьте локальный профиль выше", width / 2, rowY + 18, 0xCCFFFFFF);
        }
        String liveName = Minecraft.getInstance().getUser().getName();
        g.drawCenteredString(font, "Аккаунт Minecraft: " + liveName, width / 2, y + ph - 68, 0xD0FFFFFF);
        g.drawCenteredString(font, "Локальные профили не меняют авторизованный Microsoft-аккаунт.", width / 2, y + ph - 51, 0xA8FFFFFF);
        if (!notice.isEmpty()) g.drawCenteredString(font, notice, width / 2, y + ph - 32, 0xFFFFC9A6);
        UiDraw.rounded(g, width / 2 - 58, y + ph - 34, 116, 27, 12, 0x45FFFFFF);
        g.drawCenteredString(font, "НАЗАД", width / 2, y + ph - 25, 0xFFFFFFFF);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        double mx = event.x(), my = event.y();
        int rowY = height / 2 - 66;
        if (mx >= width / 2 + 126 && mx <= width / 2 + 196 &&
                my >= height / 2 - 116 && my <= height / 2 - 80) {
            String value = nickname.getValue().trim();
            if (!value.isEmpty() && value.matches("[A-Za-z0-9_]{1,16}")) {
                if (!profiles.contains(value)) profiles.add(value);
                selectedProfile = value;
                saveProfiles();
                notice = "Профиль сохранён";
                nickname.setValue("");
            } else {
                notice = "Используйте 1–16 латинских букв, цифр или _";
            }
            return true;
        }
        for (int i = 0; i < Math.min(6, profiles.size()); i++) {
            int ry = rowY + i * 35;
            if (my >= ry && my <= ry + 31 && mx >= width / 2 - 194 && mx <= width / 2 + 196) {
                if (mx >= width / 2 + 154) {
                    String removed = profiles.remove(i);
                    if (removed.equals(selectedProfile)) selectedProfile = "";
                    saveProfiles();
                    notice = "Профиль удалён";
                } else {
                    selectedProfile = profiles.get(i);
                    notice = "Выбран профиль интерфейса: " + selectedProfile;
                }
                return true;
            }
        }
        int panelBottom = height / 2 + Math.min(430, height - 30) / 2;
        if (my >= panelBottom - 40 && mx >= width / 2 - 65 && mx <= width / 2 + 65) {
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
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(new RealDlcHomeScreen());
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
