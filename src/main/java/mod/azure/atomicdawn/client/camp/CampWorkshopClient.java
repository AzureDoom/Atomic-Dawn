package mod.azure.atomicdawn.client.camp;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.camp.CampBlueprint;
import mod.azure.atomicdawn.camp.CampBlueprints;
import mod.azure.atomicdawn.camp.CampBuildable;
import mod.azure.atomicdawn.camp.CampMaterials;
import mod.azure.atomicdawn.camp.CampPlaceContext;
import mod.azure.atomicdawn.camp.CampPlans;
import mod.azure.atomicdawn.camp.CampStorage;
import mod.azure.atomicdawn.camp.CampWorkshopService;
import mod.azure.atomicdawn.camp.PlacementMode;
import mod.azure.atomicdawn.client.input.ADKeyMappings;
import mod.azure.atomicdawn.network.CampBlueprintPayload;
import mod.azure.atomicdawn.network.CampMovePayload;
import mod.azure.atomicdawn.network.CampSyncPayload;
import mod.azure.atomicdawn.network.CampWorkshopPayload;
import mod.azure.atomicdawn.registry.ADDataComponents;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class CampWorkshopClient {

    private static final int TOAST_TICKS = 60;

    private static final int SCRAP_CONFIRM_TICKS = 40;

    private static final int EXIT_MARGIN = 4;

    private static boolean active;

    @Nullable
    private static Identifier selected;

    private static Direction facing = Direction.NORTH;

    private static PlacementMode mode = PlacementMode.COLLISION;

    private static int nonce;

    @Nullable
    private static BlockPos scrapArmed;

    private static int scrapArmedTicks;

    private static boolean scrapReleased;

    @Nullable
    private static UUID activeCamp;

    @Nullable
    private static Component toast;

    @Nullable
    private static Identifier selectedBlueprint;

    private static boolean selecting;

    @Nullable
    private static BlockPos cornerA;

    @Nullable
    private static BlockPos cornerB;

    @Nullable
    private static CampBlueprintPreview preview;

    @Nullable
    private static UUID selectedStored;

    private static boolean storedSeen;

    private static int storedTicks;

    private static boolean movingModule;

    private record StoredView(
        CampStorage.Stored stored,
        CampBlueprint blueprint
    ) {}

    @Nullable
    private static StoredView storedView;

    private static final int STORED_WAIT_TICKS = 100;

    private record GhostCache(
        CampBlueprintPreview preview,
        BlockPos anchor,
        long tick,
        List<CampModulePlacementClient.GhostPart> ghosts,
        Optional<CampWorkshopService.Failure> failure
    ) {}

    @Nullable
    private static GhostCache ghostCache;

    private static int toastTicks;

    private CampWorkshopClient() {}

    public static boolean active() {
        return active;
    }

    public static @Nullable Identifier selected() {
        return selected;
    }

    public static Direction facing() {
        return facing;
    }

    public static PlacementMode mode() {
        return mode;
    }

    public static @Nullable Component toast() {
        return toastTicks > 0 ? toast : null;
    }

    public static boolean scrapArmed(BlockPos pos) {
        return scrapArmed != null && scrapArmed.equals(pos) && scrapArmedTicks > 0;
    }

    public static @Nullable Identifier selectedBlueprint() {
        return selectedBlueprint;
    }

    public static boolean selecting() {
        return selecting;
    }

    public static @Nullable BlockPos cornerA() {
        return cornerA;
    }

    public static @Nullable BlockPos cornerB() {
        return cornerB;
    }

    public static boolean hasSelection() {
        return cornerA != null && cornerB != null;
    }

    public static @Nullable UUID selectedStored() {
        return selectedStored;
    }

    public static boolean movingModule() {
        return movingModule;
    }

    public static boolean placingBlueprint() {
        return selectedBlueprint != null || selectedStored != null;
    }

    public static void selectBlueprint(Identifier id) {
        clearSelection();
        selectedBlueprint = id;
    }

    public static void selectStored(UUID id) {
        clearSelection();
        selectedStored = id;
        storedSeen = false;
        storedTicks = 0;
    }

    private static void clearSelection() {
        selected = null;
        selectedBlueprint = null;
        selectedStored = null;
        storedView = null;
        selecting = false;
        movingModule = false;
        preview = null;
        ghostCache = null;
    }

    public static Optional<CampStorage.Stored> stored() {
        var player = Minecraft.getInstance().player;
        if (player == null || selectedStored == null) {
            return Optional.empty();
        }
        return CampStorage.of(player).get(selectedStored);
    }

    public static Optional<CampBlueprint> blueprint() {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return Optional.empty();
        }
        if (selectedStored != null) {
            var stored = stored();
            if (stored.isEmpty()) {
                return Optional.empty();
            }
            var view = storedView;
            if (view == null || view.stored() != stored.get()) {
                var blueprint = new CampBlueprint("", CampBlueprint.Kind.PREFAB, List.of(stored.get().entry()));
                view = new StoredView(stored.get(), blueprint);
                storedView = view;
            }
            return Optional.of(view.blueprint());
        }
        if (selectedBlueprint == null) {
            return Optional.empty();
        }
        return CampBlueprints.get(player, selectedBlueprint);
    }

    public static Component placementName() {
        var player = Minecraft.getInstance().player;
        var level = Minecraft.getInstance().level;
        if (selectedStored != null && level != null) {
            return stored().flatMap(stored -> CampBuildable.get(level.registryAccess(), stored.entry().definition()))
                .map(CampBuildable::name)
                .orElse(Component.translatable("camp.atomicdawn.storage.unknown"));
        }
        if (player != null && selectedBlueprint != null) {
            return blueprint().map(found -> found.displayName(selectedBlueprint))
                .orElse(Component.translatable("camp.atomicdawn.blueprint.unknown"));
        }
        return Component.empty();
    }

    public static boolean moduleMoveValid(BlockPos target) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        var state = CampClientState.get();
        if (player == null || level == null || state == null) {
            return false;
        }
        var camp = workshopCamp(player);
        if (camp == null || !camp.own() || target.equals(camp.origin()) || !insideClaim(camp, target, 0)) {
            return false;
        }
        var reach = CampWorkshopService.reach();
        if (player.distanceToSqr(Vec3.atCenterOf(target)) > (double) reach * reach) {
            return false;
        }
        if (level.isOutsideBuildHeight(target) || !level.getBlockState(target).canBeReplaced()) {
            return false;
        }
        if (camp.objectAt(target).isPresent()) {
            return false;
        }
        var spacing = (long) state.spacing();
        for (var other : state.nearby()) {
            if (other.campId().equals(camp.campId())) {
                continue;
            }
            long dx = other.origin().getX() - target.getX();
            long dz = other.origin().getZ() - target.getZ();
            if (dx * dx + dz * dz < spacing * spacing) {
                return false;
            }
        }
        long radius = state.radius();
        for (var object : camp.objects()) {
            for (var part : object.parts()) {
                var dy = part.getY() - target.getY();
                long dx = part.getX() - target.getX();
                long dz = part.getZ() - target.getZ();
                if (dy > state.up() || dy < -state.down() || dx * dx + dz * dz > radius * radius) {
                    return false;
                }
            }
        }
        return true;
    }

    public static @Nullable CampBlueprintPreview blueprintPreview() {
        var blueprint = blueprint();
        if (blueprint.isEmpty()) {
            preview = null;
            return null;
        }
        var current = preview;
        if (current == null || !current.matches(blueprint.get(), facing)) {
            current = CampBlueprintPreview.of(blueprint.get(), facing);
            preview = current;
        }
        return current;
    }

    public static @Nullable BlockPos blueprintAnchor() {
        var player = Minecraft.getInstance().player;
        var blueprint = blueprint();
        if (player == null || blueprint.isEmpty()) {
            return null;
        }
        if (blueprint.get().kind() == CampBlueprint.Kind.CAMP) {
            var camp = workshopCamp(player);
            return camp == null ? null : camp.origin();
        }
        return target();
    }

    @Nullable
    private static GhostCache ghostCache() {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        var current = blueprintPreview();
        var anchor = blueprintAnchor();
        if (player == null || level == null || current == null || anchor == null) {
            return null;
        }
        var tick = level.getGameTime();
        var cached = ghostCache;
        if (cached != null && cached.preview() == current && cached.anchor().equals(anchor) && cached.tick() == tick) {
            return cached;
        }
        var ghosts = computeGhosts(current, anchor);
        var built = new GhostCache(current, anchor, tick, ghosts, computeFailure(anchor, ghosts));
        ghostCache = built;
        return built;
    }

    public static List<CampModulePlacementClient.GhostPart> blueprintGhosts() {
        var cached = ghostCache();
        return cached == null ? List.of() : cached.ghosts();
    }

    private static List<CampModulePlacementClient.GhostPart> computeGhosts(
        CampBlueprintPreview current,
        BlockPos anchor
    ) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null) {
            return List.of();
        }
        var camp = workshopCamp(player);
        var result = new ArrayList<CampModulePlacementClient.GhostPart>(current.parts().size());
        var seen = new HashSet<BlockPos>();
        for (var part : current.parts()) {
            var pos = anchor.offset(part.offset());
            var valid = camp != null && insideClaim(camp, pos, 0) && !pos.equals(camp.origin()) && seen.add(pos)
                && !level.isOutsideBuildHeight(pos) && level.getBlockState(pos).canBeReplaced();
            result.add(new CampModulePlacementClient.GhostPart(pos, part.state(), valid));
        }
        return result;
    }

    public static Optional<CampWorkshopService.Failure> blueprintFailure() {
        if (blueprint().isEmpty()) {
            return Optional.of(CampWorkshopService.Failure.UNKNOWN_DEFINITION);
        }
        var cached = ghostCache();
        return cached == null ? Optional.of(CampWorkshopService.Failure.NO_CAMP) : cached.failure();
    }

    private static Optional<CampWorkshopService.Failure> computeFailure(
        BlockPos anchor,
        List<CampModulePlacementClient.GhostPart> ghosts
    ) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null) {
            return Optional.of(CampWorkshopService.Failure.NO_CAMP);
        }
        var camp = workshopCamp(player);
        if (camp == null) {
            return Optional.of(CampWorkshopService.Failure.NO_CAMP);
        }
        var blueprint = blueprint();
        if (blueprint.isEmpty()) {
            return Optional.of(CampWorkshopService.Failure.UNKNOWN_DEFINITION);
        }
        var reach = CampWorkshopService.reach();
        if (
            blueprint.get().kind() == CampBlueprint.Kind.PREFAB
                && player.distanceToSqr(Vec3.atCenterOf(anchor)) > (double) reach * reach
        ) {
            return Optional.of(CampWorkshopService.Failure.TOO_FAR);
        }
        if (selectedStored == null && !CampPlans.unlocked(player, blueprint.get())) {
            return Optional.of(CampWorkshopService.Failure.PLAN_LOCKED);
        }
        var access = level.registryAccess();
        for (var entry : blueprint.get().entries()) {
            var definition = CampBuildable.get(access, entry.definition());
            if (definition.isEmpty()) {
                return Optional.of(CampWorkshopService.Failure.UNKNOWN_DEFINITION);
            }
            if (selectedStored == null && !CampPlans.unlocked(player, definition.get())) {
                return Optional.of(CampWorkshopService.Failure.PLAN_LOCKED);
            }
        }
        for (var ghost : ghosts) {
            if (!ghost.valid()) {
                return Optional.of(
                    insideClaim(camp, ghost.pos(), 0)
                        ? CampWorkshopService.Failure.OCCUPIED
                        : CampWorkshopService.Failure.OUTSIDE_CLAIM
                );
            }
        }
        if (camp.budgetUsed() + blueprint.get().budget(access) > camp.budgetLimit()) {
            return Optional.of(CampWorkshopService.Failure.BUDGET);
        }
        for (var count : blueprint.get().counts().entrySet()) {
            var limit = CampBuildable.get(access, count.getKey()).map(CampBuildable::limit).orElse(0);
            if (limit > 0 && camp.count(count.getKey()) + count.getValue() > limit) {
                return Optional.of(CampWorkshopService.Failure.LIMIT);
            }
        }
        if (selectedStored == null && !CampMaterials.has(player, blueprint.get().cost(access))) {
            return Optional.of(CampWorkshopService.Failure.MATERIALS);
        }
        return Optional.empty();
    }

    public static void saveSelection(String name) {
        if (cornerA == null || cornerB == null) {
            toast(
                Component.translatable(
                    "camp.atomicdawn.catalog.needs_selection",
                    ADKeyMappings.WORKSHOP_BLUEPRINT.getTranslatedKeyMessage()
                )
            );
            return;
        }
        selecting = false;
        ClientPacketDistributor.sendToServer(
            CampBlueprintPayload.savePrefab(name, cornerA, cornerB, facing, ++nonce)
        );
    }

    public static void saveCamp(String name) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ClientPacketDistributor.sendToServer(
            CampBlueprintPayload.saveCamp(name, player.blockPosition(), facing, ++nonce)
        );
    }

    public static void scrapStored(UUID id) {
        if (id.equals(selectedStored)) {
            selectedStored = null;
            storedView = null;
            preview = null;
        }
        ClientPacketDistributor.sendToServer(CampMovePayload.scrapStored(id));
    }

    public static void deleteBlueprint(Identifier id) {
        if (id.equals(selectedBlueprint)) {
            selectedBlueprint = null;
            preview = null;
        }
        ClientPacketDistributor.sendToServer(CampBlueprintPayload.delete(id));
    }

    public static void select(Identifier id) {
        clearSelection();
        selected = id;
        definition().ifPresent(definition -> {
            if (!definition.modes().contains(mode)) {
                mode = definition.defaultMode();
            }
        });
    }

    public static @Nullable BlockState previewState(BlockPos pos) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var definition = definition();
        if (player == null || definition.isEmpty()) {
            return null;
        }
        var block = definition.get().block();
        if (!(block.asItem() instanceof BlockItem item)) {
            return null;
        }
        var state = block.getStateForPlacement(new CampPlaceContext(player, new ItemStack(item), pos, facing));
        return state == null ? block.defaultBlockState() : state;
    }

    public static Optional<CampBuildable> definition() {
        var level = Minecraft.getInstance().level;
        if (level == null || selected == null) {
            return Optional.empty();
        }
        return CampBuildable.get(level.registryAccess(), selected);
    }

    static void toast(Component message) {
        toast = message;
        toastTicks = TOAST_TICKS;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (toastTicks > 0) {
            toastTicks--;
        }
        if (scrapArmedTicks > 0 && --scrapArmedTicks == 0) {
            scrapArmed = null;
        }
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.level == null) {
            active = false;
            return;
        }
        while (ADKeyMappings.WORKSHOP.consumeClick()) {
            toggle(player);
        }
        if (!active) {
            CampModulePlacementClient.tick(player);
            drain();
            return;
        }
        if (scrapArmed != null && !minecraft.options.keyAttack.isDown()) {
            scrapReleased = true;
        }
        var camp = workshopCamp(player);
        if (camp == null || !insideClaim(camp, player.blockPosition(), EXIT_MARGIN)) {
            active = false;
            toast(Component.translatable("camp.atomicdawn.workshop.left"));
            drain();
            return;
        }
        while (ADKeyMappings.WORKSHOP_CATALOG.consumeClick()) {
            minecraft.gui.setScreen(new CampCatalogScreen());
        }
        while (ADKeyMappings.WORKSHOP_ROTATE.consumeClick()) {
            facing = player.isShiftKeyDown() ? facing.getCounterClockWise() : facing.getClockWise();
        }
        while (ADKeyMappings.WORKSHOP_BLUEPRINT.consumeClick()) {
            toggleSelecting();
        }
        while (ADKeyMappings.WORKSHOP_MOVE.consumeClick()) {
            move(player, camp);
        }
        trackStored(player);
        while (ADKeyMappings.WORKSHOP_MODE.consumeClick()) {
            definition().ifPresent(definition -> {
                var modes = definition.modes();
                var index = modes.indexOf(mode);
                mode = modes.get((index + 1) % modes.size());
            });
        }
    }

    private static void drain() {
        while (ADKeyMappings.WORKSHOP_CATALOG.consumeClick()) {
        }
        while (ADKeyMappings.WORKSHOP_ROTATE.consumeClick()) {
        }
        while (ADKeyMappings.WORKSHOP_MODE.consumeClick()) {
        }
        while (ADKeyMappings.WORKSHOP_BLUEPRINT.consumeClick()) {
        }
        while (ADKeyMappings.WORKSHOP_MOVE.consumeClick()) {
        }
    }

    private static void trackStored(LocalPlayer player) {
        if (selectedStored == null) {
            return;
        }
        if (CampStorage.of(player).has(selectedStored)) {
            storedSeen = true;
            return;
        }
        if (storedSeen || ++storedTicks > STORED_WAIT_TICKS) {
            selectedStored = null;
            storedView = null;
            preview = null;
        }
    }

    private static void move(LocalPlayer player, CampSyncPayload.CampView camp) {
        if (movingModule) {
            movingModule = false;
            toast(Component.translatable("camp.atomicdawn.module.move_cancelled"));
            return;
        }
        var hit = hit();
        if (hit == null) {
            return;
        }
        if (hit.getBlockPos().equals(camp.origin())) {
            if (!camp.own()) {
                toast(Component.translatable("camp.atomicdawn.error.not_owner"));
                return;
            }
            clearSelection();
            movingModule = true;
            toast(Component.translatable("camp.atomicdawn.module.move_start"));
            return;
        }
        var object = hoveredObject();
        if (object.isEmpty()) {
            toast(Component.translatable("camp.atomicdawn.storage.nothing"));
            return;
        }
        if (!camp.own()) {
            toast(CampWorkshopService.Failure.NOT_ALLOWED.message());
            return;
        }
        var id = UUID.randomUUID();
        ClientPacketDistributor.sendToServer(CampMovePayload.store(object.get().pos(), id, ++nonce));
        if (player.isShiftKeyDown()) {
            toast(Component.translatable("camp.atomicdawn.storage.storing"));
            return;
        }
        selectStored(id);
        toast(Component.translatable("camp.atomicdawn.storage.moving"));
    }

    private static void toggleSelecting() {
        movingModule = false;
        selecting = !selecting;
        if (selecting) {
            cornerA = null;
            cornerB = null;
            toast(
                Component.translatable(
                    "camp.atomicdawn.blueprint.select_on",
                    ADKeyMappings.WORKSHOP_BLUEPRINT.getTranslatedKeyMessage(),
                    ADKeyMappings.WORKSHOP_CATALOG.getTranslatedKeyMessage()
                )
            );
        } else {
            toast(Component.translatable("camp.atomicdawn.blueprint.select_off"));
        }
    }

    private static void corner() {
        var hit = hit();
        if (hit == null) {
            return;
        }
        var pos = hit.getBlockPos().immutable();
        if (cornerA == null || cornerB != null) {
            cornerA = pos;
            cornerB = null;
            toast(Component.translatable("camp.atomicdawn.blueprint.corner_a", pos.getX(), pos.getY(), pos.getZ()));
            return;
        }
        cornerB = pos;
        toast(
            Component.translatable(
                "camp.atomicdawn.blueprint.corner_b",
                Math.abs(cornerA.getX() - pos.getX()) + 1,
                Math.abs(cornerA.getY() - pos.getY()) + 1,
                Math.abs(cornerA.getZ() - pos.getZ()) + 1
            )
        );
    }

    private static void toggle(LocalPlayer player) {
        if (active) {
            active = false;
            selecting = false;
            movingModule = false;
            toast(Component.translatable("camp.atomicdawn.workshop.exit"));
            return;
        }
        var camp = buildableCampAt(player, player.blockPosition());
        if (camp == null) {
            var own = camp(player);
            toast(
                Component.translatable(
                    own == null
                        ? "camp.atomicdawn.workshop.error.no_camp"
                        : "camp.atomicdawn.workshop.error.outside_claim"
                )
            );
            CampClientState.request();
            return;
        }
        activeCamp = camp.campId();
        active = true;
        facing = player.getDirection().getOpposite();
        toast(Component.translatable("camp.atomicdawn.workshop.enter"));
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().has(ADDataComponents.CAMP_BLOCK_DATA.get())) {
            event.getToolTip()
                .add(Component.translatable("camp.atomicdawn.item.keeps_data").withStyle(ChatFormatting.GRAY));
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        active = false;
        selected = null;
        clearSelection();
        cornerA = null;
        cornerB = null;
        scrapArmed = null;
        CampModulePlacementClient.reset();
        CampRestoreClient.clear();
        CampClientState.clear();
    }

    public static CampSyncPayload.@Nullable CampView workshopCamp(LocalPlayer player) {
        if (!active || activeCamp == null) {
            return buildableCampAt(player, player.blockPosition());
        }
        for (var view : candidates(player)) {
            if (view.campId().equals(activeCamp)) {
                return view;
            }
        }
        return null;
    }

    private static List<CampSyncPayload.CampView> candidates(LocalPlayer player) {
        var list = new ArrayList<CampSyncPayload.CampView>();
        var own = camp(player);
        if (own != null) {
            list.add(own);
        }
        var state = CampClientState.get();
        if (state != null) {
            var dimension = player.level().dimension().identifier().toString();
            for (var view : state.nearby()) {
                if (view.canBuild() && view.dimension().equals(dimension)) {
                    list.add(view);
                }
            }
        }
        return list;
    }

    private static CampSyncPayload.@Nullable CampView buildableCampAt(LocalPlayer player, BlockPos pos) {
        for (var view : candidates(player)) {
            if (insideClaim(view, pos, 0)) {
                return view;
            }
        }
        return null;
    }

    public static CampSyncPayload.@Nullable CampView camp(LocalPlayer player) {
        var state = CampClientState.get();
        if (state == null || state.camp().isEmpty()) {
            return null;
        }
        var camp = state.camp().get();
        return camp.dimension().equals(player.level().dimension().identifier().toString()) ? camp : null;
    }

    private static boolean insideClaim(CampSyncPayload.CampView camp, BlockPos pos, int margin) {
        var state = CampClientState.get();
        if (state == null) {
            return false;
        }
        var origin = camp.origin();
        var dy = pos.getY() - origin.getY();
        if (dy > state.up() + margin || dy < -state.down() - margin) {
            return false;
        }
        long dx = pos.getX() - origin.getX();
        long dz = pos.getZ() - origin.getZ();
        long radius = state.radius() + margin;
        return dx * dx + dz * dz <= radius * radius;
    }

    public static @Nullable BlockHitResult hit() {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null) {
            return null;
        }
        var partial = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        var result = player.pick(CampWorkshopService.reach(), partial, false);
        return result instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK ? block : null;
    }

    public static @Nullable BlockPos target() {
        var hit = hit();
        var level = Minecraft.getInstance().level;
        if (hit == null || level == null) {
            return null;
        }
        var clicked = hit.getBlockPos();
        return level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(hit.getDirection());
    }

    public static Optional<CampSyncPayload.ObjectView> hoveredObject() {
        var player = Minecraft.getInstance().player;
        var hit = hit();
        if (player == null || hit == null) {
            return Optional.empty();
        }
        var camp = workshopCamp(player);
        return camp == null ? Optional.empty() : camp.objectAt(hit.getBlockPos());
    }

    public static Optional<CampWorkshopService.Failure> failure(BlockPos pos) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null) {
            return Optional.of(CampWorkshopService.Failure.NO_CAMP);
        }
        var camp = workshopCamp(player);
        if (camp == null) {
            return Optional.of(CampWorkshopService.Failure.NO_CAMP);
        }
        var definition = definition();
        if (definition.isEmpty() || selected == null) {
            return Optional.of(CampWorkshopService.Failure.UNKNOWN_DEFINITION);
        }
        var buildable = definition.get();
        if (!CampPlans.unlocked(player, buildable)) {
            return Optional.of(CampWorkshopService.Failure.PLAN_LOCKED);
        }
        if (!insideClaim(camp, pos, 0)) {
            return Optional.of(CampWorkshopService.Failure.OUTSIDE_CLAIM);
        }
        var reach = CampWorkshopService.reach();
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > (double) reach * reach) {
            return Optional.of(CampWorkshopService.Failure.TOO_FAR);
        }
        if (level.isOutsideBuildHeight(pos) || !level.getBlockState(pos).canBeReplaced()) {
            return Optional.of(CampWorkshopService.Failure.OCCUPIED);
        }
        if (!buildable.modes().contains(mode)) {
            return Optional.of(CampWorkshopService.Failure.MODE_NOT_ALLOWED);
        }
        if (mode == PlacementMode.COLLISION && !CampWorkshopService.supported(level, pos)) {
            return Optional.of(CampWorkshopService.Failure.NO_SUPPORT);
        }
        if (mode == PlacementMode.SNAP && !snaps(camp, pos)) {
            return Optional.of(CampWorkshopService.Failure.NO_SNAP);
        }
        if (camp.budgetUsed() + buildable.budget() > camp.budgetLimit()) {
            return Optional.of(CampWorkshopService.Failure.BUDGET);
        }
        if (buildable.limit() > 0 && camp.count(selected) >= buildable.limit()) {
            return Optional.of(CampWorkshopService.Failure.LIMIT);
        }
        if (!CampMaterials.has(player, buildable.cost())) {
            return Optional.of(CampWorkshopService.Failure.MATERIALS);
        }
        return Optional.empty();
    }

    private static boolean snaps(CampSyncPayload.CampView camp, BlockPos pos) {
        for (var direction : Direction.values()) {
            var neighbor = pos.relative(direction);
            if (neighbor.equals(camp.origin()) || camp.objectAt(neighbor).isPresent()) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!active) {
            return;
        }
        if (selecting && (event.isUseItem() || event.isAttack())) {
            event.setCanceled(true);
            event.setSwingHand(false);
            if (event.isUseItem()) {
                corner();
            } else if (cornerA != null) {
                cornerA = null;
                cornerB = null;
                toast(Component.translatable("camp.atomicdawn.blueprint.cleared"));
            }
            return;
        }
        if (movingModule && (event.isUseItem() || event.isAttack())) {
            event.setCanceled(true);
            event.setSwingHand(false);
            var target = target();
            if (event.isAttack()) {
                movingModule = false;
                toast(Component.translatable("camp.atomicdawn.module.move_cancelled"));
            } else if (target != null && moduleMoveValid(target)) {
                movingModule = false;
                ClientPacketDistributor.sendToServer(CampMovePayload.moveModule(target, ++nonce));
            } else {
                toast(Component.translatable("camp.atomicdawn.module.move_invalid"));
            }
            return;
        }
        if (event.isUseItem()) {
            event.setCanceled(true);
            event.setSwingHand(false);
            if (placingBlueprint()) {
                placeBlueprint();
            } else {
                place();
            }
        } else if (event.isAttack()) {
            event.setCanceled(true);
            event.setSwingHand(false);
            scrap();
        } else if (event.isPickBlock()) {
            hoveredObject().ifPresent(object -> {
                event.setCanceled(true);
                select(object.definition());
            });
        }
    }

    private static void place() {
        if (selected == null) {
            toast(
                Component.translatable(
                    "camp.atomicdawn.workshop.select_first",
                    ADKeyMappings.WORKSHOP_CATALOG.getTranslatedKeyMessage()
                )
            );
            return;
        }
        var target = target();
        if (target == null) {
            return;
        }
        var failure = failure(target);
        if (failure.isPresent()) {
            toast(failure.get().message());
            return;
        }
        ClientPacketDistributor.sendToServer(CampWorkshopPayload.place(selected, target, facing, mode, ++nonce));
    }

    private static void placeBlueprint() {
        var anchor = blueprintAnchor();
        if (!placingBlueprint() || anchor == null) {
            return;
        }
        var failure = blueprintFailure();
        if (failure.isPresent()) {
            toast(failure.get().message());
            return;
        }
        if (selectedStored != null) {
            ClientPacketDistributor.sendToServer(CampMovePayload.placeStored(selectedStored, anchor, facing, ++nonce));
            return;
        }
        ClientPacketDistributor.sendToServer(
            CampBlueprintPayload.place(selectedBlueprint, anchor, facing, ++nonce)
        );
    }

    private static void scrap() {
        var object = hoveredObject();
        if (object.isEmpty()) {
            return;
        }
        var pos = object.get().pos();
        if (scrapArmed(pos)) {
            if (!scrapReleased) {
                return;
            }
            scrapArmed = null;
            scrapArmedTicks = 0;
            ClientPacketDistributor.sendToServer(CampWorkshopPayload.scrap(pos, ++nonce));
            return;
        }
        scrapArmed = pos;
        scrapReleased = false;
        scrapArmedTicks = SCRAP_CONFIRM_TICKS;
        toast(Component.translatable("camp.atomicdawn.workshop.scrap_confirm"));
    }
}
