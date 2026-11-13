package mod.azure.atomicdawn.client.pipboy.tab;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.pipboy.PipBoyScreen;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.client.pipboy.Rect;
import mod.azure.atomicdawn.client.pipboy.map.PipBoyMapRenderer;
import mod.azure.atomicdawn.config.ADConfigHelper;
import mod.azure.atomicdawn.network.WaypointActionPayload;
import mod.azure.atomicdawn.pipboy.PipBoyNames;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.registry.ADSounds;

public final class MapTab implements PipBoyTab {

    public static final String WORLD = "world";

    public static final String WAYPOINTS = "waypoints";

    private static final int CELL = 2;

    private static final int TOOLBAR = 15;

    private static final int INFO = 11;

    private static final int HOVER_RADIUS = 5;

    private static final String[] DIRECTIONS = { "S", "SW", "W", "NW", "N", "NE", "E", "SE" };

    private static final PipBoyMapRenderer RENDERER = new PipBoyMapRenderer();

    private static int nextColor;

    @Override
    public String id() {
        return "map";
    }

    @Override
    public List<String> subtabs() {
        return List.of(WORLD, WAYPOINTS);
    }

    public static void focus(PipBoyScreen screen, BlockPos pos) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        RENDERER.panX = pos.getX() - Mth.floor(player.getX());
        RENDERER.panZ = pos.getZ() - Mth.floor(player.getZ());
        screen.open("map", WORLD);
    }

    static void addWaypoint(BlockPos pos, String name) {
        ClientPacketDistributor.sendToServer(WaypointActionPayload.add(pos, name, nextColor));
        nextColor = (nextColor + 1) % 8;
    }

    private static Rect mapArea(Rect area) {
        var box = area.below(TOOLBAR);
        var height = Math.max(CELL, box.height() - INFO);
        var cols = Math.max(1, box.width() / CELL);
        var rows = Math.max(1, height / CELL);
        var w = cols * CELL;
        var h = rows * CELL;
        return new Rect(box.x() + (box.width() - w) / 2, box.y(), w, h);
    }

    private static float scale() {
        return CELL / (float) RENDERER.zoom();
    }

    @Override
    public void init(PipBoyScreen screen, Rect area, String subtab) {
        if (!AtomicDawn.CONFIG.pipboy.PIPBOY_ENABLE_MAP) {
            return;
        }
        if (WAYPOINTS.equals(subtab)) {
            WaypointsPage.init(screen, area);
            return;
        }
        var x = area.x();
        var y = area.y();
        screen.button(
            Component.literal("-"),
            x,
            y,
            14,
            12,
            RENDERER.zoomIndex < PipBoyMapRenderer.ZOOMS.length - 1,
            Component.translatable("pipboy.atomicdawn.map.zoom_out"),
            () -> zoom(1, screen)
        );
        screen.button(
            Component.literal("+"),
            x + 16,
            y,
            14,
            12,
            RENDERER.zoomIndex > 0,
            Component.translatable("pipboy.atomicdawn.map.zoom_in"),
            () -> zoom(-1, screen)
        );
        x += 36;
        var center = Component.translatable("pipboy.atomicdawn.map.center");
        var centerW = screen.font().width(center) + 10;
        screen.button(center, x, y, centerW, 12, RENDERER.panX != 0 || RENDERER.panZ != 0, () -> {
            RENDERER.center();
            screen.refresh();
        });
        x += centerW + 6;
        var step = Math.max(8, mapArea(area).width() / CELL / 4) * RENDERER.zoom();
        x = pan(screen, x, y, "◀", -step, 0);
        x = pan(screen, x, y, "▲", 0, -step);
        x = pan(screen, x, y, "▼", 0, step);
        x = pan(screen, x, y, "▶", step, 0);
        x += 6;
        var colorMode = AtomicDawn.CONFIG.clientPipboy.PIPBOY_MAP_COLOR;
        var mode = Component.translatable(
            colorMode ? "pipboy.atomicdawn.map.mode.color" : "pipboy.atomicdawn.map.mode.mono"
        );
        var modeW = screen.font().width(mode) + 10;
        screen.button(mode, x, y, modeW, 12, true, () -> {
            ADConfigHelper.setClientBoolean(
                "PIPBOY_MAP_COLOR",
                !AtomicDawn.CONFIG.clientPipboy.PIPBOY_MAP_COLOR
            );
            screen.refresh();
        });
        x += modeW + 6;
        var mark = Component.translatable("pipboy.atomicdawn.map.mark");
        screen.button(
            mark,
            x,
            y,
            screen.font().width(mark) + 10,
            12,
            true,
            Component.translatable("pipboy.atomicdawn.map.mark_hint"),
            () -> markAtCursorOrPlayer(screen)
        );
    }

    private static int pan(PipBoyScreen screen, int x, int y, String arrow, int dx, int dz) {
        screen.button(Component.literal(arrow), x, y, 14, 12, true, () -> {
            RENDERER.panX += dx;
            RENDERER.panZ += dz;
            screen.refresh();
        });
        return x + 16;
    }

    private static void zoom(int direction, PipBoyScreen screen) {
        RENDERER.zoomIndex = Mth.clamp(RENDERER.zoomIndex + direction, 0, PipBoyMapRenderer.ZOOMS.length - 1);
        screen.refresh();
    }

    @Nullable
    private static BlockPos cursorBlock(PipBoyScreen screen) {
        var map = mapArea(screen.contentArea());
        var mouseX = screen.lastMouseX();
        var mouseY = screen.lastMouseY();
        if (!map.contains(mouseX, mouseY) || RENDERER.originX() == Integer.MIN_VALUE) {
            return null;
        }
        var level = Minecraft.getInstance().level;
        var x = Mth.floor(RENDERER.originX() + (mouseX - map.x()) / scale());
        var z = Mth.floor(RENDERER.originZ() + (mouseY - map.y()) / scale());
        var y = level != null && level.hasChunk(x >> 4, z >> 4)
            ? level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z)
            : Mth.floor(Minecraft.getInstance().player.getY());
        return new BlockPos(x, y, z);
    }

    private static void markAtCursorOrPlayer(PipBoyScreen screen) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        var cursor = cursorBlock(screen);
        addWaypoint(cursor != null ? cursor : player.blockPosition(), "");
    }

    @Override
    public boolean keyPressed(PipBoyScreen screen, String subtab, int key) {
        if (!WORLD.equals(subtab) || key != InputConstants.KEY_M || !AtomicDawn.CONFIG.pipboy.PIPBOY_ENABLE_MAP) {
            return false;
        }
        markAtCursorOrPlayer(screen);
        screen.playSound(ADSounds.PIPBOY_SELECT.get());
        return true;
    }

    @Override
    public boolean scroll(PipBoyScreen screen, String subtab, int direction) {
        if (WAYPOINTS.equals(subtab)) {
            return WaypointsPage.scroll(direction, screen.contentArea());
        }
        var next = Mth.clamp(RENDERER.zoomIndex + direction, 0, PipBoyMapRenderer.ZOOMS.length - 1);
        if (next == RENDERER.zoomIndex) {
            return false;
        }
        RENDERER.zoomIndex = next;
        return true;
    }

    @Override
    public void tick(PipBoyScreen screen, String subtab) {
        var minecraft = Minecraft.getInstance();
        if (
            WORLD.equals(subtab) && minecraft.level != null && minecraft.player != null
                && AtomicDawn.CONFIG.pipboy.PIPBOY_ENABLE_MAP
        ) {
            RENDERER.update(minecraft.level, minecraft.player);
        }
    }

    @Nullable
    static Component biomeAt(ClientLevel level, int x, int z) {
        if (!level.hasChunk(x >> 4, z >> 4)) {
            return null;
        }
        var y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        return level.getBiome(new BlockPos(x, y, z))
            .unwrapKey()
            .<Component>map(key -> PipBoyNames.biome(key.identifier()))
            .orElse(null);
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
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null) {
            return;
        }
        if (!AtomicDawn.CONFIG.pipboy.PIPBOY_ENABLE_MAP) {
            var width = Math.min(area.width() - 20, 260);
            screen.wrapped(
                graphics,
                Component.translatable("pipboy.atomicdawn.map.disabled"),
                area.x() + (area.width() - width) / 2,
                area.y() + area.height() / 2 - 10,
                width,
                area.bottom(),
                PipBoyTheme.dim()
            );
            return;
        }
        if (WAYPOINTS.equals(subtab)) {
            WaypointsPage.render(graphics, screen, area);
            return;
        }
        var map = mapArea(area);
        RENDERER.resize(map.width() / CELL, map.height() / CELL);
        if (RENDERER.originX() == Integer.MIN_VALUE) {
            RENDERER.update(level, player);
        }
        graphics.fill(map.x() - 1, map.y() - 1, map.right() + 1, map.bottom() + 1, PipBoyTheme.border());
        RENDERER.render(graphics, map.x(), map.y(), CELL);

        var scale = scale();
        Component hovered = null;
        var hoveredDistance = HOVER_RADIUS * HOVER_RADIUS + 1;

        for (var location : player.getData(ADAttachments.LOCATIONS.get()).discovered()) {
            if (location.dimension() != level.dimension()) {
                continue;
            }
            var lx = toScreenX(map, location.pos().getX() + 0.5, scale);
            var lz = toScreenY(map, location.pos().getZ() + 0.5, scale);
            if (!map.contains(lx, lz)) {
                continue;
            }
            outline(graphics, lx, lz, PipBoyTheme.text());
            var d = (lx - mouseX) * (lx - mouseX) + (lz - mouseY) * (lz - mouseY);
            if (d < hoveredDistance) {
                hoveredDistance = d;
                hovered = PipBoyNames.location(location.structure());
            }
        }

        var waypoints = player.getData(ADAttachments.LOCATIONS.get()).waypoints();
        for (var i = 0; i < waypoints.size(); i++) {
            var waypoint = waypoints.get(i);
            if (waypoint.dimension() != level.dimension()) {
                continue;
            }
            var wx = toScreenX(map, waypoint.pos().getX() + 0.5, scale);
            var wz = toScreenY(map, waypoint.pos().getZ() + 0.5, scale);
            var inside = map.contains(wx, wz);
            wx = Mth.clamp(wx, map.x() + 2, map.right() - 3);
            wz = Mth.clamp(wz, map.y() + 2, map.bottom() - 3);
            graphics.fill(wx - 3, wz - 3, wx + 4, wz + 4, 0xFF000000);
            graphics.fill(
                wx - 2,
                wz - 2,
                wx + 3,
                wz + 3,
                inside ? waypoint.argb() : (waypoint.argb() & 0x00FFFFFF) | 0x90000000
            );
            var d = (wx - mouseX) * (wx - mouseX) + (wz - mouseY) * (wz - mouseY);
            if (d < hoveredDistance) {
                hoveredDistance = d;
                hovered = WaypointsPage.name(waypoint, i);
            }
        }

        var otherColor = PipBoyTheme.warn();
        for (var other : level.players()) {
            if (other == player || other.isSpectator()) {
                continue;
            }
            var ox = toScreenX(map, other.getX(), scale);
            var oz = toScreenY(map, other.getZ(), scale);
            if (map.contains(ox, oz)) {
                graphics.fill(
                    ox - 1,
                    oz - 1,
                    ox + 2,
                    oz + 2,
                    player.isAlliedTo(other) ? PipBoyTheme.text() : otherColor
                );
                var d = (ox - mouseX) * (ox - mouseX) + (oz - mouseY) * (oz - mouseY);
                if (d < hoveredDistance) {
                    hoveredDistance = d;
                    hovered = other.getDisplayName();
                }
            }
        }

        player.getLastDeathLocation().ifPresent(death -> {
            if (death.dimension() == level.dimension()) {
                var dx = toScreenX(map, death.pos().getX() + 0.5, scale);
                var dz = toScreenY(map, death.pos().getZ() + 0.5, scale);
                if (map.contains(dx, dz)) {
                    var mark = Component.literal("✕");
                    screen.text(graphics, mark, dx - screen.font().width(mark) / 2, dz - 4, PipBoyTheme.bad());
                }
            }
        });

        var px = toScreenX(map, player.getX(), scale);
        var pz = toScreenY(map, player.getZ(), scale);
        if (map.contains(px, pz)) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(px, pz);
            graphics.pose().rotate((player.getYRot() + 180F) * Mth.DEG_TO_RAD);
            var arrow = Component.literal("▲");
            graphics.text(screen.font(), arrow, -screen.font().width(arrow) / 2, -4, 0xFFFFFFFF, true);
            graphics.pose().popMatrix();
        }

        var north = Component.literal("N");
        screen.text(
            graphics,
            north,
            map.x() + map.width() / 2 - screen.font().width(north) / 2,
            map.y() + 2,
            PipBoyTheme.text()
        );

        var infoY = map.bottom() + 2;
        Component info;
        if (hovered != null && map.contains(mouseX, mouseY)) {
            info = hovered;
        } else if (map.contains(mouseX, mouseY)) {
            var bx = Mth.floor(RENDERER.originX() + (mouseX - map.x()) / scale);
            var bz = Mth.floor(RENDERER.originZ() + (mouseY - map.y()) / scale);
            var biome = biomeAt(level, bx, bz);
            info = Component.translatable("pipboy.atomicdawn.map.cursor", bx, bz);
            if (biome != null) {
                info = info.copy().append("  ").append(biome);
            }
        } else {
            var pos = player.blockPosition();
            info = Component.translatable("pipboy.atomicdawn.map.position", pos.getX(), pos.getY(), pos.getZ());
        }
        var currentBiome = level.getBiome(player.blockPosition())
            .unwrapKey()
            .<Component>map(key -> PipBoyNames.biome(key.identifier()))
            .orElse(Component.empty());
        var index = Math.floorMod(Math.round(Mth.wrapDegrees(player.getYRot()) / 45F), 8);
        var right = Component.translatable(
            "pipboy.atomicdawn.map.status",
            currentBiome,
            DIRECTIONS[index],
            RENDERER.zoom()
        );
        var rightW = screen.font().width(right);
        screen.fitted(graphics, info, map.x(), infoY, Math.max(40, map.width() - rightW - 8), PipBoyTheme.dim());
        screen.text(graphics, right, map.right() - rightW, infoY, PipBoyTheme.text());
    }

    private static void outline(GuiGraphicsExtractor graphics, int x, int y, int color) {
        graphics.fill(x - 3, y - 3, x + 4, y - 2, color);
        graphics.fill(x - 3, y + 3, x + 4, y + 4, color);
        graphics.fill(x - 3, y - 2, x - 2, y + 3, color);
        graphics.fill(x + 3, y - 2, x + 4, y + 3, color);
    }

    private static int toScreenX(Rect map, double worldX, float scale) {
        return map.x() + Mth.floor((worldX - RENDERER.originX()) * scale);
    }

    private static int toScreenY(Rect map, double worldZ, float scale) {
        return map.y() + Mth.floor((worldZ - RENDERER.originZ()) * scale);
    }
}
