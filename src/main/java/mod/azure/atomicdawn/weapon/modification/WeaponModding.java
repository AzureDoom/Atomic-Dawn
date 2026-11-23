package mod.azure.atomicdawn.weapon.modification;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;
import java.util.Optional;

import mod.azure.atomicdawn.item.WeaponItem;
import mod.azure.atomicdawn.junk.JunkMaterials;
import mod.azure.atomicdawn.progression.ModUnlocks;
import mod.azure.atomicdawn.quest.QuestActions;
import mod.azure.atomicdawn.registry.ADDataComponents;
import mod.azure.atomicdawn.weapon.instance.Weapons;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class WeaponModding {

    public enum Result {
        INSTALLED,
        NO_CHANGE,
        NOT_A_WEAPON,
        SLOT_NOT_AVAILABLE,
        INCOMPATIBLE,
        LOCKED,
        MISSING_MATERIALS,
        REPAIRED,
        ALREADY_REPAIRED,
        NOT_REPAIRABLE,
        SCRAPPED,
        NOT_SCRAPPABLE,
        CRAFTED,
        NOT_CRAFTABLE,
        LEGENDARY_APPLIED,
        LEGENDARY_NOT_ALLOWED,
        NOTHING_TO_DO
    }

    private WeaponModding() {}

    public static Result install(
        ServerPlayer player,
        ItemStack weapon,
        ModSlot slot,
        Optional<ResourceKey<WeaponModification>> requested
    ) {
        var level = player.level();
        if (!(weapon.getItem() instanceof WeaponItem weaponItem)) {
            return Result.NOT_A_WEAPON;
        }
        var base = Weapons.baseDefinition(level, weapon);
        if (base == null) {
            return Result.NOT_A_WEAPON;
        }
        if (!base.modifications().slots().contains(slot)) {
            return Result.SLOT_NOT_AVAILABLE;
        }

        var access = level.registryAccess();
        var target = requested.or(() -> Optional.ofNullable(base.modifications().defaults().get(slot)));
        var current = ModificationResolver.installed(weapon, base);
        if (current.get(slot).equals(target)) {
            return Result.NO_CHANGE;
        }

        List<SizedIngredient> cost = List.of();
        if (target.isPresent()) {
            var mod = ModificationResolver.lookup(access, target.get());
            var compatible = mod.isPresent()
                && mod.get().value().slot() == slot
                && ModificationResolver.compatible(access, weaponItem.definitionKey(), slot).contains(mod.get());
            if (!compatible) {
                return Result.INCOMPATIBLE;
            }
            if (!ModUnlocks.isUnlocked(player, mod.get())) {
                return Result.LOCKED;
            }
            var isDefault = target.equals(Optional.ofNullable(base.modifications().defaults().get(slot)));
            cost = isDefault ? List.of() : mod.get().value().cost();
        }

        if (!player.hasInfiniteMaterials()) {
            if (!hasMaterials(player, cost)) {
                return Result.MISSING_MATERIALS;
            }
            consumeMaterials(player, cost);
        }

        var updated = current.with(slot, target);
        weapon.set(ADDataComponents.WEAPON_MODS.get(), updated);

        var effective = Weapons.definition(level, weapon);
        if (effective != null) {
            var state = Weapons.ensureState(weapon, effective);
            if (effective.condition().enabled()) {
                Weapons.setCondition(weapon, Weapons.condition(weapon, effective));
            }
            var magazine = effective.ammo().magazineSize();
            if (state.loadedAmmo() > magazine) {
                var excess = state.loadedAmmo() - magazine;
                Weapons.setState(weapon, state.withLoadedAmmo(magazine));
                if (!player.hasInfiniteMaterials()) {
                    mod.azure.atomicdawn.ammunition.AmmoInventory.giveRounds(player, effective.ammo().item(), excess);
                }
            }
        }
        QuestActions.trigger(player, QuestActions.INSTALL_MOD);
        return Result.INSTALLED;
    }

    public static boolean hasMaterials(Player player, List<SizedIngredient> cost) {
        if (player.hasInfiniteMaterials()) {
            return true;
        }
        for (var entry : cost) {
            if (count(player, entry) < entry.count()) {
                return false;
            }
        }
        return true;
    }

    public static int count(Player player, SizedIngredient entry) {
        var inventory = player.getInventory();
        var total = 0;
        for (var i = 0; i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (!stack.isEmpty() && entry.ingredient().test(stack)) {
                total += stack.getCount();
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, (long) total + JunkMaterials.extra(player, entry));
    }

    public static void consumeMaterials(Player player, List<SizedIngredient> cost) {
        var inventory = player.getInventory();
        for (var entry : cost) {
            var remaining = entry.count();
            for (var i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
                var stack = inventory.getItem(i);
                if (!stack.isEmpty() && entry.ingredient().test(stack)) {
                    var taken = Math.min(remaining, stack.getCount());
                    stack.shrink(taken);
                    remaining -= taken;
                }
            }
            JunkMaterials.consume(player, entry, remaining);
        }
        inventory.setChanged();
    }
}
