package mod.azure.atomicdawn.entity.scorched;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.registry.ADItems;
import mod.azure.atomicdawn.weapon.combat.MobGunfire;
import mod.azure.atomicdawn.weapon.definition.WeaponCategory;
import mod.azure.atomicdawn.weapon.definition.WeaponDefinition;
import mod.azure.atomicdawn.weapon.instance.Weapons;

public final class ScorchedWeapons {

    public enum Kind {
        FISTS,
        MELEE,
        GUN
    }

    private static final List<Item> VANILLA_MELEE = List.of(
        Items.WOODEN_SWORD,
        Items.STONE_SWORD,
        Items.IRON_SWORD,
        Items.STONE_AXE,
        Items.IRON_AXE,
        Items.GOLDEN_SWORD
    );

    private ScorchedWeapons() {}

    @Nullable
    public static WeaponDefinition definition(ScorchedEntity scorched) {
        return Weapons.definition(scorched.level(), scorched.getMainHandItem());
    }

    public static Kind kind(ScorchedEntity scorched) {
        var stack = scorched.getMainHandItem();
        if (stack.isEmpty()) {
            return Kind.FISTS;
        }
        var definition = definition(scorched);
        if (definition == null) {
            return Weapons.isWeapon(stack) ? Kind.FISTS : Kind.MELEE;
        }
        if (MobGunfire.canFire(definition)) {
            return scorched.variant().usesGuns() ? Kind.GUN : Kind.FISTS;
        }
        return definition.isMelee() || definition.category().melee() ? Kind.MELEE : Kind.FISTS;
    }

    public static boolean pistol(@Nullable WeaponDefinition definition) {
        return definition != null && definition.category() == WeaponCategory.PISTOL;
    }

    public static ScorchedStance stance(ScorchedEntity scorched, boolean aiming) {
        return switch (kind(scorched)) {
            case FISTS -> ScorchedStance.NONE;
            case MELEE -> ScorchedStance.MELEE;
            case GUN -> pistol(definition(scorched))
                ? aiming ? ScorchedStance.PISTOL_AIM : ScorchedStance.PISTOL
                : aiming ? ScorchedStance.RIFLE_AIM : ScorchedStance.RIFLE;
        };
    }

    public static ScorchedAction fireAction(@Nullable WeaponDefinition definition) {
        return pistol(definition) ? ScorchedAction.FIRE_PISTOL : ScorchedAction.FIRE_RIFLE;
    }

    public static ScorchedAction reloadAction(@Nullable WeaponDefinition definition) {
        return pistol(definition) ? ScorchedAction.RELOAD_PISTOL : ScorchedAction.RELOAD_RIFLE;
    }

    public static int fireInterval(WeaponDefinition definition, RandomSource random) {
        return Math.max(12, Math.round(definition.firing().intervalTicks() * 2F)) + random.nextInt(9);
    }

    public static float meleeDamage(ScorchedEntity scorched) {
        var definition = definition(scorched);
        return definition != null && kind(scorched) == Kind.MELEE ? definition.damage().base() : -1F;
    }

    public static ItemStack roll(Level level, ScorchedVariant variant, RandomSource random) {
        var config = AtomicDawn.CONFIG.scorched;
        if (variant.usesGuns() && random.nextDouble() < config.GUN_CHANCE) {
            var roll = random.nextInt(9);
            Item gun = roll < 4
                ? ADItems.TEN_MM_PISTOL.get()
                : roll < 7 ? ADItems.COMBAT_SHOTGUN.get() : ADItems.HUNTING_RIFLE.get();
            return Weapons.createNew(level, gun);
        }
        if (random.nextDouble() < config.MELEE_WEAPON_CHANCE) {
            return new ItemStack(VANILLA_MELEE.get(random.nextInt(VANILLA_MELEE.size())));
        }
        return ItemStack.EMPTY;
    }
}
