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

import mod.azure.atomicdawn.block.WeaponsWorkbenchBlockEntity;
import mod.azure.atomicdawn.registry.ADBlocks;
import mod.azure.atomicdawn.registry.ADMenus;
import mod.azure.atomicdawn.weapon.instance.Weapons;

@SuppressWarnings("unused")
public class WorkbenchMenu extends AbstractContainerMenu {

    public static final int WEAPON_SLOT = 0;

    public static final int IMAGE_WIDTH = 320;

    public static final int IMAGE_HEIGHT = 230;

    public static final int WEAPON_SLOT_X = 12;

    public static final int WEAPON_SLOT_Y = 20;

    public static final int INVENTORY_X = (IMAGE_WIDTH - 162) / 2;

    public static final int INVENTORY_Y = IMAGE_HEIGHT - 82;

    private static final int PLAYER_SLOTS_START = 1;

    private static final int PLAYER_SLOTS_END = PLAYER_SLOTS_START + 36;

    private final ContainerLevelAccess access;

    @Nullable
    private final WeaponsWorkbenchBlockEntity workbench;

    private final Container weaponContainer = new SimpleContainer(1) {

        @Override
        public void setChanged() {
            super.setChanged();
            WorkbenchMenu.this.slotsChanged(this);
        }
    };

    public WorkbenchMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(containerId, inventory, ContainerLevelAccess.NULL, null);
        data.readBlockPos();
    }

    public WorkbenchMenu(
        int containerId,
        Inventory inventory,
        ContainerLevelAccess access,
        @Nullable WeaponsWorkbenchBlockEntity workbench
    ) {
        super(ADMenus.WEAPONS_WORKBENCH.get(), containerId);
        this.access = access;
        this.workbench = workbench;

        addSlot(new Slot(weaponContainer, WEAPON_SLOT, WEAPON_SLOT_X, WEAPON_SLOT_Y) {

            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return Weapons.isWeapon(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addStandardInventorySlots(inventory, INVENTORY_X, INVENTORY_Y);

        if (workbench != null) {
            workbench.startOpen();
        }
    }

    public ItemStack weapon() {
        return weaponContainer.getItem(WEAPON_SLOT);
    }

    public void setWeapon(ItemStack stack) {
        weaponContainer.setItem(WEAPON_SLOT, stack);
        broadcastChanges();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(access, player, ADBlocks.WEAPONS_WORKBENCH.get());
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, weaponContainer));
        if (workbench != null) {
            workbench.stopOpen();
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        var slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        var stack = slot.getItem();
        var original = stack.copy();
        if (index == WEAPON_SLOT) {
            if (!moveItemStackTo(stack, PLAYER_SLOTS_START, PLAYER_SLOTS_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (Weapons.isWeapon(stack)) {
            if (!moveItemStackTo(stack, WEAPON_SLOT, WEAPON_SLOT + 1, false)) {
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
