package mod.azure.atomicdawn.entity.scorchbeast;

import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.registry.ADEntities;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID)
public final class ScorchbeastEvents {

    private ScorchbeastEvents() {}

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ADEntities.SCORCHBEAST.get(), ScorchbeastEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ScorchbeastSpawning.tick(event.getServer().overworld());
    }

    @SubscribeEvent
    public static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
            ADEntities.SCORCHBEAST.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            ScorchbeastSpawning::checkSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
    }
}
