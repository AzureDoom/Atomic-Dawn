package mod.azure.atomicdawn.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.ExplosiveProjectileEntity;
import mod.azure.atomicdawn.npc.NpcEntity;
import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;
import mod.azure.atomicdawn.radiation.FalloutCloudEntity;

public final class ADEntities {

    public static final DeferredRegister<EntityType<?>> REGISTER = DeferredRegister.create(
        Registries.ENTITY_TYPE,
        AtomicDawn.MOD_ID
    );

    public static final DeferredHolder<EntityType<?>, EntityType<ExplosiveProjectileEntity>> MINI_NUKE = REGISTER
        .register(
            "mini_nuke",
            id -> EntityType.Builder.of(ExplosiveProjectileEntity::new, MobCategory.MISC)
                .sized(0.5F, 0.5F)
                .clientTrackingRange(16)
                .updateInterval(1)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, id))
        );

    public static final DeferredHolder<EntityType<?>, EntityType<FalloutCloudEntity>> FALLOUT_CLOUD = REGISTER.register(
        "fallout_cloud",
        id -> EntityType.Builder.of(FalloutCloudEntity::new, MobCategory.MISC)
            .sized(1F, 1F)
            .noSummon()
            .fireImmune()
            .clientTrackingRange(16)
            .updateInterval(20)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, id))
    );

    public static final DeferredHolder<EntityType<?>, EntityType<PowerArmorEntity>> POWER_ARMOR = REGISTER.register(
        "power_armor",
        id -> EntityType.Builder.of(PowerArmorEntity::new, MobCategory.MISC)
            .sized(1.0F, 2.3F)
            .eyeHeight(2.05F)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, id))
    );

    public static final DeferredHolder<EntityType<?>, EntityType<NpcEntity>> NPC = REGISTER.register(
        "npc",
        id -> EntityType.Builder.of(NpcEntity::new, MobCategory.MISC)
            .sized(0.6F, 1.8F)
            .eyeHeight(1.62F)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, id))
    );

    private ADEntities() {}
}
