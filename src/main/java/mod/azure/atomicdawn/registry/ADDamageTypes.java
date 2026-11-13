package mod.azure.atomicdawn.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import mod.azure.atomicdawn.AtomicDawn;

@SuppressWarnings("unused")
public final class ADDamageTypes {

    public static final ResourceKey<DamageType> BALLISTIC = key("ballistic");

    public static final ResourceKey<DamageType> ENERGY = key("energy");

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

    public static DamageSource ballistic(Level level, Entity shooter) {
        return source(level, BALLISTIC, shooter);
    }

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, AtomicDawn.id(name));
    }
}
