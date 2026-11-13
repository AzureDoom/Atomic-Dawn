package mod.azure.atomicdawn.ammunition;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

import mod.azure.atomicdawn.registry.ADDataComponents;
import mod.azure.atomicdawn.registry.ADItems;
import mod.azure.atomicdawn.util.PlayerItems;

public final class AmmoInventory {

    private AmmoInventory() {}

    public static boolean hasInfiniteAmmo(Player player) {
        return player.hasInfiniteMaterials();
    }

    private static int boxSize(Item ammo) {
        return ammo instanceof AmmoItem item ? item.roundsPerItem() : 1;
    }

    private static int roundsInBox(ItemStack stack, int size) {
        return Math.clamp(stack.getOrDefault(ADDataComponents.AMMO_REMAINING.get(), size), 1, size);
    }

    public static int rounds(ItemStack stack) {
        return stack.isEmpty() ? 0 : stack.getCount() * roundsInBox(stack, boxSize(stack.getItem()));
    }

    public static int count(Player player, Item ammo) {
        var inventory = player.getInventory();
        int total = 0;
        int size = boxSize(ammo);
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            if (stack.is(ammo)) {
                total += stack.getCount() * roundsInBox(stack, size);
            } else if (stack.is(ADItems.AMMO_CONTAINER.get())) {
                for (var contained : AmmoContainerStorage.read(stack)) {
                    if (contained.is(ammo)) {
                        total += contained.getCount() * roundsInBox(contained, size);
                    }
                }
            }
        }
        return total;
    }

    private static int consume(
        Item ammo,
        int amount,
        int size,
        int slots,
        IntFunction<ItemStack> get,
        List<ItemStack> leftovers
    ) {
        int remaining = amount;
        for (int pass = 0; pass < 2 && remaining > 0; pass++) {
            for (int slot = 0; slot < slots && remaining > 0; slot++) {
                var stack = get.apply(slot);
                if (!stack.is(ammo)) {
                    continue;
                }
                int available = roundsInBox(stack, size);
                if ((pass == 0) != (available < size)) {
                    continue;
                }
                while (!stack.isEmpty() && remaining > 0) {
                    available = roundsInBox(stack, size);
                    int used = Math.min(remaining, available);
                    remaining -= used;
                    if (used == available) {
                        stack.shrink(1);
                    } else if (stack.getCount() == 1) {
                        stack.set(ADDataComponents.AMMO_REMAINING.get(), available - used);
                    } else {
                        stack.shrink(1);
                        var partial = new ItemStack(ammo);
                        partial.set(ADDataComponents.AMMO_REMAINING.get(), available - used);
                        leftovers.add(partial);
                    }
                    if (used < available) {
                        break;
                    }
                }
            }
        }
        return remaining;
    }

    public static int take(Player player, Item ammo, int amount) {
        if (amount <= 0) {
            return 0;
        }
        if (hasInfiniteAmmo(player)) {
            return amount;
        }
        var inventory = player.getInventory();
        int remaining = amount;
        int size = boxSize(ammo);
        List<ItemStack> leftovers = new ArrayList<>();
        remaining = consume(ammo, remaining, size, inventory.getContainerSize(), inventory::getItem, leftovers);
        if (remaining != amount) {
            inventory.setChanged();
        }
        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
            var box = inventory.getItem(slot);
            if (!box.is(ADItems.AMMO_CONTAINER.get())) {
                continue;
            }
            var contents = AmmoContainerStorage.read(box);
            int previous = remaining;
            List<ItemStack> boxLeftovers = new ArrayList<>();
            remaining = consume(ammo, remaining, size, contents.size(), contents::get, boxLeftovers);
            if (remaining != previous) {
                for (var partial : boxLeftovers) {
                    AmmoContainerStorage.insert(contents, partial);
                    if (!partial.isEmpty()) {
                        leftovers.add(partial);
                    }
                }
                AmmoContainerStorage.write(box, contents);
                inventory.setChanged();
            }
        }
        if (player instanceof ServerPlayer serverPlayer) {
            for (var partial : leftovers) {
                PlayerItems.give(serverPlayer, partial);
            }
            if (remaining != amount) {
                AmmoContainerEvents.markDirty(serverPlayer);
            }
        }
        return amount - remaining;
    }

    public static void consolidate(ServerPlayer player) {
        var inventory = player.getInventory();
        var partials = new HashMap<AmmoItem, Integer>();
        var changed = false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            if (stack.is(ADItems.AMMO_CONTAINER.get())) {
                var contents = AmmoContainerStorage.read(stack);
                var dirty = false;
                for (var contained : contents) {
                    dirty |= normalize(contained, partials);
                }
                if (dirty) {
                    AmmoContainerStorage.write(stack, contents);
                    changed = true;
                }
            } else {
                changed |= normalize(stack, partials);
            }
        }
        var merge = new HashMap<AmmoItem, Integer>();
        partials.forEach((ammo, boxes) -> {
            if (boxes > 1) {
                merge.put(ammo, 0);
            }
        });
        if (merge.isEmpty()) {
            if (changed) {
                inventory.setChanged();
            }
            return;
        }
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            if (stack.is(ADItems.AMMO_CONTAINER.get())) {
                var contents = AmmoContainerStorage.read(stack);
                var dirty = false;
                for (int i = 0; i < contents.size(); i++) {
                    if (collectPartial(contents.get(i), merge)) {
                        contents.set(i, ItemStack.EMPTY);
                        dirty = true;
                    }
                }
                if (dirty) {
                    AmmoContainerStorage.write(stack, contents);
                }
            } else if (collectPartial(stack, merge)) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
        inventory.setChanged();
        merge.forEach((ammo, rounds) -> giveRounds(player, ammo, rounds));
    }

    private static boolean normalize(ItemStack stack, Map<AmmoItem, Integer> partials) {
        if (!(stack.getItem() instanceof AmmoItem ammo) || !stack.has(ADDataComponents.AMMO_REMAINING.get())) {
            return false;
        }
        if (!ammo.isPartial(stack)) {
            stack.remove(ADDataComponents.AMMO_REMAINING.get());
            return true;
        }
        partials.merge(ammo, stack.getCount(), Integer::sum);
        return false;
    }

    private static boolean collectPartial(ItemStack stack, Map<AmmoItem, Integer> merge) {
        if (!(stack.getItem() instanceof AmmoItem ammo) || !merge.containsKey(ammo) || !ammo.isPartial(stack)) {
            return false;
        }
        merge.merge(ammo, ammo.roundsInBox(stack) * stack.getCount(), Integer::sum);
        return true;
    }

    public static void giveRounds(ServerPlayer player, Item ammo, int rounds) {
        if (rounds <= 0) {
            return;
        }
        int size = boxSize(ammo);
        int boxes = rounds / size;
        while (boxes > 0) {
            int count = Math.min(boxes, ammo.getDefaultMaxStackSize());
            PlayerItems.give(player, new ItemStack(ammo, count));
            boxes -= count;
        }
        int extra = rounds % size;
        if (extra > 0) {
            var partial = new ItemStack(ammo);
            partial.set(ADDataComponents.AMMO_REMAINING.get(), extra);
            PlayerItems.give(player, partial);
            AmmoContainerEvents.markDirty(player);
        }
    }
}
