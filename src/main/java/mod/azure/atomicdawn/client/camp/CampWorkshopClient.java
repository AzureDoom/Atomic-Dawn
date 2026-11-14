package mod.azure.atomicdawn.client.camp;

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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.camp.CampBuildable;
import mod.azure.atomicdawn.camp.CampMaterials;
import mod.azure.atomicdawn.camp.CampPlaceContext;
import mod.azure.atomicdawn.camp.CampPlans;
import mod.azure.atomicdawn.camp.CampWorkshopService;
import mod.azure.atomicdawn.camp.PlacementMode;
import mod.azure.atomicdawn.client.input.ADKeyMappings;
import mod.azure.atomicdawn.network.CampSyncPayload;
import mod.azure.atomicdawn.network.CampWorkshopPayload;

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

    public static void select(Identifier id) {
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

    private static void toast(Component message) {
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
    }

    private static void toggle(LocalPlayer player) {
        if (active) {
            active = false;
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
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        active = false;
        selected = null;
        scrapArmed = null;
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
        if (event.isUseItem()) {
            event.setCanceled(true);
            event.setSwingHand(false);
            place();
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
