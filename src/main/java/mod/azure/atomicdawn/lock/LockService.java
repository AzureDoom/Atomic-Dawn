package mod.azure.atomicdawn.lock;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.registry.ADItems;
import mod.azure.atomicdawn.util.PlayerItems;

public final class LockService {

    public static final TagKey<Block> LOCKABLE = TagKey.create(Registries.BLOCK, AtomicDawn.id("lockable"));

    public static final TagKey<Block> UNLOCKABLE = TagKey.create(Registries.BLOCK, AtomicDawn.id("unlockable"));

    public record Located(
        BlockPos pos,
        Lock lock
    ) {}

    private LockService() {}

    public static boolean enabled() {
        return AtomicDawn.CONFIG.common.settlement.locks.locking.LOCKS_ENABLED;
    }

    public static boolean lockable(Level level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.is(UNLOCKABLE)) {
            return false;
        }
        return state.is(LOCKABLE) || level.getBlockEntity(pos) instanceof Container;
    }

    public static BlockPos anchor(BlockState state, BlockPos pos) {
        if (
            state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER
        ) {
            return pos.below();
        }
        return pos.immutable();
    }

    public static Optional<Located> find(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        var found = at(level, anchor(state, pos));
        if (found.isPresent()) {
            return found;
        }
        if (
            state.getBlock() instanceof ChestBlock && state.hasProperty(ChestBlock.TYPE)
                && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE
        ) {
            return at(level, pos.relative(ChestBlock.getConnectedDirection(state)));
        }
        return Optional.empty();
    }

    private static Optional<Located> at(Level level, BlockPos pos) {
        var lock = map(level, pos).get(pos);
        if (lock == null) {
            return Optional.empty();
        }
        var state = level.getBlockState(pos);
        if (!lockable(level, pos, state) || !anchor(state, pos).equals(pos)) {
            put(level, pos, null);
            return Optional.empty();
        }
        return Optional.of(new Located(pos, lock));
    }

    private static LockMap map(Level level, BlockPos pos) {
        var chunk = level.getChunkAt(pos);
        return chunk.hasData(ADAttachments.LOCKS.get()) ? chunk.getData(ADAttachments.LOCKS.get()) : LockMap.EMPTY;
    }

    public static void put(Level level, BlockPos pos, @Nullable Lock lock) {
        var chunk = level.getChunkAt(pos);
        var type = ADAttachments.LOCKS.get();
        var current = chunk.hasData(type) ? chunk.getData(type) : LockMap.EMPTY;
        var updated = lock == null ? current.without(pos) : current.with(pos, lock);
        if (updated == current) {
            return;
        }
        if (updated.isEmpty()) {
            chunk.removeData(type);
        } else {
            chunk.setData(type, updated);
        }
        chunk.markUnsaved();
    }

    public static boolean canOpen(Player player, Lock lock) {
        return lock.grants(player.getUUID()) || creativeBypass(player);
    }

    public static boolean canRemove(Player player, Lock lock) {
        return lock.ownedBy(player.getUUID()) || creativeBypass(player);
    }

    private static boolean creativeBypass(Player player) {
        return AtomicDawn.CONFIG.common.settlement.locks.locking.LOCKS_CREATIVE_BYPASS && player.isCreative();
    }

    public static boolean install(ServerPlayer player, BlockPos pos, ItemStack stack, LockLevel lockLevel) {
        var level = player.level();
        var state = level.getBlockState(pos);
        if (!lockable(level, pos, state)) {
            notify(player, "lock.atomicdawn.not_lockable", ChatFormatting.RED);
            return false;
        }
        if (find(level, pos).isPresent()) {
            notify(player, "lock.atomicdawn.already_locked", ChatFormatting.RED);
            return false;
        }
        var anchor = anchor(state, pos);
        put(level, anchor, Lock.owned(lockLevel, player.getUUID()));
        if (!player.hasInfiniteMaterials()) {
            stack.shrink(1);
        }
        level.playSound(null, anchor, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.7F, 1.4F);
        notify(player, "lock.atomicdawn.installed", ChatFormatting.GREEN, lockLevel.component());
        return true;
    }

    public static void removeByHand(ServerPlayer player, Located located) {
        var level = player.level();
        put(level, located.pos(), null);
        if (located.lock().owner().isPresent() && !player.hasInfiniteMaterials()) {
            PlayerItems.give(player, padlock(located.lock().level()));
        }
        level.playSound(null, located.pos(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.7F, 1.2F);
        notify(player, "lock.atomicdawn.removed", ChatFormatting.YELLOW, located.lock().level().component());
    }

    public static void onBroken(Level level, Located located, @Nullable Player breaker) {
        put(level, located.pos(), null);
        var dropsPadlock = located.lock().owner().isPresent()
            && (breaker == null || !breaker.hasInfiniteMaterials());
        if (dropsPadlock) {
            Block.popResource(level, located.pos(), padlock(located.lock().level()));
        }
    }

    public static void grantAccess(Level level, BlockPos pos, UUID id) {
        var lock = map(level, pos).get(pos);
        if (lock != null) {
            put(level, pos, lock.withAccess(id));
        }
    }

    public static ItemStack padlock(LockLevel level) {
        return new ItemStack(ADItems.padlock(level));
    }

    public static void notify(ServerPlayer player, String key, ChatFormatting color, Object... args) {
        player.sendSystemMessage(Component.translatable(key, args).withStyle(color), true);
    }
}
