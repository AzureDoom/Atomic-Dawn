package mod.azure.atomicdawn.client.pipboy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.registry.ADAttachments;

public final class PipBoyHousing {

    public record Layout(
        boolean fullscreen,
        Rect device,
        Rect screen,
        @Nullable Rect power,
        Rect[] selectorLabels,
        Rect[] selectorLamps,
        int[] selectorTabs,
        @Nullable Object parts
    ) {}

    public record State(
        float power,
        float tubes,
        int selectorLit,
        int hovered,
        long millis
    ) {}

    private PipBoyHousing() {}

    public static Layout layout(int width, int height, boolean fullscreen) {
        if (fullscreen || width < 260 || height < 160) {
            var screen = new Rect(4, 4, Math.max(0, width - 8), Math.max(0, height - 8));
            return new Layout(true, screen, screen, null, new Rect[0], new Rect[0], new int[0], null);
        }
        return switch (PipBoyModel.layout()) {
            case FALLOUT_76 -> PipBoyHousing76.layout(width, height);
            case FALLOUT_4 -> PipBoyHousing4.layout(width, height);
            case NEW_VEGAS, FALLOUT_3 -> PipBoyHousing3000.layout(width, height);
            case FALLOUT_1_2 -> PipBoyHousing2000.layout(width, height);
        };
    }

    public static void render(GuiGraphicsExtractor graphics, PipBoyScreen screen, Layout l, State s) {
        if (l.fullscreen() || l.parts() == null) {
            return;
        }
        switch (l.parts()) {
            case PipBoyHousing76.Parts _ -> PipBoyHousing76.render(graphics, screen, l, s);
            case PipBoyHousing4.Parts _ -> PipBoyHousing4.render(graphics, screen, l, s);
            case PipBoyHousing3000.Parts _ -> PipBoyHousing3000.render(graphics, screen, l, s);
            case PipBoyHousing2000.Parts _ -> PipBoyHousing2000.render(graphics, screen, l, s);
            default -> {}
        }
    }

    static boolean hovered(State s, int index) {
        return s.hovered() == index;
    }

    static boolean lit(State s, int index) {
        return index == s.selectorLit() && s.power() > 0.5F;
    }

    static int shadow() {
        return 0x50000000;
    }

    static void ribs(GuiGraphicsExtractor graphics, Rect r, int pitch, int light, int dark, boolean horizontal) {
        if (horizontal) {
            for (var y = r.y(); y < r.bottom(); y += pitch) {
                var end = Math.min(r.bottom(), y + pitch);
                graphics.fill(r.x(), y, r.right(), end, dark);
                graphics.fill(r.x(), y, r.right(), Math.min(end, y + Math.max(1, pitch / 2)), light);
            }
            return;
        }
        for (var x = r.x(); x < r.right(); x += pitch) {
            var end = Math.min(r.right(), x + pitch);
            graphics.fill(x, r.y(), end, r.bottom(), dark);
            graphics.fill(x, r.y(), Math.min(end, x + Math.max(1, pitch / 2)), r.bottom(), light);
        }
    }

    static void roundButton(GuiGraphicsExtractor graphics, Rect r, boolean lit, boolean hovered, int on, int off) {
        var cx = r.x() + r.width() / 2;
        var cy = r.y() + r.height() / 2;
        var radius = Math.max(3, Math.min(r.width(), r.height()) / 2);
        PipBoyDraw.disc(graphics, cx, cy, radius, 0xFF141412);
        PipBoyDraw.disc(graphics, cx, cy, radius - 1, 0xFF8A8A84);
        PipBoyDraw.disc(graphics, cx, cy, radius - 2, 0xFF3A3A36);
        if (lit) {
            PipBoyDraw.glow(graphics, r.inset(1), on, 0x70, 3);
        }
        var face = lit ? 0xFF000000 | on : 0xFF000000 | off;
        PipBoyDraw.disc(graphics, cx, cy, Math.max(1, radius - 3), face);
        if (hovered && !lit) {
            PipBoyDraw.disc(graphics, cx, cy, Math.max(1, radius - 3), PipBoyTheme.withAlpha(0xFFFFFF, 0x30));
        }
        PipBoyDraw.disc(
            graphics,
            cx - Math.max(1, radius / 3),
            cy - Math.max(1, radius / 3),
            Math.max(1, radius / 4),
            lit ? 0xFFFFF2D8 : PipBoyTheme.withAlpha(0xFFFFFF, 0x50)
        );
    }

    static void gauge(
        GuiGraphicsExtractor graphics,
        int cx,
        int cy,
        int radius,
        float fraction,
        PipBoyModel.Palette p
    ) {
        PipBoyDraw.disc(graphics, cx, cy, radius + 2, p.outline());
        PipBoyDraw.disc(graphics, cx, cy, radius + 1, 0xFFA9ADA6);
        PipBoyDraw.disc(graphics, cx, cy, radius, 0xFFE6E0C8);
        var start = Math.PI * 0.8;
        var sweep = Math.PI * 1.4;
        for (var i = 0; i <= 10; i++) {
            var a = start + sweep * i / 10.0;
            var len = i % 5 == 0 ? radius * 0.3F : radius * 0.18F;
            var x0 = cx + (float) Math.cos(a) * (radius - 2);
            var y0 = cy + (float) Math.sin(a) * (radius - 2);
            var x1 = cx + (float) Math.cos(a) * (radius - 2 - len);
            var y1 = cy + (float) Math.sin(a) * (radius - 2 - len);
            PipBoyDraw.line(graphics, x0, y0, x1, y1, 1, i >= 8 ? 0xFFC8281E : 0xFF2A2A26);
        }
        var needle = start + sweep * Math.clamp(fraction, 0F, 1F);
        PipBoyDraw.line(
            graphics,
            cx,
            cy,
            cx + (float) Math.cos(needle) * (radius - 3),
            cy + (float) Math.sin(needle) * (radius - 3),
            1,
            0xFF141412
        );
        PipBoyDraw.disc(graphics, cx, cy, Math.max(1, radius / 6), 0xFF2A2A26);
        graphics.fill(cx - radius / 2, cy - radius + 2, cx + radius / 2, cy - radius + 3, 0x40FFFFFF);
    }

    static void dialKnob(
        GuiGraphicsExtractor graphics,
        int cx,
        int cy,
        int radius,
        double angle,
        PipBoyModel.Palette p
    ) {
        PipBoyDraw.disc(graphics, cx, cy, radius + 4, p.outline());
        PipBoyDraw.disc(graphics, cx, cy, radius + 3, 0xFFA7ABA4);
        for (var i = 0; i < 8; i++) {
            var a = Math.PI * 0.75 + Math.PI * 1.5 * i / 7.0;
            PipBoyDraw.line(
                graphics,
                cx + (float) Math.cos(a) * (radius + 3),
                cy + (float) Math.sin(a) * (radius + 3),
                cx + (float) Math.cos(a) * (radius + 1),
                cy + (float) Math.sin(a) * (radius + 1),
                1,
                0xFF3A3C36
            );
        }
        PipBoyDraw.disc(graphics, cx + 1, cy + 2, radius - 1, 0x60000000);
        PipBoyDraw.disc(graphics, cx, cy, radius - 1, 0xFF1C1C1A);
        PipBoyDraw.disc(graphics, cx - 1, cy - 1, Math.max(1, radius - 3), 0xFF2E2E2B);
        PipBoyDraw.line(
            graphics,
            cx,
            cy,
            cx + (float) Math.cos(angle) * (radius - 2),
            cy + (float) Math.sin(angle) * (radius - 2),
            2,
            0xFF4E4E4A
        );
        graphics.fill(cx - radius / 2, cy - radius + 2, cx, cy - radius + 3, 0x30FFFFFF);
    }

    static float radiation() {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return 0F;
        }
        var rads = player.getData(ADAttachments.RADIATION.get()).rads();
        return Math.clamp(rads / (float) AtomicDawn.CONFIG.radiation.MAX_RADS, 0F, 1F);
    }
}
