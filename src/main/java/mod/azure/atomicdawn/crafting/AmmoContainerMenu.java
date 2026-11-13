package mod.azure.atomicdawn.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import mod.azure.atomicdawn.ammunition.AmmoContainerStorage;
import mod.azure.atomicdawn.ammunition.AmmoItem;
import mod.azure.atomicdawn.registry.ADItems;
import mod.azure.atomicdawn.registry.ADMenus;

public class AmmoContainerMenu extends AbstractContainerMenu {

    public static final int WIDTH = 176;

    public static final int HEIGHT = 184;

    private static final int BOX_SLOTS = AmmoContainerStorage.SLOTS;

    private final Inventory inventory;

    private final int carrierSlot;

    private final ItemStack carrier;

    private final boolean loading;

    public AmmoContainerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(id, inventory, data.readVarInt());
    }

    public AmmoContainerMenu(int id, Inventory inventory, int carrierSlot) {
        super(ADMenus.AMMO_CONTAINER.get(), id);
        this.inventory = inventory;
        this.carrierSlot = carrierSlot;
        this.carrier = inventory.getItem(carrierSlot);
        Container storage = new SimpleContainer(BOX_SLOTS) {

            @Override
            public void setChanged() {
                super.setChanged();
                if (!loading && !carrier.isEmpty()) {
                    NonNullList<ItemStack> items = NonNullList.withSize(BOX_SLOTS, ItemStack.EMPTY);
                    for (int i = 0; i < BOX_SLOTS; i++) {
                        items.set(i, getItem(i).copy());
                    }
                    AmmoContainerStorage.write(carrier, items);
                    inventory.setChanged();
                }
            }
        };
        if (carrier.is(ADItems.AMMO_CONTAINER.get())) {
            var items = AmmoContainerStorage.read(carrier);
            for (int i = 0; i < BOX_SLOTS; i++) {
                storage.setItem(i, items.get(i));
            }
        }
        loading = false;
        for (int i = 0; i < BOX_SLOTS; i++) {
            addSlot(new Slot(storage, i, 53 + (i % 4) * 18, 18 + (i / 4) * 18) {

                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return stack.getItem() instanceof AmmoItem;
                }
            });
        }
        addStandardInventorySlots(inventory, 8, 102);
        int menuSlot = carrierSlot == 40
            ? -1
            : carrierSlot < 9 ? BOX_SLOTS + 27 + carrierSlot : BOX_SLOTS + carrierSlot - 9;
        if (menuSlot >= 0) {
            lockedMenuSlot = menuSlot;
        }
    }

    private int lockedMenuSlot = -1;

    @Override
    public boolean stillValid(@NotNull Player player) {
        return inventory.getItem(carrierSlot) == carrier && carrier.is(ADItems.AMMO_CONTAINER.get());
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, @NonNull ContainerInput containerInput, @NonNull Player player) {
        if (
            slotIndex == lockedMenuSlot ||
                (containerInput == ContainerInput.SWAP && buttonNum == carrierSlot)
        ) {
            return;
        }
        super.clicked(slotIndex, buttonNum, containerInput, player);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        if (index < 0 || index >= slots.size() || index == lockedMenuSlot) {
            return ItemStack.EMPTY;
        }
        var slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        var source = slot.getItem();
        if (index >= BOX_SLOTS && !(source.getItem() instanceof AmmoItem)) {
            return ItemStack.EMPTY;
        }
        var original = source.copy();
        boolean moved = index < BOX_SLOTS
            ? moveItemStackTo(source, BOX_SLOTS, slots.size(), true)
            : moveItemStackTo(source, 0, BOX_SLOTS, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (source.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }
}
