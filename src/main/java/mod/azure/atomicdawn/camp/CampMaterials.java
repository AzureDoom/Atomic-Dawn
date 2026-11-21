package mod.azure.atomicdawn.camp;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.HashMap;
import java.util.List;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.util.PlayerItems;
import mod.azure.atomicdawn.weapon.modification.WeaponModding;

@SuppressWarnings("unused")
public final class CampMaterials {

    public static final TagKey<Item> SCRAP = TagKey.create(Registries.ITEM, AtomicDawn.id("camp_scrap"));

    private CampMaterials() {}

    public static int stored(Player player, SizedIngredient entry) {
        var total = 0;
        for (var stored : CampScrapbox.of(player).counts().entrySet()) {
            var item = BuiltInRegistries.ITEM.getOptional(stored.getKey());
            if (item.isPresent() && entry.ingredient().test(new ItemStack(item.get()))) {
                total += stored.getValue();
            }
        }
        return total;
    }

    public static int count(Player player, SizedIngredient entry) {
        return WeaponModding.count(player, entry) + stored(player, entry);
    }

    public static boolean has(Player player, List<SizedIngredient> cost) {
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

    public static void consume(Player player, List<SizedIngredient> cost) {
        if (player.hasInfiniteMaterials()) {
            return;
        }
        var inventory = player.getInventory();
        var scrapbox = new HashMap<>(CampScrapbox.of(player).counts());
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
            for (var stored : scrapbox.entrySet()) {
                if (remaining <= 0) {
                    break;
                }
                var item = BuiltInRegistries.ITEM.getOptional(stored.getKey());
                if (item.isEmpty() || !entry.ingredient().test(new ItemStack(item.get()))) {
                    continue;
                }
                var taken = Math.min(remaining, stored.getValue());
                stored.setValue(stored.getValue() - taken);
                remaining -= taken;
            }
        }
        inventory.setChanged();
        player.setData(CampAttachments.SCRAPBOX.get(), new CampScrapbox(scrapbox));
    }

    public static void refund(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (stack.is(SCRAP)) {
            var updated = CampScrapbox.of(player).add(stack.getItem(), stack.getCount());
            player.setData(CampAttachments.SCRAPBOX.get(), updated);
            return;
        }
        if (player instanceof ServerPlayer serverPlayer)
            PlayerItems.give(serverPlayer, stack);
    }

    public static int deposit(Player player) {
        var inventory = player.getInventory();
        var scrapbox = CampScrapbox.of(player);
        var moved = 0;
        for (var i = 0; i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (stack.isEmpty() || !stack.is(SCRAP) || i == inventory.getSelectedSlot()) {
                continue;
            }
            scrapbox = scrapbox.add(stack.getItem(), stack.getCount());
            moved += stack.getCount();
            inventory.setItem(i, ItemStack.EMPTY);
        }
        if (moved > 0) {
            inventory.setChanged();
            player.setData(CampAttachments.SCRAPBOX.get(), scrapbox);
        }
        return moved;
    }
}
