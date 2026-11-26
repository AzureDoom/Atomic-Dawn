package mod.azure.atomicdawn.client.pipboy.tab;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.Locale;

import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.client.pipboy.PipBoyClientState;
import mod.azure.atomicdawn.client.pipboy.PipBoyScreen;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.client.pipboy.Rect;
import mod.azure.atomicdawn.network.PipBoyActionPayload;
import mod.azure.atomicdawn.network.SpecialActionPayload;

final class GeneralPage {

    private GeneralPage() {}

    static void init(PipBoyScreen screen, Rect area) {
        request();
    }

    static void tick(PipBoyScreen screen) {
        if (screen.ticks() % 40 == 0) {
            request();
        }
    }

    private static void request() {
        ClientPacketDistributor.sendToServer(
            new PipBoyActionPayload(PipBoyActionPayload.Action.REQUEST_STATS, SpecialActionPayload.NONE)
        );
    }

    static void render(ADGuiGraphics graphics, PipBoyScreen screen, Rect area) {
        var stats = PipBoyClientState.stats;
        if (stats == null) {
            screen.text(
                graphics,
                Component.translatable("pipboy.atomicdawn.general.loading"),
                area.x() + 4,
                area.y() + 4,
                PipBoyTheme.dim()
            );
            return;
        }
        var vatsTotal = stats.vatsHits() + stats.vatsMisses();
        Object[][] rows = {
            { "play_time", playTime(stats.playTimeTicks()) },
            { "mob_kills", stats.mobKills() },
            { "player_kills", stats.playerKills() },
            { "deaths", stats.deaths() },
            { "distance", String.format(Locale.ROOT, "%.1f km", stats.distanceCm() / 100_000.0) },
            { "shots_fired", stats.shotsFired() },
            { "vats_hits", stats.vatsHits() },
            { "vats_misses", stats.vatsMisses() },
            { "vats_accuracy", vatsTotal == 0 ? "-" : (stats.vatsHits() * 100 / vatsTotal) + "%" },
            { "critical_hits", stats.criticalHits() },
            { "rads_absorbed", stats.radsAbsorbed() }
        };
        var columns = area.width() > 300 ? 2 : 1;
        var colW = area.width() / columns;
        var perColumn = (rows.length + columns - 1) / columns;
        for (var i = 0; i < rows.length; i++) {
            var x = area.x() + 4 + (i / perColumn) * colW;
            var y = area.y() + 4 + (i % perColumn) * 12;
            if (y + 9 > area.bottom()) {
                continue;
            }
            screen.text(
                graphics,
                Component.translatable("pipboy.atomicdawn.general." + rows[i][0]),
                x,
                y,
                PipBoyTheme.dim()
            );
            var value = Component.literal(String.valueOf(rows[i][1]));
            screen.text(graphics, value, x + colW - 12 - screen.font().width(value), y, PipBoyTheme.text());
        }
    }

    private static String playTime(long ticks) {
        var minutes = ticks / 1200;
        return String.format(Locale.ROOT, "%dh %02dm", minutes / 60, minutes % 60);
    }
}
