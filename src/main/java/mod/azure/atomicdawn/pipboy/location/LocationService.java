package mod.azure.atomicdawn.pipboy.location;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.ArrayList;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.pipboy.PipBoyNames;
import mod.azure.atomicdawn.pipboy.PipBoyNotifications;
import mod.azure.atomicdawn.quest.QuestService;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.special.SpecialLoadout;
import mod.azure.atomicdawn.special.SpecialService;

public final class LocationService {

    public static final TagKey<Structure> PIPBOY_LOCATIONS = TagKey.create(
        Registries.STRUCTURE,
        AtomicDawn.id("pipboy_locations")
    );

    private static final int SAME_LOCATION_DISTANCE = 96;

    private LocationService() {}

    public static LocationLog log(ServerPlayer player) {
        return player.getData(ADAttachments.LOCATIONS.get());
    }

    public static void tick(ServerPlayer player) {
        if (player.isSpectator()) {
            return;
        }
        var level = player.level();
        var pos = player.blockPosition();
        var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var manager = level.structureManager();
        for (var structure : manager.getAllStructuresAt(pos).keySet()) {
            var holder = structures.wrapAsHolder(structure);
            if (!holder.is(PIPBOY_LOCATIONS)) {
                continue;
            }
            var start = manager.getStructureAt(pos, structure);
            if (!start.isValid()) {
                continue;
            }
            var key = structures.getKey(structure);
            if (key == null) {
                continue;
            }
            var center = start.getBoundingBox().getCenter();
            discover(player, new DiscoveredLocation(key, level.dimension(), center, level.getGameTime()));
        }
    }

    private static void discover(ServerPlayer player, DiscoveredLocation location) {
        var log = log(player);
        for (var existing : log.discovered()) {
            if (
                existing.structure().equals(location.structure()) && existing.dimension() == location.dimension()
                    && existing.pos().closerThan(location.pos(), SAME_LOCATION_DISTANCE)
            ) {
                return;
            }
        }
        if (log.discovered().size() >= LocationLog.MAX_DISCOVERED) {
            return;
        }
        player.setData(ADAttachments.LOCATIONS.get(), log.withDiscovered(location));
        PipBoyNotifications.send(
            player,
            PipBoyNotifications.Kind.LOCATION_DISCOVERED,
            0,
            PipBoyNames.key("location", location.structure())
        );
        int xp = AtomicDawn.CONFIG.common.pipboy.locations.PIPBOY_DISCOVERY_XP;
        if (xp > 0) {
            SpecialService.awardXp(player, xp);
        }
        QuestService.evaluate(player);
    }

    public static void addWaypoint(ServerPlayer player, BlockPos pos, String name, int color) {
        var log = log(player);
        if (
            log.waypoints().size() >= Math.min(
                AtomicDawn.CONFIG.common.pipboy.locations.PIPBOY_MAX_WAYPOINTS,
                LocationLog.MAX_WAYPOINTS
            )
        ) {
            return;
        }
        var level = player.level();
        if (!level.getWorldBorder().isWithinBounds(pos)) {
            return;
        }
        var clamped = new BlockPos(pos.getX(), Math.clamp(pos.getY(), level.getMinY(), level.getMaxY()), pos.getZ());
        var list = new ArrayList<>(log.waypoints());
        list.add(new Waypoint(name, level.dimension(), clamped, color));
        player.setData(ADAttachments.LOCATIONS.get(), log.withWaypoints(list));
    }

    public static void removeWaypoint(ServerPlayer player, int index) {
        var log = log(player);
        if (index < 0 || index >= log.waypoints().size()) {
            return;
        }
        var list = new ArrayList<>(log.waypoints());
        list.remove(index);
        player.setData(ADAttachments.LOCATIONS.get(), log.withWaypoints(list));
    }

    public static void renameWaypoint(ServerPlayer player, int index, String name) {
        var log = log(player);
        if (index < 0 || index >= log.waypoints().size()) {
            return;
        }
        var list = new ArrayList<>(log.waypoints());
        list.set(index, list.get(index).withName(SpecialLoadout.sanitizeName(name)));
        player.setData(ADAttachments.LOCATIONS.get(), log.withWaypoints(list));
    }

    public static void recolorWaypoint(ServerPlayer player, int index, int color) {
        var log = log(player);
        if (index < 0 || index >= log.waypoints().size()) {
            return;
        }
        var list = new ArrayList<>(log.waypoints());
        list.set(index, list.get(index).withColor(color));
        player.setData(ADAttachments.LOCATIONS.get(), log.withWaypoints(list));
    }
}
