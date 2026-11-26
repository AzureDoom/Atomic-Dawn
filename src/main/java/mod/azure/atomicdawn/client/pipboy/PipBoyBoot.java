package mod.azure.atomicdawn.client.pipboy;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.client.pipboy.radio.HolotapePlayer;
import mod.azure.atomicdawn.pipboy.PipBoyNames;

public final class PipBoyBoot {

    public enum Mode {
        FULL,
        QUICK
    }

    private static final int POWER_MS = 260;

    private static final int CRT_MS = 380;

    private static final int CURSOR_MS = 520;

    private static final float HEADER_CPS = 220F;

    private static final float LINE_CPS = 150F;

    private static final int LINE_PAUSE_MS = 70;

    private static final int HOLD_MS = 280;

    private static final int SCROLL_MS = 420;

    private static final int EMBLEM_MS = 1700;

    private static final int REVEAL_MS = 320;

    private static final int QUICK_MS = 300;

    private final Mode mode;

    private final String header;

    private final List<String> lines;

    private final long[] lineStart;

    private final long[] lineEnd;

    private final long headerEnd;

    private final long typingStart;

    private final long scrollStart;

    private final long emblemStart;

    private final long revealStart;

    private final long end;

    private long start;

    public PipBoyBoot(Mode mode, long now) {
        this.mode = mode;
        this.start = now;
        this.header = Component.translatable("pipboy.atomicdawn.boot.header").getString();
        this.lines = new ArrayList<>();
        lines.add(Component.translatable("pipboy.atomicdawn.boot.copyright").getString());
        lines.add(Component.translatable("pipboy.atomicdawn.boot.loader").getString());
        lines.add(Component.translatable("pipboy.atomicdawn.boot.exec").getString());
        lines.add(Component.translatable("pipboy.atomicdawn.boot.ram").getString());
        lines.add(Component.translatable("pipboy.atomicdawn.boot.free").getString());
        var tape = HolotapePlayer.current();
        lines.add(
            tape == null
                ? Component.translatable("pipboy.atomicdawn.boot.no_holotape").getString()
                : Component.translatable("pipboy.atomicdawn.boot.holotape", PipBoyNames.holotape(tape)).getString()
        );
        lines.add(Component.translatable("pipboy.atomicdawn.boot.rom").getString());

        typingStart = POWER_MS + CRT_MS + CURSOR_MS;
        lineStart = new long[lines.size()];
        lineEnd = new long[lines.size()];
        headerEnd = typingStart + Math.round(header.length() * 1000F / HEADER_CPS);
        var t = headerEnd + LINE_PAUSE_MS * 3L;
        for (var i = 0; i < lines.size(); i++) {
            lineStart[i] = t;
            lineEnd[i] = t + Math.round(lines.get(i).length() * 1000F / LINE_CPS);
            t = lineEnd[i] + LINE_PAUSE_MS;
        }
        scrollStart = t + HOLD_MS;
        emblemStart = scrollStart + SCROLL_MS;
        revealStart = emblemStart + EMBLEM_MS;
        end = mode == Mode.FULL ? revealStart + REVEAL_MS : QUICK_MS;
    }

    public Mode mode() {
        return mode;
    }

    private long elapsed(long now) {
        return Math.max(0L, now - start);
    }

    public boolean blocking(long now) {
        return mode == Mode.FULL && elapsed(now) < revealStart;
    }

    public boolean finished(long now) {
        return elapsed(now) >= end;
    }

    public void skip(long now) {
        if (mode == Mode.FULL && elapsed(now) < revealStart) {
            start = now - revealStart;
        }
    }

    public int typingSegment(long now) {
        if (mode != Mode.FULL) {
            return -1;
        }
        var e = elapsed(now);
        if (e >= typingStart && e < headerEnd) {
            return 0;
        }
        for (var i = 0; i < lines.size(); i++) {
            if (e >= lineStart[i] && e < lineEnd[i]) {
                return i + 1;
            }
        }
        return -1;
    }

    public float power(long now) {
        var e = elapsed(now);
        if (mode == Mode.QUICK) {
            return 1F;
        }
        return e < 90 ? 0F : e < 150 ? 0.5F : 1F;
    }

    public float tubes(long now) {
        var e = elapsed(now);
        if (mode == Mode.QUICK) {
            return Math.clamp(0.6F + e / (float) QUICK_MS * 0.4F, 0F, 1F);
        }
        var warm = Math.clamp((e - 120F) / 1500F, 0F, 1F);
        if (e > 200 && e < 420 && (e / 45) % 2 == 0) {
            warm *= 0.3F;
        }
        return warm * warm * (3F - 2F * warm);
    }

    public float crtOpen(long now) {
        var e = elapsed(now);
        if (mode == Mode.QUICK) {
            return Math.clamp(e / (float) QUICK_MS, 0F, 1F);
        }
        return Math.clamp((e - POWER_MS) / (float) CRT_MS, 0F, 1F);
    }

    public void render(ADGuiGraphics graphics, PipBoyScreen screen, Rect area, long now) {
        var e = elapsed(now);
        if (e < POWER_MS + CRT_MS * 0.6F) {
            return;
        }
        if (e < emblemStart) {
            terminal(graphics, screen, area, e);
        } else {
            emblem(graphics, screen, area, e - emblemStart);
        }
    }

    private void terminal(ADGuiGraphics graphics, PipBoyScreen screen, Rect area, long e) {
        var font = screen.font();
        var x = area.x() + 10;
        var top = area.y() + 8;
        var lineH = font.lineHeight + 1;
        var scroll = 0;
        if (e >= scrollStart) {
            var p = Math.clamp((e - scrollStart) / (float) SCROLL_MS, 0F, 1F);
            scroll = Math.round(p * p * ((lines.size() + 3) * lineH + 12));
        }
        var color = PipBoyTheme.text();
        var cursorOn = (e / 260) % 2 == 0;

        if (e < typingStart) {
            if (cursorOn) {
                graphics.fill(x, top, x + 5, top + 8, color);
            }
            return;
        }

        int cursorX;
        int cursorY = top - scroll;
        var shown = typed(header, e - typingStart, HEADER_CPS);
        textLine(graphics, screen, shown, x, top - scroll, area, color);
        cursorX = x + font.width(shown) + 1;

        for (var i = 0; i < lines.size(); i++) {
            if (e < lineStart[i]) {
                break;
            }
            var y = top - scroll + (i + 2) * lineH + 6;
            var part = typed(lines.get(i), e - lineStart[i], LINE_CPS);
            textLine(graphics, screen, part, x, y, area, color);
            cursorX = x + font.width(part) + 1;
            cursorY = y;
        }
        if (e < scrollStart && (cursorOn || e < scrollStart - HOLD_MS)) {
            if (cursorY >= area.y() && cursorY + 8 <= area.bottom()) {
                graphics.fill(cursorX, cursorY, cursorX + 5, cursorY + 8, color);
            }
        }
    }

    private static String typed(String text, long elapsed, float cps) {
        var count = (int) Math.min(text.length(), Math.max(0L, elapsed) * cps / 1000F);
        return text.substring(0, count);
    }

    private static void textLine(
        ADGuiGraphics graphics,
        PipBoyScreen screen,
        String text,
        int x,
        int y,
        Rect area,
        int color
    ) {
        if (text.isEmpty() || y < area.y() || y + 8 > area.bottom()) {
            return;
        }

        screen.text(graphics, Component.literal(text), x, y + 1, PipBoyTheme.phosphor(0.25F, 0x80));
        screen.fitted(graphics, Component.literal(text), x, y, area.right() - x - 6, color);
    }

    private void emblem(ADGuiGraphics graphics, PipBoyScreen screen, Rect area, long t) {
        var bright = PipBoyTheme.text();
        var mid = PipBoyTheme.phosphor(0.55F, 0xFF);
        var low = PipBoyTheme.phosphor(0.22F, 0xFF);

        var unit = Math.max(8, Math.min(area.width() / 14, area.height() / 9));
        var cx = area.x() + area.width() / 2;
        var horizon = area.y() + area.height() * 47 / 100;

        var groundW = Math.min(area.width() - 40, unit * 9);
        for (var i = 1; i <= 4; i++) {
            var y = horizon + i * i * 2 + i * 2;
            if (y >= area.bottom() - 30) {
                break;
            }
            var w = groundW * (5 - i) / 5 + groundW / 4;
            graphics.fill(cx - w / 2, y, cx + w / 2, y + 1, low);
        }

        var rise = PipBoyDraw.ease(Math.clamp(t / 700F, 0F, 1F));
        var radius = unit * 3 / 2;
        var sunY = horizon + Math.round(radius * 1.15F * (1F - rise));
        PipBoyDraw.disc(graphics, cx, sunY, radius, horizon, low);
        PipBoyDraw.disc(graphics, cx, sunY, radius - 3, horizon, PipBoyTheme.phosphor(0.32F, 0xFF));

        var rays = Math.clamp((t - 550) / 300F, 0F, 1F);
        if (rays > 0) {
            var rayColor = PipBoyTheme.phosphor(0.55F * rays, 0xFF);
            for (var i = 0; i < 9; i++) {
                var angle = Math.PI + Math.PI * (i + 0.5) / 9;
                var r0 = radius + 4;
                var r1 = radius + 4 + unit * (i % 2 == 0 ? 0.9F : 0.55F);
                PipBoyDraw.line(
                    graphics,
                    (float) (cx + Math.cos(angle) * r0),
                    (float) (sunY + Math.sin(angle) * r0),
                    (float) (cx + Math.cos(angle) * r1),
                    (float) (sunY + Math.sin(angle) * r1),
                    2,
                    rayColor
                );
            }
        }

        graphics.fill(cx - groundW / 2, horizon, cx + groundW / 2, horizon + 1, bright);

        var atom = Math.clamp((t - 350) / 400F, 0F, 1F);
        if (atom > 0) {
            var orbitColor = PipBoyTheme.phosphor(0.35F + 0.65F * atom, 0xFF);
            var rx = radius * 1.45F * (0.6F + 0.4F * atom);
            var ry = radius * 0.42F;
            var spin = t / 1000.0;
            for (var i = 0; i < 3; i++) {
                var angle = (float) (Math.PI / 3 * i);
                PipBoyDraw.ellipse(graphics, cx, sunY, rx, ry, angle, orbitColor);
                var p = PipBoyDraw.ellipsePoint(cx, sunY, rx, ry, angle, spin * (3.2 + i * 0.7) + i * 2.0);
                PipBoyDraw.disc(graphics, Math.round(p[0]), Math.round(p[1]), 2, bright);
            }
            PipBoyDraw.disc(graphics, cx, sunY, Math.max(3, radius / 4), bright);
        }

        var brand = Component.translatable("pipboy.atomicdawn.boot.brand");
        var brandAlpha = Math.clamp((t - 650) / 350F, 0F, 1F);
        var textTop = horizon + Math.round(radius * 1.45F * 0.87F) + 10;
        if (brandAlpha > 0 && textTop + 12 < area.bottom()) {
            var scale = 1.5F;
            var bw = screen.font().width(brand) * scale;
            graphics.push();
            graphics.translate(cx - bw / 2F, textTop);
            graphics.scale(scale, scale);
            screen.text(graphics, brand, 0, 0, PipBoyTheme.phosphor(brandAlpha, 0xFF));
            graphics.pop();
        }
        var dots = (int) ((t / 300) % 4);
        var status = Component.translatable("pipboy.atomicdawn.boot.initiating").append(".".repeat(dots));
        var base = Component.translatable("pipboy.atomicdawn.boot.initiating").append("...");
        var sx = cx - screen.font().width(base) / 2;
        var sy = textTop + 18;
        if (sy + 8 < area.bottom()) {
            screen.text(graphics, status, sx, sy, mid);
        }
    }

    public void renderReveal(ADGuiGraphics graphics, Rect area, long now) {
        if (mode != Mode.FULL) {
            return;
        }
        var e = elapsed(now) - revealStart;
        if (e < 0 || e >= REVEAL_MS) {
            return;
        }
        var p = e / (float) REVEAL_MS;

        if (p < 0.18F) {
            PipBoyDraw.fill(graphics, area, PipBoyTheme.withAlpha(0x000000, Math.round(0xE0 * (1F - p / 0.18F))));
        }
        PipBoyDraw.fill(graphics, area, PipBoyTheme.phosphor(1F, Math.round(0x50 * (1F - p))));
        var seed = e / 40;
        for (var i = 0; i < 4; i++) {
            var hash = (int) ((seed * 73856093L) ^ (i * 19349663L));
            var y = area.y() + Math.floorMod(hash, Math.max(1, area.height() - 3));
            var h = 1 + Math.floorMod(hash >> 8, 3);
            graphics.fill(area.x(), y, area.right(), y + h, PipBoyTheme.phosphor(1F, Math.round(0x60 * (1F - p))));
        }
    }
}
