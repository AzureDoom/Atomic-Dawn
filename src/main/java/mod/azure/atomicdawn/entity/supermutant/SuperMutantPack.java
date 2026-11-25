package mod.azure.atomicdawn.entity.supermutant;

import net.minecraft.world.entity.LivingEntity;

import mod.azure.atomicdawn.AtomicDawn;

public final class SuperMutantPack {

    private SuperMutantPack() {}

    public static void call(SuperMutantEntity source, LivingEntity target) {
        var brain = source.brain();
        if (brain == null || !source.isAlive()) {
            return;
        }
        var now = source.level().getGameTime();
        if (!brain.tryCall(target, now)) {
            return;
        }
        var range = AtomicDawn.CONFIG.superMutants.PACK_RANGE;
        if (range <= 0) {
            return;
        }
        var origin = source.blockPosition();
        for (
            var other : source.level()
                .getEntitiesOfClass(
                    SuperMutantEntity.class,
                    source.getBoundingBox().inflate(range),
                    mutant -> mutant != source && mutant.isAlive() && mutant.distanceToSqr(source) <= range * range
                )
        ) {
            other.receivePackCall(target, origin, now);
        }
    }
}
