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

/** Local nickname preset manager. Online identity remains managed by the signed-in Microsoft account. */
public final class RealDlcAccountScreen extends Screen {
    private final List<String> profiles = new ArrayList<>();
    private EditBox nickname;
    private String message = "";
    private boolean messageError;
    private final Path file = Minecraft.getInstance().gameDirectory.toPath().resolve("realdlc-nicknames.txt");
    private static String activeNickname = "";
    private static boolean loaded;

    public RealDlcAccountScreen() {
        super(Component.literal("RealDLC Accounts"));
        loadProfiles();
    }

    public static String displayName() {
        if (!loaded) {
            loaded = true;
            Path path = Minecraft.getInstance().gameDirectory.toPath().resolve("realdlc-nicknames.txt");
            try {
                if (Files.exists(path)) {
                    for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                        if (line.startsWith("*") && line.length() > 1) {
                            String candidate = line.substring(1).trim();
                            if (candidate.matches("[A-Za-z0-9_]{1,16}")) { activeNickname = candidate; break; }
                        }
                    }
                }
            } catch (IOException ignored) { }
        }
        return activeNickname.isBlank() ? Minecraft.getInstance().getUser().getName() : activeNickname;
    }

    private void loadProfiles() {
        loaded = true;
        try {
            if (!Files.exists(file)) return;
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String candidate = line.trim();
                if (candidate.startsWith("*")) {
                    candidate = candidate.substring(1).trim();
                    if (candidate.matches("[A-Za-z0-9_]{1,16}")) activeNickname = candidate;
                }
                if (candidate.matches("[A-Za-z0-9_]{1,16}") && !profiles.contains(candidate)) profiles.add(candidate);
            }
        } catch (IOException ignored) {
            setMessage("Не удалось прочитать профили", true);
        }
    }

    private void saveProfiles() {
        try {
            List<String> lines = new ArrayList<>();
            for (String value : profiles) lines.add(value.equals(activeNickname) ? "*" + value : value);
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            setMessage("Не удалось сохранить профили", true);
        }
    }

    @Override
    protected void init() {
        int pw = Math.min(560, width - 32);
        int x = (width - pw) / 2;
        int y = (height - Math.min(452, height - 24)) / 2;
        nickname = new EditBox(font, x + 28, y + 80, pw - 148, 32, Component.literal("Никнейм"));
        nickname.setMaxLength(16);
        nickname.setHint(UiDraw.text("Новый ник"));
        nickname.setBordered(false);
        nickname.setTextColor(0xFFFFFFFF);
        addRenderableWidget(nickname);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        UiDraw.background(g, width, height, System.currentTimeMillis());
        int pw = Math.min(560, width - 32), ph = Math.min(452, height - 24);
        int x = (width - pw)/2, y = (height - ph)/2;
        UiDraw.panel(g, x, y, pw, ph, 24, true);
        UiDraw.centered(g, font, "УПРАВЛЕНИЕ ПРОФИЛЯМИ", width/2, y + 24, 0xFFFFFFFF);
        UiDraw.centered(g, font, "В игре отображается: " + displayName(), width/2, y + 47, 0xD0EAF7FF);
        UiDraw.text(g, font, "СОХРАНЁННЫЕ НИКИ", x + 28, y + 61, 0xAEE2F2FF);
        UiDraw.panel(g, x + 20, y + 76, pw - 144, 40, 10, false);
        UiDraw.panel(g, x + pw - 112, y + 76, 92, 40, 10, true);
        UiDraw.centered(g, font, "СОХРАНИТЬ", x + pw - 66, y + 91, 0xFFFFFFFF);

        int rowY = y + 130, rowH = 36;
        int visible = Math.min(6, Math.max(0, (y + ph - 106 - rowY) / rowH));
        for (int i = 0; i < visible && i < profiles.size(); i++) {
            String value = profiles.get(i);
            int ry = rowY + i * rowH;
            boolean active = value.equals(activeNickname);
            UiDraw.panel(g, x + 22, ry, pw - 44, 31, 10, active);
            UiDraw.text(g, font, value, x + 35, ry + 10, 0xFFFFFFFF);
            UiDraw.text(g, font, active ? "АКТИВЕН" : "ВЫБРАТЬ", x + pw - 154, ry + 10, active ? 0xFFB5EBFF : 0xCCFFFFFF);
            UiDraw.centered(g, font, "×", x + pw - 45, ry + 8, 0xDFFFFFFF);
        }
        if (profiles.isEmpty()) UiDraw.centered(g, font, "Добавьте первый локальный профиль", width/2, rowY + 18, 0xBDFFFFFF);

        UiDraw.centered(g, font, "Локальные профили меняют подпись меню, а не Microsoft-аккаунт.", width/2, y + ph - 64, 0xACD7E8F4);
        UiDraw.centered(g, font, "Для реального сетевого ника нужна отдельная авторизованная учётная запись.", width/2, y + ph - 48, 0xACD7E8F4);
        UiDraw.panel(g, width/2 - 58, y + ph - 36, 116, 26, 9, false);
        UiDraw.centered(g, font, "ГОТОВО", width/2, y + ph - 28, 0xFFFFFFFF);
        if (!message.isEmpty()) UiDraw.centered(g, font, message, width/2, y + ph - 82, messageError ? 0xFFFFB9A2 : 0xFFB5F3CF);
        super.render(g, mx, my, partialTick);
    }

    private void setMessage(String text, boolean error) { message = text; messageError = error; }

    private void addProfile() {
        String value = nickname.getValue().trim();
        if (!value.matches("[A-Za-z0-9_]{1,16}")) {
            setMessage("Используйте 1–16 латинских букв, цифр или _", true);
            return;
        }
        if (!profiles.contains(value)) profiles.add(value);
        activeNickname = value;
        saveProfiles();
        nickname.setValue("");
        setMessage("Профиль выбран для меню", false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        int pw = Math.min(560, width - 32), ph = Math.min(452, height - 24);
        int x = (width - pw)/2, y = (height - ph)/2;
        int saveX = x + pw - 112, saveY = y + 76;
        if (event.x() >= saveX && event.x() <= saveX+92 && event.y() >= saveY && event.y() <= saveY+40) {
            addProfile();
            return true;
        }
        int rowY = y + 130, rowH = 36;
        int visible = Math.min(6, Math.max(0, (y + ph - 106 - rowY) / rowH));
        for (int i=0; i<visible && i<profiles.size(); i++) {
            int ry = rowY+i*rowH;
            if (event.x() >= x+22 && event.x() <= x+pw-22 && event.y() >= ry && event.y() <= ry+31) {
                if (event.x() >= x+pw-66) {
                    String removed = profiles.remove(i);
                    if (removed.equals(activeNickname)) activeNickname = profiles.isEmpty() ? "" : profiles.get(0);
                    saveProfiles();
                    setMessage("Профиль удалён", false);
                } else {
                    activeNickname = profiles.get(i);
                    saveProfiles();
                    setMessage("Выбран профиль: " + activeNickname, false);
                }
                return true;
            }
        }
        if (event.y() >= y + ph - 40 && event.x() >= width/2 - 65 && event.x() <= width/2 + 65) {
            minecraft.setScreen(new RealDlcHomeScreen());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) { minecraft.setScreen(new RealDlcHomeScreen()); return true; }
        if (event.key() == GLFW.GLFW_KEY_ENTER && nickname.isFocused()) { addProfile(); return true; }
        return super.keyPressed(event);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return true; }
}
