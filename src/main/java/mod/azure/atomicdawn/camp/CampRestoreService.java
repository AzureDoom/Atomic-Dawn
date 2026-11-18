package mod.azure.atomicdawn.camp;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.UUID;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.network.ADNetwork;
import mod.azure.atomicdawn.network.CampRestorePayload;
import mod.azure.atomicdawn.util.PlayerItems;

public final class CampRestoreService {

    public static final TagKey<Block> CLEARABLE = TagKey.create(Registries.BLOCK, AtomicDawn.id("camp_clearable"));

    private CampRestoreService() {}

    static boolean clearable(ServerLevel level, ServerPlayer player, CampSlot camp, BlockPos pos, BlockState state) {
        if (!state.is(CLEARABLE) || level.getBlockEntity(pos) != null || camp.objectAt(pos).isPresent()) {
            return false;
        }
        if (state.getDestroySpeed(level, pos) < 0) {
            return false;
        }
        return !NeoForge.EVENT_BUS.post(new BreakBlockEvent(level, pos, state, player)).isCanceled();
    }

    public static void offer(ServerPlayer player, CampSlot camp, boolean deploying) {
        var plan = CampSlotService.restorePlan(player, camp);
        if (plan.blocked() == 0) {
            CampSlotService.rebuild(player, camp, false, deploying);
            clear(player);
            return;
        }
        send(player, camp.campId(), plan);
        CampPlacementService.notify(
            player,
            Component.translatable("camp.atomicdawn.restore.blocked", plan.blocked()),
            ChatFormatting.YELLOW
        );
        CampSync.send(player);
    }

    public static void partial(ServerPlayer player, CampSlot camp) {
        var result = CampSlotService.rebuild(player, camp, true, false);
        var updated = CampManager.get(player.level().getServer(), camp.campId());
        if (result.blocked() > 0 && updated.isPresent()) {
            send(player, camp.campId(), CampSlotService.restorePlan(player, updated.get()));
        } else {
            clear(player);
        }
    }

    public static void clearAndRestore(ServerPlayer player, CampSlot camp) {
        if (!CampPermissionService.canManage(player, camp) || !camp.deployed()) {
            return;
        }
        var server = player.level().getServer();
        var level = server.getLevel(camp.dimension());
        if (level == null) {
            return;
        }
        var plan = CampSlotService.restorePlan(player, camp);
        if (plan.blocked() == 0) {
            CampSlotService.rebuild(player, camp, false, false);
            clear(player);
            return;
        }
        if (!plan.clearable()) {
            send(player, camp.campId(), plan);
            CampPlacementService.notify(
                player,
                Component.translatable("camp.atomicdawn.restore.not_clearable"),
                ChatFormatting.RED
            );
            return;
        }
        var tool = player.getMainHandItem();
        var cleared = 0;
        for (var blocker : plan.blockers()) {
            var pos = blocker.pos();
            var state = level.getBlockState(pos);
            if (state.canBeReplaced() || !clearable(level, player, camp, pos, state)) {
                continue;
            }
            if (!player.hasInfiniteMaterials()) {
                for (var drop : Block.getDrops(state, level, pos, null, player, tool)) {
                    PlayerItems.give(player, drop);
                }
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.levelEvent(2001, pos, Block.getId(state));
            cleared++;
        }
        var updated = CampManager.get(server, camp.campId());
        if (updated.isEmpty()) {
            return;
        }
        var result = CampSlotService.rebuild(player, updated.get(), false, false);
        if (result.blocked() > 0) {
            var after = CampManager.get(server, camp.campId());
            after.ifPresent(slot -> send(player, slot.campId(), CampSlotService.restorePlan(player, slot)));
            return;
        }
        clear(player);
        CampPlacementService.notify(
            player,
            Component.translatable("camp.atomicdawn.restore.cleared", cleared, result.placed()),
            ChatFormatting.GREEN
        );
    }

    private static void send(ServerPlayer player, UUID campId, CampSlotService.RestorePlan plan) {
        ADNetwork.sendTo(player, CampRestorePayload.open(campId, plan));
    }

    public static void clear(ServerPlayer player) {
        ADNetwork.sendTo(player, CampRestorePayload.closed());
    }
}
