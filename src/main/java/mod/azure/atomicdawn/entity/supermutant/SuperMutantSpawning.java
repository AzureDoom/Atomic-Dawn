package mod.azure.atomicdawn.entity.supermutant;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;

import mod.azure.atomicdawn.AtomicDawn;

public final class SuperMutantSpawning {

    public static final TagKey<Structure> CAMPS = TagKey.create(
        Registries.STRUCTURE,
        AtomicDawn.id("super_mutant_camps")
    );

    private SuperMutantSpawning() {}

    public static boolean checkSpawnRules(
        EntityType<SuperMutantEntity> type,
        ServerLevelAccessor level,
        EntitySpawnReason reason,
        BlockPos pos,
        RandomSource random
    ) {
        if (!Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random)) {
            return false;
        }
        var natural = reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION;
        return !natural || campCenter(level.getLevel(), pos).isPresent();
    }

    public static Optional<BlockPos> campCenter(ServerLevel level, BlockPos pos) {
        var start = level.structureManager().getStructureWithPieceAt(pos, holder -> holder.is(CAMPS));
        if (!start.isValid()) {
            return Optional.empty();
        }
        return Optional.of(start.getBoundingBox().getCenter());
    }

    public static SuperMutantVariant variantFor(LevelReader level, BlockPos pos) {
        var biome = level.getBiome(pos);
        for (var variant : SuperMutantVariant.values()) {
            if (biome.is(variant.biomes())) {
                return variant;
            }
        }
        return SuperMutantVariant.APPALACHIAN;
    }
}
