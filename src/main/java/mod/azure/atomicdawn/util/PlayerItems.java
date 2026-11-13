package mod.azure.atomicdawn.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.item.ItemStack;

import mod.azure.atomicdawn.ammunition.AmmoContainerStorage;
import mod.azure.atomicdawn.ammunition.AmmoItem;
import mod.azure.atomicdawn.registry.ADItems;

public final class PlayerItems {

    private PlayerItems() {}

    public static void give(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (stack.getItem() instanceof AmmoItem) {
            var inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize() && !stack.isEmpty(); i++) {
                var carrier = inventory.getItem(i);
                if (!carrier.is(ADItems.AMMO_CONTAINER.get())) {
                    continue;
                }
                var contents = AmmoContainerStorage.read(carrier);
                if (AmmoContainerStorage.insert(contents, stack) > 0) {
                    AmmoContainerStorage.write(carrier, contents);
                    inventory.setChanged();
                }
            }
        }
        if (!stack.isEmpty()) {
            player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
        }
    }
}
