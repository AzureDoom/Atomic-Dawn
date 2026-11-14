package mod.azure.atomicdawn.lock;

import net.minecraft.world.entity.player.Player;

import mod.azure.atomicdawn.registry.ADItems;

public final class BobbyPins {

    public static final int INFINITE = -1;

    private BobbyPins() {}

    public static int count(Player player) {
        if (player.hasInfiniteMaterials()) {
            return INFINITE;
        }
        var total = 0;
        var inventory = player.getInventory();
        for (var i = 0; i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (stack.is(ADItems.BOBBY_PIN.get())) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static boolean available(Player player) {
        return count(player) != 0;
    }

    public static boolean consume(Player player) {
        if (player.hasInfiniteMaterials()) {
            return true;
        }
        var inventory = player.getInventory();
        for (var i = 0; i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (stack.is(ADItems.BOBBY_PIN.get())) {
                stack.shrink(1);
                inventory.setChanged();
                return true;
            }
        }
        return false;
    }
}
