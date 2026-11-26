package mod.azure.atomicdawn.entity.scorchbeast;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.scorched.ScorchedEntity;
import mod.azure.atomicdawn.helper.ADEntityUtil;
import mod.azure.atomicdawn.registry.ADEntities;

public final class ScorchbeastSummons {

    public static final String TAG = "atomicdawn.scorchbeast_summon";

    private static final double COUNT_RANGE = 48.0;

    private static final int ATTEMPTS = 8;

    private ScorchbeastSummons() {}

    public static int active(ServerLevel level, ScorchbeastEntity beast) {
        return level.getEntitiesOfClass(
            ScorchedEntity.class,
            beast.getBoundingBox().inflate(COUNT_RANGE),
            scorched -> scorched.isAlive() && scorched.isScorchbeastSummon()
        ).size();
    }

    public static void summon(ServerLevel level, ScorchbeastEntity beast) {
        var config = AtomicDawn.CONFIG.server.entities.bosses.scorchbeast.shockwave;
        var allowed = Math.min(config.SUMMONS_PER_SHOCKWAVE, config.MAX_ACTIVE_SUMMONS - active(level, beast));
        var target = beast.getTarget();
        var random = beast.getRandom();
        for (var i = 0; i < allowed; i++) {
            for (var attempt = 0; attempt < ATTEMPTS; attempt++) {
                var angle = random.nextDouble() * Math.PI * 2;
                var distance = beast.getBbWidth() + 2.0 + random.nextDouble() * 6.0;
                var x = beast.getX() + Math.cos(angle) * distance;
                var z = beast.getZ() + Math.sin(angle) * distance;
                var y = beast.groundY(x, z);
                if (Math.abs(y - beast.getY()) > 4.0) {
                    continue;
                }
                var scorched = ADEntityUtil.create(ADEntities.SCORCHED.get(), level, EntitySpawnReason.MOB_SUMMONED);
                if (scorched == null) {
                    return;
                }
                scorched.setPos(x, y, z);
                if (!level.noCollision(scorched) || level.containsAnyLiquid(scorched.getBoundingBox())) {
                    scorched.discard();
                    continue;
                }
                scorched.finalizeSpawn(
                    level,
                    level.getCurrentDifficultyAt(scorched.blockPosition()),
                    EntitySpawnReason.MOB_SUMMONED,
                    null
                );
                scorched.setScorchbeastSummon(true);
                level.addFreshEntity(scorched);
                var brain = scorched.brain();
                if (target != null && brain != null) {
                    brain.acceptShared(target, beast.blockPosition(), level.getGameTime());
                }
                level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 1.0, z, 12, 0.4, 0.8, 0.4, 0.02);
                break;
            }
        }
    }
}
