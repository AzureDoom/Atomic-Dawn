package mod.azure.atomicdawn.entity.scorchbeast;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.scorched.ScorchedSpawning;
import mod.azure.atomicdawn.registry.ADEntities;

public final class ScorchbeastSpawning {

    private static final int SCAN_RADIUS = 112;

    private static final int SCAN_STEP = 16;

    private static final double MIN_PLAYER_DISTANCE = 32.0;

    private static final double ARRIVAL_MIN = 48.0;

    private static final double ARRIVAL_MAX = 72.0;

    private static final int SPAWN_ATTEMPTS = 10;

    private static final Map<ResourceKey<Level>, Map<Long, long[]>> LAST_SPAWN = new HashMap<>();

    private ScorchbeastSpawning() {}

    public static boolean checkSpawnRules(
        EntityType<ScorchbeastEntity> type,
        ServerLevelAccessor level,
        EntitySpawnReason reason,
        BlockPos pos,
        RandomSource random
    ) {
        var natural = reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION;
        return !natural && Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random);
    }

    public static void tick(ServerLevel level) {
        var config = AtomicDawn.CONFIG.server.entities.bosses.scorchbeast.spawning;
        if (config.SPAWN_CHANCE <= 0.0 || level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        var interval = Math.max(5, config.SPAWN_INTERVAL_SECONDS) * 20L;
        if (level.getGameTime() % interval != 0) {
            return;
        }
        var sites = new LinkedHashSet<BlockPos>();
        for (var player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            var origin = player.blockPosition();
            for (var dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx += SCAN_STEP) {
                for (var dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz += SCAN_STEP) {
                    var x = origin.getX() + dx;
                    var z = origin.getZ() + dz;
                    if (!level.hasChunkAt(new BlockPos(x, origin.getY(), z))) {
                        continue;
                    }
                    var y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                    ScorchedSpawning.fissureCenter(level, new BlockPos(x, y, z)).ifPresent(sites::add);
                }
            }
        }
        var random = level.getRandom();
        for (var site : sites) {
            if (!eligible(level, site) || random.nextDouble() >= config.SPAWN_CHANCE) {
                continue;
            }
            if (spawnAt(level, site)) {
                LAST_SPAWN.computeIfAbsent(level.dimension(), key -> new HashMap<>())
                    .put(site.asLong(), new long[] { level.getGameTime(), level.getDefaultClockTime() });
            }
        }
    }

    private static boolean eligible(ServerLevel level, BlockPos site) {
        var config = AtomicDawn.CONFIG.server.entities.bosses.scorchbeast.spawning;
        var last = LAST_SPAWN.getOrDefault(level.dimension(), Map.of()).get(site.asLong());
        if (last != null) {
            var elapsed = Math.max(level.getGameTime() - last[0], level.getDefaultClockTime() - last[1]);
            if (elapsed < config.SITE_RESPAWN_MINUTES * 1200L) {
                return false;
            }
        }
        return !siteOccupied(level, site);
    }

    private static boolean spawnAt(ServerLevel level, BlockPos site) {
        var random = level.getRandom();
        for (var attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            var angle = random.nextDouble() * Math.PI * 2;
            var distance = ARRIVAL_MIN + random.nextDouble() * (ARRIVAL_MAX - ARRIVAL_MIN);
            var x = site.getX() + 0.5 + Math.cos(angle) * distance;
            var z = site.getZ() + 0.5 + Math.sin(angle) * distance;
            if (!level.hasChunkAt(BlockPos.containing(x, site.getY(), z)) || playerNear(level, x, z)) {
                continue;
            }
            var ground = level.getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (int) Math.floor(x),
                (int) Math.floor(z)
            );
            var y = ground + 28.0 + random.nextDouble() * 12.0;
            var beast = ADEntities.SCORCHBEAST.get().create(level, EntitySpawnReason.EVENT);
            if (beast == null) {
                return false;
            }
            beast.setPos(x, y, z);
            beast.setYRot(random.nextFloat() * 360F);
            if (!level.noCollision(beast)) {
                beast.discard();
                continue;
            }
            beast.finalizeSpawn(level, level.getCurrentDifficultyAt(site), EntitySpawnReason.EVENT, null);
            beast.spawnAirborne(site);
            level.addFreshEntity(beast);
            return true;
        }
        return false;
    }

    private static boolean playerNear(ServerLevel level, double x, double z) {
        for (var player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            var dx = player.getX() - x;
            var dz = player.getZ() - z;
            if (dx * dx + dz * dz < MIN_PLAYER_DISTANCE * MIN_PLAYER_DISTANCE) {
                return true;
            }
        }
        return false;
    }

    public static boolean siteOccupied(ServerLevel level, BlockPos center) {
        var scan = AtomicDawn.CONFIG.server.entities.bosses.scorchbeast.behavior.LEASH_RANGE + 16.0;
        var area = new AABB(center).inflate(scan);
        return !level.getEntitiesOfClass(
            ScorchbeastEntity.class,
            area,
            beast -> beast.isAlive() && (center.equals(beast.home()) || beast.home() == null)
        ).isEmpty();
    }
}
