package mod.azure.atomicdawn.npc;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.registry.ADMenus;

public class NpcGearMenu extends AbstractContainerMenu {

    public static final int WIDTH = 176;

    public static final int HEIGHT = 200;

    public static final EquipmentSlot[] SLOTS = {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET,
        EquipmentSlot.MAINHAND,
        EquipmentSlot.OFFHAND };

    public static final int[][] POSITIONS = {
        { 8, 20 },
        { 8, 38 },
        { 8, 56 },
        { 8, 74 },
        { 152, 38 },
        { 152, 56 } };

    public static final int INVENTORY_Y = 118;

    private final @Nullable NpcEntity npc;

    private final Container gear;

    private boolean loading = true;

    public NpcGearMenu(int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(
            id,
            inventory,
            inventory.player.level().getEntity(data.readVarInt()) instanceof NpcEntity found ? found : null
        );
    }

    public NpcGearMenu(int id, Inventory inventory, @Nullable NpcEntity npc) {
        super(ADMenus.NPC_GEAR.get(), id);
        this.npc = npc;
        var player = inventory.player;
        this.gear = new SimpleContainer(SLOTS.length) {

            @Override
            public void setChanged() {
                super.setChanged();
                if (!loading && npc != null && player instanceof ServerPlayer serverPlayer) {
                    NpcGear.apply(serverPlayer, npc, this);
                }
            }
        };
        for (var i = 0; i < SLOTS.length; i++) {
            if (npc != null && !player.level().isClientSide()) {
                gear.setItem(i, npc.getItemBySlot(SLOTS[i]).copy());
            }
            var equipment = SLOTS[i];
            addSlot(new Slot(gear, i, POSITIONS[i][0], POSITIONS[i][1]) {

                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return accepts(equipment, stack);
                }

                @Override
                public int getMaxStackSize() {
                    return equipment.getType() == EquipmentSlot.Type.HAND ? super.getMaxStackSize() : 1;
                }
            });
        }
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        loading = false;
    }

    public @Nullable NpcEntity npc() {
        return npc;
    }

    public static boolean accepts(EquipmentSlot slot, ItemStack stack) {
        if (slot.getType() == EquipmentSlot.Type.HAND) {
            return true;
        }
        var equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot() == slot;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return npc != null && npc.isAlive() && !npc.isRemoved() && npc.distanceToSqr(player) <= 64.0;
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
        var moved = false;
        if (index < SLOTS.length) {
            moved = moveItemStackTo(source, SLOTS.length, slots.size(), true);
        } else {
            for (var i = 0; i < SLOTS.length && !moved; i++) {
                if (SLOTS[i].getType() != EquipmentSlot.Type.HAND && accepts(SLOTS[i], source)) {
                    moved = moveItemStackTo(source, i, i + 1, false);
                }
            }
            for (var i = 0; i < SLOTS.length && !moved; i++) {
                if (SLOTS[i].getType() == EquipmentSlot.Type.HAND && !slots.get(i).hasItem()) {
                    moved = moveItemStackTo(source, i, i + 1, false);
                }
            }
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

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        if (npc != null && player instanceof ServerPlayer serverPlayer) {
            NpcGear.save(serverPlayer, npc, gear);
        }
    }
}
