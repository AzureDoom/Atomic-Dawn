package mod.azure.atomicdawn.powerarmor;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.registry.ADMenus;

public class PowerArmorStationMenu extends AbstractContainerMenu {

    public static final int WIDTH = 330;

    public static final int HEIGHT = 226;

    public static final int[][] PIECE_POSITIONS = {
        { 43, 22 },
        { 43, 44 },
        { 65, 44 },
        { 21, 44 },
        { 54, 66 },
        { 32, 66 } };

    public static final int CORE_X = 43;

    public static final int CORE_Y = 96;

    public static final int DYE_X = 65;

    public static final int DYE_Y = 96;

    public static final int INVENTORY_X = (WIDTH - 162) / 2;

    public static final int INVENTORY_Y = 144;

    public static final int DYE_SLOT = PowerArmorEntity.SLOT_COUNT;

    private static final int PLAYER_START = DYE_SLOT + 1;

    @Nullable
    private final PowerArmorEntity armor;

    private final SimpleContainer dye = new SimpleContainer(1);

    public PowerArmorStationMenu(int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(
            id,
            inventory,
            inventory.player.level().getEntity(data.readVarInt()) instanceof PowerArmorEntity armor3 ? armor3 : null
        );
    }

    public PowerArmorStationMenu(int id, Inventory inventory, @Nullable PowerArmorEntity armor) {
        super(ADMenus.POWER_ARMOR_STATION.get(), id);
        this.armor = armor;
        Container container = armor != null ? armor.container() : new SimpleContainer(PowerArmorEntity.SLOT_COUNT);
        for (var slot : PowerArmorSlot.all()) {
            var position = PIECE_POSITIONS[slot.ordinal()];
            addSlot(new FilteredSlot(container, slot.ordinal(), position[0], position[1]));
        }
        addSlot(new FilteredSlot(container, PowerArmorEntity.CORE_SLOT, CORE_X, CORE_Y));
        addSlot(new Slot(dye, 0, DYE_X, DYE_Y) {

            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof DyeItem;
            }
        });
        addStandardInventorySlots(inventory, INVENTORY_X, INVENTORY_Y);
    }

    @Nullable
    public PowerArmorEntity armor() {
        return armor;
    }

    public ItemStack dyeStack() {
        return dye.getItem(0);
    }

    public void consumeDye(Player player) {
        if (!player.hasInfiniteMaterials()) {
            dye.removeItem(0, 1);
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return armor != null && armor.isAlive() && armor.isDocked() && player.distanceToSqr(armor) < 64.0;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        clearContainer(player, dye);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        var slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        var source = slot.getItem();
        var original = source.copy();
        boolean moved;
        if (index < PLAYER_START) {
            moved = moveItemStackTo(source, PLAYER_START, slots.size(), true);
        } else if (source.getItem() instanceof PowerArmorPieceItem piece) {
            moved = moveItemStackTo(source, piece.slot().ordinal(), piece.slot().ordinal() + 1, false);
        } else if (source.getItem() instanceof PowerCoreItem) {
            moved = moveItemStackTo(source, PowerArmorEntity.CORE_SLOT, PowerArmorEntity.CORE_SLOT + 1, false);
        } else if (source.getItem() instanceof DyeItem) {
            moved = moveItemStackTo(source, DYE_SLOT, DYE_SLOT + 1, false);
        } else {
            return ItemStack.EMPTY;
        }
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

    private static final class FilteredSlot extends Slot {

        FilteredSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return PowerArmorEntity.accepts(getContainerSlot(), stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
