package mod.azure.atomicdawn.junk;

import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.camp.CampScrapbox;
import mod.azure.atomicdawn.util.PlayerItems;

public final class JunkMaterials {

    private JunkMaterials() {}

    private static boolean autoScrap() {
        return AtomicDawn.CONFIG.junk.AUTO_SCRAP_JUNK;
    }

    private static boolean scrapboxCrafting() {
        return AtomicDawn.CONFIG.junk.JUNK_STORAGE_CRAFTING;
    }

    private static int scrapYield(RegistryAccess access, Item item, SizedIngredient entry) {
        var outputs = ScrapRecipes.of(access, item);
        if (outputs.isEmpty()) {
            return 0;
        }
        var total = 0;
        for (var output : outputs.get()) {
            if (entry.ingredient().test(new ItemStack(output.item()))) {
                total += output.count();
            }
        }
        return total;
    }

    public static int extra(Player player, SizedIngredient entry) {
        var access = player.level().registryAccess();
        var total = 0L;
        if (scrapboxCrafting()) {
            for (var stored : CampScrapbox.of(player).items().entrySet()) {
                var stack = new ItemStack(stored.getKey());
                if (entry.ingredient().test(stack)) {
                    total += stored.getValue();
                } else if (autoScrap() && stack.is(ScrapRecipes.JUNK)) {
                    total += stored.getValue() * scrapYield(access, stored.getKey(), entry);
                }
            }
        }
        if (autoScrap()) {
            var inventory = player.getInventory();
            for (var i = 0; i < inventory.getContainerSize(); i++) {
                var stack = inventory.getItem(i);
                if (ScrapRecipes.scrappable(access, stack) && !entry.ingredient().test(stack)) {
                    total += (long) stack.getCount() * scrapYield(access, stack.getItem(), entry);
                }
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    public static int consume(Player player, SizedIngredient entry, int remaining) {
        if (remaining <= 0 || !(player instanceof ServerPlayer serverPlayer)) {
            return remaining;
        }
        var access = player.level().registryAccess();
        var box = scrapboxCrafting() ? new HashMap<>(CampScrapbox.of(player).items()) : new HashMap<Item, Long>();
        var changed = false;
        for (var item : Map.copyOf(box).keySet()) {
            if (remaining <= 0) {
                break;
            }
            if (entry.ingredient().test(new ItemStack(item))) {
                var taken = (int) Math.min(remaining, box.get(item));
                take(box, item, taken);
                remaining -= taken;
                changed = true;
            }
        }
        if (autoScrap() && remaining > 0) {
            var inventory = player.getInventory();
            for (var i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
                var stack = inventory.getItem(i);
                if (!ScrapRecipes.scrappable(access, stack) || entry.ingredient().test(stack)) {
                    continue;
                }
                var perUnit = scrapYield(access, stack.getItem(), entry);
                if (perUnit <= 0) {
                    continue;
                }
                var units = Math.min(stack.getCount(), (remaining + perUnit - 1) / perUnit);
                var item = stack.getItem();
                stack.shrink(units);
                remaining = distribute(access, item, units, entry, remaining, leftover -> give(serverPlayer, leftover));
            }
            inventory.setChanged();
            for (var item : Map.copyOf(box).keySet()) {
                if (remaining <= 0) {
                    break;
                }
                if (!new ItemStack(item).is(ScrapRecipes.JUNK) || entry.ingredient().test(new ItemStack(item))) {
                    continue;
                }
                var perUnit = scrapYield(access, item, entry);
                if (perUnit <= 0) {
                    continue;
                }
                var units = Math.min(box.get(item), (remaining + perUnit - 1) / perUnit);
                take(box, item, units);
                changed = true;
                remaining = distribute(
                    access,
                    item,
                    units,
                    entry,
                    remaining,
                    leftover -> box.merge(leftover.getItem(), (long) leftover.getCount(), Long::sum)
                );
            }
        }
        if (changed) {
            CampScrapbox.set(player, CampScrapbox.fromItems(box));
        }
        return Math.max(0, remaining);
    }

    private static void take(Map<Item, Long> box, Item item, long amount) {
        var left = box.getOrDefault(item, 0L) - amount;
        if (left > 0L) {
            box.put(item, left);
        } else {
            box.remove(item);
        }
    }

    private static int distribute(
        RegistryAccess access,
        Item item,
        long units,
        SizedIngredient entry,
        int remaining,
        Consumer<ItemStack> leftovers
    ) {
        var outputs = ScrapRecipes.of(access, item);
        if (outputs.isEmpty() || units <= 0L) {
            return remaining;
        }
        for (var output : outputs.get()) {
            var amount = output.count() * units;
            if (entry.ingredient().test(new ItemStack(output.item()))) {
                var used = Math.min(remaining, amount);
                remaining -= (int) used;
                amount -= used;
            }
            while (amount > 0L) {
                var chunk = (int) Math.min(amount, output.item().getDefaultMaxStackSize());
                leftovers.accept(new ItemStack(output.item(), chunk));
                amount -= chunk;
            }
        }
        return remaining;
    }

    public static void give(ServerPlayer player, ItemStack stack) {
        var remaining = stack.getCount();
        var max = Math.max(1, stack.getMaxStackSize());
        while (remaining > 0) {
            var chunk = Math.min(remaining, max);
            PlayerItems.give(player, stack.copyWithCount(chunk));
            remaining -= chunk;
        }
    }
}
