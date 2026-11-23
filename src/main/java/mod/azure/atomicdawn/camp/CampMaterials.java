package mod.azure.atomicdawn.camp;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.junk.ScrapRecipes;
import mod.azure.atomicdawn.junk.ScrapboxService;
import mod.azure.atomicdawn.util.PlayerItems;
import mod.azure.atomicdawn.weapon.modification.WeaponModding;

@SuppressWarnings("unused")
public final class CampMaterials {

    public static final TagKey<Item> SCRAP = TagKey.create(Registries.ITEM, AtomicDawn.id("camp_scrap"));

    private CampMaterials() {}

    public static int count(Player player, SizedIngredient entry) {
        return WeaponModding.count(player, entry);
    }

    public static boolean has(Player player, List<SizedIngredient> cost) {
        if (player.hasInfiniteMaterials()) {
            return true;
        }
        return WeaponModding.hasMaterials(player, cost);
    }

    public static void consume(Player player, List<SizedIngredient> cost) {
        if (player.hasInfiniteMaterials()) {
            return;
        }
        WeaponModding.consumeMaterials(player, cost);
    }

    public static void refund(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (ScrapRecipes.storable(player.level().registryAccess(), stack)) {
            CampScrapbox.set(player, CampScrapbox.of(player).add(stack.getItem(), stack.getCount()));
            return;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerItems.give(serverPlayer, stack);
        }
    }

    public static long deposit(Player player) {
        return ScrapboxService.depositAll(player);
    }
}
