package mod.azure.atomicdawn.weapon.definition;

import net.minecraft.resources.ResourceKey;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.registry.ADRegistries;

public final class WeaponDefinitions {

    public static final ResourceKey<WeaponDefinition> TEN_MM_PISTOL = key("10mm_pistol");

    public static final ResourceKey<WeaponDefinition> HUNTING_RIFLE = key("hunting_rifle");

    public static final ResourceKey<WeaponDefinition> COMBAT_SHOTGUN = key("combat_shotgun");

    public static final ResourceKey<WeaponDefinition> LASER_RIFLE = key("laser_rifle");

    public static final ResourceKey<WeaponDefinition> SUPER_SLEDGE = key("super_sledge");

    public static final ResourceKey<WeaponDefinition> FAT_MAN = key("fat_man");

    private WeaponDefinitions() {}

    private static ResourceKey<WeaponDefinition> key(String name) {
        return ResourceKey.create(ADRegistries.WEAPON_DEFINITION, AtomicDawn.id(name));
    }
}
