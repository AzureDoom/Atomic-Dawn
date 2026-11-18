package mod.azure.atomicdawn.camp;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import mod.azure.atomicdawn.registry.ADBlocks;
import mod.azure.atomicdawn.registry.ADItems;
import mod.azure.atomicdawn.util.PlayerItems;

public final class CampSlotService {

    private record PlannedPart(
        BlockPos pos,
        BlockState state,
        CompoundTag data
    ) {}

    private record Planned(
        CampObject object,
        BlockPos pos,
        Direction facing,
        List<PlannedPart> parts
    ) {}

    public record RebuildResult(
        int placed,
        int blocked
    ) {}

    private CampSlotService() {}

    public static Rotation rotation(Direction from, Direction to) {
        var turns = Math.floorMod(to.get2DDataValue() - from.get2DDataValue(), 4);
        return switch (turns) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    private static Direction rotate(Direction direction, Rotation rotation) {
        return direction.getAxis().isHorizontal() ? rotation.rotate(direction) : direction;
    }

    public static void select(ServerPlayer player, int slotIndex) {
        var server = player.level().getServer();
        if (slotIndex < 0 || slotIndex >= CampPlacementService.maxSlots()) {
            return;
        }
        CampManager.select(server, player.getUUID(), slotIndex);
        CampPlacementService.notify(
            player,
            Component.translatable("camp.atomicdawn.slot.selected", slotIndex + 1),
            ChatFormatting.GREEN
        );
        CampSync.send(player);
    }

    public static boolean store(ServerPlayer player, CampSlot camp) {
        if (!CampPermissionService.canManage(player, camp)) {
            CampPlacementService.notify(
                player,
                Component.translatable("camp.atomicdawn.error.not_owner"),
                ChatFormatting.RED
            );
            return false;
        }
        if (!camp.deployed()) {
            return false;
        }
        var server = player.level().getServer();
        var level = server.getLevel(camp.dimension());
        if (level == null) {
            return false;
        }
        var toNorth = rotation(camp.facing(), Direction.NORTH);
        var deployed = camp.objects().stream().filter(CampObject::deployed).toList();
        var assigned = CampContents.assign(
            level,
            deployed,
            claimArea(camp),
            pos -> CampClaim.contains(camp.origin(), pos)
        );
        var objects = new ArrayList<CampObject>();
        var removals = new ArrayList<CampObject>();
        var leaving = new ArrayList<Entity>();
        for (var object : camp.objects()) {
            if (object.stored()) {
                objects.add(object);
                continue;
            }
            var parts = new ArrayList<PartSnapshot>();
            for (var part : object.parts()) {
                var state = level.getBlockState(part);
                if (!CampWorkshopService.matches(level, part, object)) {
                    continue;
                }
                var offset = part.subtract(camp.origin()).rotate(toNorth);
                var snapshot = PartSnapshot.capture(offset, state.rotate(toNorth));
                parts.add(snapshot.withData(CampContents.blockData(level, part)));
            }
            if (parts.isEmpty()) {
                continue;
            }
            var entities = assigned.getOrDefault(object.objectId(), List.of());
            var captured = entities.stream()
                .map(entity -> CampContents.capture(level, entity, camp.origin(), toNorth))
                .toList();
            var relative = object.pos().subtract(camp.origin()).rotate(toNorth);
            objects.add(object.asStored(relative, rotate(object.facing(), toNorth), parts, captured));
            removals.add(object);
            leaving.addAll(entities);
        }
        CampManager.put(server, camp.asStored(objects));
        for (var entity : leaving) {
            entity.discard();
        }
        for (var object : removals) {
            for (var part : object.parts()) {
                if (CampWorkshopService.matches(level, part, object)) {
                    CampContents.clear(level, part);
                }
            }
        }
        for (var i = removals.size() - 1; i >= 0; i--) {
            for (var part : removals.get(i).parts()) {
                if (CampWorkshopService.matches(level, part, removals.get(i))) {
                    level.setBlock(part, Blocks.AIR.defaultBlockState(), CampWorkshopService.REMOVE_FLAGS);
                }
            }
        }
        var moduleRecovered = false;
        if (level.getBlockState(camp.origin()).is(ADBlocks.CAMP_MODULE.get())) {
            level.setBlock(camp.origin(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            moduleRecovered = true;
        }
        if (moduleRecovered && !player.hasInfiniteMaterials()) {
            PlayerItems.give(player, new ItemStack(ADItems.CAMP_MODULE.get()));
        }
        level.playSound(null, camp.origin(), SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.6F, 1.2F);
        CampPlacementService.notify(
            player,
            Component.translatable("camp.atomicdawn.slot.stored", removals.size()),
            ChatFormatting.YELLOW
        );
        CampSync.changed(server);
        return true;
    }

    public static AABB claimArea(CampSlot camp) {
        var origin = camp.origin();
        var radius = CampClaim.radius();
        return new AABB(
            origin.getX() - radius,
            origin.getY() - CampClaim.down(),
            origin.getZ() - radius,
            origin.getX() + radius + 1,
            origin.getY() + CampClaim.up() + 1,
            origin.getZ() + radius + 1
        );
    }

    public static RebuildResult rebuild(ServerPlayer player, CampSlot camp, boolean partial, boolean quiet) {
        var server = player.level().getServer();
        var level = server.getLevel(camp.dimension());
        if (level == null || !camp.deployed() || camp.storedCount() == 0) {
            return new RebuildResult(0, 0);
        }
        if (!CampPermissionService.canManage(player, camp)) {
            return new RebuildResult(0, 0);
        }
        var fromNorth = rotation(Direction.NORTH, camp.facing());
        var claimed = new HashSet<BlockPos>();
        var ready = new ArrayList<Planned>();
        var blocked = 0;
        for (var object : camp.objects()) {
            if (!object.stored()) {
                continue;
            }
            var plan = plan(level, camp, object, fromNorth, claimed);
            if (plan.isEmpty()) {
                blocked++;
                continue;
            }
            ready.add(plan.get());
            for (var part : plan.get().parts()) {
                claimed.add(part.pos());
            }
        }
        if (blocked > 0 && !partial) {
            if (!quiet) {
                CampPlacementService.notify(
                    player,
                    Component.translatable("camp.atomicdawn.slot.rebuild_blocked", blocked),
                    ChatFormatting.RED
                );
            }
            CampSync.send(player);
            return new RebuildResult(0, blocked);
        }
        var placed = new HashMap<UUID, CampObject>();
        for (var planned : ready) {
            for (var part : planned.parts()) {
                level.setBlock(part.pos(), part.state(), CampWorkshopService.RESTORE_FLAGS);
            }
            for (var part : planned.parts()) {
                CampContents.applyBlockData(level, part.pos(), part.data());
            }
            var worldParts = planned.parts().stream().map(PlannedPart::pos).toList();
            for (var entity : planned.object().entities()) {
                CampContents.spawn(level, entity, camp.origin(), fromNorth, worldParts);
            }
            var deployed = planned.object().asDeployed(planned.pos(), planned.facing(), worldParts);
            placed.put(deployed.objectId(), deployed);
        }
        CampManager.update(
            server,
            camp.campId(),
            slot -> slot.withObjects(
                slot.objects().stream().map(existing -> placed.getOrDefault(existing.objectId(), existing)).toList()
            )
        );
        level.playSound(null, camp.origin(), SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.6F, 1.2F);
        if (!quiet || blocked > 0) {
            CampPlacementService.notify(
                player,
                Component.translatable("camp.atomicdawn.slot.rebuilt", ready.size(), blocked),
                blocked > 0 ? ChatFormatting.YELLOW : ChatFormatting.GREEN
            );
        }
        CampSync.changed(server);
        return new RebuildResult(ready.size(), blocked);
    }

    private static Optional<Planned> plan(
        ServerLevel level,
        CampSlot camp,
        CampObject object,
        Rotation fromNorth,
        HashSet<BlockPos> claimed
    ) {
        var parts = new ArrayList<PlannedPart>();
        for (var snapshot : object.snapshot()) {
            var rotated = snapshot.rotated(fromNorth);
            var state = rotated.state();
            if (state.isEmpty()) {
                return Optional.empty();
            }
            var pos = camp.origin().offset(rotated.offset());
            if (!CampClaim.contains(camp.origin(), pos) || !level.isLoaded(pos) || level.isOutsideBuildHeight(pos)) {
                return Optional.empty();
            }
            if (claimed.contains(pos) || pos.equals(camp.origin()) || !level.getBlockState(pos).canBeReplaced()) {
                return Optional.empty();
            }
            parts.add(new PlannedPart(pos, state.get(), rotated.data()));
        }
        if (parts.isEmpty()) {
            return Optional.empty();
        }
        var pos = camp.origin().offset(object.pos().rotate(fromNorth));
        return Optional.of(new Planned(object, pos, rotate(object.facing(), fromNorth), parts));
    }

    public static boolean delete(ServerPlayer player, CampSlot camp) {
        if (!camp.ownedBy(player.getUUID()) || camp.deployed()) {
            return false;
        }
        var server = player.level().getServer();
        var level = player.level();
        for (var object : camp.objects()) {
            CampWorkshopService.refund(player, object);
            for (var stack : CampContents.drops(level, object)) {
                PlayerItems.give(player, stack);
            }
        }
        CampManager.remove(server, camp.campId());
        CampPlacementService.notify(
            player,
            Component.translatable("camp.atomicdawn.slot.deleted", camp.slotIndex() + 1),
            ChatFormatting.YELLOW
        );
        CampSync.changed(server);
        return true;
    }
}
