package mod.azure.atomicdawn.client.pipboy.tab;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.client.pipboy.PipBoyScreen;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.client.pipboy.Rect;
import mod.azure.atomicdawn.network.FastTravelPayload;
import mod.azure.atomicdawn.network.WaypointActionPayload;
import mod.azure.atomicdawn.pipboy.PipBoyNames;
import mod.azure.atomicdawn.pipboy.location.DiscoveredLocation;
import mod.azure.atomicdawn.pipboy.location.FastTravelService;
import mod.azure.atomicdawn.registry.ADAttachments;

final class LocationsPage {

    private static final String SCROLL = "data.locations";

    private static int selected = -1;

    private LocationsPage() {}

    private static List<DiscoveredLocation> sorted(LocalPlayer player) {
        var list = new ArrayList<>(player.getData(ADAttachments.LOCATIONS.get()).discovered());
        var dimension = player.level().dimension();
        var pos = player.blockPosition();
        list.sort(
            Comparator.<DiscoveredLocation>comparingInt(location -> location.dimension() == dimension ? 0 : 1)
                .thenComparingDouble(location -> location.pos().distSqr(pos))
        );
        return list;
    }

    static boolean scroll(int direction, Rect area) {
        var player = Minecraft.getInstance().player;
        return player != null && PageList.scroll(
            SCROLL,
            direction,
            sorted(player).size(),
            PageList.rows(PageList.list(area, false))
        );
    }

    static String distance(LocalPlayer player, DiscoveredLocation location) {
        if (location.dimension() != player.level().dimension()) {
            return "";
        }
        var meters = Math.sqrt(location.pos().distToCenterSqr(player.position()));
        return meters >= 1000 ? String.format(Locale.ROOT, "%.1fkm", meters / 1000) : Math.round(meters) + "m";
    }

    static void init(PipBoyScreen screen, Rect area) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        var list = PageList.list(area, false);
        var locations = sorted(player);
        var rows = PageList.rows(list);
        var first = PageList.first(SCROLL, locations.size(), rows);
        for (var r = 0; r < rows && first + r < locations.size(); r++) {
            var index = first + r;
            var location = locations.get(index);
            var label = PipBoyNames.location(location.structure());
            var distance = distance(player, location);
            if (!distance.isEmpty()) {
                label.append("  " + distance);
            }
            screen.row(
                label,
                list.x(),
                list.y() + r * PageList.ROW,
                list.width(),
                PageList.ROW - 1,
                index == selected,
                () -> {
                    selected = index;
                    screen.refresh();
                }
            );
        }
        if (selected < 0 || selected >= locations.size()) {
            return;
        }
        var location = locations.get(selected);
        var detail = PageList.detail(area);
        var y = detail.bottom() - 13;
        var x = detail.x();
        var sameDimension = location.dimension() == player.level().dimension();
        var show = Component.translatable("pipboy.atomicdawn.locations.show");
        var showW = screen.font().width(show) + 10;
        screen.button(show, x, y, showW, 13, sameDimension, () -> MapTab.focus(screen, location.pos()));
        var mark = Component.translatable("pipboy.atomicdawn.locations.mark");
        var markW = screen.font().width(mark) + 10;
        screen.button(
            mark,
            x + showW + 2,
            y,
            markW,
            13,
            sameDimension,
            () -> ClientPacketDistributor.sendToServer(
                WaypointActionPayload.add(location.pos(), PipBoyNames.location(location.structure()).getString(), 1)
            )
        );
        if (!FastTravelService.enabled()) {
            return;
        }
        var travel = Component.translatable("pipboy.atomicdawn.fast_travel.button");
        var reachable = sameDimension || AtomicDawn.CONFIG.common.pipboy.fastTravel.FAST_TRAVEL_CROSS_DIMENSION;
        screen.button(
            travel,
            x + showW + markW + 4,
            y,
            screen.font().width(travel) + 10,
            13,
            reachable,
            () -> {
                ClientPacketDistributor.sendToServer(new FastTravelPayload(location));
                screen.onClose();
            }
        );
    }

    static void render(ADGuiGraphics graphics, PipBoyScreen screen, Rect area) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        var list = PageList.list(area, false);
        var detail = PageList.detail(area);
        var locations = sorted(player);
        PageList.decorate(graphics, list, detail, SCROLL, locations.size());
        if (locations.isEmpty()) {
            screen.wrapped(
                graphics,
                Component.translatable("pipboy.atomicdawn.data.locations.empty"),
                list.x() + 2,
                list.y() + 3,
                list.width() - 4,
                list.bottom(),
                PipBoyTheme.dim()
            );
        }
        if (selected < 0 || selected >= locations.size()) {
            screen.wrapped(
                graphics,
                Component.translatable("pipboy.atomicdawn.locations.select"),
                detail.x(),
                detail.y() + 2,
                detail.width(),
                detail.bottom(),
                PipBoyTheme.dim()
            );
            return;
        }
        var location = locations.get(selected);
        var x = detail.x();
        var y = detail.y() + 1;
        screen.fitted(graphics, PipBoyNames.location(location.structure()), x, y, detail.width(), PipBoyTheme.text());
        y += 12;
        screen.text(
            graphics,
            Component.translatable(
                "pipboy.atomicdawn.locations.dimension",
                PipBoyNames.pretty(location.dimension().identifier().getPath())
            ),
            x,
            y,
            PipBoyTheme.dim()
        );
        y += 11;
        var pos = location.pos();
        screen.text(
            graphics,
            Component.translatable("pipboy.atomicdawn.locations.coords", pos.getX(), pos.getY(), pos.getZ()),
            x,
            y,
            PipBoyTheme.dim()
        );
        y += 11;
        screen.text(
            graphics,
            Component.translatable("pipboy.atomicdawn.locations.day", location.discoveredAt() / 24000L + 1),
            x,
            y,
            PipBoyTheme.dim()
        );
        y += 11;
        if (location.dimension() == player.level().dimension()) {
            var dx = pos.getX() + 0.5 - player.getX();
            var dz = pos.getZ() + 0.5 - player.getZ();
            var yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F;
            screen.text(
                graphics,
                Component.translatable(
                    "pipboy.atomicdawn.locations.distance",
                    distance(player, location),
                    compass(yaw)
                ),
                x,
                y,
                PipBoyTheme.text()
            );
        }
    }

    static String compass(float yaw) {
        String[] directions = { "S", "SW", "W", "NW", "N", "NE", "E", "SE" };
        return directions[Math.floorMod(Math.round(Mth.wrapDegrees(yaw) / 45F), 8)];
    }
}
