package mod.azure.atomicdawn.entity.scorched;

import net.minecraft.world.entity.LivingEntity;

import mod.azure.atomicdawn.AtomicDawn;

public final class ScorchedHiveMind {

    private ScorchedHiveMind() {}

    public static void broadcast(ScorchedEntity source, LivingEntity target, boolean force) {
        var brain = source.brain();
        if (brain == null || !source.isAlive()) {
            return;
        }
        var config = AtomicDawn.CONFIG.scorched;
        var now = source.level().getGameTime();
        if (!brain.tryBroadcast(target, now, force ? 10 : config.HIVE_ALERT_INTERVAL)) {
            return;
        }
        var range = config.HIVE_RANGE;
        if (range <= 0) {
            return;
        }
        var origin = source.blockPosition();
        for (
            var other : source.level()
                .getEntitiesOfClass(
                    ScorchedEntity.class,
                    source.getBoundingBox().inflate(range),
                    scorched -> scorched != source && scorched.isAlive() && scorched.distanceToSqr(source) <= range
                        * range
                )
        ) {
            other.receiveHiveAlert(target, origin, now);
        }
    }
}
