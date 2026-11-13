package mod.azure.atomicdawn.progression;

import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;

import mod.azure.atomicdawn.armor.upgrade.ArmorLegendary;
import mod.azure.atomicdawn.armor.upgrade.ArmorUpgrades;
import mod.azure.atomicdawn.registry.ADDataComponents;
import mod.azure.atomicdawn.registry.ADItems;
import mod.azure.atomicdawn.weapon.instance.Weapons;
import mod.azure.atomicdawn.weapon.legendary.LegendaryEffect;
import mod.azure.atomicdawn.weapon.legendary.WeaponLegendary;
import mod.azure.atomicdawn.weapon.modification.ModificationResolver;

public final class LegendaryRolls {

    private LegendaryRolls() {}

    public static ItemStack randomLegendaryWeapon(Level level, RandomSource random) {
        var weapons = ADItems.weapons().toList();
        if (weapons.isEmpty()) {
            return ItemStack.EMPTY;
        }
        var stack = Weapons.createNew(level, weapons.get(random.nextInt(weapons.size())));
        var base = Weapons.baseDefinition(level, stack);
        if (base == null) {
            return stack;
        }
        var stars = 1 + (random.nextFloat() < 0.25F ? 1 + (random.nextFloat() < 0.25F ? 1 : 0) : 0);
        var effects = new ArrayList<Holder.Reference<LegendaryEffect>>();
        for (var star = 1; star <= stars; star++) {
            var candidates = new ArrayList<Holder.Reference<LegendaryEffect>>();
            for (var effect : ModificationResolver.legendaryEffectsForStar(level.registryAccess(), star)) {
                if (effect.value().appliesTo(base.category())) {
                    candidates.add(effect);
                }
            }
            if (!candidates.isEmpty()) {
                effects.add(candidates.get(random.nextInt(candidates.size())));
            }
        }
        if (!effects.isEmpty()) {
            stack.set(
                ADDataComponents.LEGENDARY.get(),
                new WeaponLegendary(effects.stream().map(Holder.Reference::key).toList())
            );
            var effective = Weapons.definition(level, stack);
            if (effective != null) {
                Weapons.setState(
                    stack,
                    Weapons.state(stack, effective).withLoadedAmmo(effective.ammo().magazineSize())
                );
                Weapons.ensureState(stack, effective);
            }
        }
        return stack;
    }

    public static ItemStack randomLegendaryArmor(Level level, RandomSource random) {
        var pieces = ADItems.armorPieces().toList();
        if (pieces.isEmpty()) {
            return ItemStack.EMPTY;
        }
        var stack = new ItemStack(pieces.get(random.nextInt(pieces.size())));
        var access = level.registryAccess();
        var stars = 1 + (random.nextFloat() < 0.25F ? 1 + (random.nextFloat() < 0.25F ? 1 : 0) : 0);
        var upgrades = ArmorUpgrades.EMPTY;
        for (var star = 1; star <= stars; star++) {
            var candidates = ArmorLegendary.all(access, star);
            if (!candidates.isEmpty()) {
                var pick = candidates.get(random.nextInt(candidates.size()));
                upgrades = upgrades.withLegendary(access, star, pick.key().identifier());
            }
        }
        ArmorUpgrades.set(stack, upgrades);
        if (stack.isDamageableItem()) {
            stack.setDamageValue(random.nextInt(Math.max(1, stack.getMaxDamage() / 2)));
        }
        return stack;
    }

    public static int stars(ItemStack stack) {
        var legendary = stack.get(ADDataComponents.LEGENDARY.get());
        return legendary == null ? 0 : legendary.effects().size();
    }
}
