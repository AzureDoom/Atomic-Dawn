package mod.azure.atomicdawn.entity.scorched;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.worldgen.structure.FissurePiece;

public final class ScorchedSpawning {

    public static final ResourceKey<Structure> FISSURE_SITE = ResourceKey.create(
        Registries.STRUCTURE,
        AtomicDawn.id("radiation_sites/fissure_site")
    );

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
        return !natural || fissureCenter(level.getLevel(), pos).isPresent();
    }

    public static Optional<BlockPos> fissureCenter(ServerLevel level, BlockPos pos) {
        var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(FISSURE_SITE);
        if (structure == null) {
            return Optional.empty();
        }
        var start = level.structureManager().getStructureWithPieceAt(pos, holder -> holder.is(FISSURE_SITE));
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
        var config = AtomicDawn.CONFIG.scorched;
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
