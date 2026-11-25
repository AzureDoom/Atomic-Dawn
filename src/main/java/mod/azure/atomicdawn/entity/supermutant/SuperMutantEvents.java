package mod.azure.atomicdawn.entity.supermutant;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.registry.ADEntities;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID)
public final class SuperMutantEvents {

    private static final double COMBAT_NOISE_RANGE = 16.0;

    private SuperMutantEvents() {}

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ADEntities.SUPER_MUTANT.get(), SuperMutantEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
            ADEntities.SUPER_MUTANT.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            SuperMutantSpawning::checkSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level) {
            SuperMutantDisturbances.emit(
                level,
                event.getExplosion().center(),
                AtomicDawn.CONFIG.superMutants.HEARING_RANGE * 1.5,
                null
            );
        }
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        var victim = event.getEntity();
        if (victim instanceof SuperMutantEntity || !(victim.level() instanceof ServerLevel level)) {
            return;
        }
        SuperMutantDisturbances.emit(level, victim.position(), COMBAT_NOISE_RANGE, event.getSource().getEntity());
    }
}
