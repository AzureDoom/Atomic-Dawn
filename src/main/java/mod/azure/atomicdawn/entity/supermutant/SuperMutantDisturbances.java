package mod.azure.atomicdawn.entity.supermutant;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.AtomicDawn;

public final class SuperMutantDisturbances {

    private SuperMutantDisturbances() {}

    public static void gunfire(ServerLevel level, Entity shooter) {
        emit(level, shooter.position(), AtomicDawn.CONFIG.superMutants.HEARING_RANGE, shooter);
    }

    public static void emit(ServerLevel level, Vec3 pos, double radius, @Nullable Entity source) {
        if (radius <= 0 || source instanceof SuperMutantEntity) {
            return;
        }
        var box = new AABB(pos, pos).inflate(radius);
        for (
            var mutant : level.getEntitiesOfClass(
                SuperMutantEntity.class,
                box,
                candidate -> candidate.isAlive() && candidate.position().distanceToSqr(pos) <= radius * radius
            )
        ) {
            mutant.hearDisturbance(pos, source);
        }
    }
}
