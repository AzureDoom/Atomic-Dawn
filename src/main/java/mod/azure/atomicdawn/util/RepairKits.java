package mod.azure.atomicdawn.util;

import net.minecraft.world.entity.player.Player;

import mod.azure.atomicdawn.registry.ADItems;

public final class RepairKits {

    private RepairKits() {}

    public static int count(Player player) {
        var total = 0;
        var inventory = player.getInventory();
        for (var i = 0; i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (stack.is(ADItems.REPAIR_KIT.get())) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static boolean available(Player player) {
        return player.hasInfiniteMaterials() || count(player) > 0;
    }

    public static boolean consume(Player player) {
        if (player.hasInfiniteMaterials()) {
            return true;
        }
        var inventory = player.getInventory();
        for (var i = 0; i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (stack.is(ADItems.REPAIR_KIT.get())) {
                stack.shrink(1);
                inventory.setChanged();
                return true;
            }
        }
        return false;
    }
}
