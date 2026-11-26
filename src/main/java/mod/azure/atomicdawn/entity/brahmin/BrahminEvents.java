package mod.azure.atomicdawn.entity.brahmin;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.radiation.RadiationService;
import mod.azure.atomicdawn.registry.ADDataComponents;
import mod.azure.atomicdawn.registry.ADEntities;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID)
public final class BrahminEvents {

    private BrahminEvents() {}

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ADEntities.BRAHMIN.get(), BrahminEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
            ADEntities.BRAHMIN.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            BrahminEvents::checkSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
    }

    private static boolean checkSpawnRules(
        EntityType<BrahminEntity> type,
        ServerLevelAccessor level,
        EntitySpawnReason reason,
        BlockPos pos,
        RandomSource random
    ) {
        return Mob.checkMobSpawnRules(type, level, reason, pos, random) && level.getRawBrightness(pos, 0) > 8;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof BrahminEntity)) {
            return;
        }
        for (var drop : event.getDrops()) {
            var stack = drop.getItem();
            if (stack.is(Items.BEEF) && !stack.has(ADDataComponents.IRRADIATED.get())) {
                var irradiated = stack.copy();
                irradiated.set(ADDataComponents.IRRADIATED.get(), Unit.INSTANCE);
                drop.setItem(irradiated);
            }
        }
    }

    @SubscribeEvent
    public static void onFinishEating(LivingEntityUseItemEvent.Finish event) {
        var entity = event.getEntity();
        if (entity.level().isClientSide() || !event.getItem().has(ADDataComponents.IRRADIATED.get())) {
            return;
        }
        RadiationService.expose(
            entity,
            (float) AtomicDawn.CONFIG.common.survival.radiation.consumables.RAW_BRAHMIN_BEEF_RADS
        );
    }
}
