package mod.azure.atomicdawn.camp;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.junk.JunkMaterials;

public final class CampStashService {

    private static final double MAX_DISTANCE_SQR = 8.0 * 8.0;

    private CampStashService() {}

    public static boolean canUse(ServerPlayer player, BlockPos pos) {
        var level = player.level();
        if (
            !level.isLoaded(pos) || Vec3.atCenterOf(pos).distanceToSqr(player.position()) > MAX_DISTANCE_SQR
                || !(level.getBlockState(pos).getBlock() instanceof CampStashBlock)
        ) {
            return false;
        }
        if (!AtomicDawn.CONFIG.camp.STASH_REQUIRES_OWN_CAMP) {
            return true;
        }
        var tracked = CampManager.objectAt(level.getServer(), level.dimension(), pos);
        return tracked.isPresent() && tracked.get().camp().ownedBy(player.getUUID());
    }

    public static void depositSlot(ServerPlayer player, BlockPos pos, int slot, boolean whole) {
        var inventory = player.getInventory();
        if (!canUse(player, pos) || slot < 0 || slot >= inventory.getContainerSize()) {
            return;
        }
        var stack = inventory.getItem(slot);
        if (stack.isEmpty()) {
            return;
        }
        var stash = CampStash.of(player);
        var amount = (int) Math.min(whole ? stack.getCount() : 1, stash.space());
        if (amount <= 0) {
            notify(player, Component.translatable("camp.atomicdawn.stash.full", CampStash.limit()), ChatFormatting.RED);
            return;
        }
        CampStash.set(player, stash.add(stack, amount));
        stack.shrink(amount);
        inventory.setChanged();
        sound(player, pos);
    }

    public static void withdraw(ServerPlayer player, BlockPos pos, int index, Identifier id, int amount) {
        if (!canUse(player, pos) || amount <= 0) {
            return;
        }
        var stash = CampStash.of(player);
        if (index < 0 || index >= stash.entries().size()) {
            return;
        }
        var entry = stash.entries().get(index);
        if (!BuiltInRegistries.ITEM.getKey(entry.stack().getItem()).equals(id)) {
            return;
        }
        var taken = (int) Math.min(entry.count(), Math.min(amount, entry.stack().getMaxStackSize() * 36L));
        if (taken <= 0) {
            return;
        }
        CampStash.set(player, stash.remove(index, taken));
        JunkMaterials.give(player, entry.stack().copyWithCount(taken));
        sound(player, pos);
    }

    private static void sound(ServerPlayer player, BlockPos pos) {
        player.level().playSound(null, pos, SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS, 0.5F, 1.2F);
    }

    private static void notify(ServerPlayer player, Component message, ChatFormatting color) {
        player.sendSystemMessage(message.copy().withStyle(color), true);
    }
}
