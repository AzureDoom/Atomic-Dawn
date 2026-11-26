package mod.azure.atomicdawn.camp;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;

@SuppressWarnings("MagicConstant")
public final class CampStorageService {

    private CampStorageService() {}

    public static int maxStored() {
        return Math.clamp(
            AtomicDawn.CONFIG.common.settlement.camp.storage.CAMP_STORAGE_SLOTS,
            0,
            CampStorage.MAX_OBJECTS
        );
    }

    private static void message(ServerPlayer player, Component text, ChatFormatting color) {
        CampPlacementService.notify(player, text, color);
        CampSync.send(player);
    }

    private static void fail(ServerPlayer player, CampWorkshopService.Failure failure) {
        message(player, failure.message(), ChatFormatting.RED);
    }

    public static Component name(ServerPlayer player, CampBlueprint.Entry entry) {
        return CampBuildable.get(player.level().registryAccess(), entry.definition())
            .map(CampBuildable::name)
            .orElse(Component.literal(entry.definition().toString()));
    }

    public static void store(ServerPlayer player, BlockPos pos, UUID requested, int nonce) {
        if (CampWorkshopService.throttled(player, nonce)) {
            return;
        }
        var level = player.level();
        var server = level.getServer();
        var tracked = CampManager.objectAt(server, level.dimension(), pos);
        if (tracked.isEmpty()) {
            CampSync.send(player);
            return;
        }
        var camp = tracked.get().camp();
        var object = tracked.get().object();
        if (!CampPermissionService.canManage(player, camp)) {
            fail(player, CampWorkshopService.Failure.NOT_ALLOWED);
            return;
        }
        var reach = (double) CampWorkshopService.reach();
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > reach * reach) {
            fail(player, CampWorkshopService.Failure.TOO_FAR);
            return;
        }
        var storage = CampStorage.of(player);
        if (storage.size() >= maxStored()) {
            message(player, Component.translatable("camp.atomicdawn.storage.full", maxStored()), ChatFormatting.RED);
            return;
        }
        var anchor = object.pos();
        var toNorth = CampSlotService.rotation(object.facing(), Direction.NORTH);
        var parts = new ArrayList<PartSnapshot>();
        var live = new ArrayList<BlockPos>();
        for (var part : object.parts()) {
            if (!level.isLoaded(part) || !CampWorkshopService.matches(level, part, object)) {
                continue;
            }
            var snapshot = PartSnapshot.capture(
                part.subtract(anchor).rotate(toNorth),
                level.getBlockState(part).rotate(level, part, toNorth)
            );
            parts.add(snapshot.withData(CampContents.blockData(level, part)));
            live.add(part);
        }
        if (parts.isEmpty()) {
            CampWorkshopService.prune(level, camp);
            CampSync.send(player);
            return;
        }
        var bounds = new AABB(live.getFirst());
        for (var part : live) {
            bounds = bounds.minmax(new AABB(part));
        }
        var entities = CampContents.assign(level, List.of(object), bounds.inflate(1.0), _ -> true)
            .getOrDefault(object.objectId(), List.of())
            .stream()
            .filter(entity -> attached(entity, live))
            .toList();
        var captured = entities.stream().map(entity -> CampContents.capture(level, entity, anchor, toNorth)).toList();
        var entry = new CampBlueprint.Entry(
            object.definition(),
            BlockPos.ZERO,
            Direction.NORTH,
            object.mode(),
            parts,
            captured
        );
        CampManager.update(server, camp.campId(), slot -> slot.withoutObject(object.objectId()));
        for (var entity : entities) {
            entity.discard();
        }
        for (var part : live) {
            CampContents.clear(level, part);
        }
        for (var part : live) {
            if (CampWorkshopService.matches(level, part, object)) {
                level.setBlock(part, Blocks.AIR.defaultBlockState(), CampWorkshopService.REMOVE_FLAGS);
            }
        }
        var id = requested == null || storage.has(requested) ? UUID.randomUUID() : requested;
        player.setData(CampAttachments.STORAGE.get(), storage.with(new CampStorage.Stored(id, entry)));
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 0.8F);
        message(
            player,
            Component.translatable("camp.atomicdawn.storage.stored", name(player, entry)),
            ChatFormatting.GREEN
        );
    }

    private static boolean attached(Entity entity, List<BlockPos> parts) {
        if (entity instanceof PowerArmorEntity) {
            return true;
        }
        if (entity instanceof HangingEntity hanging) {
            return parts.contains(hanging.getPos().relative(hanging.getDirection().getOpposite()));
        }
        return parts.contains(entity.blockPosition());
    }

    public static void place(ServerPlayer player, UUID id, BlockPos target, Direction facing, int nonce) {
        if (CampWorkshopService.throttled(player, nonce)) {
            return;
        }
        var level = player.level();
        var server = level.getServer();
        var storage = CampStorage.of(player);
        var stored = storage.get(id);
        if (stored.isEmpty()) {
            message(player, Component.translatable("camp.atomicdawn.storage.unknown"), ChatFormatting.RED);
            return;
        }
        var found = CampWorkshopService.refreshed(level, CampManager.claimAt(server, level.dimension(), target));
        if (found.isEmpty()) {
            fail(
                player,
                CampManager.active(server, player.getUUID()).isEmpty()
                    ? CampWorkshopService.Failure.NO_CAMP
                    : CampWorkshopService.Failure.OUTSIDE_CLAIM
            );
            return;
        }
        var camp = found.get();
        if (!CampPermissionService.canBuild(player, camp)) {
            fail(player, CampWorkshopService.Failure.NOT_ALLOWED);
            return;
        }
        var reach = (double) CampWorkshopService.reach();
        if (player.distanceToSqr(Vec3.atCenterOf(target)) > reach * reach) {
            fail(player, CampWorkshopService.Failure.TOO_FAR);
            return;
        }
        var placed = CampBlueprintService.placeEntries(
            player,
            camp,
            List.of(stored.get().entry()),
            target.immutable(),
            facing,
            true,
            true
        );
        if (placed.isEmpty()) {
            return;
        }
        player.setData(CampAttachments.STORAGE.get(), CampStorage.of(player).without(id));
        level.playSound(null, target, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.4F, 1.4F);
        message(
            player,
            Component.translatable("camp.atomicdawn.storage.placed", name(player, stored.get().entry())),
            ChatFormatting.GREEN
        );
    }

    public static void scrap(ServerPlayer player, UUID id) {
        var storage = CampStorage.of(player);
        var stored = storage.get(id);
        if (stored.isEmpty()) {
            CampSync.send(player);
            return;
        }
        var entry = stored.get().entry();
        player.setData(CampAttachments.STORAGE.get(), storage.without(id));
        var block = CampBuildable.get(player.level().registryAccess(), entry.definition())
            .map(definition -> BuiltInRegistries.BLOCK.getKey(definition.block()))
            .orElseGet(() -> entry.parts().isEmpty() ? entry.definition() : entry.parts().getFirst().block());
        var object = CampObject.placed(
            id,
            entry.definition(),
            block,
            BlockPos.ZERO,
            Direction.NORTH,
            entry.mode(),
            0,
            List.of()
        );
        var returned = player.hasInfiniteMaterials()
            ? List.<ItemStack>of()
            : CampWorkshopService.giveBack(player, object, entry.parts(), entry.pos(), entry.entities());
        message(
            player,
            Component.translatable("camp.atomicdawn.storage.scrapped", name(player, entry), returned.size()),
            ChatFormatting.YELLOW
        );
    }
}
