package mod.azure.atomicdawn.client.pipboy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.time.LocalTime;
import java.util.Locale;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.pipboy.radio.HolotapePlayer;
import mod.azure.atomicdawn.client.pipboy.radio.RadioPlayer;
import mod.azure.atomicdawn.registry.ADAttachments;

public final class PipBoyHousing {

    private static final int OUTLINE = 0xFF1E2016;

    private static final int OLIVE = 0xFF5F664A;

    private static final int OLIVE_LIGHT = 0xFF7F8763;

    private static final int OLIVE_SHADE = 0xFF434833;

    private static final int OLIVE_DARK = 0xFF353927;

    private static final int RECESS = 0xFF24271B;

    private static final int RECESS_DEEP = 0xFF141610;

    private static final int BRASS = 0xFFB48D3E;

    private static final int BRASS_LIGHT = 0xFFDDB865;

    private static final int BRASS_DARK = 0xFF6E5420;

    private static final int PLATE_TEXT = 0xFF2C3021;

    private static final int PLATE_TEXT_HI = 0xFF98A07A;

    private static final int LABEL_OFF = 0xFF9BA285;

    private static final int LABEL_ON = 0xFFF0B84E;

    private static final int LAMP_OFF = 0xFF4C1210;

    private static final int LAMP_ON = 0xFFE3271E;

    private static final int STEEL = 0xFF8C9898;

    private static final int STEEL_LIGHT = 0xFFB7C3C2;

    private static final int STEEL_SHADE = 0xFF5D6969;

    public record Layout(
        boolean fullscreen,
        Rect device,
        Rect screen,
        @Nullable Rect body,
        @Nullable Rect main,
        @Nullable Rect plate,
        @Nullable Rect bezel,
        @Nullable Rect strip,
        @Nullable Rect power,
        @Nullable Rect lcd,
        @Nullable Rect side,
        @Nullable Rect selector,
        Rect[] selectorLabels,
        Rect[] selectorLamps,
        @Nullable Rect hazard,
        @Nullable Rect grille,
        @Nullable Rect geiger,
        @Nullable Rect knobTop,
        @Nullable Rect knobBottom
    ) {

        public boolean hasSide() {
            return side != null;
        }
    }

    public record State(
        float power,
        float tubes,
        int selectorLit,
        int hovered,
        long millis
    ) {}

    private PipBoyHousing() {}

    public static Layout layout(int width, int height, boolean fullscreen) {
        if (fullscreen) {
            var screen = new Rect(4, 4, width - 8, height - 8);
            return new Layout(
                true,
                screen,
                screen,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new Rect[0],
                new Rect[0],
                null,
                null,
                null,
                null,
                null
            );
        }
        var maxW = width - 10;
        var maxH = height - 8;
        var withSide = maxW >= 470 && maxH >= 200;
        var w = Math.min(maxW, withSide ? 590 : 430);
        var h = Math.min(maxH, 320);
        var x0 = (width - w) / 2;
        var y0 = (height - h) / 2;

        var knobW = withSide ? 12 : 0;
        var body = new Rect(x0, y0 + 8, w - knobW, h - 8);
        var sideW = withSide ? Math.clamp(Math.round(body.width() * 0.23F), 104, 136) : 0;
        var main = new Rect(body.x(), body.y(), body.width() - sideW, body.height());
        var stripH = Math.clamp(Math.round(h * 0.1F), 20, 28);
        var bezel = new Rect(main.x() + 7, main.y() + 10, main.width() - 14, main.height() - 10 - stripH - 10);
        var screen = bezel.inset(5);
        var strip = new Rect(main.x() + 7, bezel.bottom() + 4, main.width() - 14, stripH);
        var btn = stripH - 8;
        var power = new Rect(strip.x() + 10, strip.y() + 4, btn, btn);
        var lcdX = strip.x() + Math.max(btn + 24, strip.width() * 45 / 100);
        var lcd = new Rect(lcdX, strip.y() + 4, strip.right() - 6 - lcdX, stripH - 8);
        var plateW = Math.min(118, bezel.width() / 3);
        var plate = new Rect(bezel.x() + (bezel.width() - plateW) / 2, y0, plateW, 17);

        if (!withSide) {
            return new Layout(
                false,
                new Rect(x0, y0, w, h),
                screen,
                body,
                main,
                plate,
                bezel,
                strip,
                power,
                lcd,
                null,
                null,
                new Rect[0],
                new Rect[0],
                null,
                null,
                null,
                null,
                null
            );
        }

        var side = new Rect(main.right() - 1, body.y() + 6, sideW + 1, body.height() - 12);
        var topH = Math.round(side.height() * 0.41F);
        var grilleH = Math.round(side.height() * 0.27F);
        var selector = new Rect(side.x(), side.y(), side.width(), topH);
        var grilleBand = new Rect(side.x() + 3, selector.bottom() + 2, side.width() - 6, grilleH);
        var geiger = new Rect(side.x(), grilleBand.bottom() + 2, side.width(), side.bottom() - grilleBand.bottom() - 2);

        var hazardW = Math.max(16, side.width() * 22 / 100);
        var hazard = new Rect(selector.right() - hazardW - 6, selector.y() + 7, hazardW, selector.height() - 14);
        var labels = new Rect[3];
        var lamps = new Rect[3];
        var rowsH = selector.height() - 18;
        var rowH = Math.clamp(rowsH / 3 - 3, 10, 18);
        var gap = Math.clamp((rowsH - rowH * 3L) / 2, 2, 12);
        var rowsTop = selector.y() + (selector.height() - (rowH * 3 + gap * 2)) / 2;
        var labelX = selector.x() + 8;
        var labelW = hazard.x() - labelX - rowH - 12;
        for (var i = 0; i < 3; i++) {
            var y = rowsTop + i * (rowH + gap);
            labels[i] = new Rect(labelX, y, labelW, rowH);
            lamps[i] = new Rect(labelX + labelW + 5, y, rowH, rowH);
        }
        var knobTop = new Rect(side.right(), selector.y() + 8, knobW, selector.height() - 16);
        var knobBottom = new Rect(side.right(), geiger.y() + geiger.height() / 4, knobW, geiger.height() / 2);
        return new Layout(
            false,
            new Rect(x0, y0, w, h),
            screen,
            body,
            main,
            plate,
            bezel,
            strip,
            power,
            lcd,
            side,
            selector,
            labels,
            lamps,
            hazard,
            grilleBand,
            geiger,
            knobTop,
            knobBottom
        );
    }

    public static void render(GuiGraphicsExtractor graphics, PipBoyScreen screen, Layout l, State s) {
        if (l.fullscreen() || l.body() == null) {
            return;
        }

        var m = l.main();
        graphics.fill(m.x() + 4, m.y() + 4, m.right() + 4, m.bottom() + 4, 0x50000000);
        if (l.hasSide()) {
            var sd = l.side();
            graphics.fill(sd.x() + 4, sd.y() + 4, l.knobTop().right() + 3, sd.bottom() + 4, 0x50000000);
        }

        if (l.hasSide()) {
            knob(graphics, l.knobTop());
            knob(graphics, l.knobBottom());
            sidePanel(graphics, screen, l, s);
        }
        mainPanel(graphics, screen, l, s);
    }

    private static void mainPanel(GuiGraphicsExtractor graphics, PipBoyScreen screen, Layout l, State s) {
        var main = l.main();
        PipBoyDraw.bevel(graphics, main, OUTLINE, OLIVE, OLIVE_LIGHT, OLIVE_SHADE);

        graphics.fill(main.x() + 1, main.y() + 26, main.x() + 4, main.y() + 27, OLIVE_DARK);
        graphics.fill(main.x() + 3, main.y() + 10, main.x() + 4, main.bottom() - 10, OLIVE_SHADE);

        var bezel = l.bezel();
        PipBoyDraw.recess(graphics, bezel, RECESS, RECESS_DEEP, OLIVE_SHADE);
        var brass = bezel.inset(2);
        PipBoyDraw.fill(graphics, brass, BRASS);
        graphics.fill(brass.x(), brass.y(), brass.right(), brass.y() + 1, BRASS_LIGHT);
        graphics.fill(brass.x(), brass.y(), brass.x() + 1, brass.bottom(), BRASS_LIGHT);
        graphics.fill(brass.x(), brass.bottom() - 1, brass.right(), brass.bottom(), BRASS_DARK);
        graphics.fill(brass.right() - 1, brass.y(), brass.right(), brass.bottom(), BRASS_DARK);
        PipBoyDraw.fill(graphics, brass.inset(2), BRASS_DARK);

        var plate = l.plate();
        PipBoyDraw.bevel(graphics, plate, OUTLINE, OLIVE, OLIVE_LIGHT, OLIVE_SHADE);
        PipBoyDraw.screw(graphics, plate.x() + 5, plate.y() + 7);
        PipBoyDraw.screw(graphics, plate.right() - 9, plate.y() + 7);
        var title = screen.getTitle();
        var tw = screen.font().width(title);
        var tx = plate.x() + (plate.width() - tw) / 2;
        var ty = plate.y() + 5;
        screen.text(graphics, title, tx + 1, ty + 1, PLATE_TEXT_HI);
        screen.text(graphics, title, tx, ty, PLATE_TEXT);

        var strip = l.strip();
        PipBoyDraw.recess(graphics, strip, RECESS, RECESS_DEEP, OLIVE_SHADE);
        powerButton(graphics, l.power(), s);
        lcd(graphics, screen, l.lcd(), s);
    }

    private static void powerButton(GuiGraphicsExtractor graphics, Rect r, State s) {
        PipBoyDraw.bevel(graphics, r.inset(-2), OUTLINE, OLIVE, OLIVE_LIGHT, OLIVE_SHADE);
        var face = r.inset(1);
        if (s.power() > 0.05F) {
            var a = Math.round(0x60 * s.power());
            PipBoyDraw.glow(graphics, face, 0xFF7A20, a, 3);
            PipBoyDraw.fill(graphics, face, 0xFFD2541A);
            graphics.fill(face.x(), face.y() + face.height() / 2, face.right(), face.bottom(), 0xFFC23E14);
            graphics.fill(face.x(), face.y(), face.right(), face.y() + 1, 0xFFF39A3A);
            graphics.fill(face.x() + 2, face.y() + 2, face.x() + 3, face.y() + 5, 0xFFFFF0D8);
            if (s.power() < 1F) {
                PipBoyDraw.fill(graphics, face, PipBoyTheme.withAlpha(0x3A140C, Math.round(0xFF * (1F - s.power()))));
            }
        } else {
            PipBoyDraw.fill(graphics, face, 0xFF3A160E);
            graphics.fill(face.x() + 2, face.y() + 2, face.x() + 4, face.y() + 4, 0xFF5A2414);
        }
        if (s.hovered() == PipBoyScreen.HOVER_POWER) {
            PipBoyDraw.outline(graphics, r.inset(-1), 0x60FFFFFF);
        }
    }

    private static void lcd(GuiGraphicsExtractor graphics, PipBoyScreen screen, Rect r, State s) {
        PipBoyDraw.recess(graphics, r, 0xFF060805, 0xFF000000, OLIVE_DARK);
        var lit = 0.2F + 0.8F * s.power();
        var on = PipBoyTheme.phosphor(lit, 0xFF);
        var ghost = PipBoyTheme.phosphor(0.12F, 0xFF);
        var minecraft = Minecraft.getInstance();
        var dayNumber = minecraft.level == null ? 0L : minecraft.level.getGameTime() / 24000L + 1L;
        var day = Component.translatable(
            "pipboy.atomicdawn.lcd.day",
            String.format(Locale.ROOT, "%04d", Math.min(9999L, dayNumber))
        );
        var now = LocalTime.now();
        var time = Component.literal(String.format(Locale.ROOT, "%02d:%02d", now.getHour(), now.getMinute()));
        var y = r.y() + (r.height() - 8) / 2 + 1;
        var x = r.x() + 8;
        screen.text(graphics, day, x, y, on);
        var tw = screen.font().width(time);
        screen.text(graphics, Component.literal("88:88"), r.right() - 8 - tw, y, ghost);
        screen.text(graphics, time, r.right() - 8 - tw, y, on);
        if (RadioPlayer.isPlaying() || HolotapePlayer.isPlaying()) {

            var gx = r.x() + r.width() / 2 + 6;
            var gy = r.y() + r.height() / 2 - 3;
            graphics.fill(gx, gy + 2, gx + 2, gy + 5, on);
            graphics.fill(gx + 2, gy + 1, gx + 3, gy + 6, on);
            graphics.fill(gx + 3, gy, gx + 4, gy + 7, on);
            if (s.millis() / 400 % 2 == 0) {
                graphics.fill(gx + 6, gy + 2, gx + 7, gy + 5, on);
            }
            graphics.fill(gx + 8, gy + 1, gx + 9, gy + 6, on);
        }

        graphics.fill(r.x() + 1, r.y() + 1, r.right() - 1, r.y() + 2, 0x18FFFFFF);
    }

    private static void sidePanel(GuiGraphicsExtractor graphics, PipBoyScreen screen, Layout l, State s) {
        var side = l.side();
        PipBoyDraw.bevel(graphics, side, OUTLINE, OLIVE, OLIVE_LIGHT, OLIVE_SHADE);

        var selector = l.selector();
        PipBoyDraw.bevel(graphics, selector, OUTLINE, OLIVE, OLIVE_LIGHT, OLIVE_SHADE);
        PipBoyDraw.screw(graphics, selector.x() + 3, selector.y() + 3);
        for (var i = 0; i < 3; i++) {
            var label = l.selectorLabels()[i];
            var lamp = l.selectorLamps()[i];
            var lit = i == s.selectorLit() && s.power() > 0.5F;
            var hovered = s.hovered() == i;
            PipBoyDraw.bevel(graphics, label.inset(-1), OUTLINE, OLIVE_DARK, OLIVE_SHADE, OLIVE_LIGHT);
            PipBoyDraw.fill(graphics, label.inset(1), hovered ? 0xFF4A4F38 : 0xFF3D4130);
            var text = PipBoyScreen.TABS.get(PipBoyScreen.SELECTOR_TABS[i]).label();
            var tw = screen.font().width(text);
            var tx = label.x() + 4;
            var ty = label.y() + (label.height() - 8) / 2 + 1;
            if (tw > label.width() - 6) {
                screen.fitted(graphics, text, tx, ty, label.width() - 6, lit ? LABEL_ON : LABEL_OFF);
            } else {
                if (lit) {
                    screen.text(graphics, text, tx, ty + 1, 0x60000000);
                }
                screen.text(graphics, text, tx, ty, lit ? LABEL_ON : LABEL_OFF);
            }
            lamp(graphics, lamp, lit, hovered);
        }
        var hazard = l.hazard();
        PipBoyDraw.bevel(graphics, hazard.inset(-2), OUTLINE, BRASS, BRASS_LIGHT, BRASS_DARK);
        PipBoyDraw.stripes(graphics, hazard, 16, 0xFF1C1C15, 0xFFD6A62C);
        graphics.fill(hazard.x(), hazard.y(), hazard.right(), hazard.y() + 1, 0x40FFFFFF);
        graphics.fill(hazard.right() - 1, hazard.y(), hazard.right(), hazard.bottom(), 0x50000000);

        var grille = l.grille();
        graphics.fill(grille.x(), grille.y(), grille.right(), grille.y() + 4, 0xFF15170F);
        graphics.fill(grille.x(), grille.bottom() - 4, grille.right(), grille.bottom(), 0xFF15170F);
        graphics.fill(grille.x(), grille.y() + 1, grille.right(), grille.y() + 2, 0xFF2C2F22);
        graphics.fill(grille.x(), grille.bottom() - 3, grille.right(), grille.bottom() - 2, 0xFF2C2F22);
        var cage = new Rect(grille.x() + 4, grille.y() + 5, grille.width() - 8, grille.height() - 10);
        PipBoyDraw.fill(graphics, cage, 0xFF1B1D15);
        if (s.tubes() > 0.02F) {
            PipBoyDraw.fill(graphics, cage, PipBoyTheme.withAlpha(0x7A2A10, Math.round(0x50 * s.tubes())));
        }
        for (var i = 0; i < 3; i++) {
            var cx = cage.x() + cage.width() * (2 * i + 1) / 6;
            tube(graphics, cx, cage, s, i);
        }
        PipBoyDraw.mesh(graphics, cage, 7, 0xFF4B513B, 0xFF6E7556);
        PipBoyDraw.outline(graphics, cage, 0xFF262919);

        geiger(graphics, l.geiger(), s);
    }

    private static void lamp(GuiGraphicsExtractor graphics, Rect r, boolean lit, boolean hovered) {
        PipBoyDraw.bevel(graphics, r, OUTLINE, OLIVE_DARK, OLIVE_LIGHT, OUTLINE);
        var face = r.inset(2);
        if (lit) {
            PipBoyDraw.glow(graphics, face, 0xFF2010, 0x70, 2);
            PipBoyDraw.fill(graphics, face, LAMP_ON);
            graphics.fill(face.x(), face.bottom() - 2, face.right(), face.bottom(), 0xFFB01810);
            graphics.fill(face.x() + 1, face.y() + 1, face.x() + 4, face.y() + 3, 0xFFFFE2DA);
        } else {
            PipBoyDraw.fill(graphics, face, hovered ? 0xFF6A1A16 : LAMP_OFF);
            graphics.fill(face.x() + 1, face.y() + 1, face.x() + 3, face.y() + 2, 0xFF7A2A24);
        }
    }

    private static void tube(GuiGraphicsExtractor graphics, int cx, Rect cage, State s, int index) {
        var top = cage.y() + cage.height() / 6;
        var bottom = cage.bottom() - cage.height() / 5;
        var glass = new Rect(cx - 5, top, 11, bottom - top);
        var heat = s.tubes();
        if (heat > 0.02F) {
            var flicker = 0.85F + 0.15F * (float) Math.sin(s.millis() / (70.0 + index * 23) + index * 2.1);
            var glow = Math.clamp(heat * flicker, 0F, 1F);
            PipBoyDraw.glow(graphics, glass.inset(1), 0xFF6A20, Math.round(0xA0 * glow), 4);
            PipBoyDraw.fill(graphics, glass.inset(1), PipBoyTheme.withAlpha(0xE04A18, Math.round(0xC0 * glow)));

            graphics.fill(
                cx - 2,
                top + 3,
                cx - 1,
                bottom - 4,
                PipBoyTheme.withAlpha(0xFFD27A, Math.round(0xFF * glow))
            );
            graphics.fill(
                cx + 1,
                top + 3,
                cx + 2,
                bottom - 4,
                PipBoyTheme.withAlpha(0xFFD27A, Math.round(0xFF * glow))
            );
            graphics.fill(cx - 1, top + 3, cx + 1, top + 4, PipBoyTheme.withAlpha(0xFFF4D0, Math.round(0xFF * glow)));
        }

        graphics.fill(glass.x(), glass.y() + 2, glass.x() + 1, glass.bottom(), 0x90C8C8BC);
        graphics.fill(glass.right() - 1, glass.y() + 2, glass.right(), glass.bottom(), 0x60C8C8BC);
        graphics.fill(glass.x() + 1, glass.y(), glass.right() - 1, glass.y() + 2, 0x90C8C8BC);
        graphics.fill(cx, glass.y() - 2, cx + 1, glass.y(), 0xFFB0B0A4);

        graphics.fill(glass.x() - 1, bottom, glass.right() + 1, bottom + 4, 0xFF4E4A44);
        graphics.fill(glass.x() - 1, bottom, glass.right() + 1, bottom + 1, 0xFF8A857C);
    }

    private static void geiger(GuiGraphicsExtractor graphics, Rect g, State s) {
        PipBoyDraw.bevel(graphics, g, OUTLINE, OLIVE, OLIVE_LIGHT, OLIVE_SHADE);
        var plate = g.inset(6);
        PipBoyDraw.bevel(graphics, plate, OUTLINE, STEEL, STEEL_LIGHT, STEEL_SHADE);
        PipBoyDraw.screw(graphics, plate.x() + 3, plate.y() + 3);
        PipBoyDraw.screw(graphics, plate.x() + 3, plate.bottom() - 7);

        var r = Math.max(8, Math.min(plate.height() * 36 / 100, plate.width() * 22 / 100));
        var cx = plate.x() + 10 + r;
        var cy = plate.y() + plate.height() / 2;
        PipBoyDraw.disc(graphics, cx, cy, r + 1, 0xFF4A5656);
        PipBoyDraw.disc(graphics, cx, cy, r, 0xFF1A2020);
        PipBoyDraw.ring(graphics, cx, cy, r - 2, 1, 0xFF2C3535);
        PipBoyDraw.ring(graphics, cx, cy, Math.max(3, r * 2 / 3), 1, 0xFF3A4545);
        PipBoyDraw.disc(graphics, cx, cy, Math.max(2, r / 3), 0xFF52605F);
        PipBoyDraw.disc(graphics, cx, cy, Math.max(1, r / 6), 0xFF1A2020);
        graphics.fill(cx, cy, cx + 1, cy + 1, 0xFFA8B4B4);
        for (var i = 0; i < 4; i++) {
            var a = Math.PI / 2 * i;
            var px = cx + (int) Math.round(Math.cos(a) * (r + 3));
            var py = cy + (int) Math.round(Math.sin(a) * (r + 3));
            graphics.fill(px, py, px + 1, py + 1, 0xFF2C3535);
        }

        var meterH = plate.height() - 10;
        var meter = new Rect(plate.right() - 18, plate.y() + 5, 11, meterH);
        PipBoyDraw.recess(graphics, meter.inset(-1), 0xFF0E1010, 0xFF000000, STEEL_LIGHT);
        int[] bands = { 0xFFDA2020, 0xFFF07A12, 0xFFE8E01A, 0xFF7ACC1A, 0xFF18A824 };
        var inner = meter.inset(1);
        var bandH = inner.height() / (float) bands.length;
        for (var i = 0; i < bands.length; i++) {
            var y0 = inner.y() + Math.round(i * bandH);
            var y1 = inner.y() + Math.round((i + 1) * bandH);
            graphics.fill(inner.x(), y0, inner.right() - 3, y1, bands[i]);
            graphics.fill(inner.x(), y0, inner.right() - 3, y0 + 1, 0x30FFFFFF);
            graphics.fill(inner.right() - 2, y0, inner.right(), y0 + 1, 0xFF000000);
        }
        graphics.fill(meter.right() - 2, meter.y() - 2, meter.right() + 1, meter.y() + 1, STEEL_SHADE);

        var fraction = 0F;
        var player = Minecraft.getInstance().player;
        if (player != null && s.power() > 0.5F) {
            var rads = player.getData(ADAttachments.RADIATION.get()).rads();
            fraction = Math.clamp(rads / (float) AtomicDawn.CONFIG.radiation.MAX_RADS, 0F, 1F);
            if (rads > 0.5F) {
                fraction += (float) Math.sin(s.millis() / 37.0) * 0.012F + (float) Math.sin(s.millis() / 11.0) * 0.006F;
            }
        }
        var needleY = inner.bottom() - 2 - Math.round((inner.height() - 3) * Math.clamp(fraction, 0F, 1F));
        graphics.fill(meter.x() - 3, needleY, meter.right() + 1, needleY + 2, 0xFFF4F4EC);
        graphics.fill(meter.x() - 3, needleY + 2, meter.right() + 1, needleY + 3, 0x70000000);
    }

    private static void knob(GuiGraphicsExtractor graphics, Rect r) {
        graphics.fill(r.x(), r.y() + 3, r.x() + 3, r.bottom() - 3, OLIVE_DARK);
        graphics.fill(r.x(), r.y() + 3, r.x() + 3, r.y() + 4, OLIVE_LIGHT);
        var body = new Rect(r.x() + 3, r.y(), r.width() - 3, r.height());
        PipBoyDraw.fill(graphics, body, OUTLINE);
        var face = body.inset(1);
        for (var y = face.y(); y < face.bottom(); y += 4) {
            var bottom = Math.min(face.bottom(), y + 4);
            graphics.fill(face.x(), y, face.right(), bottom, 0xFF5A5C55);
            graphics.fill(face.x(), y, face.right(), Math.min(bottom, y + 2), 0xFF9A9C94);
            graphics.fill(face.x(), y, face.x() + 3, Math.min(bottom, y + 2), 0xFFC4C6BD);
            graphics.fill(face.right() - 2, y, face.right(), bottom, 0xFF45473F);
        }
    }
}
