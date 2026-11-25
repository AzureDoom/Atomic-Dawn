package mod.azure.atomicdawn.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.ExplosiveProjectileEntity;
import mod.azure.atomicdawn.entity.brahmin.BrahminEntity;
import mod.azure.atomicdawn.entity.ghoul.FeralGhoulEntity;
import mod.azure.atomicdawn.entity.misterhandy.MisterHandyEntity;
import mod.azure.atomicdawn.entity.scorchbeast.ScorchbeastCloudEntity;
import mod.azure.atomicdawn.entity.scorchbeast.ScorchbeastEntity;
import mod.azure.atomicdawn.entity.scorchbeast.SonicBlastEntity;
import mod.azure.atomicdawn.entity.scorched.ScorchedEntity;
import mod.azure.atomicdawn.entity.supermutant.SuperMutantEntity;
import mod.azure.atomicdawn.npc.NpcEntity;
import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;
import mod.azure.atomicdawn.radiation.FalloutCloudEntity;

@SuppressWarnings("NullableProblems")
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

    public static final DeferredHolder<EntityType<?>, EntityType<FeralGhoulEntity>> FERAL_GHOUL = REGISTER.register(
        "feral_ghoul",
        id -> EntityType.Builder.of(FeralGhoulEntity::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .eyeHeight(1.74F)
            .clientTrackingRange(8)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, id))
    );

    public static final DeferredHolder<EntityType<?>, EntityType<ScorchedEntity>> SCORCHED = REGISTER.register(
        "scorched",
        id -> EntityType.Builder.of(ScorchedEntity::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .eyeHeight(1.74F)
            .clientTrackingRange(8)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, id))
    );

    public static final DeferredHolder<EntityType<?>, EntityType<BrahminEntity>> BRAHMIN = REGISTER.register(
        "brahmin",
        id -> EntityType.Builder.of(BrahminEntity::new, MobCategory.CREATURE)
            .sized(1.2F, 1.75F)
            .eyeHeight(1.35F)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, id))
    );

    public static final DeferredHolder<EntityType<?>, EntityType<SuperMutantEntity>> SUPER_MUTANT = REGISTER.register(
        "super_mutant",
        id -> EntityType.Builder.of(SuperMutantEntity::new, MobCategory.MONSTER)
            .sized(1.2F, 3.0F)
            .eyeHeight(2.7F)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, id))
    );

    public static final DeferredHolder<EntityType<?>, EntityType<ScorchbeastEntity>> SCORCHBEAST = REGISTER.register(
        "scorchbeast",
        id -> EntityType.Builder.of(ScorchbeastEntity::new, MobCategory.MONSTER)
            .sized(3.5F, 3.0F)
            .eyeHeight(2.6F)
            .clientTrackingRange(16)
            .updateInterval(2)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, id))
    );

    public static final DeferredHolder<EntityType<?>, EntityType<MisterHandyEntity>> MISTER_HANDY = REGISTER.register(
        "mister_handy",
        id -> EntityType.Builder.of(MisterHandyEntity::new, MobCategory.MONSTER)
            .sized(1.1F, 2.2F)
            .eyeHeight(1.9F)
            .fireImmune()
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, id))
    );

    public static final DeferredHolder<EntityType<?>, EntityType<SonicBlastEntity>> SONIC_BLAST = REGISTER.register(
        "sonic_blast",
        id -> EntityType.Builder.<SonicBlastEntity>of(SonicBlastEntity::new, MobCategory.MISC)
            .sized(0.75F, 0.75F)
            .clientTrackingRange(12)
            .updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, id))
    );

    public static final DeferredHolder<EntityType<?>, EntityType<ScorchbeastCloudEntity>> SCORCHBEAST_CLOUD = REGISTER
        .register(
            "scorchbeast_cloud",
            id -> EntityType.Builder.<ScorchbeastCloudEntity>of(ScorchbeastCloudEntity::new, MobCategory.MISC)
                .sized(0.5F, 0.5F)
                .clientTrackingRange(10)
                .updateInterval(10)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, id))
        );

    private ADEntities() {}
}
