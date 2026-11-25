package mod.azure.atomicdawn.entity.supermutant;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.registry.ADItems;
import mod.azure.atomicdawn.weapon.combat.MobGunfire;
import mod.azure.atomicdawn.weapon.definition.WeaponCategory;
import mod.azure.atomicdawn.weapon.definition.WeaponDefinition;
import mod.azure.atomicdawn.weapon.instance.Weapons;

public final class SuperMutantWeapons {

    public enum Kind {
        FISTS,
        MELEE,
        GUN
    }

    private SuperMutantWeapons() {}

    @Nullable
    public static WeaponDefinition definition(SuperMutantEntity mutant) {
        return Weapons.definition(mutant.level(), mutant.getMainHandItem());
    }

    public static Kind kind(SuperMutantEntity mutant) {
        var stack = mutant.getMainHandItem();
        if (stack.isEmpty()) {
            return Kind.FISTS;
        }
        var definition = definition(mutant);
        if (definition == null) {
            return Weapons.isWeapon(stack) ? Kind.FISTS : Kind.MELEE;
        }
        if (MobGunfire.canFire(definition)) {
            return Kind.GUN;
        }
        return definition.isMelee() || definition.category().melee() ? Kind.MELEE : Kind.FISTS;
    }

    public static boolean pistol(@Nullable WeaponDefinition definition) {
        return definition != null && definition.category() == WeaponCategory.PISTOL;
    }

    public static SuperMutantStance stance(SuperMutantEntity mutant) {
        return switch (kind(mutant)) {
            case FISTS -> SuperMutantStance.NONE;
            case MELEE -> SuperMutantStance.ONE_HANDED;
            case GUN -> pistol(definition(mutant)) ? SuperMutantStance.ONE_HANDED : SuperMutantStance.RIFLE;
        };
    }

    public static SuperMutantAction fireAction(@Nullable WeaponDefinition definition) {
        return pistol(definition) ? SuperMutantAction.FIRE_PISTOL : SuperMutantAction.FIRE_RIFLE;
    }

    public static SuperMutantAction reloadAction(@Nullable WeaponDefinition definition) {
        return pistol(definition) ? SuperMutantAction.RELOAD_PISTOL : SuperMutantAction.RELOAD_RIFLE;
    }

    public static int fireInterval(WeaponDefinition definition, RandomSource random) {
        return Math.max(14, Math.round(definition.firing().intervalTicks() * 2.2F)) + random.nextInt(10);
    }

    public static float meleeDamage(SuperMutantEntity mutant) {
        var definition = definition(mutant);
        if (definition != null && kind(mutant) == Kind.MELEE) {
            return definition.damage().base() * (float) AtomicDawn.CONFIG.superMutants.MELEE_WEAPON_DAMAGE_MULTIPLIER;
        }
        return -1F;
    }

    public static ItemStack roll(Level level, RandomSource random) {
        var config = AtomicDawn.CONFIG.superMutants;
        if (random.nextDouble() < config.RANGED_CHANCE) {
            var roll = random.nextInt(20);
            Item gun;
            if (roll < 7) {
                gun = ADItems.HUNTING_RIFLE.get();
            } else if (roll < 13) {
                gun = ADItems.COMBAT_SHOTGUN.get();
            } else if (roll < 17) {
                gun = ADItems.TEN_MM_PISTOL.get();
            } else {
                gun = ADItems.LASER_RIFLE.get();
            }
            return Weapons.createNew(level, gun);
        }
        if (random.nextDouble() < config.MELEE_WEAPON_CHANCE) {
            var roll = random.nextInt(20);
            if (roll < 3) {
                return Weapons.createNew(level, ADItems.SUPER_SLEDGE.get());
            }
            if (roll < 9) {
                return new ItemStack(Items.IRON_AXE);
            }
            if (roll < 14) {
                return new ItemStack(Items.STONE_AXE);
            }
            if (roll < 17) {
                return new ItemStack(Items.IRON_SWORD);
            }
            return new ItemStack(Items.WOODEN_SWORD);
        }
        return ItemStack.EMPTY;
    }
}
