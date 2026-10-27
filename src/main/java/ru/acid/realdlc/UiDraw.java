package ru.acid.realdlc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Resolution-independent UI primitives; no enlarged bitmap artwork is used for panels and icons. */
final class UiDraw {
    private UiDraw() {}

    static int alpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    static void rounded(GuiGraphics g, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (r == 0) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        for (int row = 0; row < h; row++) {
            int edge = Math.min(row, h - 1 - row);
            double inset = 0;
            if (edge < r) {
                double d = r - edge - 0.45;
                inset = r - Math.sqrt(Math.max(0.0, (double) r * r - d * d));
            }
            int left = x + (int) Math.ceil(inset);
            int right = x + w - (int) Math.ceil(inset);
            if (right > left) g.fill(left, y + row, right, y + row + 1, color);
        }
    }

    static void panel(GuiGraphics g, int x, int y, int w, int h, int radius, boolean selected) {
        rounded(g, x, y + 5, w, h, radius, 0x30000000);
        rounded(g, x, y, w, h, radius, selected ? 0x4A9AEFFF : 0x32FFFFFF);
        rounded(g, x + 1, y + 1, w - 2, h - 2, Math.max(1, radius - 1), selected ? 0xD8172536 : 0xD9101722);
        rounded(g, x + 10, y + 2, Math.max(1, w - 20), 1, 1, 0x3FFFFFFF);
    }

    static void line(GuiGraphics g, int x1, int y1, int x2, int y2, int color, int thickness) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps == 0) {
            g.fill(x1, y1, x1 + thickness, y1 + thickness, color);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int y = (int) Math.round(y1 + (y2 - y1) * t);
            g.fill(x - thickness / 2, y - thickness / 2,
                    x + (thickness + 1) / 2, y + (thickness + 1) / 2, color);
        }
    }

    static void circle(GuiGraphics g, int cx, int cy, int radius, int color) {
        if (radius <= 0) return;
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int) Math.floor(Math.sqrt(Math.max(0, radius * radius - dy * dy)));
            g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    static void ring(GuiGraphics g, int cx, int cy, int radius, int color, int thickness) {
        int inner = Math.max(0, radius - thickness);
        circle(g, cx, cy, radius, color);
        if (inner > 0) circle(g, cx, cy, inner, 0x00000000);
        // Draw the inner cutout with a small dark transparent-looking disc is avoided; callers
        // use rings only on dark UI, so use thin radial ticks for a crisp vector-like contour.
        int dots = Math.max(40, radius * 8);
        for (int i = 0; i < dots; i++) {
            double a = Math.PI * 2 * i / dots;
            int x = cx + (int) Math.round(Math.cos(a) * radius);
            int y = cy + (int) Math.round(Math.sin(a) * radius);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    static void drawBackground(GuiGraphics g, int w, int h, long age) {
        g.fillGradient(0, 0, w, h, 0xFF0A1422, 0xFF070B13);
        // Layered soft glows are generated at GUI resolution rather than stretched from a texture.
        int gx = (int)(w * 0.78 + Math.sin(age * 0.008) * w * 0.035);
        int gy = (int)(h * 0.34 + Math.cos(age * 0.006) * h * 0.025);
        for (int i = 15; i >= 1; i--) {
            int r = Math.max(1, (int)(Math.min(w, h) * i * 0.022));
            circle(g, gx, gy, r, alpha(0xFF347EA0, (16 - i) * 2));
        }
        int sx = (int)(w * 0.16 + Math.sin(age * 0.004) * 20);
        int sy = (int)(h * 0.80);
        for (int i = 14; i >= 1; i--) {
            circle(g, sx, sy, Math.max(1, i * 12), alpha(0xFF5B72BA, (15 - i) * 2));
        }

        // Drifting dust particles and very fine stars.
        for (int i = 0; i < 100; i++) {
            int px = Math.floorMod(i * 173 + 29 + (int)(Math.sin(age * 0.012 + i) * 8), Math.max(1, w));
            int py = Math.floorMod(i * 79 + 41 - (int)(age * (0.12 + (i % 4) * 0.035)), Math.max(1, h));
            int size = i % 19 == 0 ? 2 : 1;
            int col = i % 3 == 0 ? 0xA6B8E9FF : 0x5AFFFFFF;
            g.fill(px, py, px + size, py + size, col);
        }
        // Low-contrast, angular distant ridges give the background depth without Minecraft textures.
        int horizon = (int)(h * 0.70);
        g.fillGradient(0, horizon, w, h, 0x180E4560, 0xB8070C15);
        polygon(g, 0x502A5B78, new int[]{0,(int)(h*.69), (int)(w*.13),(int)(h*.55), (int)(w*.24),(int)(h*.66), (int)(w*.42),(int)(h*.49), (int)(w*.59),(int)(h*.67), (int)(w*.77),(int)(h*.53), w,(int)(h*.68), w,h, 0,h});
        polygon(g, 0x66203B55, new int[]{0,(int)(h*.77), (int)(w*.18),(int)(h*.65), (int)(w*.34),(int)(h*.75), (int)(w*.53),(int)(h*.61), (int)(w*.73),(int)(h*.76), (int)(w*.91),(int)(h*.64), w,(int)(h*.72), w,h, 0,h});
        for (int i = 0; i < 24; i++) {
            int x = Math.floorMod(i * 127 + 37, Math.max(1, w));
            int y = (int)(h * 0.79) + (i % 8) * Math.max(2, h / 105);
            int rw = Math.max(8, w / (22 + i % 9));
            g.fill(x, y, Math.min(w, x + rw), y + 1, (i % 3 == 0) ? 0x2A9DD8E8 : 0x1FFFFFFF);
        }
        g.fillGradient(0, 0, w, Math.max(1, h / 5), 0x28000000, 0x00000000);
        g.fillGradient(0, h - Math.max(1, h / 4), w, h, 0x00000000, 0x70000000);
    }

    private static void polygon(GuiGraphics g, int color, int[] xy) {
        if (xy.length < 6 || (xy.length & 1) != 0) return;
        int edges = xy.length / 2;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (int i = 1; i < xy.length; i += 2) {
            minY = Math.min(minY, xy[i]);
            maxY = Math.max(maxY, xy[i]);
        }
        for (int y = minY; y <= maxY; y++) {
            double[] nodes = new double[edges];
            int count = 0;
            for (int i = 0, j = edges - 1; i < edges; j = i++) {
                int xi = xy[i * 2], yi = xy[i * 2 + 1];
                int xj = xy[j * 2], yj = xy[j * 2 + 1];
                if ((yi < y && yj >= y) || (yj < y && yi >= y)) {
                    nodes[count++] = xi + (double)(y - yi) * (xj - xi) / (yj - yi);
                }
            }
            java.util.Arrays.sort(nodes, 0, count);
            for (int i = 0; i + 1 < count; i += 2) {
                g.fill((int)Math.ceil(nodes[i]), y, (int)Math.floor(nodes[i + 1]) + 1, y + 1, color);
            }
        }
    }

    static void drawClock(GuiGraphics g, Font font, int x, int y, int color) {
        String value = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        g.drawString(font, value, x, y, color, false);
    }

    static void plusIcon(GuiGraphics g, int cx, int cy, int size, int color) {
        int t = Math.max(2, size / 10);
        rounded(g, cx - t / 2, cy - size / 3, t, size * 2 / 3, t / 2, color);
        rounded(g, cx - size / 3, cy - t / 2, size * 2 / 3, t, t / 2, color);
    }

    static void worldIcon(GuiGraphics g, int cx, int cy, int s, int color) {
        int w = s, h = (int)(s * 0.66);
        rounded(g, cx - w/2, cy - h/2, w, h, Math.max(3, s/8), color);
        line(g, cx - w/2 + 3, cy + h/2 - 3, cx - w/5, cy - h/6, 0xFF8DE1D7, Math.max(1,s/18));
        line(g, cx - w/5, cy - h/6, cx + w/8, cy + h/5, 0xFF8DE1D7, Math.max(1,s/18));
        line(g, cx + w/8, cy + h/5, cx + w/3, cy - h/3, 0xFF8DE1D7, Math.max(1,s/18));
        circle(g, cx + w/4, cy - h/4, Math.max(2,s/10), 0xFFFFDB9D);
    }

    static void serverIcon(GuiGraphics g, int cx, int cy, int s, int color) {
        int w = s * 2 / 3, h = s / 3;
        for (int row = -1; row <= 1; row++) {
            int y = cy + row * (h + 3) - h / 2;
            rounded(g, cx - w/2, y, w, h, Math.max(2, h/3), color);
            circle(g, cx - w/3, y + h/2, Math.max(1, h/8), 0xFF8DF1D3);
            rounded(g, cx, y + h/2 - 1, w/3, 2, 1, 0x5FFFFFFF);
        }
    }

    static void drawAvatar(GuiGraphics g, int x, int y, int size, Identifier skin) {
        rounded(g, x - 3, y - 3, size + 6, size + 6, size / 3, 0x45FFFFFF);
        rounded(g, x - 1, y - 1, size + 2, size + 2, size / 3, 0xFF7FA9C9);
        rounded(g, x, y, size, size, size / 3, 0xFF1D2A39);
        if (skin != null) {
            // Draw only the front face of the actual 64x64 Minecraft skin, scaled to the avatar tile.
            g.blit(skin, x + 2, y + 2, size - 4, size - 4, 0.125F, 0.25F, 0.125F, 0.25F);
        } else {
            circle(g, x + size/2, y + size/3, Math.max(4, size/6), 0xFFB9CDE0);
            rounded(g, x + size/4, y + size/2, size/2, size/3, size/6, 0xFFB9CDE0);
        }
    }

    static void drawCardArtwork(GuiGraphics g, int x, int y, int w, int h, boolean world, int color, long age, boolean selected) {
        rounded(g, x + 8, y + 7, w - 16, h - 14, 14, 0x26000000);
        rounded(g, x, y, w, h, 18, selected ? 0x65A8EFFF : 0x20FFFFFF);
        rounded(g, x + 1, y + 1, w - 2, h - 2, 17, selected ? 0xE71C2A3A : 0xD9141E2B);
        rounded(g, x + 8, y + 8, w - 16, h - 16, 12, color);
        int top = y + 8, left = x + 8, innerW = w - 16, innerH = h - 16;
        g.fillGradient(left, top, left + innerW, top + innerH, alpha(0xFF5B8CB4, 60), alpha(0xFF0B1420, 210));
        circle(g, left + innerW * 2 / 3, top + innerH / 3, Math.max(8, innerW/12), 0x60E6F8FF);
        polygon(g, 0x88354F69, new int[]{left,top+innerH*3/5,left+innerW/4,top+innerH/3,left+innerW*2/5,top+innerH*2/3,left+innerW*2/3,top+innerH/2,left+innerW,top+innerH*2/3,left+innerW,top+innerH,left,top+innerH});
        int cx = x + w/2, cy = y + h/2;
        if (world) worldIcon(g, cx, cy + 6, Math.min(w,h)/3, 0xE6FFFFFF);
        else serverIcon(g, cx, cy + 4, Math.min(w,h)/3, 0xE6FFFFFF);
    }

    static void drawLabel(GuiGraphics g, Font font, String text, int x, int y, int maxWidth, int color) {
        String value = text == null ? "" : text;
        while (font.width(value) > maxWidth && value.length() > 4) value = value.substring(0, value.length() - 2) + "…";
        g.drawCenteredString(font, value, x, y, color);
    }
}
