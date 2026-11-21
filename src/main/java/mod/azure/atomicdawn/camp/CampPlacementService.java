package mod.azure.atomicdawn.camp;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.registry.ADBlocks;

public final class CampPlacementService {

    private CampPlacementService() {}

    public static boolean enabled() {
        return AtomicDawn.CONFIG.camp.CAMP_ENABLED;
    }

    public static boolean dimensionAllowed(ServerLevel level) {
        var allowed = AtomicDawn.CONFIG.camp.CAMP_DIMENSIONS;
        return allowed.length == 0 || Arrays.asList(allowed).contains(level.dimension().identifier().toString());
    }

    public static Optional<Component> validateDeploy(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (!enabled()) {
            return Optional.of(Component.translatable("camp.atomicdawn.error.disabled"));
        }
        if (!dimensionAllowed(level)) {
            return Optional.of(Component.translatable("camp.atomicdawn.error.dimension"));
        }
        if (!level.getWorldBorder().isWithinBounds(pos)) {
            return Optional.of(Component.translatable("camp.atomicdawn.error.border"));
        }
        var server = level.getServer();
        var owned = CampManager.owned(server, player.getUUID());
        if (owned.stream().filter(CampSlot::deployed).count() >= maxActive()) {
            return Optional.of(Component.translatable("camp.atomicdawn.error.active_exists"));
        }
        var selected = selectedSlot(server, player.getUUID());
        if (CampManager.inSlot(server, player.getUUID(), selected).isEmpty() && owned.size() >= maxSlots()) {
            return Optional.of(Component.translatable("camp.atomicdawn.error.slots_full", maxSlots()));
        }
        for (var other : CampManager.inDimension(server, level.dimension())) {
            if (CampClaim.tooClose(other.origin(), pos)) {
                return Optional.of(
                    Component.translatable(
                        "camp.atomicdawn.error.too_close",
                        CampClaim.spacing(),
                        (int) Math.sqrt(CampClaim.horizontalDistanceSqr(other.origin(), pos))
                    )
                );
            }
        }
        return Optional.empty();
    }

    public static int maxSlots() {
        return Math.clamp(AtomicDawn.CONFIG.camp.CAMP_SLOTS, 1, 10);
    }

    public static int maxActive() {
        return 1;
    }

    public static int selectedSlot(MinecraftServer server, UUID owner) {
        return Math.clamp(CampManager.selectedSlot(server, owner), 0, maxSlots() - 1);
    }

    public static void deploy(ServerPlayer player, ServerLevel level, BlockPos pos, Direction facing) {
        var failure = validateDeploy(player, level, pos);
        if (failure.isPresent()) {
            notify(player, failure.get(), ChatFormatting.RED);
            return;
        }
        var server = level.getServer();
        var slotIndex = selectedSlot(server, player.getUUID());
        var existing = CampManager.inSlot(server, player.getUUID(), slotIndex);
        var camp = existing.map(slot -> slot.deployedAt(level.dimension(), pos, facing))
            .orElseGet(
                () -> new CampSlot(
                    UUID.randomUUID(),
                    player.getUUID(),
                    player.getScoreboardName(),
                    slotIndex,
                    "",
                    level.dimension(),
                    pos,
                    facing,
                    true,
                    AtomicDawn.CONFIG.camp.CAMP_BUDGET,
                    level.getGameTime(),
                    CampSlot.CURRENT_VERSION,
                    List.of(),
                    CampAccess.DEFAULT
                )
            );
        CampManager.put(server, camp);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6F, 1.4F);
        notify(
            player,
            Component.translatable("camp.atomicdawn.deployed", CampClaim.radius()),
            ChatFormatting.GREEN
        );
        if (camp.storedCount() > 0) {
            CampRestoreService.offer(player, camp, true);
        }
        CampSync.changed(server);
        CampManager.get(server, camp.campId());
    }

    public static void moveModule(ServerPlayer player, BlockPos target, int nonce) {
        if (CampWorkshopService.throttled(player, nonce)) {
            return;
        }
        var level = player.level();
        var server = level.getServer();
        var found = CampManager.claimAt(server, level.dimension(), target);
        if (found.isEmpty()) {
            moveFailed(player, CampWorkshopService.Failure.OUTSIDE_CLAIM.message());
            return;
        }
        var camp = found.get();
        if (!CampPermissionService.canManage(player, camp)) {
            moveFailed(player, Component.translatable("camp.atomicdawn.error.not_owner"));
            return;
        }
        var origin = camp.origin();
        if (target.equals(origin)) {
            CampSync.send(player);
            return;
        }
        var reach = (double) CampWorkshopService.reach();
        if (player.distanceToSqr(Vec3.atCenterOf(target)) > reach * reach) {
            moveFailed(player, CampWorkshopService.Failure.TOO_FAR.message());
            return;
        }
        if (!level.getWorldBorder().isWithinBounds(target)) {
            moveFailed(player, Component.translatable("camp.atomicdawn.error.border"));
            return;
        }
        if (
            !level.isLoaded(target)
                || level.isOutsideBuildHeight(target)
                || camp.objectAt(target).isPresent()
                || !level.getBlockState(target).canBeReplaced()
        ) {
            moveFailed(player, CampWorkshopService.Failure.OCCUPIED.message());
            return;
        }
        for (var other : CampManager.inDimension(server, level.dimension())) {
            if (!other.campId().equals(camp.campId()) && CampClaim.tooClose(other.origin(), target)) {
                moveFailed(
                    player,
                    Component.translatable(
                        "camp.atomicdawn.error.too_close",
                        CampClaim.spacing(),
                        (int) Math.sqrt(CampClaim.horizontalDistanceSqr(other.origin(), target))
                    )
                );
                return;
            }
        }
        var outside = 0;
        for (var object : camp.objects()) {
            if (object.deployed() && object.parts().stream().anyMatch(part -> !CampClaim.contains(target, part))) {
                outside++;
            }
        }
        if (outside > 0) {
            moveFailed(player, Component.translatable("camp.atomicdawn.module.move_outside", outside));
            return;
        }
        var toNorth = CampSlotService.rotation(camp.facing(), Direction.NORTH);
        var delta = origin.subtract(target).rotate(toNorth);
        var objects = camp.objects()
            .stream()
            .map(object -> object.stored() ? object.shifted(delta) : object)
            .toList();
        var current = level.getBlockState(origin);
        var state = current.is(ADBlocks.CAMP_MODULE.get())
            ? current
            : ADBlocks.CAMP_MODULE.get().defaultBlockState().setValue(CampModuleBlock.FACING, camp.facing());
        CampManager.put(server, camp.movedTo(target, objects));
        level.setBlock(target, state, Block.UPDATE_ALL);
        if (current.is(ADBlocks.CAMP_MODULE.get())) {
            level.setBlock(origin, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.playSound(null, target, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.6F, 1.2F);
        notify(player, Component.translatable("camp.atomicdawn.module.moved"), ChatFormatting.GREEN);
        CampSync.changed(server);
    }

    private static void moveFailed(ServerPlayer player, Component message) {
        notify(player, message, ChatFormatting.RED);
        CampSync.send(player);
    }

    public static void dissolve(ServerLevel level, CampSlot camp) {
        var server = level.getServer();
        CampManager.put(server, camp.asStored(camp.objects().stream().filter(CampObject::stored).toList()));
        var owner = server.getPlayerList().getPlayer(camp.owner());
        if (owner != null) {
            notify(owner, Component.translatable("camp.atomicdawn.dissolved"), ChatFormatting.YELLOW);
        }
        CampSync.changed(server);
    }

    public static void notify(ServerPlayer player, Component message, ChatFormatting color) {
        player.sendSystemMessage(message.copy().withStyle(color), true);
    }
}
