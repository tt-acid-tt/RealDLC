package ru.acid.realdlc;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class UiDraw {
    private UiDraw() {}

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
                double depth = r - edge;
                inset = r - Math.sqrt(Math.max(0.0, (double) r * r - depth * depth));
            }
            int left = x + (int) Math.ceil(inset);
            int right = x + w - (int) Math.ceil(inset);
            if (right > left) g.fill(left, y + row, right, y + row + 1, color);
        }
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

    static void polygon(GuiGraphics g, int color, int... xy) {
        if (xy.length < 6 || (xy.length & 1) != 0) return;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (int i = 1; i < xy.length; i += 2) {
            minY = Math.min(minY, xy[i]);
            maxY = Math.max(maxY, xy[i]);
        }
        int edges = xy.length / 2;
        for (int y = minY; y <= maxY; y++) {
            double[] nodes = new double[edges];
            int count = 0;
            for (int i = 0, j = edges - 1; i < edges; j = i++) {
                int xi = xy[i * 2], yi = xy[i * 2 + 1];
                int xj = xy[j * 2], yj = xy[j * 2 + 1];
                if ((yi < y && yj >= y) || (yj < y && yi >= y)) {
                    nodes[count++] = xi + (double) (y - yi) * (xj - xi) / (yj - yi);
                }
            }
            for (int i = 0; i < count - 1; i++) {
                for (int j = i + 1; j < count; j++) {
                    if (nodes[j] < nodes[i]) {
                        double t = nodes[i]; nodes[i] = nodes[j]; nodes[j] = t;
                    }
                }
            }
            for (int i = 0; i + 1 < count; i += 2) {
                g.fill((int) Math.ceil(nodes[i]), y, (int) Math.floor(nodes[i + 1]) + 1, y + 1, color);
            }
        }
    }

    static void circle(GuiGraphics g, int cx, int cy, int radius, int color) {
        rounded(g, cx - radius, cy - radius, radius * 2, radius * 2, radius, color);
    }

    static void ring(GuiGraphics g, int cx, int cy, int radius, int color, int thickness) {
        int dots = Math.max(48, radius * 7);
        for (int i = 0; i < dots; i++) {
            double a = Math.PI * 2.0 * i / dots;
            int x = cx + (int) Math.round(Math.cos(a) * radius);
            int y = cy + (int) Math.round(Math.sin(a) * radius);
            g.fill(x - thickness / 2, y - thickness / 2,
                   x + (thickness + 1) / 2, y + (thickness + 1) / 2, color);
        }
    }

    static void drawScene(GuiGraphics g, int w, int h, int variant) {
        int v = Math.floorMod(variant, 4);
        int[][] palette = {
            {0xFF081827, 0xFF1D596B, 0xFF365F69, 0xFF193D49, 0xFF0B202B, 0xFF09141F},
            {0xFF100D2A, 0xFF343A79, 0xFF65568A, 0xFF35305E, 0xFF1A1C3D, 0xFF0A0D22},
            {0xFF091C2C, 0xFF2C7182, 0xFF71969A, 0xFF315A63, 0xFF17323D, 0xFF091922},
            {0xFF1B112B, 0xFF714C70, 0xFFBB796C, 0xFF694255, 0xFF34253E, 0xFF111322}
        };
        int[] p = palette[v];
        g.fillGradient(0, 0, w, h, p[0], p[1]);

        // A deterministic star field is rendered as vector points, not as a tiled texture.
        for (int i = 0; i < 72; i++) {
            int sx = Math.floorMod(i * 131 + 29, Math.max(1, w));
            int sy = Math.floorMod(i * 47 + (i % 7) * 13, Math.max(1, h / 2));
            int size = (i % 11 == 0) ? 2 : 1;
            g.fill(sx, sy, sx + size, sy + size, i % 3 == 0 ? 0xBFFFFFFF : 0x75E7F4FF);
        }

        int sunX = (int) (w * (v == 3 ? 0.68 : 0.73));
        int sunY = (int) (h * 0.35);
        circle(g, sunX, sunY, Math.max(18, Math.min(w, h) / 19), v == 1 ? 0xAFC4B9FF : 0xC7F2D4B0);
        circle(g, sunX, sunY, Math.max(11, Math.min(w, h) / 27), 0xAFFFFFFF);

        polygon(g, p[2],
            0, (int)(h*.56), (int)(w*.14), (int)(h*.40), (int)(w*.25), (int)(h*.51),
            (int)(w*.42), (int)(h*.31), (int)(w*.58), (int)(h*.52),
            (int)(w*.75), (int)(h*.37), w, (int)(h*.55), w, h, 0, h);
        polygon(g, p[3],
            0, (int)(h*.66), (int)(w*.18), (int)(h*.54), (int)(w*.33), (int)(h*.67),
            (int)(w*.52), (int)(h*.48), (int)(w*.69), (int)(h*.65),
            (int)(w*.87), (int)(h*.51), w, (int)(h*.62), w, h, 0, h);
        polygon(g, p[4],
            0, (int)(h*.76), (int)(w*.15), (int)(h*.67), (int)(w*.34), (int)(h*.76),
            (int)(w*.53), (int)(h*.63), (int)(w*.74), (int)(h*.76),
            (int)(w*.91), (int)(h*.65), w, (int)(h*.70), w, h, 0, h);

        // A dark lake with thin low-contrast reflections.
        g.fillGradient(0, (int)(h*.76), w, h, 0x9C112D3B, p[5]);
        for (int i = 0; i < 17; i++) {
            int yy = (int)(h*.79) + i * Math.max(2, h / 150);
            int rw = Math.max(8, (int)(w * (0.05 + (i % 5) * 0.035)));
            int rx = Math.floorMod(i * 83 + 17, Math.max(1, w - rw));
            g.fill(rx, yy, rx + rw, yy + 1, 0x256FE2E9);
        }

        // Sparse foreground silhouettes make each wallpaper feel like a different Minecraft vista.
        for (int i = 0; i < 8; i++) {
            int tx = Math.floorMod(i * 173 + 37, Math.max(1, w));
            int base = (int)(h * (0.78 + (i % 3) * 0.04));
            int th = Math.max(10, h / (8 + i % 3));
            polygon(g, 0xCC07121A, tx, base, tx + 2, base - th,
                    tx + 4, base - th/2, tx + 6, base - th*2/3, tx + 8, base);
        }
        g.fill(0, h - 3, w, h, 0x55000000);
    }

    static void drawClock(GuiGraphics g, Font font, int w, int y) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("ru-RU")));
        g.pose().pushMatrix();
        g.pose().translate(w / 2.0f, y);
        g.pose().scale(2.8f, 2.8f);
        g.drawCenteredString(font, time, 0, 0, 0xFFFFFFFF);
        g.pose().popMatrix();
        g.drawCenteredString(font, date, w / 2, y + 48, 0xE6FFFFFF);
    }

    static void glassButton(GuiGraphics g, int x, int y, int size, boolean hover) {
        rounded(g, x, y + 3, size, size, size / 2, 0x25000000);
        rounded(g, x, y, size, size, size / 2, hover ? 0x50FFFFFF : 0x32FFFFFF);
        rounded(g, x + 1, y + 1, size - 2, size - 2, size / 2 - 1, 0x10FFFFFF);
        ring(g, x + size / 2, y + size / 2, size / 2 - 2, 0x52FFFFFF, 1);
    }

    static void layersIcon(GuiGraphics g, int cx, int cy, int scale, int color) {
        int halfW = 15 * scale, halfH = 8 * scale, gap = 7 * scale;
        for (int layer = 0; layer < 3; layer++) {
            int ox = 0, oy = (layer - 1) * gap;
            int topX = cx + ox, topY = cy + oy - halfH;
            int rightX = cx + halfW, rightY = cy + oy;
            int bottomX = cx, bottomY = cy + oy + halfH;
            int leftX = cx - halfW, leftY = cy + oy;
            line(g, topX, topY, rightX, rightY, color, Math.max(1, scale));
            line(g, rightX, rightY, bottomX, bottomY, color, Math.max(1, scale));
            line(g, bottomX, bottomY, leftX, leftY, color, Math.max(1, scale));
            line(g, leftX, leftY, topX, topY, color, Math.max(1, scale));
        }
    }

    static void personIcon(GuiGraphics g, int cx, int cy, int scale, int color) {
        circle(g, cx, cy - 10 * scale, 5 * scale, color);
        rounded(g, cx - 9 * scale, cy - 3 * scale, 18 * scale, 19 * scale, 8 * scale, color);
    }

    static void multiplayerIcon(GuiGraphics g, int cx, int cy, int scale, int color) {
        personIcon(g, cx - 8 * scale, cy, Math.max(1, scale - 1), color);
        circle(g, cx + 11 * scale, cy - 9 * scale, 4 * scale, color);
        rounded(g, cx + 4 * scale, cy - 2 * scale, 15 * scale, 16 * scale, 7 * scale, color);
    }

    static void accountIcon(GuiGraphics g, int cx, int cy, int scale, int color) {
        int r = 12 * scale;
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI * 2 / 24.0;
            if ((i % 2) == 0) {
                int x = cx - 9 * scale + (int)(Math.cos(a) * r * .42);
                int y = cy + (int)(Math.sin(a) * r * .72);
                circle(g, x, y, Math.max(1, scale), color);
            }
        }
        personIcon(g, cx - 5 * scale, cy + 2 * scale, Math.max(1, scale - 1), color);
        circle(g, cx + 13 * scale, cy + 12 * scale, 7 * scale, 0xE9212732);
        circle(g, cx + 13 * scale, cy + 12 * scale, 6 * scale, color);
        g.fill(cx + 12 * scale, cy + 8 * scale, cx + 14 * scale, cy + 16 * scale, 0xFF19232D);
        g.fill(cx + 9 * scale, cy + 11 * scale, cx + 17 * scale, cy + 13 * scale, 0xFF19232D);
    }

    static void powerIcon(GuiGraphics g, int cx, int cy, int scale, int color) {
        ring(g, cx, cy, 13 * scale, color, Math.max(2, 2 * scale));
        g.fill(cx - 2 * scale, cy - 17 * scale, cx + 2 * scale, cy - 1 * scale, color);
        g.fill(cx - 2 * scale, cy - 2 * scale, cx + 2 * scale, cy + 3 * scale, 0xFF111A24);
    }
}
