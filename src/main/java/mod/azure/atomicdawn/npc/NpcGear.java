package mod.azure.atomicdawn.npc;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;

import mod.azure.atomicdawn.util.PlayerItems;

public final class NpcGear {

    private NpcGear() {}

    public static void openEditor(ServerPlayer player, NpcEntity npc) {
        player.openMenu(
            new SimpleMenuProvider(
                (id, inventory, _) -> new NpcGearMenu(id, inventory, npc),
                Component.translatable("npc.atomicdawn.gear.title")
            ),
            buffer -> buffer.writeVarInt(npc.getId())
        );
    }

    static void apply(ServerPlayer player, NpcEntity npc, Container container) {
        if (!npc.isAlive() || npc.isRemoved() || !NpcService.canEdit(player)) {
            return;
        }
        for (var i = 0; i < NpcGearMenu.SLOTS.length; i++) {
            var slot = NpcGearMenu.SLOTS[i];
            var stack = container.getItem(i);
            if (!ItemStack.matches(npc.getItemBySlot(slot), stack)) {
                npc.setItemSlot(slot, stack.copy());
            }
        }
    }

    static void save(ServerPlayer player, NpcEntity npc, Container container) {
        if (!npc.isAlive() || npc.isRemoved() || !NpcService.canEdit(player)) {
            for (var i = 0; i < container.getContainerSize(); i++) {
                var stack = container.removeItemNoUpdate(i);
                if (!stack.isEmpty()) {
                    PlayerItems.give(player, stack);
                }
            }
            return;
        }
        for (var i = 0; i < NpcGearMenu.SLOTS.length; i++) {
            npc.setItemSlot(NpcGearMenu.SLOTS[i], container.getItem(i).copy());
        }
        player.sendSystemMessage(
            Component.translatable("npc.atomicdawn.gear.saved").withStyle(ChatFormatting.GRAY),
            true
        );
    }
}
