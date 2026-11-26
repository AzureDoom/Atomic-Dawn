package mod.azure.atomicdawn.radiation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.registry.ADAttachments;

public final class BlastZoneService {

    private BlastZoneService() {}

    public static boolean enabled() {
        return AtomicDawn.CONFIG.common.survival.radiation.blastZones.BLAST_ZONES_ENABLED;
    }

    public static BlastZones zones(ServerLevel level) {
        return level.hasData(ADAttachments.BLAST_ZONES.get())
            ? level.getData(ADAttachments.BLAST_ZONES.get())
            : BlastZones.EMPTY;
    }

    public static void add(ServerLevel level, Vec3 at, float blastRadius) {
        var config = AtomicDawn.CONFIG.common.survival.radiation.blastZones;
        var radius = blastRadius * (float) config.BLAST_ZONE_RADIUS_MULTIPLIER;
        var duration = config.BLAST_ZONE_DURATION_TICKS;
        if (!enabled() || radius <= 0F || duration <= 0) {
            return;
        }
        var now = level.getGameTime();
        var zone = new BlastZone(BlockPos.containing(at), radius, now, now + duration);
        level.setData(ADAttachments.BLAST_ZONES.get(), zones(level).pruned(now).with(zone));
    }

    public static void prune(ServerLevel level) {
        if (!level.hasData(ADAttachments.BLAST_ZONES.get())) {
            return;
        }
        var current = zones(level);
        var pruned = current.pruned(level.getGameTime());
        if (pruned != current) {
            level.setData(ADAttachments.BLAST_ZONES.get(), pruned);
        }
    }

    public static boolean inZone(ServerLevel level, BlockPos pos) {
        if (!enabled()) {
            return false;
        }
        var now = level.getGameTime();
        for (var zone : zones(level).zones()) {
            if (!zone.expired(now) && zone.proximity(pos) > 0F) {
                return true;
            }
        }
        return false;
    }

    public static float radiation(ServerLevel level, BlockPos pos) {
        if (!enabled()) {
            return 0F;
        }
        var now = level.getGameTime();
        var best = 0F;
        for (var zone : zones(level).zones()) {
            if (zone.expired(now)) {
                continue;
            }
            best = Math.max(best, zone.proximity(pos) * zone.strength(now));
        }
        return best * (float) AtomicDawn.CONFIG.common.survival.radiation.blastZones.BLAST_ZONE_RADS_PER_SECOND;
    }
}
