package mod.azure.atomicdawn.client.pipboy.tab;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.client.pipboy.PipBoyClientState;
import mod.azure.atomicdawn.client.pipboy.PipBoyScreen;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.client.pipboy.Rect;
import mod.azure.atomicdawn.network.WaypointActionPayload;
import mod.azure.atomicdawn.pipboy.PipBoyNames;
import mod.azure.atomicdawn.pipboy.location.Waypoint;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.special.SpecialLoadout;

final class WaypointsPage {

    private static final String SCROLL = "map.waypoints";

    private static int selected = -1;

    @Nullable
    private static EditBox renameBox;

    private WaypointsPage() {}

    static Component name(Waypoint waypoint, int index) {
        return waypoint.name().isEmpty()
            ? Component.translatable("pipboy.atomicdawn.waypoint.default", index + 1)
            : Component.literal(waypoint.name());
    }

    private static List<Waypoint> waypoints(LocalPlayer player) {
        return player.getData(ADAttachments.LOCATIONS.get()).waypoints();
    }

    private static String distance(LocalPlayer player, Waypoint waypoint) {
        if (waypoint.dimension() != player.level().dimension()) {
            return PipBoyNames.pretty(waypoint.dimension().identifier().getPath());
        }
        var meters = Math.sqrt(waypoint.pos().distToCenterSqr(player.position()));
        return meters >= 1000 ? String.format(Locale.ROOT, "%.1fkm", meters / 1000) : Math.round(meters) + "m";
    }

    static boolean scroll(int direction, Rect area) {
        var player = Minecraft.getInstance().player;
        return player != null && PageList.scroll(
            SCROLL,
            direction,
            waypoints(player).size(),
            PageList.rows(PageList.list(area, true))
        );
    }

    static void init(PipBoyScreen screen, Rect area) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        var markHere = Component.translatable("pipboy.atomicdawn.waypoint.mark_here");
        screen.button(
            markHere,
            area.x(),
            area.y(),
            screen.font().width(markHere) + 10,
            12,
            true,
            () -> MapTab.addWaypoint(player.blockPosition(), "")
        );

        var list = PageList.list(area, true);
        var waypoints = waypoints(player);
        var rows = PageList.rows(list);
        var first = PageList.first(SCROLL, waypoints.size(), rows);
        for (var r = 0; r < rows && first + r < waypoints.size(); r++) {
            var index = first + r;
            var waypoint = waypoints.get(index);
            var label = name(waypoint, index).copy().append("  " + distance(player, waypoint));
            screen.row(
                label,
                list.x() + 12,
                list.y() + r * PageList.ROW,
                list.width() - 12,
                PageList.ROW - 1,
                index == selected,
                () -> {
                    selected = index;
                    screen.refresh();
                }
            );
        }
        renameBox = null;
        if (selected < 0 || selected >= waypoints.size()) {
            return;
        }
        var waypoint = waypoints.get(selected);
        var index = selected;
        var detail = PageList.detail(area.below(PageList.TOOLBAR));
        var boxY = detail.y() + 60;
        var box = new EditBox(
            screen.font(),
            detail.x(),
            boxY,
            Math.max(60, detail.width() - 56),
            12,
            Component.translatable("pipboy.atomicdawn.waypoint.rename")
        );
        box.setMaxLength(SpecialLoadout.MAX_NAME_LENGTH);
        box.setValue(waypoint.name());
        renameBox = screen.widget(box);
        screen.button(
            Component.translatable("pipboy.atomicdawn.waypoint.rename"),
            detail.right() - 52,
            boxY - 1,
            52,
            14,
            true,
            () -> {
                if (renameBox != null) {
                    ClientPacketDistributor.sendToServer(
                        WaypointActionPayload.edit(WaypointActionPayload.Action.RENAME, index, renameBox.getValue(), 0)
                    );
                }
            }
        );

        var y = detail.bottom() - 13;
        var x = detail.x();
        var sameDimension = waypoint.dimension() == player.level().dimension();
        var show = Component.translatable("pipboy.atomicdawn.locations.show");
        var showW = screen.font().width(show) + 10;
        screen.button(show, x, y, showW, 13, sameDimension, () -> MapTab.focus(screen, waypoint.pos()));
        x += showW + 2;
        var color = Component.translatable("pipboy.atomicdawn.waypoint.color");
        var colorW = screen.font().width(color) + 10;
        screen.button(
            color,
            x,
            y,
            colorW,
            13,
            true,
            () -> ClientPacketDistributor.sendToServer(
                WaypointActionPayload.edit(WaypointActionPayload.Action.RECOLOR, index, "", waypoint.color() + 1)
            )
        );
        x += colorW + 2;
        var delete = Component.translatable("pipboy.atomicdawn.waypoint.delete");
        screen.button(delete, x, y, screen.font().width(delete) + 10, 13, true, () -> {
            ClientPacketDistributor.sendToServer(
                WaypointActionPayload.edit(WaypointActionPayload.Action.REMOVE, index, "", 0)
            );
            selected = -1;
            screen.refresh();
        });
    }

    static void render(ADGuiGraphics graphics, PipBoyScreen screen, Rect area) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        var list = PageList.list(area, true);
        var detail = PageList.detail(area.below(PageList.TOOLBAR));
        var waypoints = waypoints(player);
        PageList.decorate(graphics, list, detail, SCROLL, waypoints.size());
        var first = PipBoyClientState.scroll(SCROLL);
        for (var r = 0; r < PageList.rows(list) && first + r < waypoints.size(); r++) {
            var y = list.y() + r * PageList.ROW + 3;
            graphics.fill(list.x() + 2, y, list.x() + 9, y + 7, waypoints.get(first + r).argb());
        }
        if (waypoints.isEmpty()) {
            screen.wrapped(
                graphics,
                Component.translatable("pipboy.atomicdawn.waypoint.none"),
                list.x() + 2,
                list.y() + 3,
                list.width() - 4,
                list.bottom(),
                PipBoyTheme.dim()
            );
        }
        if (selected < 0 || selected >= waypoints.size()) {
            screen.wrapped(
                graphics,
                Component.translatable("pipboy.atomicdawn.waypoint.select"),
                detail.x(),
                detail.y() + 2,
                detail.width(),
                detail.bottom(),
                PipBoyTheme.dim()
            );
            return;
        }
        var waypoint = waypoints.get(selected);
        var x = detail.x();
        var y = detail.y() + 1;
        graphics.fill(x, y + 1, x + 7, y + 8, waypoint.argb());
        screen.fitted(graphics, name(waypoint, selected), x + 11, y, detail.width() - 11, PipBoyTheme.text());
        y += 12;
        var pos = waypoint.pos();
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
            Component.translatable(
                "pipboy.atomicdawn.locations.dimension",
                PipBoyNames.pretty(waypoint.dimension().identifier().getPath())
            ),
            x,
            y,
            PipBoyTheme.dim()
        );
        y += 11;
        if (waypoint.dimension() == player.level().dimension()) {
            var dx = pos.getX() + 0.5 - player.getX();
            var dz = pos.getZ() + 0.5 - player.getZ();
            var yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F;
            screen.text(
                graphics,
                Component.translatable(
                    "pipboy.atomicdawn.locations.distance",
                    distance(player, waypoint),
                    LocationsPage.compass(yaw)
                ),
                x,
                y,
                PipBoyTheme.text()
            );
        }
    }
}
