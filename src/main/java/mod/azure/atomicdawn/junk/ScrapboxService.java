package mod.azure.atomicdawn.junk;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.camp.CampScrapbox;

public final class ScrapboxService {

    private static final double MAX_DISTANCE_SQR = 8.0 * 8.0;

    private ScrapboxService() {}

    public static boolean near(ServerPlayer player, BlockPos pos) {
        var level = player.level();
        return level.isLoaded(pos) && Vec3.atCenterOf(pos).distanceToSqr(player.position()) <= MAX_DISTANCE_SQR
            && level.getBlockState(pos).getBlock() instanceof ScrapboxBlock;
    }

    private static boolean accepts(Player player, ItemStack stack, boolean components) {
        if (!ScrapRecipes.storable(player.level().registryAccess(), stack)) {
            return false;
        }
        return components || stack.is(ScrapRecipes.JUNK);
    }

    public static long depositAll(Player player) {
        var inventory = player.getInventory();
        var components = AtomicDawn.CONFIG.junk.JUNK_DEPOSIT_COMPONENTS;
        var box = CampScrapbox.of(player);
        var moved = 0L;
        for (var i = 0; i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (!accepts(player, stack, components)) {
                continue;
            }
            box = box.add(stack.getItem(), stack.getCount());
            moved += stack.getCount();
            inventory.setItem(i, ItemStack.EMPTY);
        }
        if (moved > 0L) {
            inventory.setChanged();
            CampScrapbox.set(player, box);
        }
        return moved;
    }

    public static void depositAll(ServerPlayer player, BlockPos pos) {
        if (!near(player, pos)) {
            return;
        }
        var moved = depositAll(player);
        if (moved > 0L) {
            sound(player, pos, false);
        }
        notify(player, "junk.atomicdawn.deposited", moved > 0L ? ChatFormatting.GREEN : ChatFormatting.GRAY, moved);
    }

    public static void depositSlot(ServerPlayer player, BlockPos pos, int slot) {
        var inventory = player.getInventory();
        if (!near(player, pos) || slot < 0 || slot >= inventory.getContainerSize()) {
            return;
        }
        var stack = inventory.getItem(slot);
        if (!accepts(player, stack, true)) {
            return;
        }
        CampScrapbox.set(player, CampScrapbox.of(player).add(stack.getItem(), stack.getCount()));
        inventory.setItem(slot, ItemStack.EMPTY);
        inventory.setChanged();
        sound(player, pos, false);
    }

    public static void depositHeld(ServerPlayer player, BlockPos pos, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!near(player, pos) || !accepts(player, stack, true)) {
            return;
        }
        var moved = stack.getCount();
        CampScrapbox.set(player, CampScrapbox.of(player).add(stack.getItem(), moved));
        player.setItemInHand(hand, ItemStack.EMPTY);
        sound(player, pos, false);
        notify(player, "junk.atomicdawn.deposited", ChatFormatting.GREEN, moved);
    }

    public static void withdraw(ServerPlayer player, BlockPos pos, Identifier id, int amount) {
        var item = BuiltInRegistries.ITEM.getOptional(id);
        if (!near(player, pos) || item.isEmpty() || amount <= 0) {
            return;
        }
        var box = CampScrapbox.of(player);
        var max = new ItemStack(item.get()).getMaxStackSize();
        var taken = Math.min(box.count(item.get()), Math.min(amount, max * 36L));
        if (taken <= 0L) {
            return;
        }
        CampScrapbox.set(player, box.remove(item.get(), taken));
        JunkMaterials.give(player, new ItemStack(item.get(), (int) taken));
    }

    public static void scrap(ServerPlayer player, BlockPos pos, @Nullable Identifier id) {
        if (!near(player, pos)) {
            return;
        }
        var access = player.level().registryAccess();
        var items = new HashMap<>(CampScrapbox.of(player).items());
        var scrapped = 0L;
        for (var entry : Map.copyOf(items).entrySet()) {
            var item = entry.getKey();
            if (id != null && !BuiltInRegistries.ITEM.getKey(item).equals(id)) {
                continue;
            }
            if (!new ItemStack(item).is(ScrapRecipes.JUNK)) {
                continue;
            }
            var outputs = ScrapRecipes.of(access, item);
            if (outputs.isEmpty()) {
                continue;
            }
            var units = entry.getValue();
            items.remove(item);
            for (var output : outputs.get()) {
                items.merge(output.item(), output.count() * units, Long::sum);
            }
            scrapped += units;
        }
        if (scrapped > 0L) {
            CampScrapbox.set(player, CampScrapbox.fromItems(items));
            sound(player, pos, true);
        }
        notify(
            player,
            "junk.atomicdawn.scrapped",
            scrapped > 0L ? ChatFormatting.GREEN : ChatFormatting.GRAY,
            scrapped
        );
    }

    private static void sound(ServerPlayer player, BlockPos pos, boolean scrap) {
        player.level()
            .playSound(
                null,
                pos,
                scrap ? SoundEvents.ANVIL_USE : SoundEvents.ARMOR_EQUIP_IRON.value(),
                SoundSource.BLOCKS,
                scrap ? 0.5F : 0.8F,
                scrap ? 1.4F : 1F
            );
    }

    private static void notify(ServerPlayer player, String key, ChatFormatting color, long amount) {
        player.sendSystemMessage(Component.translatable(key, amount).withStyle(color), true);
    }
}
