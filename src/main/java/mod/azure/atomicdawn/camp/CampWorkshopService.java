package mod.azure.atomicdawn.camp;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.util.PlayerItems;

public final class CampWorkshopService {

    public enum Failure {

        NO_CAMP,
        WRONG_DIMENSION,
        NOT_ALLOWED,
        UNKNOWN_DEFINITION,
        PLAN_LOCKED,
        OUTSIDE_CLAIM,
        TOO_FAR,
        OCCUPIED,
        MODE_NOT_ALLOWED,
        NO_SUPPORT,
        NO_SNAP,
        BUDGET,
        LIMIT,
        MATERIALS,
        PLACEMENT_FAILED,
        PROTECTED;

        public Component message() {
            return Component.translatable("camp.atomicdawn.workshop.error." + name().toLowerCase(Locale.ROOT));
        }
    }

    public static final int REMOVE_FLAGS =
        Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    public static final int RESTORE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private static final ThreadLocal<Boolean> WORKSHOP_PLACING = ThreadLocal.withInitial(() -> false);

    private static final long PLACE_COOLDOWN = 3L;

    private static final int SCAN_HORIZONTAL = 2;

    private static final int SCAN_DOWN = 1;

    private static final int SCAN_UP = 3;

    private static final Map<UUID, Long> LAST_REQUEST = new ConcurrentHashMap<>();

    private static final Map<UUID, Integer> LAST_NONCE = new ConcurrentHashMap<>();

    private CampWorkshopService() {}

    public static int reach() {
        return AtomicDawn.CONFIG.camp.CAMP_BUILD_REACH;
    }

    private static boolean throttled(ServerPlayer player, int nonce) {
        var now = player.level().getGameTime();
        var last = LAST_REQUEST.get(player.getUUID());
        if (last != null && now - last < PLACE_COOLDOWN) {
            return true;
        }
        var previous = LAST_NONCE.put(player.getUUID(), nonce);
        if (previous != null && previous == nonce) {
            return true;
        }
        LAST_REQUEST.put(player.getUUID(), now);
        return false;
    }

    public static void forget(UUID player) {
        LAST_REQUEST.remove(player);
        LAST_NONCE.remove(player);
    }

    public static void place(
        ServerPlayer player,
        Identifier definitionId,
        BlockPos pos,
        Direction facing,
        PlacementMode mode,
        int nonce
    ) {
        if (throttled(player, nonce)) {
            return;
        }
        var level = player.level();
        var server = level.getServer();
        var camp = refreshed(level, CampManager.claimAt(server, level.dimension(), pos));
        if (camp.isEmpty()) {
            var reason = CampManager.active(server, player.getUUID()).isEmpty()
                ? Failure.NO_CAMP
                : Failure.OUTSIDE_CLAIM;
            fail(player, reason);
            return;
        }
        var definition = CampBuildable.get(level.registryAccess(), definitionId);
        if (definition.isEmpty()) {
            fail(player, Failure.UNKNOWN_DEFINITION);
            return;
        }
        var horizontal = facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
        var failure = validate(player, level, camp.get(), definitionId, definition.get(), pos, mode);
        if (failure.isPresent()) {
            fail(player, failure.get());
            return;
        }
        var buildable = definition.get();
        if (!(buildable.block().asItem() instanceof BlockItem blockItem)) {
            fail(player, Failure.UNKNOWN_DEFINITION);
            return;
        }
        var before = scan(level, pos, buildable.block());
        var snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        var context = new CampPlaceContext(player, new ItemStack(blockItem), pos, horizontal);
        var result = blockItem.place(context);
        if (!result.consumesAction() || !level.getBlockState(pos).is(buildable.block())) {
            fail(player, Failure.PLACEMENT_FAILED);
            return;
        }
        var parts = new ArrayList<BlockPos>();
        parts.add(pos.immutable());
        for (var part : scan(level, pos, buildable.block())) {
            if (!before.contains(part) && !part.equals(pos)) {
                parts.add(part);
            }
        }
        if (cancelledByOthers(player, level, pos, snapshot)) {
            for (var part : parts) {
                if (!part.equals(pos) && level.getBlockState(part).is(buildable.block())) {
                    level.setBlock(part, Blocks.AIR.defaultBlockState(), REMOVE_FLAGS);
                }
            }
            snapshot.restore();
            fail(player, Failure.PROTECTED);
            return;
        }
        CampMaterials.consume(player, buildable.cost());
        var object = CampObject.placed(
            UUID.randomUUID(),
            definitionId,
            BuiltInRegistries.BLOCK.getKey(buildable.block()),
            pos,
            horizontal,
            mode,
            buildable.budget(),
            parts
        );
        CampManager.update(server, camp.get().campId(), slot -> slot.withObject(object));
        CampSync.send(player);
    }

    private static boolean cancelledByOthers(
        ServerPlayer player,
        ServerLevel level,
        BlockPos pos,
        BlockSnapshot snapshot
    ) {
        WORKSHOP_PLACING.set(true);
        try {
            var event = new BlockEvent.EntityPlaceEvent(snapshot, level.getBlockState(pos.below()), player);
            return NeoForge.EVENT_BUS.post(event).isCanceled();
        } finally {
            WORKSHOP_PLACING.set(false);
        }
    }

    public static boolean workshopPlacing() {
        return WORKSHOP_PLACING.get();
    }

    public static Optional<Failure> validate(
        ServerPlayer player,
        ServerLevel level,
        CampSlot camp,
        Identifier definitionId,
        CampBuildable definition,
        BlockPos pos,
        PlacementMode mode
    ) {
        if (!camp.in(level.dimension())) {
            return Optional.of(Failure.WRONG_DIMENSION);
        }
        if (!CampPermissionService.canBuild(player, camp)) {
            return Optional.of(Failure.NOT_ALLOWED);
        }
        if (!CampPlans.unlocked(player, definition)) {
            return Optional.of(Failure.PLAN_LOCKED);
        }
        if (!CampClaim.contains(camp.origin(), pos)) {
            return Optional.of(Failure.OUTSIDE_CLAIM);
        }
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > (double) reach() * reach()) {
            return Optional.of(Failure.TOO_FAR);
        }
        if (!level.isLoaded(pos) || level.isOutsideBuildHeight(pos) || !level.getBlockState(pos).canBeReplaced()) {
            return Optional.of(Failure.OCCUPIED);
        }
        if (!definition.modes().contains(mode)) {
            return Optional.of(Failure.MODE_NOT_ALLOWED);
        }
        if (mode == PlacementMode.COLLISION && !supported(level, pos)) {
            return Optional.of(Failure.NO_SUPPORT);
        }
        if (mode == PlacementMode.SNAP && !snaps(camp, pos)) {
            return Optional.of(Failure.NO_SNAP);
        }
        if (camp.budgetUsed() + definition.budget() > camp.budgetLimit()) {
            return Optional.of(Failure.BUDGET);
        }
        if (definition.limit() > 0 && camp.count(definitionId) >= definition.limit()) {
            return Optional.of(Failure.LIMIT);
        }
        if (!CampMaterials.has(player, definition.cost())) {
            return Optional.of(Failure.MATERIALS);
        }
        return Optional.empty();
    }

    public static boolean supported(Level level, BlockPos pos) {
        for (var direction : Direction.values()) {
            if (!level.getBlockState(pos.relative(direction)).isAir()) {
                return true;
            }
        }
        return false;
    }

    public static boolean snaps(CampSlot camp, BlockPos pos) {
        for (var direction : Direction.values()) {
            var neighbor = pos.relative(direction);
            if (neighbor.equals(camp.origin()) || camp.objectAt(neighbor).isPresent()) {
                return true;
            }
        }
        return false;
    }

    public static Set<BlockPos> scan(Level level, BlockPos center, Block block) {
        var found = new HashSet<BlockPos>();
        for (var x = -SCAN_HORIZONTAL; x <= SCAN_HORIZONTAL; x++) {
            for (var y = -SCAN_DOWN; y <= SCAN_UP; y++) {
                for (var z = -SCAN_HORIZONTAL; z <= SCAN_HORIZONTAL; z++) {
                    var pos = center.offset(x, y, z);
                    if (level.getBlockState(pos).is(block)) {
                        found.add(pos.immutable());
                    }
                }
            }
        }
        return found;
    }

    public static void scrap(ServerPlayer player, BlockPos pos, int nonce) {
        if (throttled(player, nonce)) {
            return;
        }
        var level = player.level();
        var server = level.getServer();
        var tracked = CampManager.objectAt(server, level.dimension(), pos);
        if (tracked.isEmpty()) {
            return;
        }
        var camp = tracked.get().camp();
        var object = tracked.get().object();
        if (!CampPermissionService.canBuild(player, camp)) {
            fail(player, Failure.NOT_ALLOWED);
            return;
        }
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > (double) reach() * reach()) {
            fail(player, Failure.TOO_FAR);
            return;
        }
        remove(level, object);
        CampManager.update(server, camp.campId(), slot -> slot.withoutObject(object.objectId()));
        var returned = player.hasInfiniteMaterials() ? List.<ItemStack>of() : refund(player, object);
        level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.35F, 1.6F);
        CampPlacementService.notify(player, scrappedMessage(returned), ChatFormatting.YELLOW);
        CampSync.send(player);
    }

    private static void remove(ServerLevel level, CampObject object) {
        for (var part : object.parts()) {
            if (!level.isLoaded(part) || !matches(level, part, object)) {
                continue;
            }
            if (level.getBlockEntity(part) instanceof Container container) {
                Containers.dropContents(level, part, container);
            }
        }
        for (var part : object.parts()) {
            if (level.isLoaded(part) && matches(level, part, object)) {
                level.setBlock(part, Blocks.AIR.defaultBlockState(), REMOVE_FLAGS);
            }
        }
    }

    public static boolean matches(Level level, BlockPos pos, CampObject object) {
        return BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).equals(object.block());
    }

    public static List<ItemStack> refundItems(ServerPlayer player, CampObject object) {
        var items = new ArrayList<ItemStack>();
        if (object.manual()) {
            BuiltInRegistries.BLOCK.getOptional(object.block())
                .map(Block::asItem)
                .filter(item -> item != Items.AIR)
                .ifPresent(item -> items.add(new ItemStack(item)));
            return items;
        }
        var definition = CampBuildable.get(player.level().registryAccess(), object.definition());
        if (definition.isEmpty()) {
            return items;
        }
        var fraction = AtomicDawn.CONFIG.camp.CAMP_SCRAP_REFUND;
        if (fraction <= 0.0) {
            return items;
        }
        for (var ingredient : definition.get().cost()) {
            var item = ingredient.ingredient().items().findFirst().orElse(null);
            var count = Math.max(1, (int) Math.round(ingredient.count() * fraction));
            if (item != null) {
                items.add(new ItemStack(item, Math.min(count, ingredient.count())));
            }
        }
        return items;
    }

    public static List<ItemStack> refund(ServerPlayer player, CampObject object) {
        var items = refundItems(player, object);
        for (var stack : items) {
            PlayerItems.give(player, stack.copy());
        }
        return items;
    }

    private static Component scrappedMessage(List<ItemStack> returned) {
        if (returned.isEmpty()) {
            return Component.translatable("camp.atomicdawn.workshop.scrapped");
        }
        var list = Component.empty();
        for (var i = 0; i < returned.size(); i++) {
            if (i > 0) {
                list.append(", ");
            }
            list.append(returned.get(i).getCount() + "x ").append(returned.get(i).getHoverName());
        }
        return Component.translatable("camp.atomicdawn.workshop.scrapped_returned", list);
    }

    public static Optional<Component> manualCheck(ServerPlayer player, ServerLevel level, BlockPos pos, Block block) {
        if (!AtomicDawn.CONFIG.camp.CAMP_TRACK_MANUAL) {
            return Optional.empty();
        }
        var camp = refreshed(level, CampManager.claimAt(level.getServer(), level.dimension(), pos));
        if (camp.isEmpty() || !camp.get().ownedBy(player.getUUID())) {
            return Optional.empty();
        }
        var holder = CampBuildable.forBlock(level.registryAccess(), block);
        if (holder.isEmpty()) {
            return Optional.empty();
        }
        var definition = holder.get().value();
        var id = holder.get().key().identifier();
        if (camp.get().budgetUsed() + definition.budget() > camp.get().budgetLimit()) {
            return Optional.of(Failure.BUDGET.message());
        }
        if (definition.limit() > 0 && camp.get().count(id) >= definition.limit()) {
            return Optional.of(Failure.LIMIT.message());
        }
        return Optional.empty();
    }

    public static void trackManual(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
        if (!AtomicDawn.CONFIG.camp.CAMP_TRACK_MANUAL) {
            return;
        }
        var server = level.getServer();
        var camp = CampManager.claimAt(server, level.dimension(), pos);
        if (camp.isEmpty() || !camp.get().ownedBy(player.getUUID()) || camp.get().objectAt(pos).isPresent()) {
            return;
        }
        var holder = CampBuildable.forBlock(level.registryAccess(), state.getBlock());
        if (holder.isEmpty()) {
            return;
        }
        var definition = holder.get().value();
        var parts = new ArrayList<BlockPos>();
        parts.add(pos.immutable());
        for (var part : scan(level, pos, state.getBlock())) {
            if (!part.equals(pos) && camp.get().objectAt(part).isEmpty() && !part.equals(camp.get().origin())) {
                parts.add(part);
            }
        }
        var facing = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
            ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
            : Direction.NORTH;
        var object = CampObject.placed(
            UUID.randomUUID(),
            holder.get().key().identifier(),
            BuiltInRegistries.BLOCK.getKey(state.getBlock()),
            pos,
            facing,
            PlacementMode.FREE,
            definition.budget(),
            parts
        ).asManual();
        CampManager.update(server, camp.get().campId(), slot -> slot.withObject(object));
        CampSync.send(player);
    }

    private static Optional<CampSlot> refreshed(ServerLevel level, Optional<CampSlot> camp) {
        if (camp.isEmpty() || !camp.get().in(level.dimension())) {
            return camp;
        }
        prune(level, camp.get());
        return CampManager.get(level.getServer(), camp.get().campId());
    }

    public static void prune(ServerLevel level, CampSlot camp) {
        var stale = new ArrayList<UUID>();
        for (var object : camp.objects()) {
            if (object.stored()) {
                continue;
            }
            if (level.isLoaded(object.pos()) && !matches(level, object.pos(), object)) {
                stale.add(object.objectId());
            }
        }
        if (stale.isEmpty()) {
            return;
        }
        CampManager.update(
            level.getServer(),
            camp.campId(),
            slot -> slot.withObjects(slot.objects().stream().filter(o -> !stale.contains(o.objectId())).toList())
        );
    }

    private static void fail(ServerPlayer player, Failure failure) {
        CampPlacementService.notify(player, failure.message(), ChatFormatting.RED);
        CampSync.send(player);
    }
}
