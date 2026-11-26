package mod.azure.atomicdawn.client.pipboy;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.time.LocalTime;
import java.util.Locale;

import mod.azure.atomicdawn.client.helper.ADGuiGraphics;

final class PipBoyHousing2000 {

    private static final int[] WIRES = { 0xFF3A5AC8, 0xFFC83A2A, 0xFF3AA046, 0xFF3A5AC8, 0xFFD8D8D0, 0xFFC83A2A };

    private static final int BRAND_RED = 0xFFC8321E;

    private static final int BRAND_YELLOW = 0xFFE8C040;

    record Parts(
        Rect column,
        Rect date,
        Rect logo,
        Rect grille,
        Rect panel,
        Rect hazard,
        Rect closeLabel,
        Rect closeButton,
        Rect[] connectors,
        Rect crt,
        Rect clamp
    ) {}

    private PipBoyHousing2000() {}

    private static PipBoyModel.Palette p() {
        return PipBoyModel.palette();
    }

    static PipBoyHousing.Layout layout(int width, int height) {
        var maxW = width - 10;
        var maxH = height - 8;
        var w = Math.min(maxW, 580);
        var h = Math.min(maxH, 340);
        var x0 = (width - w) / 2;
        var y0 = (height - h) / 2;
        var device = new Rect(x0, y0, w, h);

        var columnW = Math.clamp(Math.round(w * 0.34F), 116, 200);
        var column = new Rect(x0, y0, columnW, h);
        var crt = new Rect(column.right() + 4, y0 + 6, w - columnW - 4, h - 6);
        var clamp = new Rect(crt.x() + crt.width() / 2 - 26, y0, 52, 12);
        var screen = crt.inset(Math.clamp(crt.width() / 24, 10, 16));

        var date = new Rect(column.x() + 4, column.y() + 4, column.width() - 8, 18);
        var logo = new Rect(column.x() + 4, date.bottom() + 4, column.width() - 8, Math.round(h * 0.22F));
        var grille = new Rect(column.x() + 4, logo.bottom() + 4, column.width() - 8, Math.round(h * 0.18F));
        var panel = new Rect(
            column.x() + 4,
            grille.bottom() + 4,
            column.width() - 8,
            column.bottom() - 4 - grille.bottom() - 4
        );
        var hazard = new Rect(panel.x() + 5, panel.y() + 6, 12, Math.max(0, panel.height() - 12));

        var tabs = PipBoyScreen.TABS.size();
        var rows = tabs + 1;
        var rowH = Math.clamp((panel.height() - 12) / rows, 9, 18);
        var rowsTop = panel.y() + (panel.height() - rowH * rows) / 2;
        var button = Math.clamp(rowH - 3, 6, 12);
        var connectorW = 14;
        var connectorX = panel.right() - connectorW - 3;
        var labels = new Rect[tabs];
        var lamps = new Rect[tabs];
        var connectors = new Rect[rows];
        var selectorTabs = new int[tabs];
        Rect closeLabel = null;
        Rect closeButton = null;
        for (var i = 0; i < rows; i++) {
            var y = rowsTop + i * rowH;
            var lamp = new Rect(hazard.right() + 6, y + (rowH - button) / 2, button, button);
            var label = new Rect(lamp.right() + 4, y, Math.max(1, connectorX - 4 - lamp.right() - 4), rowH);
            connectors[i] = new Rect(connectorX, y + (rowH - 6) / 2, connectorW, 6);
            if (i < tabs) {
                labels[i] = label;
                lamps[i] = lamp;
                selectorTabs[i] = i;
            } else {
                closeLabel = label;
                closeButton = lamp;
            }
        }
        var power = new Rect(
            closeButton.x(),
            closeLabel.y(),
            closeLabel.right() - closeButton.x(),
            closeLabel.height()
        );
        return new PipBoyHousing.Layout(
            false,
            device,
            screen,
            power,
            labels,
            lamps,
            selectorTabs,
            new Parts(column, date, logo, grille, panel, hazard, closeLabel, closeButton, connectors, crt, clamp)
        );
    }

    static void render(
        ADGuiGraphics graphics,
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
            new Rect(q.column().x() + 4, q.column().y() + 4, q.column().width(), q.column().height()),
            shadow
        );
        PipBoyDraw.fill(
            graphics,
            new Rect(q.crt().x() + 4, q.crt().y() + 4, q.crt().width(), q.crt().height()),
            shadow
        );
        crt(graphics, q);
        column(graphics, screen, l, q, s);
    }

    private static void crt(ADGuiGraphics graphics, Parts q) {
        var clamp = q.clamp();
        PipBoyDraw.bevel(graphics, clamp, p().outline(), p().trim(), p().trimLight(), p().trimDark());
        PipBoyDraw.screw(graphics, clamp.x() + 4, clamp.y() + 4);
        PipBoyDraw.screw(graphics, clamp.right() - 8, clamp.y() + 4);
        var crt = q.crt();
        PipBoyDraw.bevel(graphics, crt, p().outline(), p().bodyShade(), p().bodyLight(), p().bodyDark());
        var band = crt.inset(4);
        PipBoyDraw.bevel(graphics, band, p().outline(), p().body(), p().bodyLight(), p().bodyShade());
        var lip = crt.inset(Math.clamp(crt.width() / 24, 10, 16) - 3);
        PipBoyDraw.recess(graphics, lip, 0xFF0A0A09, 0xFF000000, p().bodyLight());
        var corner = Math.max(3, lip.width() / 40);
        for (var i = 0; i < corner; i++) {
            var cut = corner - i;
            graphics.fill(lip.x(), lip.y() + i, lip.x() + cut, lip.y() + i + 1, p().body());
            graphics.fill(lip.right() - cut, lip.y() + i, lip.right(), lip.y() + i + 1, p().body());
            graphics.fill(lip.x(), lip.bottom() - i - 1, lip.x() + cut, lip.bottom() - i, p().body());
            graphics.fill(lip.right() - cut, lip.bottom() - i - 1, lip.right(), lip.bottom() - i, p().body());
        }
        PipBoyDraw.screw(graphics, crt.x() + 6, crt.y() + 6);
        PipBoyDraw.screw(graphics, crt.right() - 10, crt.y() + 6);
        PipBoyDraw.screw(graphics, crt.x() + 6, crt.bottom() - 10);
        PipBoyDraw.screw(graphics, crt.right() - 10, crt.bottom() - 10);
    }

    private static void column(
        ADGuiGraphics graphics,
        PipBoyScreen screen,
        PipBoyHousing.Layout l,
        Parts q,
        PipBoyHousing.State s
    ) {
        var column = q.column();
        PipBoyDraw.bevel(graphics, column, p().outline(), p().bodyDark(), p().bodyShade(), p().outline());
        date(graphics, screen, q.date(), s);
        logo(graphics, screen, q.logo());
        grille(graphics, q.grille(), s);
        panel(graphics, screen, l, q, s);
    }

    private static void date(ADGuiGraphics graphics, PipBoyScreen screen, Rect r, PipBoyHousing.State s) {
        PipBoyDraw.bevel(graphics, r, p().outline(), p().trimDark(), p().trim(), p().outline());
        var inner = r.inset(3);
        PipBoyDraw.fill(graphics, inner, 0xFF060606);
        var minecraft = Minecraft.getInstance();
        var dayNumber = minecraft.level == null ? 1L : minecraft.level.getGameTime() / 24000L + 1L;
        var day = Component.translatable(
            "pipboy.atomicdawn.lcd.day",
            String.format(Locale.ROOT, "%04d", Math.min(9999L, dayNumber))
        );
        var now = LocalTime.now();
        var time = Component.literal(String.format(Locale.ROOT, "%02d%02d", now.getHour(), now.getMinute()));
        var color = s.power() > 0.2F ? 0xFFD8D8D0 : 0xFF505048;
        var timeW = screen.font().width(time) + 6;
        var split = inner.right() - timeW - 12;
        graphics.fill(split, inner.y(), split + 1, inner.bottom(), p().trimDark());
        graphics.fill(split + 11, inner.y(), split + 12, inner.bottom(), p().trimDark());
        var icon = new Rect(split + 3, inner.y() + 3, 6, inner.height() - 6);
        PipBoyDraw.fill(graphics, icon, s.power() > 0.2F ? BRAND_YELLOW : 0xFF3A3018);
        screen.fitted(
            graphics,
            day,
            inner.x() + 3,
            inner.y() + (inner.height() - 8) / 2 + 1,
            split - inner.x() - 5,
            color
        );
        screen.text(graphics, time, split + 15, inner.y() + (inner.height() - 8) / 2 + 1, color);
    }

    private static void logo(ADGuiGraphics graphics, PipBoyScreen screen, Rect r) {
        PipBoyDraw.bevel(graphics, r, p().outline(), p().trimDark(), p().trim(), p().outline());
        var plate = new Rect(r.x() + 4, r.y() + 4, Math.max(0, r.width() - 26), Math.max(0, r.height() - 8));
        PipBoyDraw.bevel(graphics, plate, p().outline(), 0xFF6E5A40, 0xFF8E7656, 0xFF4A3C28);
        for (var i = 0; i < 6; i++) {
            var x = plate.x() + 3 + (i * 37) % Math.max(1, plate.width() - 6);
            var y = plate.y() + 3 + (i * 23) % Math.max(1, plate.height() - 6);
            graphics.fill(x, y, x + 3, y + 2, 0x40301A0C);
        }
        var brand = Component.translatable("pipboy.atomicdawn.housing.brand");
        var model = Component.translatable("pipboy.atomicdawn.housing.model_2000");
        var font = screen.font();
        var brandScale = Math.clamp((plate.width() - 10) / (float) Math.max(1, font.width(brand)), 0.6F, 1.6F);
        var modelScale = Math.clamp(
            Math.min((plate.width() - 10) / (float) Math.max(1, font.width(model)), (plate.height() - 14) / 9F),
            0.8F,
            2.6F
        );
        scaled(graphics, screen, brand, plate.x() + 5, plate.y() + 4, brandScale, BRAND_RED);
        var modelY = plate.y() + 6 + Math.round(9 * brandScale);
        scaled(graphics, screen, model, plate.x() + 6, modelY + 1, modelScale, 0xFF3A2410);
        scaled(graphics, screen, model, plate.x() + 5, modelY, modelScale, BRAND_YELLOW);
        var slot = new Rect(plate.right() + 5, r.y() + 6, 12, Math.max(0, r.height() / 2 - 8));
        PipBoyDraw.recess(graphics, slot, 0xFF0C0C0A, 0xFF000000, p().trim());
        for (var y = slot.y() + 2; y < slot.bottom() - 2; y += 3) {
            graphics.fill(slot.x() + 2, y, slot.right() - 2, y + 1, 0xFF2A2A26);
        }
        var toggle = new Rect(
            plate.right() + 6,
            slot.bottom() + 6,
            10,
            Math.max(0, r.bottom() - 6 - slot.bottom() - 6)
        );
        PipBoyDraw.bevel(graphics, toggle, p().outline(), 0xFF222220, 0xFF3A3A36, 0xFF0E0E0C);
    }

    private static void scaled(
        ADGuiGraphics graphics,
        PipBoyScreen screen,
        Component text,
        int x,
        int y,
        float scale,
        int color
    ) {
        graphics.push();
        graphics.translate(x, y);
        graphics.scale(scale, scale);
        graphics.text(screen.font(), text, 0, 0, color, true);
        graphics.pop();
    }

    private static void grille(ADGuiGraphics graphics, Rect r, PipBoyHousing.State s) {
        PipBoyDraw.bevel(graphics, r, p().outline(), p().trimDark(), p().trim(), p().outline());
        var cage = r.inset(4);
        PipBoyDraw.fill(graphics, cage, 0xFF141210);
        if (s.tubes() > 0.02F) {
            PipBoyDraw.fill(graphics, cage, PipBoyTheme.withAlpha(0x7A2A10, Math.round(0x60 * s.tubes())));
        }
        for (var i = 0; i < 2; i++) {
            var cx = cage.x() + cage.width() * (2 * i + 1) / 4;
            PipBoyHousing76.tube(graphics, cx, cage, s, i);
        }
        PipBoyDraw.mesh(graphics, cage, 6, 0xFF4A4840, 0xFF6E6B60);
        PipBoyDraw.outline(graphics, cage, 0xFF1A1814);
    }

    private static void panel(
        ADGuiGraphics graphics,
        PipBoyScreen screen,
        PipBoyHousing.Layout l,
        Parts q,
        PipBoyHousing.State s
    ) {
        var panel = q.panel();
        PipBoyDraw.bevel(graphics, panel, p().outline(), p().body(), p().bodyLight(), p().bodyShade());
        var hazard = q.hazard();
        if (hazard.height() > 0) {
            PipBoyDraw.bevel(
                graphics,
                hazard.inset(-1),
                p().outline(),
                p().bodyShade(),
                p().bodyDark(),
                p().bodyLight()
            );
            PipBoyDraw.stripes(graphics, hazard, 10, 0xFF1C1C15, 0xFFD6A62C);
            PipBoyDraw.screw(graphics, hazard.x() + 4, hazard.bottom() + 1);
        }
        for (var i = 0; i < l.selectorLabels().length; i++) {
            var lit = PipBoyHousing.lit(s, i);
            var hovered = PipBoyHousing.hovered(s, i);
            row(
                graphics,
                screen,
                l.selectorLamps()[i],
                l.selectorLabels()[i],
                PipBoyScreen.TABS.get(l.selectorTabs()[i]).label(),
                lit,
                hovered
            );
        }
        row(
            graphics,
            screen,
            q.closeButton(),
            q.closeLabel(),
            Component.translatable("pipboy.atomicdawn.housing.close"),
            false,
            s.hovered() == PipBoyScreen.HOVER_POWER
        );
        var crtX = q.crt().x() + 4;
        for (var i = 0; i < q.connectors().length; i++) {
            var connector = q.connectors()[i];
            var wire = WIRES[i % WIRES.length];
            var wy = connector.y() + connector.height() / 2;
            graphics.fill(connector.right(), wy - 1, crtX, wy + 1, wire);
            graphics.fill(connector.right(), wy - 1, crtX, wy, PipBoyTheme.withAlpha(0xFFFFFF, 0x40));
            PipBoyDraw.bevel(graphics, connector, p().outline(), p().trimDark(), p().trimLight(), p().outline());
            graphics.fill(connector.x() + 2, connector.y() + 2, connector.x() + 5, connector.bottom() - 2, wire);
        }
    }

    private static void row(
        ADGuiGraphics graphics,
        PipBoyScreen screen,
        Rect lamp,
        Rect label,
        Component text,
        boolean lit,
        boolean hovered
    ) {
        PipBoyHousing.roundButton(graphics, lamp, lit, hovered, p().lampOn(), p().lampOff());
        var y = label.y() + (label.height() - 8) / 2 + 1;
        screen.fitted(graphics, text, label.x() + 1, y + 1, label.width() - 2, 0xA0000000);
        screen.fitted(graphics, text, label.x(), y, label.width() - 2, lit || hovered ? p().labelOn() : p().labelOff());
    }
}
