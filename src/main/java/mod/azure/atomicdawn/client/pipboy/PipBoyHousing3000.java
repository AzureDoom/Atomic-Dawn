package mod.azure.atomicdawn.client.pipboy;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class PipBoyHousing3000 {

    record Parts(
        Rect shell,
        Rect bezel,
        Rect ribs,
        Rect bay,
        @Nullable Rect left,
        @Nullable Rect gauge,
        @Nullable Rect plate,
        @Nullable Rect grip,
        @Nullable Rect wheel,
        @Nullable Rect modelLabel,
        @Nullable Rect knob
    ) {}

    private PipBoyHousing3000() {}

    private static PipBoyModel.Palette p() {
        return PipBoyModel.palette();
    }

    static PipBoyHousing.Layout layout(int width, int height) {
        var maxW = width - 10;
        var maxH = height - 8;
        var withSide = maxW >= 440 && maxH >= 200;
        var w = Math.min(maxW, withSide ? 580 : 420);
        var h = Math.min(maxH, 330);
        var x0 = (width - w) / 2;
        var y0 = (height - h) / 2;
        var device = new Rect(x0, y0, w, h);

        var leftW = withSide ? Math.clamp(Math.round(w * 0.25F), 100, 150) : 0;
        var bayH = Math.clamp(Math.round(h * 0.2F), 44, 66);
        var shellX = x0 + Math.max(0, leftW - 8);
        var shell = new Rect(shellX, y0, x0 + w - shellX, h - bayH + 12);
        var ribW = Math.clamp(Math.round(shell.width() * 0.06F), 14, 30);
        var bezel = new Rect(shell.x() + 10, shell.y() + 8, shell.width() - 22 - ribW, shell.height() - 16);
        var screen = bezel.inset(7);
        var ribs = new Rect(bezel.right() + 5, bezel.y() + 18, ribW, Math.max(0, bezel.height() - 36));

        var bayW = Math.clamp(bezel.width() / 2, 120, 220);
        var bay = new Rect(
            bezel.x() + (bezel.width() - bayW) / 2,
            shell.bottom() - 14,
            bayW,
            y0 + h - (shell.bottom() - 14)
        );
        var button = Math.clamp(bay.height() - 32, 10, 24);
        var labels = new Rect[3];
        var lamps = new Rect[3];
        var slot = bay.width() / 3;
        for (var i = 0; i < 3; i++) {
            var cx = bay.x() + slot * i + slot / 2;
            labels[i] = new Rect(cx - slot / 2 + 4, bay.y() + 16, slot - 8, 10);
            lamps[i] = new Rect(cx - button / 2, labels[i].bottom() + 2, button, button);
        }

        if (!withSide) {
            return new PipBoyHousing.Layout(
                false,
                device,
                screen,
                null,
                labels,
                lamps,
                PipBoyScreen.SELECTOR_TABS,
                new Parts(shell, bezel, ribs, bay, null, null, null, null, null, null, null)
            );
        }

        var left = new Rect(x0, y0 + 12, leftW, h - bayH - 6);
        var gaugeR = Math.clamp(Math.round(Math.min(leftW * 0.2F, left.height() * 0.1F)), 10, 26);
        var gauge = new Rect(left.x() + 10, left.y() + 8, gaugeR * 2, gaugeR * 2);
        var plate = new Rect(left.x() + 6, gauge.bottom() + 8, left.width() - 34, 16);
        var gripH = Math.round(left.height() * 0.22F);
        var grip = new Rect(left.x() + 6, plate.bottom() + 4, left.width() - 34, gripH);
        var wheel = new Rect(grip.right() + 4, plate.y(), 16, grip.bottom() - plate.y());
        var modelLabel = new Rect(left.x() + 10, grip.bottom() + 6, left.width() - 40, 11);
        var remaining = left.bottom() - 6 - (modelLabel.bottom() + 6);
        Rect knob = null;
        if (remaining >= 22) {
            var knobR = Math.clamp(Math.min(remaining / 2 - 4, Math.round(leftW * 0.2F)), 8, 26);
            var cx = left.x() + left.width() * 2 / 5;
            var cy = modelLabel.bottom() + 6 + remaining / 2;
            knob = new Rect(cx - knobR, cy - knobR, knobR * 2, knobR * 2);
        }
        return new PipBoyHousing.Layout(
            false,
            device,
            screen,
            knob,
            labels,
            lamps,
            PipBoyScreen.SELECTOR_TABS,
            new Parts(shell, bezel, ribs, bay, left, gauge, plate, grip, wheel, modelLabel, knob)
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
            new Rect(q.shell().x() + 4, q.shell().y() + 4, q.shell().width(), q.shell().height()),
            shadow
        );
        PipBoyDraw.fill(
            graphics,
            new Rect(q.bay().x() + 4, q.bay().y() + 4, q.bay().width(), q.bay().height()),
            shadow
        );
        if (q.left() != null) {
            leftPanel(graphics, screen, l, q, s);
        }
        shell(graphics, q);
        bay(graphics, screen, l, q, s);
    }

    private static void shell(GuiGraphicsExtractor graphics, Parts q) {
        var shell = q.shell();
        PipBoyDraw.bevel(graphics, shell, p().outline(), p().body(), p().bodyLight(), p().bodyShade());
        var frame = q.bezel().inset(-4);
        PipBoyDraw.bevel(graphics, frame, p().outline(), p().bodyShade(), p().bodyDark(), p().bodyLight());
        PipBoyDraw.recess(graphics, q.bezel(), p().recess(), p().recessDeep(), p().bodyShade());
        PipBoyDraw.fill(graphics, q.bezel().inset(3), 0xFF050605);
        if (q.ribs().height() > 0) {
            PipBoyDraw.recess(graphics, q.ribs().inset(-2), p().bodyDark(), p().recessDeep(), p().bodyLight());
            PipBoyHousing.ribs(graphics, q.ribs(), 7, p().bodyLight(), p().bodyShade(), true);
        }
        PipBoyDraw.screw(graphics, shell.x() + 4, shell.y() + 4);
        PipBoyDraw.screw(graphics, shell.right() - 8, shell.y() + 4);
        PipBoyDraw.screw(graphics, shell.right() - 8, shell.bottom() - 8);
        graphics.fill(
            frame.x() + frame.width() / 3,
            shell.y() + 1,
            frame.right() - frame.width() / 3,
            shell.y() + 3,
            p().bodyLight()
        );
    }

    private static void bay(
        GuiGraphicsExtractor graphics,
        PipBoyScreen screen,
        PipBoyHousing.Layout l,
        Parts q,
        PipBoyHousing.State s
    ) {
        var bay = q.bay();
        PipBoyDraw.bevel(graphics, bay, p().outline(), p().body(), p().bodyLight(), p().bodyShade());
        var face = new Rect(bay.x() + 6, bay.y() + 12, bay.width() - 12, Math.max(0, bay.height() - 16));
        PipBoyDraw.recess(graphics, face, p().bodyShade(), p().bodyDark(), p().bodyLight());
        graphics.fill(bay.x() + 3, bay.y() + 3, bay.x() + 5, bay.bottom() - 3, p().bodyDark());
        graphics.fill(bay.right() - 5, bay.y() + 3, bay.right() - 3, bay.bottom() - 3, p().bodyDark());
        for (var i = 0; i < l.selectorLabels().length; i++) {
            var label = l.selectorLabels()[i];
            var lit = PipBoyHousing.lit(s, i);
            var hovered = PipBoyHousing.hovered(s, i);
            var text = PipBoyScreen.TABS.get(l.selectorTabs()[i]).label();
            var tw = Math.min(label.width(), screen.font().width(text));
            var tx = label.x() + (label.width() - tw) / 2;
            screen.fitted(graphics, text, tx + 1, label.y() + 2, label.width(), 0x80000000);
            screen.fitted(
                graphics,
                text,
                tx,
                label.y() + 1,
                label.width(),
                lit || hovered ? p().labelOn() : p().labelOff()
            );
            PipBoyHousing.roundButton(graphics, l.selectorLamps()[i], lit, hovered, p().lampOn(), p().lampOff());
        }
    }

    private static void leftPanel(
        GuiGraphicsExtractor graphics,
        PipBoyScreen screen,
        PipBoyHousing.Layout l,
        Parts q,
        PipBoyHousing.State s
    ) {
        var left = q.left();
        PipBoyDraw.bevel(graphics, left, p().outline(), p().body(), p().bodyLight(), p().bodyShade());
        var gauge = q.gauge();
        var radius = gauge.width() / 2;
        PipBoyHousing.gauge(
            graphics,
            gauge.x() + radius,
            gauge.y() + radius,
            radius,
            s.power() > 0.5F ? PipBoyHousing.radiation() : 0F,
            p()
        );
        PipBoyDraw.screw(graphics, gauge.right() + 8, gauge.y() + radius - 2);

        var plate = q.plate();
        PipBoyDraw.bevel(graphics, plate, p().outline(), p().bodyShade(), p().bodyLight(), p().bodyDark());
        var brand = Component.translatable("pipboy.atomicdawn.housing.brand");
        var bw = Math.min(plate.width() - 8, screen.font().width(brand));
        var bx = plate.x() + (plate.width() - bw) / 2;
        screen.fitted(graphics, brand, bx + 1, plate.y() + 5, plate.width() - 8, p().plateText());
        screen.fitted(graphics, brand, bx, plate.y() + 4, plate.width() - 8, p().plateTextHi());

        var grip = q.grip();
        PipBoyDraw.recess(graphics, grip, p().bodyDark(), p().recessDeep(), p().bodyLight());
        PipBoyHousing.ribs(graphics, grip.inset(2), 7, p().bodyLight(), p().bodyShade(), true);

        var wheel = q.wheel();
        PipBoyDraw.recess(graphics, wheel, p().recessDeep(), 0xFF000000, p().bodyLight());
        var drum = wheel.inset(3);
        var offset = (int) (s.millis() / 400 % 3);
        PipBoyDraw.fill(graphics, drum, p().trimDark());
        for (var y = drum.y() - offset; y < drum.bottom(); y += 3) {
            var y0 = Math.max(drum.y(), y);
            var y1 = Math.min(drum.bottom(), y + 1);
            if (y1 > y0) {
                graphics.fill(drum.x(), y0, drum.right(), y1, p().trimLight());
            }
        }
        graphics.fill(drum.right() - 2, drum.y(), drum.right(), drum.bottom(), 0x50000000);

        var model = q.modelLabel();
        PipBoyDraw.recess(graphics, model, p().recessDeep(), 0xFF000000, p().bodyLight());
        var label = Component.translatable("pipboy.atomicdawn.housing.model_3000");
        var lw = Math.min(model.width() - 4, screen.font().width(label));
        screen.fitted(
            graphics,
            label,
            model.x() + (model.width() - lw) / 2,
            model.y() + 2,
            model.width() - 4,
            0xFFD8D4C4
        );

        var knob = q.knob();
        if (knob != null) {
            var r = knob.width() / 2;
            var cx = knob.x() + r;
            var cy = knob.y() + r;
            var angle = Math.PI * 0.75 + Math.PI * 0.75 * s.selectorLit();
            PipBoyHousing.dialKnob(graphics, cx, cy, r, angle, p());
            if (s.hovered() == PipBoyScreen.HOVER_POWER) {
                PipBoyDraw.ring(graphics, cx, cy, r + 5, 1, 0x60FFFFFF);
            }
            PipBoyDraw.screw(graphics, left.x() + 4, left.bottom() - 9);
        }
    }
}
