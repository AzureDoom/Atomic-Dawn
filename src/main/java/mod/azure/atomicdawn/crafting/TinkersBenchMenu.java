package mod.azure.atomicdawn.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import mod.azure.atomicdawn.block.TinkersBenchBlockEntity;
import mod.azure.atomicdawn.registry.ADBlocks;
import mod.azure.atomicdawn.registry.ADMenus;

@SuppressWarnings("unused")
public class TinkersBenchMenu extends AbstractContainerMenu {

    public static final int IMAGE_WIDTH = WorkbenchMenu.IMAGE_WIDTH;

    public static final int IMAGE_HEIGHT = WorkbenchMenu.IMAGE_HEIGHT;

    public static final int INVENTORY_X = WorkbenchMenu.INVENTORY_X;

    public static final int INVENTORY_Y = WorkbenchMenu.INVENTORY_Y;

    private final ContainerLevelAccess access;

    @Nullable
    private final TinkersBenchBlockEntity bench;

    public TinkersBenchMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(containerId, inventory, ContainerLevelAccess.NULL, null);
        data.readBlockPos();
    }

    public TinkersBenchMenu(
        int containerId,
        Inventory inventory,
        ContainerLevelAccess access,
        @Nullable TinkersBenchBlockEntity bench
    ) {
        super(ADMenus.TINKERS_BENCH.get(), containerId);
        this.access = access;
        this.bench = bench;
        addStandardInventorySlots(inventory, INVENTORY_X, INVENTORY_Y);
        if (bench != null) {
            bench.startOpen();
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(access, player, ADBlocks.TINKERS_BENCH.get());
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        if (bench != null) {
            bench.stopOpen();
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        return ItemStack.EMPTY;
    }

    public Optional<BlockPos> position() {
        return access.evaluate((level, pos) -> pos);
    }
}
