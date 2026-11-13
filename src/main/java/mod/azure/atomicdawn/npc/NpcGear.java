package mod.azure.atomicdawn.npc;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

public final class NpcGear {

    private static final EquipmentSlot[] LAYOUT = {
        EquipmentSlot.MAINHAND,
        EquipmentSlot.OFFHAND,
        null,
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET,
        null,
        null };

    private NpcGear() {}

    private static boolean isMarker(int slot) {
        return slot >= 0 && slot < LAYOUT.length && LAYOUT[slot] == null;
    }

    public static void openEditor(ServerPlayer player, NpcEntity npc) {
        var container = new SimpleContainer(LAYOUT.length);
        for (var i = 0; i < LAYOUT.length; i++) {
            if (LAYOUT[i] == null) {
                var pane = new ItemStack(Items.STAINED_GLASS_PANE.pick(DyeColor.BLACK));
                pane.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
                container.setItem(i, pane);
            } else {
                container.setItem(i, npc.getItemBySlot(LAYOUT[i]).copy());
            }
        }
        player.openMenu(
            new SimpleMenuProvider(
                (id, inventory, _) -> new ChestMenu(MenuType.GENERIC_9x1, id, inventory, container, 1) {

                    @Override
                    public void clicked(
                        int slotIndex,
                        int buttonNum,
                        @NotNull ContainerInput input,
                        @NotNull Player clicker
                    ) {
                        if (isMarker(slotIndex)) {
                            return;
                        }
                        super.clicked(slotIndex, buttonNum, input, clicker);
                    }

                    @Override
                    public @NotNull ItemStack quickMoveStack(@NotNull Player mover, int index) {
                        return isMarker(index) ? ItemStack.EMPTY : super.quickMoveStack(mover, index);
                    }

                    @Override
                    public void removed(@NotNull Player closing) {
                        super.removed(closing);
                        if (closing instanceof ServerPlayer serverPlayer) {
                            save(serverPlayer, npc, container);
                        }
                    }
                },
                Component.translatable("npc.atomicdawn.gear.title")
            )
        );
    }

    private static void save(ServerPlayer player, NpcEntity npc, SimpleContainer container) {
        if (!npc.isAlive() || npc.isRemoved() || !NpcService.canEdit(player)) {
            return;
        }
        for (var i = 0; i < LAYOUT.length; i++) {
            if (LAYOUT[i] != null) {
                npc.setItemSlot(LAYOUT[i], container.getItem(i).copy());
            }
        }
        player.sendSystemMessage(
            Component.translatable("npc.atomicdawn.gear.saved").withStyle(ChatFormatting.GRAY),
            true
        );
    }
}
