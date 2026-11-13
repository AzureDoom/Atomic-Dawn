package mod.azure.atomicdawn.npc;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class NpcCompanions {

    private NpcCompanions() {}

    public static boolean canHire(ServerPlayer player, NpcEntity npc) {
        return npc.config().companion().hireable() && !npc.hasOwner() && !npc.isHostile();
    }

    public static Component hireLabel(NpcEntity npc) {
        var companion = npc.config().companion();
        var duration = companion.durationMinutes() > 0
            ? Component.translatable("npc.atomicdawn.companion.minutes", companion.durationMinutes())
            : Component.translatable("npc.atomicdawn.companion.permanent");
        var item = companion.costItem().flatMap(BuiltInRegistries.ITEM::getOptional).orElse(null);
        if (item == null) {
            return Component.translatable("npc.atomicdawn.dialogue.hire_free", duration);
        }
        return Component.translatable(
            "npc.atomicdawn.dialogue.hire",
            companion.costCount(),
            new ItemStack(item).getHoverName(),
            duration
        );
    }

    public static boolean hire(ServerPlayer player, NpcEntity npc) {
        if (!canHire(player, npc)) {
            return false;
        }
        var companion = npc.config().companion();
        var item = companion.costItem().flatMap(BuiltInRegistries.ITEM::getOptional).orElse(null);
        if (item != null && !player.hasInfiniteMaterials()) {
            var inventory = player.getInventory();
            if (inventory.countItem(item) < companion.costCount()) {
                player.sendSystemMessage(
                    Component.translatable("npc.atomicdawn.companion.cannot_afford").withStyle(ChatFormatting.RED),
                    true
                );
                return false;
            }
            var remaining = companion.costCount();
            for (var slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
                var stack = inventory.getItem(slot);
                if (stack.is(item)) {
                    var taken = Math.min(remaining, stack.getCount());
                    stack.shrink(taken);
                    remaining -= taken;
                }
            }
        }
        var expires = companion.durationMinutes() > 0
            ? npc.level().getGameTime() + companion.durationMinutes() * 1200L
            : -1L;
        npc.setOwner(player.getUUID(), expires);
        player.sendSystemMessage(Component.translatable("npc.atomicdawn.companion.hired", npc.getDisplayName()));
        return true;
    }

    public static void recruit(ServerPlayer player, NpcEntity npc) {
        if (npc.hasOwner() && !npc.isOwnedBy(player)) {
            return;
        }
        npc.setOwner(player.getUUID(), -1L);
        player.sendSystemMessage(Component.translatable("npc.atomicdawn.companion.joined", npc.getDisplayName()));
    }

    public static void dismiss(ServerPlayer player, NpcEntity npc) {
        if (!npc.isOwnedBy(player)) {
            return;
        }
        npc.setOwner(null, -1L);
        player.sendSystemMessage(Component.translatable("npc.atomicdawn.companion.dismissed", npc.getDisplayName()));
    }

    public static void setMode(ServerPlayer player, NpcEntity npc, NpcEntity.CompanionMode mode) {
        if (!npc.isOwnedBy(player)) {
            return;
        }
        npc.setCompanionMode(mode);
        var key = mode == NpcEntity.CompanionMode.FOLLOW
            ? "npc.atomicdawn.companion.following"
            : "npc.atomicdawn.companion.waiting";
        player.sendSystemMessage(Component.translatable(key, npc.getDisplayName()), true);
    }
}
