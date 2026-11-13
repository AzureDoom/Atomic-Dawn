package mod.azure.atomicdawn.ammunition;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

public final class AmmoContainerStorage {

    public static final int SLOTS = 16;

    private AmmoContainerStorage() {}

    public static NonNullList<ItemStack> read(ItemStack container) {
        var items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        container.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        return items;
    }

    public static void write(ItemStack container, NonNullList<ItemStack> contents) {
        container.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
    }

    public static int insert(NonNullList<ItemStack> contents, ItemStack incoming) {
        if (!(incoming.getItem() instanceof AmmoItem) || incoming.isEmpty()) {
            return 0;
        }
        int before = incoming.getCount();
        for (int i = 0; i < SLOTS && !incoming.isEmpty(); i++) {
            var existing = contents.get(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, incoming)) {
                int add = Math.min(incoming.getCount(), existing.getMaxStackSize() - existing.getCount());
                if (add > 0) {
                    existing.grow(add);
                    incoming.shrink(add);
                }
            }
        }
        for (int i = 0; i < SLOTS && !incoming.isEmpty(); i++) {
            if (contents.get(i).isEmpty()) {
                int amount = Math.min(incoming.getCount(), incoming.getMaxStackSize());
                contents.set(i, incoming.copyWithCount(amount));
                incoming.shrink(amount);
            }
        }
        return before - incoming.getCount();
    }
}
