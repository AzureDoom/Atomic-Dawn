package mod.azure.atomicdawn.crafting;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Comparator;
import java.util.List;

import mod.azure.atomicdawn.helper.ADRegistryUtil;
import mod.azure.atomicdawn.perks.PerkHooks;
import mod.azure.atomicdawn.quest.QuestActions;
import mod.azure.atomicdawn.registry.ADRegistries;
import mod.azure.atomicdawn.util.PlayerItems;
import mod.azure.atomicdawn.weapon.modification.WeaponModding;
import mod.azure.atomicdawn.weapon.modification.WeaponModding.Result;

public final class TinkerService {

    private TinkerService() {}

    public static List<Holder.Reference<TinkerRecipe>> recipes(Level level, TinkerRecipe.Category category) {
        return ADRegistryUtil.registry(level.registryAccess(), ADRegistries.TINKER_RECIPE)
            .listElements()
            .filter(holder -> holder.value().category() == category)
            .sorted(
                Comparator.<Holder.Reference<TinkerRecipe>>comparingInt(holder -> holder.value().order())
                    .thenComparing(holder -> BuiltInRegistries.ITEM.getKey(holder.value().result()).toString())
            )
            .toList();
    }

    public static Result craft(ServerPlayer player, ResourceKey<TinkerRecipe> key, int times) {
        var recipe = ADRegistryUtil.registry(player.level().registryAccess(), ADRegistries.TINKER_RECIPE).get(key);
        if (recipe.isEmpty()) {
            return Result.NOT_CRAFTABLE;
        }
        var value = recipe.get().value();
        var crafted = 0;
        for (var i = 0; i < Math.clamp(times, 1, 16); i++) {
            if (!player.hasInfiniteMaterials()) {
                if (!WeaponModding.hasMaterials(player, value.cost())) {
                    break;
                }
                WeaponModding.consumeMaterials(player, value.cost());
            }
            var result = value.resultStack();
            PerkHooks.onCrafted(player, result);
            PlayerItems.give(player, result);
            crafted++;
        }
        if (crafted > 0) {
            QuestActions.trigger(player, QuestActions.CRAFT_TINKER, crafted);
            return Result.CRAFTED;
        }
        return Result.MISSING_MATERIALS;
    }
}
