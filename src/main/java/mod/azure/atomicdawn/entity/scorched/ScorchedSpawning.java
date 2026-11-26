package mod.azure.atomicdawn.entity.scorched;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;

import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.helper.ADRegistryUtil;
import mod.azure.atomicdawn.helper.ADWorldgenUtil;
import mod.azure.atomicdawn.worldgen.structure.FissurePiece;

public final class ScorchedSpawning {

    public static final ResourceKey<Structure> FISSURE_SITE = ResourceKey.create(
        Registries.STRUCTURE,
        AtomicDawn.id("radiation_sites/fissure_site")
    );

    private static final double COUNT_MARGIN = 48.0;

    private static final double COUNT_HEIGHT = 64.0;

    private static final Map<ResourceKey<Level>, Map<Long, Long>> LAST_NATURAL = new HashMap<>();

    private ScorchedSpawning() {}

    public static boolean checkSpawnRules(
        EntityType<ScorchedEntity> type,
        ServerLevelAccessor level,
        EntitySpawnReason reason,
        BlockPos pos,
        RandomSource random
    ) {
        if (!Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random)) {
            return false;
        }
        var natural = reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION;
        if (!natural) {
            return true;
        }
        var server = level.getLevel();
        var center = fissureCenter(server, pos);
        return center.isPresent() && allowNatural(server, center.get(), pos);
    }

    private static boolean allowNatural(ServerLevel level, BlockPos center, BlockPos pos) {
        var config = AtomicDawn.CONFIG.server.entities.mutants.scorched.spawning;
        var dx = pos.getX() - center.getX();
        var dz = pos.getZ() - center.getZ();
        if (dx * dx + dz * dz > config.FISSURE_SPAWN_RADIUS * config.FISSURE_SPAWN_RADIUS) {
            return false;
        }
        var now = level.getGameTime();
        var last = LAST_NATURAL.getOrDefault(level.dimension(), Map.of()).get(center.asLong());
        if (last != null && now != last && now - last < config.FISSURE_RESPAWN_SECONDS * 20L) {
            return false;
        }
        var scan = config.FISSURE_SPAWN_RADIUS + COUNT_MARGIN;
        var present = level.getEntitiesOfClass(
            ScorchedEntity.class,
            new AABB(center).inflate(scan, COUNT_HEIGHT, scan),
            scorched -> scorched.isAlive() && !scorched.isScorchbeastSummon() && center.equals(scorched.home())
        ).size();
        return present < config.MAX_PER_FISSURE;
    }

    public static void recordNatural(ServerLevel level, BlockPos center) {
        LAST_NATURAL.computeIfAbsent(level.dimension(), key -> new HashMap<>())
            .put(center.asLong(), level.getGameTime());
    }

    public static Optional<BlockPos> fissureCenter(ServerLevel level, BlockPos pos) {
        var structure = ADRegistryUtil.registry(level.registryAccess(), Registries.STRUCTURE).getValue(FISSURE_SITE);
        if (structure == null) {
            return Optional.empty();
        }
        var start = ADWorldgenUtil.structureWithPieceAt(level, pos, FISSURE_SITE);
        if (!start.isValid()) {
            return Optional.empty();
        }
        for (var piece : start.getPieces()) {
            if (piece instanceof FissurePiece fissure) {
                return Optional.of(fissure.center());
            }
        }
        return Optional.of(start.getBoundingBox().getCenter());
    }

    public static ScorchedVariant rollVariant(RandomSource random) {
        var config = AtomicDawn.CONFIG.server.entities.mutants.scorched.spawning;
        if (config.SEASONAL_VARIANTS && random.nextDouble() < config.SEASONAL_CHANCE) {
            var today = LocalDate.now();
            var day = today.getDayOfMonth();
            if ((today.getMonth() == Month.DECEMBER && day >= 15) || (today.getMonth() == Month.JANUARY && day <= 6)) {
                return ScorchedVariant.HOLIDAY;
            }
            if ((today.getMonth() == Month.OCTOBER && day >= 15) || (today.getMonth() == Month.NOVEMBER && day <= 7)) {
                return ScorchedVariant.SPOOKY;
            }
        }
        return random.nextDouble() < config.OFFICER_CHANCE ? ScorchedVariant.OFFICER : ScorchedVariant.STANDARD;
    }
}
