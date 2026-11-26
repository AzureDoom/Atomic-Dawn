package mod.azure.atomicdawn.client.pipboy;

import mod.azure.atomicdawn.client.helper.ADGuiGraphics;

public final class PipBoyDraw {

    private PipBoyDraw() {}

    public static void fill(ADGuiGraphics graphics, Rect rect, int color) {
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), color);
    }

    public static void outline(ADGuiGraphics graphics, Rect r, int color) {
        graphics.fill(r.x(), r.y(), r.right(), r.y() + 1, color);
        graphics.fill(r.x(), r.bottom() - 1, r.right(), r.bottom(), color);
        graphics.fill(r.x(), r.y() + 1, r.x() + 1, r.bottom() - 1, color);
        graphics.fill(r.right() - 1, r.y() + 1, r.right(), r.bottom() - 1, color);
    }

    public static void bevel(ADGuiGraphics graphics, Rect r, int outline, int base, int light, int shade) {
        outline(graphics, r, outline);
        var in = r.inset(1);
        fill(graphics, in, base);
        graphics.fill(in.x(), in.y(), in.right(), in.y() + 1, light);
        graphics.fill(in.x(), in.y() + 1, in.x() + 1, in.bottom(), light);
        graphics.fill(in.x() + 1, in.bottom() - 1, in.right(), in.bottom(), shade);
        graphics.fill(in.right() - 1, in.y() + 1, in.right(), in.bottom() - 1, shade);
    }

    public static void recess(ADGuiGraphics graphics, Rect r, int base, int shade, int light) {
        fill(graphics, r, base);
        graphics.fill(r.x(), r.y(), r.right(), r.y() + 1, shade);
        graphics.fill(r.x(), r.y() + 1, r.x() + 1, r.bottom(), shade);
        graphics.fill(r.x() + 1, r.bottom() - 1, r.right(), r.bottom(), light);
        graphics.fill(r.right() - 1, r.y() + 1, r.right(), r.bottom() - 1, light);
    }

    public static void screw(ADGuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 4, y + 4, 0xFF1A1C14);
        graphics.fill(x + 1, y + 1, x + 3, y + 3, 0xFF8E9378);
        graphics.fill(x + 1, y + 2, x + 2, y + 3, 0xFF2A2C20);
        graphics.fill(x + 2, y + 1, x + 3, y + 2, 0xFF2A2C20);
    }

    public static void disc(ADGuiGraphics graphics, int cx, int cy, int radius, int color) {
        disc(graphics, cx, cy, radius, Integer.MAX_VALUE, color);
    }

    public static void disc(ADGuiGraphics graphics, int cx, int cy, int radius, int clipBottom, int color) {
        if (radius <= 0) {
            return;
        }
        for (var dy = -radius; dy <= radius; dy++) {
            var y = cy + dy;
            if (y >= clipBottom) {
                break;
            }
            var half = (int) Math.round(Math.sqrt(radius * radius - dy * dy + 0.25));
            graphics.fill(cx - half, y, cx + half + 1, y + 1, color);
        }
    }

    public static void ring(ADGuiGraphics graphics, int cx, int cy, int radius, int thickness, int color) {
        var inner = Math.max(0, radius - thickness);
        for (var dy = -radius; dy <= radius; dy++) {
            var outerHalf = (int) Math.round(Math.sqrt(radius * radius - dy * dy + 0.25));
            if (Math.abs(dy) > inner) {
                graphics.fill(cx - outerHalf, cy + dy, cx + outerHalf + 1, cy + dy + 1, color);
                continue;
            }
            var innerHalf = (int) Math.round(Math.sqrt(inner * inner - dy * dy + 0.25));
            graphics.fill(cx - outerHalf, cy + dy, cx - innerHalf, cy + dy + 1, color);
            graphics.fill(cx + innerHalf + 1, cy + dy, cx + outerHalf + 1, cy + dy + 1, color);
        }
    }

    public static void line(
        ADGuiGraphics graphics,
        float x0,
        float y0,
        float x1,
        float y1,
        int thickness,
        int color
    ) {
        var dx = x1 - x0;
        var dy = y1 - y0;
        var length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.5F) {
            return;
        }
        graphics.push();
        graphics.translate(x0, y0);
        graphics.rotate((float) Math.atan2(dy, dx));
        graphics.fill(0, -thickness / 2, Math.round(length), thickness - thickness / 2, color);
        graphics.pop();
    }

    public static void ellipse(
        ADGuiGraphics graphics,
        float cx,
        float cy,
        float rx,
        float ry,
        float angle,
        int color
    ) {
        var cos = Math.cos(angle);
        var sin = Math.sin(angle);
        var steps = Math.max(24, (int) (Math.PI * (rx + ry) * 1.2F));
        var lastX = Integer.MIN_VALUE;
        var lastY = Integer.MIN_VALUE;
        for (var i = 0; i < steps; i++) {
            var t = Math.PI * 2 * i / steps;
            var ex = Math.cos(t) * rx;
            var ey = Math.sin(t) * ry;
            var px = (int) Math.round(cx + ex * cos - ey * sin);
            var py = (int) Math.round(cy + ex * sin + ey * cos);
            if (px == lastX && py == lastY) {
                continue;
            }
            graphics.fill(px, py, px + 1, py + 1, color);
            lastX = px;
            lastY = py;
        }
    }

    public static float[] ellipsePoint(float cx, float cy, float rx, float ry, float angle, double t) {
        var ex = Math.cos(t) * rx;
        var ey = Math.sin(t) * ry;
        return new float[] {
            (float) (cx + ex * Math.cos(angle) - ey * Math.sin(angle)),
            (float) (cy + ex * Math.sin(angle) + ey * Math.cos(angle))
        };
    }

    public static void mesh(ADGuiGraphics graphics, Rect r, int spacing, int color, int shine) {
        var left = r.x();
        var top = r.y();
        var right = r.right();
        var bottom = r.bottom();

        for (var c = left - bottom + (spacing - Math.floorMod(left - bottom, spacing)); c < right - top; c += spacing) {
            var xs = Math.max(left, top + c);
            var xe = Math.min(right, bottom + c);
            if (xe - xs < 1) {
                continue;
            }
            line(graphics, xs, xs - c, xe, xe - c, 1, color);
            line(graphics, xs + 1, xs - c, xe, xe - c - 1, 1, shine);
        }

        for (var c = left + top + (spacing - Math.floorMod(left + top, spacing)); c < right + bottom; c += spacing) {
            var xs = Math.max(left, c - bottom);
            var xe = Math.min(right, c - top);
            if (xe - xs < 1) {
                continue;
            }
            line(graphics, xs, c - xs, xe, c - xe, 1, color);
        }
    }

    public static void stripes(ADGuiGraphics graphics, Rect r, int period, int base, int stripe) {
        fill(graphics, r, base);
        var half = period / 2;
        for (var y = r.y(); y < r.bottom(); y++) {
            var offset = Math.floorMod(y - r.y(), period);
            for (var x = r.x() - offset - period; x < r.right(); x += period) {
                var x0 = Math.max(r.x(), x);
                var x1 = Math.min(r.right(), x + half);
                if (x1 > x0) {
                    graphics.fill(x0, y, x1, y + 1, stripe);
                }
            }
        }
    }

    public static void glow(ADGuiGraphics graphics, Rect r, int rgb, int alpha, int layers) {
        for (var i = layers; i >= 1; i--) {
            var a = Math.round(alpha * (1F - (float) i / (layers + 1)) / layers * 2F);
            graphics.fill(
                r.x() - i,
                r.y() - i,
                r.right() + i,
                r.bottom() + i,
                ((Math.clamp(a, 0, 255)) << 24) | (rgb & 0xFFFFFF)
            );
        }
    }

    public static void vignette(ADGuiGraphics graphics, Rect r, int steps, int maxAlpha) {
        for (var i = 0; i < steps; i++) {
            var a = Math.round(maxAlpha * (1F - (float) i / steps) * (1F - (float) i / steps)) << 24;
            graphics.fill(r.x(), r.y() + i, r.right(), r.y() + i + 1, a);
            graphics.fill(r.x(), r.bottom() - i - 1, r.right(), r.bottom() - i, a);
            graphics.fill(r.x() + i, r.y(), r.x() + i + 1, r.bottom(), a);
            graphics.fill(r.right() - i - 1, r.y(), r.right() - i, r.bottom(), a);
        }
    }

    public static void crtMask(ADGuiGraphics graphics, Rect r, float open) {
        if (open >= 1F) {
            return;
        }
        if (open <= 0F) {
            fill(graphics, r, 0xFF000000);
            return;
        }
        var cx = r.x() + r.width() / 2;
        var cy = r.y() + r.height() / 2;
        int w;
        int h;
        float flash;
        if (open < 0.35F) {
            var t = ease(open / 0.35F);
            w = Math.max(2, Math.round(r.width() * t));
            h = 2;
            flash = 1F;
        } else {
            var t = ease((open - 0.35F) / 0.65F);
            w = r.width();
            h = Math.max(2, Math.round(2 + (r.height() - 2) * t));
            flash = 1F - t;
        }
        var x0 = cx - w / 2;
        var y0 = cy - h / 2;
        var x1 = x0 + w;
        var y1 = y0 + h;
        graphics.fill(r.x(), r.y(), r.right(), y0, 0xFF000000);
        graphics.fill(r.x(), y1, r.right(), r.bottom(), 0xFF000000);
        graphics.fill(r.x(), y0, x0, y1, 0xFF000000);
        graphics.fill(x1, y0, r.right(), y1, 0xFF000000);
        var glow = PipBoyTheme.rgb();
        graphics.fill(x0, y0, x1, y1, (Math.round(0xD0 * flash) << 24) | blend(glow, 0xFFFFFF, 0.7F));
    }

    public static float ease(float t) {
        var c = Math.clamp(t, 0F, 1F);
        return 1F - (1F - c) * (1F - c) * (1F - c);
    }

    public static int blend(int rgbA, int rgbB, float t) {
        var r = Math.round(((rgbA >> 16) & 0xFF) * (1 - t) + ((rgbB >> 16) & 0xFF) * t);
        var g = Math.round(((rgbA >> 8) & 0xFF) * (1 - t) + ((rgbB >> 8) & 0xFF) * t);
        var b = Math.round((rgbA & 0xFF) * (1 - t) + (rgbB & 0xFF) * t);
        return (r << 16) | (g << 8) | b;
    }
}
