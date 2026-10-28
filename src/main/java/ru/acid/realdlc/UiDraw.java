package ru.acid.realdlc;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Style;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** UI renderer. The rounded panel, icon atlas, font and background are bundled as mod resources. */
final class UiDraw {
    static final Identifier WALLPAPER = Identifier.fromNamespaceAndPath("realdlc", "textures/gui/wallpaper.jpg");
    private static final Identifier GLASS = Identifier.fromNamespaceAndPath("realdlc", "textures/gui/glass_panel.png");
    private static final Identifier ICONS = Identifier.fromNamespaceAndPath("realdlc", "textures/gui/icons.png");
    private static final Identifier UI_FONT = Identifier.fromNamespaceAndPath("realdlc", "ui");

    private UiDraw() {}

    static Component text(String value) {
        return Component.literal(value == null ? "" : value).withStyle(Style.EMPTY.withFont(UI_FONT));
    }

    static void text(GuiGraphics g, Font font, String value, int x, int y, int color) {
        g.drawString(font, text(value), x, y, color, false);
    }

    static void centered(GuiGraphics g, Font font, String value, int x, int y, int color) {
        g.drawCenteredString(font, text(value), x, y, color);
    }

    static void background(GuiGraphics g, int w, int h, long time) {
        g.blit(WALLPAPER, 0, 0, w, h, 0.0F, 0.0F, 1.0F, 1.0F);
        g.fillGradient(0, 0, w, Math.max(1, h / 5), 0x71071322, 0x00071322);
        g.fillGradient(0, h - Math.max(1, h / 3), w, h, 0x00050B14, 0x8F050B14);
        // Low-cost drifting particles: a few individual pixels, not expensive per-frame circles.
        for (int i = 0; i < 30; i++) {
            int x = Math.floorMod(i * 173 + 31 + (int)(Math.sin(time * 0.0011 + i) * 7), Math.max(1, w));
            int y = Math.floorMod(i * 83 + 17 - (int)(time * (0.004 + (i % 3) * 0.001)), Math.max(1, h));
            int size = i % 9 == 0 ? 2 : 1;
            g.fill(x, y, x + size, y + size, i % 3 == 0 ? 0x76FFE5D8 : 0x72DDF5FF);
        }
    }

    /** Nine-slice texture rendering preserves the rounded corners at every panel size. */
    static void panel(GuiGraphics g, int x, int y, int w, int h, int radius, boolean active) {
        if (w <= 0 || h <= 0) return;
        roundedTexture(g, GLASS, x, y, w, h);
        if (active) {
            // The selected outline is painted as a thin luminous accent over the high-resolution skin.
            g.fillGradient(x + radius, y + h - 3, x + w - radius, y + h - 1, 0xB3BDEBFF, 0x0096D9FF);
        }
    }

    private static void roundedTexture(GuiGraphics g, Identifier texture, int x, int y, int w, int h) {
        int cap = Math.min(30, Math.min(w / 2, h / 2));
        int midW = Math.max(0, w - cap * 2);
        int midH = Math.max(0, h - cap * 2);
        float q = 0.25F, mid = 0.5F, far = 0.75F;
        // Four corners.
        g.blit(texture, x, y, cap, cap, 0F, 0F, q, q);
        g.blit(texture, x + w - cap, y, cap, cap, far, 0F, q, q);
        g.blit(texture, x, y + h - cap, cap, cap, 0F, far, q, q);
        g.blit(texture, x + w - cap, y + h - cap, cap, cap, far, far, q, q);
        // Four edges.
        if (midW > 0) {
            g.blit(texture, x + cap, y, midW, cap, q, 0F, mid, q);
            g.blit(texture, x + cap, y + h - cap, midW, cap, q, far, mid, q);
        }
        if (midH > 0) {
            g.blit(texture, x, y + cap, cap, midH, 0F, q, q, mid);
            g.blit(texture, x + w - cap, y + cap, cap, midH, far, q, q, mid);
        }
        if (midW > 0 && midH > 0) {
            g.blit(texture, x + cap, y + cap, midW, midH, q, q, mid, mid);
        }
    }

    static void icon(GuiGraphics g, int icon, int x, int y, int size) {
        int index = Math.floorMod(icon, 4);
        // Each SVG icon occupies one quarter of the bundled, 4x-rendered atlas.
        g.blit(ICONS, x, y, size, size, index * 0.25F, 0F, 0.25F, 1F);
    }

    static void avatar(GuiGraphics g, int x, int y, int size, Identifier skin) {
        panel(g, x - 5, y - 5, size + 10, size + 10, 12, false);
        if (skin == null) {
            icon(g, 3, x, y, size);
            return;
        }
        // Front of the player's skin head and its outer hat layer (64x64 skin layout).
        g.blit(skin, x, y, size, size, 0.125F, 0.125F, 0.125F, 0.125F);
        g.blit(skin, x, y, size, size, 0.625F, 0.125F, 0.125F, 0.125F);
    }

    static void card(GuiGraphics g, Font font, int x, int y, int w, int h, int index, boolean world, boolean add, boolean active, int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        // Crop different portions of the bundled landscape for individual card artwork.
        float u = (index % 4) * 0.13F;
        float v = (index % 2) * 0.20F;
        float du = 1.0F - u, dv = 1.0F - v;
        int ix = x + 4, iy = y + 4, iw = w - 8, imageH = h - 46;
        if (add) {
            g.fillGradient(ix, iy, ix + iw, iy + imageH, 0xFF5B6878, 0xFF2B3544);
        } else {
            g.blit(WALLPAPER, ix, iy, iw, imageH, u, v, du, dv);
            g.fillGradient(ix, iy, ix + iw, iy + imageH, 0x07050B14, 0xA8050B14);
        }
        panel(g, x, y, w, h, 18, active || hover);
        int cx = x + w / 2, cy = y + imageH / 2 + 4;
        if (add) icon(g, 0, cx - 22, cy - 22, 44);
        else icon(g, world ? 1 : 2, cx - 17, cy - 17, 34);
        // Bottom label strip remains readable against any thumbnail.
        g.fillGradient(x + 8, y + h - 44, x + w - 8, y + h - 8, 0x06101825, 0xD909111D);
        if (hover || active) {
            g.fill(x + 20, y + h - 3, x + w - 20, y + h - 1, 0xFF9CE6FF);
        }
    }

    static void label(GuiGraphics g, Font font, String value, int x, int y, int maxWidth, int color) {
        String shown = value == null ? "" : value;
        while (font.width(text(shown)) > maxWidth && shown.length() > 4) {
            shown = shown.substring(0, shown.length() - 2) + "…";
        }
        centered(g, font, shown, x, y, color);
    }

    static void clock(GuiGraphics g, Font font, int x, int y, int color) {
        text(g, font, LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), x, y, color);
    }
}
