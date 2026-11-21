package mod.azure.atomicdawn.camp;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.common.util.BlockSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import mod.azure.atomicdawn.AtomicDawn;

public final class CampBlueprintService {

    private record PlannedPart(
        BlockPos pos,
        BlockState state,
        CompoundTag data
    ) {}

    private record PlannedObject(
        CampBlueprint.Entry entry,
        CampBuildable definition,
        BlockPos pos,
        Direction facing,
        List<PlannedPart> parts
    ) {

        BlockState primary() {
            for (var part : parts) {
                if (part.pos().equals(pos)) {
                    return part.state();
                }
            }
            return parts.getFirst().state();
        }
    }

    private CampBlueprintService() {}

    public static int maxBlueprints() {
        return Math.clamp(AtomicDawn.CONFIG.camp.CAMP_BLUEPRINT_SLOTS, 0, CampBlueprintLibrary.MAX_BLUEPRINTS);
    }

    private static Direction rotate(Direction direction, Rotation rotation) {
        return direction.getAxis().isHorizontal() ? rotation.rotate(direction) : direction;
    }

    private static void message(ServerPlayer player, Component text, ChatFormatting color) {
        CampPlacementService.notify(player, text, color);
        CampSync.send(player);
    }

    private static void fail(ServerPlayer player, CampWorkshopService.Failure failure) {
        message(player, failure.message(), ChatFormatting.RED);
    }

    public static void save(
        ServerPlayer player,
        String rawName,
        CampBlueprint.Kind kind,
        BlockPos from,
        BlockPos to,
        Direction facing,
        int nonce
    ) {
        if (CampWorkshopService.throttled(player, nonce)) {
            return;
        }
        if (maxBlueprints() <= 0) {
            message(player, Component.translatable("camp.atomicdawn.blueprint.disabled"), ChatFormatting.RED);
            return;
        }
        var name = rawName.trim();
        var key = CampBlueprints.slug(name);
        if (key.isEmpty()) {
            message(player, Component.translatable("camp.atomicdawn.blueprint.invalid_name"), ChatFormatting.RED);
            return;
        }
        var level = player.level();
        var server = level.getServer();
        var probe = kind == CampBlueprint.Kind.CAMP ? player.blockPosition() : from;
        var found = CampWorkshopService.refreshed(level, CampManager.claimAt(server, level.dimension(), probe));
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
        if (kind == CampBlueprint.Kind.PREFAB) {
            var other = CampManager.claimAt(server, level.dimension(), to);
            if (other.isEmpty() || !other.get().campId().equals(camp.campId())) {
                message(player, Component.translatable("camp.atomicdawn.blueprint.selection_camp"), ChatFormatting.RED);
                return;
            }
        }
        var library = CampBlueprintLibrary.of(player);
        if (!library.has(key) && library.size() >= maxBlueprints()) {
            message(
                player,
                Component.translatable("camp.atomicdawn.blueprint.full", maxBlueprints()),
                ChatFormatting.RED
            );
            return;
        }
        var minX = Math.min(from.getX(), to.getX());
        var minY = Math.min(from.getY(), to.getY());
        var minZ = Math.min(from.getZ(), to.getZ());
        var maxX = Math.max(from.getX(), to.getX());
        var maxY = Math.max(from.getY(), to.getY());
        var maxZ = Math.max(from.getZ(), to.getZ());
        Predicate<BlockPos> inside;
        AABB area;
        BlockPos anchor;
        Rotation toNorth;
        if (kind == CampBlueprint.Kind.CAMP) {
            inside = pos -> CampClaim.contains(camp.origin(), pos);
            area = CampSlotService.claimArea(camp);
            anchor = camp.origin();
            toNorth = CampSlotService.rotation(camp.facing(), Direction.NORTH);
        } else {
            inside = pos -> pos.getX() >= minX && pos.getX() <= maxX && pos.getY() >= minY && pos.getY() <= maxY
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
            area = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
            anchor = new BlockPos(Math.floorDiv(minX + maxX, 2), minY, Math.floorDiv(minZ + maxZ, 2));
            var horizontal = facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
            toNorth = CampSlotService.rotation(horizontal, Direction.NORTH);
        }
        var chosen = camp.objects()
            .stream()
            .filter(CampObject::deployed)
            .filter(object -> object.parts().stream().anyMatch(inside))
            .limit(CampBlueprint.MAX_ENTRIES)
            .toList();
        var contents = player.hasInfiniteMaterials();
        Map<UUID, List<Entity>> assigned = contents ? CampContents.assign(level, chosen, area, inside) : Map.of();
        var entries = new ArrayList<CampBlueprint.Entry>();
        for (var object : chosen) {
            var parts = new ArrayList<PartSnapshot>();
            for (var part : object.parts()) {
                if (!CampWorkshopService.matches(level, part, object)) {
                    continue;
                }
                var snapshot = PartSnapshot.capture(
                    part.subtract(anchor).rotate(toNorth),
                    level.getBlockState(part).rotate(toNorth)
                );
                parts.add(contents ? snapshot.withData(CampContents.blockData(level, part)) : snapshot);
            }
            if (parts.isEmpty()) {
                continue;
            }
            var entities = assigned.getOrDefault(object.objectId(), List.of())
                .stream()
                .map(entity -> CampContents.capture(level, entity, anchor, toNorth))
                .toList();
            entries.add(
                new CampBlueprint.Entry(
                    object.definition(),
                    object.pos().subtract(anchor).rotate(toNorth),
                    rotate(object.facing(), toNorth),
                    object.mode(),
                    parts,
                    entities
                )
            );
        }
        if (entries.isEmpty()) {
            message(player, Component.translatable("camp.atomicdawn.blueprint.empty"), ChatFormatting.RED);
            return;
        }
        var blueprint = new CampBlueprint(name, kind, entries);
        player.setData(CampAttachments.BLUEPRINTS.get(), library.with(key, blueprint));
        if (AtomicDawn.CONFIG.camp.CAMP_BLUEPRINT_EXPORT) {
            var id = CampBlueprintFiles.exportId(player.getScoreboardName(), key);
            CampBlueprintFiles.write(server, id, blueprint)
                .ifPresent(path -> AtomicDawn.LOGGER.info("Exported C.A.M.P. blueprint {} to {}", id, path));
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8F, 1.0F);
        message(
            player,
            Component.translatable("camp.atomicdawn.blueprint.saved", blueprint.name(), entries.size()),
            ChatFormatting.GREEN
        );
    }

    public static void place(ServerPlayer player, Identifier id, BlockPos target, Direction facing, int nonce) {
        if (CampWorkshopService.throttled(player, nonce)) {
            return;
        }
        var level = player.level();
        var server = level.getServer();
        var blueprint = CampBlueprints.get(player, id);
        if (blueprint.isEmpty()) {
            message(player, Component.translatable("camp.atomicdawn.blueprint.unknown"), ChatFormatting.RED);
            return;
        }
        if (!CampPlans.unlocked(player, blueprint.get())) {
            fail(player, CampWorkshopService.Failure.PLAN_LOCKED);
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
        var whole = blueprint.get().kind() == CampBlueprint.Kind.CAMP;
        var anchor = whole ? camp.origin() : target.immutable();
        var reach = (double) CampWorkshopService.reach();
        if (!whole && player.distanceToSqr(Vec3.atCenterOf(anchor)) > reach * reach) {
            fail(player, CampWorkshopService.Failure.TOO_FAR);
            return;
        }
        var placed = placeEntries(
            player,
            camp,
            blueprint.get().entries(),
            anchor,
            facing,
            false,
            player.hasInfiniteMaterials()
        );
        if (placed.isEmpty()) {
            return;
        }
        var objects = placed.get();
        level.playSound(null, anchor, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.4F, 1.4F);
        message(
            player,
            Component.translatable("camp.atomicdawn.blueprint.placed", blueprint.get().name(), objects.size()),
            ChatFormatting.GREEN
        );
    }

    static Optional<List<CampObject>> placeEntries(
        ServerPlayer player,
        CampSlot camp,
        List<CampBlueprint.Entry> entries,
        BlockPos anchor,
        Direction facing,
        boolean free,
        boolean contents
    ) {
        var level = player.level();
        var server = level.getServer();
        var horizontal = facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
        var fromNorth = CampSlotService.rotation(Direction.NORTH, horizontal);
        var access = level.registryAccess();
        var planned = new ArrayList<PlannedObject>();
        var claimed = new HashSet<BlockPos>();
        var cost = new ArrayList<SizedIngredient>();
        var counts = new HashMap<Identifier, Integer>();
        var budget = 0;
        for (var entry : entries) {
            var definition = CampBuildable.get(access, entry.definition());
            if (definition.isEmpty()) {
                fail(player, CampWorkshopService.Failure.UNKNOWN_DEFINITION);
                return Optional.empty();
            }
            if (!free && !CampPlans.unlocked(player, definition.get())) {
                fail(player, CampWorkshopService.Failure.PLAN_LOCKED);
                return Optional.empty();
            }
            var parts = new ArrayList<PlannedPart>();
            for (var part : entry.parts()) {
                var rotated = part.rotated(fromNorth);
                var state = rotated.state();
                if (state.isEmpty()) {
                    fail(player, CampWorkshopService.Failure.UNKNOWN_DEFINITION);
                    return Optional.empty();
                }
                var pos = anchor.offset(rotated.offset());
                if (!CampClaim.contains(camp.origin(), pos)) {
                    fail(player, CampWorkshopService.Failure.OUTSIDE_CLAIM);
                    return Optional.empty();
                }
                if (
                    !level.isLoaded(pos)
                        || level.isOutsideBuildHeight(pos)
                        || pos.equals(camp.origin())
                        || !claimed.add(pos)
                        || !level.getBlockState(pos).canBeReplaced()
                ) {
                    fail(player, CampWorkshopService.Failure.OCCUPIED);
                    return Optional.empty();
                }
                parts.add(new PlannedPart(pos, state.get(), rotated.data()));
            }
            if (parts.isEmpty()) {
                continue;
            }
            budget += definition.get().budget();
            cost.addAll(definition.get().cost());
            counts.merge(entry.definition(), 1, Integer::sum);
            planned.add(
                new PlannedObject(
                    entry,
                    definition.get(),
                    anchor.offset(entry.pos().rotate(fromNorth)),
                    rotate(entry.facing(), fromNorth),
                    parts
                )
            );
        }
        if (planned.isEmpty()) {
            message(player, Component.translatable("camp.atomicdawn.blueprint.empty"), ChatFormatting.RED);
            return Optional.empty();
        }
        if (camp.budgetUsed() + budget > camp.budgetLimit()) {
            fail(player, CampWorkshopService.Failure.BUDGET);
            return Optional.empty();
        }
        for (var count : counts.entrySet()) {
            var limit = CampBuildable.get(access, count.getKey()).map(CampBuildable::limit).orElse(0);
            if (limit > 0 && camp.count(count.getKey()) + count.getValue() > limit) {
                fail(player, CampWorkshopService.Failure.LIMIT);
                return Optional.empty();
            }
        }
        var materials = free ? List.<SizedIngredient>of() : CampBlueprint.merge(cost);
        if (!CampMaterials.has(player, materials)) {
            fail(player, CampWorkshopService.Failure.MATERIALS);
            return Optional.empty();
        }
        var snapshots = new HashMap<BlockPos, BlockSnapshot>();
        for (var object : planned) {
            for (var part : object.parts()) {
                snapshots.put(part.pos(), BlockSnapshot.create(level.dimension(), level, part.pos()));
            }
        }
        for (var object : planned) {
            for (var part : object.parts()) {
                level.setBlock(part.pos(), part.state(), CampWorkshopService.RESTORE_FLAGS);
            }
        }
        for (var object : planned) {
            var primary = object.parts().getFirst().pos();
            if (CampWorkshopService.cancelledByOthers(player, level, primary, snapshots.get(primary))) {
                for (var pos : snapshots.keySet()) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), CampWorkshopService.REMOVE_FLAGS);
                }
                for (var snapshot : snapshots.values()) {
                    snapshot.restore();
                }
                fail(player, CampWorkshopService.Failure.PROTECTED);
                return Optional.empty();
            }
        }
        CampMaterials.consume(player, materials);
        var objects = new ArrayList<CampObject>();
        for (var object : planned) {
            var worldParts = object.parts().stream().map(PlannedPart::pos).toList();
            if (contents) {
                for (var part : object.parts()) {
                    CampContents.applyBlockData(level, part.pos(), part.data());
                }
                for (var entity : object.entry().entities()) {
                    CampContents.spawn(level, entity, anchor, fromNorth, worldParts);
                }
            }
            objects.add(
                CampObject.placed(
                    UUID.randomUUID(),
                    object.entry().definition(),
                    BuiltInRegistries.BLOCK.getKey(object.primary().getBlock()),
                    object.pos(),
                    object.facing(),
                    object.entry().mode(),
                    object.definition().budget(),
                    worldParts
                )
            );
        }
        CampManager.update(server, camp.campId(), slot -> {
            var list = new ArrayList<>(slot.objects());
            list.addAll(objects);
            return slot.withObjects(list);
        });
        return Optional.of(objects);
    }

    public static void delete(ServerPlayer player, Identifier id) {
        if (!CampBlueprints.personal(id)) {
            return;
        }
        var library = CampBlueprintLibrary.of(player);
        var blueprint = library.get(id.getPath());
        if (blueprint.isEmpty()) {
            return;
        }
        player.setData(CampAttachments.BLUEPRINTS.get(), library.without(id.getPath()));
        message(
            player,
            Component.translatable("camp.atomicdawn.blueprint.deleted", blueprint.get().name()),
            ChatFormatting.YELLOW
        );
    }
}
