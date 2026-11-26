package mod.azure.atomicdawn.entity.misterhandy;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;

import mod.azure.atomicdawn.AtomicDawn;

public final class MisterHandySpawning {

    private static final double CROWD_RADIUS = 48.0;

    private MisterHandySpawning() {}

    public static boolean checkSpawnRules(
        EntityType<MisterHandyEntity> type,
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
        var config = AtomicDawn.CONFIG.server.entities.robots.misterHandy.spawning;
        if (random.nextDouble() >= config.SPAWN_CHANCE) {
            return false;
        }
        return level.getEntitiesOfClass(MisterHandyEntity.class, new AABB(pos).inflate(CROWD_RADIUS))
            .size() < config.MAX_NEARBY;
    }
}
