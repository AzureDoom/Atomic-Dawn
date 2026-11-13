package mod.azure.atomicdawn.crafting;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.ArrayList;
import java.util.List;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.item.WeaponItem;
import mod.azure.atomicdawn.progression.LegendaryRolls;
import mod.azure.atomicdawn.progression.ModUnlocks;
import mod.azure.atomicdawn.registry.ADDataComponents;
import mod.azure.atomicdawn.registry.ADItems;
import mod.azure.atomicdawn.registry.ADRegistries;
import mod.azure.atomicdawn.registry.ADSounds;
import mod.azure.atomicdawn.util.PlayerItems;
import mod.azure.atomicdawn.util.RepairKits;
import mod.azure.atomicdawn.weapon.definition.WeaponDefinition;
import mod.azure.atomicdawn.weapon.instance.WeaponCondition;
import mod.azure.atomicdawn.weapon.instance.Weapons;
import mod.azure.atomicdawn.weapon.legendary.LegendaryEffect;
import mod.azure.atomicdawn.weapon.legendary.WeaponLegendary;
import mod.azure.atomicdawn.weapon.modification.ModNames;
import mod.azure.atomicdawn.weapon.modification.ModificationResolver;
import mod.azure.atomicdawn.weapon.modification.WeaponModding;
import mod.azure.atomicdawn.weapon.modification.WeaponModding.Result;

@SuppressWarnings("unused")
public final class WorkbenchService {

    private WorkbenchService() {}

    public static List<SizedIngredient> repairCost(WeaponDefinition definition, WeaponCondition condition) {
        if (!definition.condition().enabled() || condition.missing() <= 0) {
            return List.of();
        }
        var fraction = condition.missing() / (float) condition.max();
        var cost = new ArrayList<SizedIngredient>();
        for (var entry : definition.condition().repairCost()) {
            cost.add(new SizedIngredient(entry.ingredient(), Math.max(1, (int) Math.ceil(entry.count() * fraction))));
        }
        return cost;
    }

    public static Result repair(ServerPlayer player, ItemStack weapon, boolean useKit) {
        var definition = Weapons.definition(player.level(), weapon);
        if (definition == null || !definition.condition().enabled()) {
            return Result.NOT_REPAIRABLE;
        }
        var condition = Weapons.condition(weapon, definition);
        if (condition.missing() <= 0) {
            return Result.ALREADY_REPAIRED;
        }
        if (!player.hasInfiniteMaterials()) {
            if (useKit) {
                if (!RepairKits.consume(player)) {
                    return Result.MISSING_MATERIALS;
                }
            } else {
                var cost = repairCost(definition, condition);
                if (!WeaponModding.hasMaterials(player, cost)) {
                    return Result.MISSING_MATERIALS;
                }
                WeaponModding.consumeMaterials(player, cost);
            }
        }
        Weapons.setCondition(weapon, condition.withCurrent(condition.max()));
        player.playSound(ADSounds.REPAIR_WEAPON.get());
        return Result.REPAIRED;
    }

    public static Result scrap(ServerPlayer player, WorkbenchMenu menu) {
        var weapon = menu.weapon();
        if (!(weapon.getItem() instanceof WeaponItem weaponItem)) {
            return Result.NOT_SCRAPPABLE;
        }
        var level = player.level();
        var base = Weapons.baseDefinition(level, weapon);
        if (base == null) {
            return Result.NOT_SCRAPPABLE;
        }
        var access = level.registryAccess();

        var learned = new ArrayList<Component>();
        var installed = ModificationResolver.installed(weapon, base);
        for (var key : installed.installed().values()) {
            ModificationResolver.lookup(access, key)
                .filter(mod -> mod.value().requiresUnlock())
                .filter(mod -> ModUnlocks.learn(player, mod.key()))
                .ifPresent(mod -> learned.add(ModNames.mod(mod.key())));
        }
        if (player.getRandom().nextDouble() < AtomicDawn.CONFIG.workbenches.SCRAP_LEARN_CHANCE) {
            var locked = ModificationResolver.allCompatible(access, weaponItem.definitionKey())
                .stream()
                .filter(mod -> mod.value().requiresUnlock() && !ModUnlocks.known(player).knows(mod.key()))
                .toList();
            if (!locked.isEmpty()) {
                var mod = locked.get(player.getRandom().nextInt(locked.size()));
                if (ModUnlocks.learn(player, mod.key())) {
                    learned.add(ModNames.mod(mod.key()));
                }
            }
        }

        var stars = LegendaryRolls.stars(weapon);
        menu.setWeapon(ItemStack.EMPTY);
        base.crafting().ifPresent(crafting -> {
            for (var entry : crafting.scrap()) {
                PlayerItems.give(player, new ItemStack(entry.item(), entry.count()));
            }
            player.giveExperiencePoints(crafting.scrapExperience());
        });
        if (stars > 0) {
            PlayerItems.give(player, new ItemStack(ADItems.LEGENDARY_MODULE.get(), stars));
        }

        for (var name : learned) {
            player.sendSystemMessage(
                Component.translatable("message.atomicdawn.learned_mod", name).withStyle(ChatFormatting.GREEN)
            );
        }
        return Result.SCRAPPED;
    }

    public static List<SizedIngredient> craftCost(WeaponDefinition definition) {
        return definition.crafting().map(WeaponDefinition.Crafting::cost).orElse(List.of());
    }

    public static Result craft(ServerPlayer player, WorkbenchMenu menu, ResourceKey<WeaponDefinition> weaponKey) {
        var level = player.level();
        var holder = Weapons.definitionHolder(level, weaponKey);
        var item = Weapons.itemFor(weaponKey);
        if (holder.isEmpty() || item.isEmpty() || holder.get().value().crafting().isEmpty()) {
            return Result.NOT_CRAFTABLE;
        }
        var cost = craftCost(holder.get().value());
        if (!player.hasInfiniteMaterials()) {
            if (!WeaponModding.hasMaterials(player, cost)) {
                return Result.MISSING_MATERIALS;
            }
            WeaponModding.consumeMaterials(player, cost);
        }
        var crafted = Weapons.createNew(level, item.get());
        if (menu.weapon().isEmpty()) {
            menu.setWeapon(crafted);
        } else {
            PlayerItems.give(player, crafted);
        }
        return Result.CRAFTED;
    }

    public static int legendaryCost(int star) {
        return star * AtomicDawn.CONFIG.workbenches.LEGENDARY_MODULES_PER_STAR;
    }

    public static Result applyLegendary(ServerPlayer player, ItemStack weapon, ResourceKey<LegendaryEffect> effectKey) {
        var level = player.level();
        var base = Weapons.baseDefinition(level, weapon);
        var effect = level.registryAccess().lookup(ADRegistries.LEGENDARY_EFFECT).flatMap(r -> r.get(effectKey));
        if (base == null || effect.isEmpty() || !effect.get().value().appliesTo(base.category())) {
            return Result.LEGENDARY_NOT_ALLOWED;
        }
        var star = effect.get().value().star();
        var current = ModificationResolver.legendaryKeys(weapon);
        if (current.effects().contains(effectKey)) {
            return Result.NO_CHANGE;
        }
        var modules = List.of(SizedIngredient.of(ADItems.LEGENDARY_MODULE.get(), Math.max(1, legendaryCost(star))));
        if (!player.hasInfiniteMaterials() && legendaryCost(star) > 0) {
            if (!WeaponModding.hasMaterials(player, modules)) {
                return Result.MISSING_MATERIALS;
            }
            WeaponModding.consumeMaterials(player, modules);
        }

        var existingStars = new ArrayList<Integer>();
        for (var key : current.effects()) {
            existingStars.add(
                level.registryAccess()
                    .lookup(ADRegistries.LEGENDARY_EFFECT)
                    .flatMap(r -> r.get(key))
                    .map(Holder.Reference::value)
                    .map(LegendaryEffect::star)
                    .orElse(-1)
            );
        }
        WeaponLegendary updated = current.with(star, effectKey, existingStars);
        weapon.set(ADDataComponents.LEGENDARY.get(), updated);

        var effective = Weapons.definition(level, weapon);
        if (effective != null) {
            var state = Weapons.ensureState(weapon, effective);
            if (effective.usesAmmo() && state.loadedAmmo() > effective.ammo().magazineSize()) {
                Weapons.setState(weapon, state.withLoadedAmmo(effective.ammo().magazineSize()));
            }
            if (effective.condition().enabled()) {
                Weapons.setCondition(weapon, Weapons.condition(weapon, effective));
            }
        }
        return Result.LEGENDARY_APPLIED;
    }

    public static boolean canAfford(Player player, List<SizedIngredient> cost) {
        return WeaponModding.hasMaterials(player, cost);
    }
}
