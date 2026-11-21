package mod.azure.atomicdawn.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import mod.azure.atomicdawn.AtomicDawn;

@SuppressWarnings("unused")
public final class ADDamageTypes {

    public static final ResourceKey<DamageType> BALLISTIC = key("ballistic");

    public static final ResourceKey<DamageType> ENERGY = key("energy");

    public static final ResourceKey<DamageType> BALLISTIC_DIRECT = key("ballistic_direct");

    public static final ResourceKey<DamageType> ENERGY_DIRECT = key("energy_direct");

    public static final TagKey<EntityType<?>> TAKES_DIRECT_SHOTS = TagKey.create(
        Registries.ENTITY_TYPE,
        AtomicDawn.id("takes_direct_shots")
    );

    public static final ResourceKey<DamageType> MELEE = key("melee");

    public static final ResourceKey<DamageType> ARMOR_PIERCING = key("armor_piercing");

    public static final ResourceKey<DamageType> LEGENDARY_EXPLOSION = key("legendary_explosion");

    public static final ResourceKey<DamageType> NUCLEAR = key("nuclear");

    public static final ResourceKey<DamageType> RADIATION = key("radiation");

    private ADDamageTypes() {}

    public static DamageSource radiation(Level level) {
        var holder = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(RADIATION);
        return new DamageSource(holder);
    }

    public static DamageSource source(Level level, ResourceKey<DamageType> type, Entity attacker) {
        var holder = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type);
        return new DamageSource(holder, attacker);
    }

    public static ResourceKey<DamageType> forTarget(ResourceKey<DamageType> type, Entity target) {
        if (!BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(target.getType()).is(TAKES_DIRECT_SHOTS)) {
            return type;
        }
        if (type.equals(BALLISTIC)) {
            return BALLISTIC_DIRECT;
        }
        if (type.equals(ENERGY)) {
            return ENERGY_DIRECT;
        }
        return type;
    }

    public static DamageSource ballistic(Level level, Entity shooter) {
        return source(level, BALLISTIC, shooter);
    }

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, AtomicDawn.id(name));
    }
}
