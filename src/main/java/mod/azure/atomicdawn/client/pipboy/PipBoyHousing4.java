package mod.azure.atomicdawn.client.pipboy;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.Nullable;

final class PipBoyHousing4 {

    record Parts(
        Rect body,
        Rect main,
        Rect bezel,
        Rect strip,
        @Nullable Rect cuff,
        @Nullable Rect side,
        @Nullable Rect geiger,
        @Nullable Rect selector,
        @Nullable Rect knob
    ) {}

    private PipBoyHousing4() {}

    private static PipBoyModel.Palette p() {
        return PipBoyModel.palette();
    }

    static PipBoyHousing.Layout layout(int width, int height) {
        var maxW = width - 10;
        var maxH = height - 8;
        var withSide = maxW >= 440 && maxH >= 200;
        var w = Math.min(maxW, withSide ? 580 : 420);
        var h = Math.min(maxH, 320);
        var x0 = (width - w) / 2;
        var y0 = (height - h) / 2;
        var device = new Rect(x0, y0, w, h);

        var cuffW = withSide ? 14 : 0;
        var body = new Rect(x0 + cuffW, y0 + 4, w - cuffW, h - 8);
        var sideW = withSide ? Math.clamp(Math.round(body.width() * 0.24F), 100, 140) : 0;
        var main = new Rect(body.x(), body.y(), body.width() - sideW, body.height());
        var stripH = Math.clamp(Math.round(h * 0.09F), 16, 24);
        var bezel = new Rect(main.x() + 10, main.y() + 10, main.width() - 20, main.height() - 24 - stripH);
        var screen = bezel.inset(8);
        var strip = new Rect(main.x() + 10, bezel.bottom() + 6, main.width() - 20, stripH);
        var powerSize = stripH - 8;

        if (!withSide) {
            var power = new Rect(strip.x() + 8, strip.y() + 4, powerSize, powerSize);
            return new PipBoyHousing.Layout(
                false,
                device,
                screen,
                power,
                new Rect[0],
                new Rect[0],
                new int[0],
                new Parts(body, main, bezel, strip, null, null, null, null, null)
            );
        }

        var cuff = new Rect(x0, y0 + h / 6, cuffW + 4, h * 2 / 3);
        var side = new Rect(main.right() - 2, body.y() + 10, sideW + 2, body.height() - 20);
        var geiger = new Rect(side.x() + 8, side.y() + 8, side.width() - 16, Math.round(side.height() * 0.2F));
        var power = new Rect(side.x() + 10, side.bottom() - 10 - powerSize, powerSize, powerSize);
        var selector = new Rect(
            side.x() + 6,
            geiger.bottom() + 8,
            side.width() - 12,
            power.y() - 8 - geiger.bottom() - 8
        );
        var tabs = PipBoyScreen.TABS.size();
        var knobR = Math.clamp(Math.min(Math.round(selector.width() * 0.22F), selector.height() / 2 - 6), 8, 26);
        var knob = new Rect(
            selector.right() - 6 - knobR * 2,
            selector.y() + selector.height() / 2 - knobR,
            knobR * 2,
            knobR * 2
        );
        var rowH = Math.clamp((selector.height() - 8) / tabs, 9, 16);
        var rowsTop = selector.y() + (selector.height() - rowH * tabs) / 2;
        var labelX = selector.x() + 12;
        var labelW = Math.max(1, knob.x() - 8 - labelX);
        var labels = new Rect[tabs];
        var lamps = new Rect[tabs];
        var selectorTabs = new int[tabs];
        for (var i = 0; i < tabs; i++) {
            var y = rowsTop + i * rowH;
            labels[i] = new Rect(labelX, y, labelW, rowH);
            lamps[i] = new Rect(selector.x() + 4, y + (rowH - 5) / 2, 5, 5);
            selectorTabs[i] = i;
        }
        return new PipBoyHousing.Layout(
            false,
            device,
            screen,
            power,
            labels,
            lamps,
            selectorTabs,
            new Parts(body, main, bezel, strip, cuff, side, geiger, selector, knob)
        );
    }

    static void render(
        GuiGraphicsExtractor graphics,
        PipBoyScreen screen,
        PipBoyHousing.Layout l,
        PipBoyHousing.State s
    ) {
        if (!(l.parts() instanceof Parts q)) {
            return;
        }
        var shadow = PipBoyHousing.shadow();
        PipBoyDraw.fill(
            graphics,
            new Rect(q.body().x() + 4, q.body().y() + 4, q.body().width(), q.body().height()),
            shadow
        );
        if (q.cuff() != null) {
            var cuff = q.cuff();
            PipBoyDraw.bevel(graphics, cuff, p().outline(), p().trimDark(), p().trim(), p().outline());
            PipBoyHousing.ribs(graphics, cuff.inset(2), 6, p().trim(), p().trimDark(), true);
        }
        if (q.side() != null) {
            side(graphics, screen, l, q, s);
        }
        mainPanel(graphics, screen, l, q, s);
    }

    private static void mainPanel(
        GuiGraphicsExtractor graphics,
        PipBoyScreen screen,
        PipBoyHousing.Layout l,
        Parts q,
        PipBoyHousing.State s
    ) {
        var main = q.main();
        PipBoyDraw.bevel(graphics, main, p().outline(), p().body(), p().bodyLight(), p().bodyShade());
        graphics.fill(main.x() + 3, main.y() + 3, main.right() - 3, main.y() + 5, p().bodyLight());
        var rubber = q.bezel().inset(-3);
        PipBoyDraw.bevel(graphics, rubber, p().outline(), p().trim(), p().trimLight(), p().trimDark());
        PipBoyDraw.recess(graphics, q.bezel(), p().trimDark(), p().outline(), p().trim());
        PipBoyDraw.fill(graphics, q.bezel().inset(4), 0xFF050605);
        var corner = 5;
        var bezel = q.bezel();
        for (var i = 0; i < corner; i++) {
            var cut = corner - i;
            graphics.fill(bezel.x(), bezel.y() + i, bezel.x() + cut, bezel.y() + i + 1, p().trimDark());
            graphics.fill(bezel.right() - cut, bezel.y() + i, bezel.right(), bezel.y() + i + 1, p().trimDark());
            graphics.fill(bezel.x(), bezel.bottom() - i - 1, bezel.x() + cut, bezel.bottom() - i, p().trimDark());
            graphics.fill(
                bezel.right() - cut,
                bezel.bottom() - i - 1,
                bezel.right(),
                bezel.bottom() - i,
                p().trimDark()
            );
        }
        var strip = q.strip();
        PipBoyDraw.recess(graphics, strip, p().bodyShade(), p().bodyDark(), p().bodyLight());
        var title = PipBoyModel.name();
        var available = Math.max(1, strip.width() - 40);
        var tw = Math.min(available, screen.font().width(title));
        var tx = strip.x() + (strip.width() - tw) / 2;
        var ty = strip.y() + (strip.height() - 8) / 2 + 1;
        screen.fitted(graphics, title, tx + 1, ty + 1, available, p().plateText());
        screen.fitted(graphics, title, tx, ty, available, p().plateTextHi());
        PipBoyDraw.screw(graphics, main.x() + 4, main.bottom() - 8);
        PipBoyDraw.screw(graphics, main.right() - 8, main.bottom() - 8);
        if (q.side() == null && l.power() != null) {
            PipBoyHousing76.powerButton(graphics, l.power(), s);
        }
    }

    private static void side(
        GuiGraphicsExtractor graphics,
        PipBoyScreen screen,
        PipBoyHousing.Layout l,
        Parts q,
        PipBoyHousing.State s
    ) {
        var side = q.side();
        PipBoyDraw.bevel(graphics, side, p().outline(), p().body(), p().bodyLight(), p().bodyShade());
        PipBoyDraw.screw(graphics, side.right() - 8, side.y() + 4);
        PipBoyDraw.screw(graphics, side.right() - 8, side.bottom() - 8);

        var geiger = q.geiger();
        PipBoyDraw.bevel(graphics, geiger.inset(-2), p().outline(), p().trim(), p().trimLight(), p().trimDark());
        PipBoyDraw.fill(graphics, geiger, 0xFFE6E0C8);
        var scaleY = geiger.y() + geiger.height() * 2 / 3;
        graphics.fill(geiger.x() + 4, scaleY, geiger.right() - 4, scaleY + 1, 0xFF2A2A26);
        for (var i = 0; i <= 10; i++) {
            var x = geiger.x() + 4 + (geiger.width() - 8) * i / 10;
            var tall = i % 5 == 0 ? 5 : 3;
            graphics.fill(x, scaleY - tall, x + 1, scaleY, i >= 8 ? 0xFFC8281E : 0xFF2A2A26);
        }
        graphics.fill(
            geiger.x() + 4 + (geiger.width() - 8) * 8 / 10,
            scaleY + 1,
            geiger.right() - 4,
            scaleY + 3,
            0xFFC8281E
        );
        var fraction = s.power() > 0.5F ? PipBoyHousing.radiation() : 0F;
        var needleX = geiger.x() + 4 + Math.round((geiger.width() - 8) * fraction);
        PipBoyDraw.line(
            graphics,
            needleX,
            geiger.y() + 3,
            geiger.x() + geiger.width() / 2F,
            geiger.bottom() - 1,
            1,
            0xFF141412
        );
        graphics.fill(geiger.x(), geiger.y(), geiger.right(), geiger.y() + 2, 0x30FFFFFF);

        var selector = q.selector();
        PipBoyDraw.recess(graphics, selector, p().bodyShade(), p().bodyDark(), p().bodyLight());
        var selected = -1;
        for (var i = 0; i < l.selectorLabels().length; i++) {
            var label = l.selectorLabels()[i];
            var lit = PipBoyHousing.lit(s, i);
            var hovered = PipBoyHousing.hovered(s, i);
            if (lit) {
                selected = i;
            }
            var lamp = l.selectorLamps()[i];
            PipBoyDraw.fill(graphics, lamp, lit ? 0xFF000000 | p().lampOn() : 0xFF000000 | p().lampOff());
            var text = PipBoyScreen.TABS.get(l.selectorTabs()[i]).label();
            var ty = label.y() + (label.height() - 8) / 2 + 1;
            screen.fitted(graphics, text, label.x() + 1, ty + 1, label.width(), 0x80000000);
            screen.fitted(
                graphics,
                text,
                label.x(),
                ty,
                label.width(),
                lit || hovered ? p().labelOn() : p().labelOff()
            );
        }
        var knob = q.knob();
        var r = knob.width() / 2;
        var cx = knob.x() + r;
        var cy = knob.y() + r;
        var angle = Math.PI;
        if (selected >= 0) {
            var target = l.selectorLabels()[selected];
            angle = Math.atan2(target.y() + target.height() / 2.0 - cy, target.right() - cx);
        }
        PipBoyHousing.dialKnob(graphics, cx, cy, r, angle, p());

        PipBoyHousing76.powerButton(graphics, l.power(), s);
    }
}
