package mod.azure.atomicdawn.client.pipboy.tab;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Locale;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.pipboy.PipBoyClientState;
import mod.azure.atomicdawn.client.pipboy.PipBoyScreen;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.client.pipboy.Rect;
import mod.azure.atomicdawn.client.pipboy.radio.RadioPlayer;
import mod.azure.atomicdawn.config.ADConfigHelper;
import mod.azure.atomicdawn.radio.RadioStation;

public final class RadioTab implements PipBoyTab {

    private static final String SCROLL = "radio";

    private static final int ROW = 14;

    private static final int WAVE_BARS = 48;

    @Override
    public String id() {
        return "radio";
    }

    @Override
    public List<String> subtabs() {
        return List.of("stations");
    }

    private static int listWidth(Rect area) {
        return Math.max(120, area.width() * 2 / 5);
    }

    private static int rows(Rect area) {
        return Math.max(1, area.height() / ROW);
    }

    private static List<Holder.Reference<RadioStation>> stations() {
        var level = Minecraft.getInstance().level;
        return level == null ? List.of() : RadioStation.all(level.registryAccess());
    }

    @Override
    public void init(PipBoyScreen screen, Rect area, String subtab) {
        if (!AtomicDawn.CONFIG.common.pipboy.features.PIPBOY_ENABLE_RADIO) {
            return;
        }
        var stations = stations();
        if (stations.isEmpty()) {
            return;
        }
        var listW = listWidth(area);
        var first = Math.min(PipBoyClientState.scroll(SCROLL), Math.max(0, stations.size() - 1));
        var playing = RadioPlayer.station();
        for (var r = 0; r < rows(area) && first + r < stations.size(); r++) {
            var holder = stations.get(first + r);
            var id = holder.key().identifier();
            var tuned = id.equals(playing);
            var label = Component.literal(
                String.format(Locale.ROOT, "%s%5.1f  ", tuned ? "> " : "", holder.value().frequency())
            )
                .append(Component.translatable(RadioStation.translationKey(id)));
            screen.row(label, area.x(), area.y() + r * ROW, listW, ROW - 1, tuned, () -> {
                RadioPlayer.tune(id);
                screen.refresh();
            });
        }

        var detail = area.rightOf(listW, 8);
        var by = detail.bottom() - 14;
        var bx = detail.x();
        var stop = Component.translatable("pipboy.atomicdawn.radio.stop");
        var stopW = screen.font().width(stop) + 10;
        screen.button(stop, bx, by, stopW, 13, playing != null, () -> {
            RadioPlayer.stop();
            screen.refresh();
        });
        bx += stopW + 2;
        var next = Component.translatable("pipboy.atomicdawn.radio.next");
        var nextW = screen.font().width(next) + 10;
        screen.button(next, bx, by, nextW, 13, playing != null, () -> {
            RadioPlayer.next();
            screen.refresh();
        });
        bx += nextW + 6;
        var volume = AtomicDawn.CONFIG.client.pipboy.radio.PIPBOY_RADIO_VOLUME;
        screen.button(Component.literal("-"), bx, by, 14, 13, volume > 0.0, () -> setVolume(volume - 0.1, screen));
        screen.button(Component.literal("+"), bx + 46, by, 14, 13, volume < 1.0, () -> setVolume(volume + 0.1, screen));
    }

    private static void setVolume(double value, PipBoyScreen screen) {
        ADConfigHelper.setClientDouble(
            "pipboy.radio.PIPBOY_RADIO_VOLUME",
            Math.round(Math.clamp(value, 0.0, 1.0) * 10.0) / 10.0
        );
        screen.refresh();
    }

    @Override
    public boolean scroll(PipBoyScreen screen, String subtab, int direction) {
        var count = stations().size();
        var next = Math.clamp(PipBoyClientState.scroll(SCROLL) + direction, 0, Math.max(0, count - 1));
        if (next == PipBoyClientState.scroll(SCROLL)) {
            return false;
        }
        PipBoyClientState.setScroll(SCROLL, next);
        return true;
    }

    @Override
    public void render(
        GuiGraphicsExtractor graphics,
        PipBoyScreen screen,
        Rect area,
        String subtab,
        int mouseX,
        int mouseY
    ) {
        if (!AtomicDawn.CONFIG.common.pipboy.features.PIPBOY_ENABLE_RADIO) {
            centered(graphics, screen, area, Component.translatable("pipboy.atomicdawn.radio.disabled"));
            return;
        }
        if (stations().isEmpty()) {
            centered(graphics, screen, area, Component.translatable("pipboy.atomicdawn.radio.stations.empty"));
            return;
        }
        var detail = area.rightOf(listWidth(area), 8);
        graphics.fill(detail.x() - 4, detail.y(), detail.x() - 3, detail.bottom(), PipBoyTheme.faint());
        var x = detail.x();
        var y = detail.y() + 2;
        var station = RadioPlayer.station();
        if (station == null) {
            y = screen.wrapped(
                graphics,
                Component.translatable("pipboy.atomicdawn.radio.off"),
                x,
                y,
                detail.width() - 2,
                detail.bottom(),
                PipBoyTheme.dim()
            ) + 2;
        } else {
            var definition = Minecraft.getInstance().level == null
                ? null
                : RadioStation.get(Minecraft.getInstance().level.registryAccess(), station).orElse(null);
            var header = Component.translatable(RadioStation.translationKey(station));
            if (definition != null) {
                header = Component.literal(String.format(Locale.ROOT, "%.1f  ", definition.frequency())).append(header);
            }
            screen.fitted(graphics, header, x, y, detail.width(), PipBoyTheme.text());
            y += 12;
            screen.text(
                graphics,
                Component.translatable("pipboy.atomicdawn.radio.now_playing"),
                x,
                y,
                PipBoyTheme.dim()
            );
            y += 10;
            var track = RadioPlayer.track();
            if (track != null) {
                screen.fitted(
                    graphics,
                    Component.translatable(RadioStation.trackKey(track)),
                    x + 4,
                    y,
                    detail.width() - 4,
                    PipBoyTheme.text()
                );
            }
            y += 10;
            var seconds = RadioPlayer.playedTicks() / 20;
            var status = RadioPlayer.isPlaying()
                ? Component.literal(String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60))
                : Component.translatable("pipboy.atomicdawn.radio.tuning");
            screen.text(graphics, status, x + 4, y, PipBoyTheme.dim());
            y += 12;
        }
        renderWave(graphics, new Rect(x, y + 4, detail.width() - 2, Math.max(16, detail.bottom() - 20 - (y + 4))));

        var volume = Component.literal(
            Math.round(AtomicDawn.CONFIG.client.pipboy.radio.PIPBOY_RADIO_VOLUME * 100) + "%"
        );
        var vx = detail.x() + volumeOffset(screen);
        screen.text(
            graphics,
            volume,
            vx + 16 + (30 - screen.font().width(volume)) / 2,
            detail.bottom() - 11,
            PipBoyTheme.text()
        );
    }

    private static int volumeOffset(PipBoyScreen screen) {
        var stop = screen.font().width(Component.translatable("pipboy.atomicdawn.radio.stop")) + 10;
        var next = screen.font().width(Component.translatable("pipboy.atomicdawn.radio.next")) + 10;
        return stop + 2 + next + 6;
    }

    private static void renderWave(GuiGraphicsExtractor graphics, Rect box) {
        if (box.height() < 8 || box.width() < 16) {
            return;
        }
        graphics.fill(box.x(), box.y(), box.right(), box.bottom(), PipBoyTheme.panel());
        var mid = box.y() + box.height() / 2;
        graphics.fill(box.x(), mid, box.right(), mid + 1, PipBoyTheme.faint());
        if (!RadioPlayer.isPlaying()) {
            return;
        }
        var time = (System.currentTimeMillis() % 100_000L) / 1000F;
        var barW = Math.max(1, box.width() / WAVE_BARS);
        var amplitude = (float) (AtomicDawn.CONFIG.client.pipboy.radio.PIPBOY_RADIO_VOLUME * (box.height() / 2F - 2F));
        for (var i = 0; i < WAVE_BARS && box.x() + i * barW < box.right(); i++) {
            var t = i / (float) WAVE_BARS;
            var value = Mth.sin(t * 9F + time * 6F) * 0.55F + Mth.sin(t * 23F - time * 9F) * 0.3F
                + Mth.sin(t * 51F + time * 13F) * 0.15F;
            var h = Math.max(1, Math.round(Math.abs(value) * amplitude));
            var bx = box.x() + i * barW;
            graphics.fill(bx, mid - h, bx + Math.max(1, barW - 1), mid + h + 1, PipBoyTheme.text());
        }
    }

    private static void centered(GuiGraphicsExtractor graphics, PipBoyScreen screen, Rect area, Component message) {
        var width = Math.min(area.width() - 20, 260);
        screen.wrapped(
            graphics,
            message,
            area.x() + (area.width() - width) / 2,
            area.y() + area.height() / 2 - 10,
            width,
            area.bottom(),
            PipBoyTheme.dim()
        );
    }
}
