package mod.azure.atomicdawn.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import mod.azure.atomicdawn.armor.ArmorPiece;
import mod.azure.atomicdawn.block.ArmorBenchBlockEntity;
import mod.azure.atomicdawn.registry.ADBlocks;
import mod.azure.atomicdawn.registry.ADMenus;

@SuppressWarnings("unused")
public class ArmorBenchMenu extends AbstractContainerMenu {

    public static final int WORK_SLOT = 0;

    public static final int WIDTH = 330;

    public static final int HEIGHT = 226;

    public static final int WORK_X = 12;

    public static final int WORK_Y = 22;

    public static final int INVENTORY_X = (WIDTH - 162) / 2;

    public static final int INVENTORY_Y = 144;

    private static final int PLAYER_START = 1;

    private static final int PLAYER_END = PLAYER_START + 36;

    private final ContainerLevelAccess access;

    @Nullable
    private final ArmorBenchBlockEntity bench;

    private final Container work = new SimpleContainer(1) {

        @Override
        public void setChanged() {
            super.setChanged();
            ArmorBenchMenu.this.slotsChanged(this);
        }
    };

    public ArmorBenchMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(containerId, inventory, ContainerLevelAccess.NULL, null);
        data.readBlockPos();
    }

    public ArmorBenchMenu(
        int containerId,
        Inventory inventory,
        ContainerLevelAccess access,
        @Nullable ArmorBenchBlockEntity bench
    ) {
        super(ADMenus.ARMOR_BENCH.get(), containerId);
        this.access = access;
        this.bench = bench;
        addSlot(new Slot(work, WORK_SLOT, WORK_X, WORK_Y) {

            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return ArmorPiece.isArmor(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addStandardInventorySlots(inventory, INVENTORY_X, INVENTORY_Y);
        if (bench != null) {
            bench.startOpen();
        }
    }

    public ItemStack work() {
        return work.getItem(WORK_SLOT);
    }

    public void setWork(ItemStack stack) {
        work.setItem(WORK_SLOT, stack);
        broadcastChanges();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(access, player, ADBlocks.ARMOR_BENCH.get());
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, work));
        if (bench != null) {
            bench.stopOpen();
        }
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
        var stack = slot.getItem();
        var original = stack.copy();
        if (index == WORK_SLOT) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (ArmorPiece.isArmor(stack) && !slots.get(WORK_SLOT).hasItem()) {
            if (!moveItemStackTo(stack, WORK_SLOT, WORK_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    public Optional<BlockPos> position() {
        return access.evaluate((level, pos) -> pos);
    }
}
