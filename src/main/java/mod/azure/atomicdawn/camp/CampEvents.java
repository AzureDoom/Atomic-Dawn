package mod.azure.atomicdawn.camp;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.command.CampCommands;
import mod.azure.atomicdawn.registry.ADDataComponents;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID)
public final class CampEvents {

    private static final int RESYNC_TICKS = 100;

    private CampEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onModulePlaceValidate(BlockEvent.EntityPlaceEvent event) {
        if (!isModule(event) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            event.setCanceled(true);
            return;
        }
        var failure = CampPlacementService.validateDeploy(player, level, event.getPos());
        if (failure.isPresent()) {
            event.setCanceled(true);
            CampPlacementService.notify(player, failure.get(), ChatFormatting.RED);
            CampSync.send(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onModulePlaceCommit(BlockEvent.EntityPlaceEvent event) {
        if (!isModule(event) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            var state = event.getPlacedBlock();
            var facing = state.hasProperty(CampModuleBlock.FACING)
                ? state.getValue(CampModuleBlock.FACING)
                : Direction.NORTH;
            CampPlacementService.deploy(player, level, event.getPos(), facing);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onManualPlaceValidate(BlockEvent.EntityPlaceEvent event) {
        if (isModule(event) || CampWorkshopService.workshopPlacing()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var failure = CampWorkshopService.manualCheck(player, level, event.getPos(), event.getPlacedBlock().getBlock());
        if (failure.isPresent()) {
            event.setCanceled(true);
            CampPlacementService.notify(player, failure.get(), ChatFormatting.RED);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onManualPlaceCommit(BlockEvent.EntityPlaceEvent event) {
        if (isModule(event) || CampWorkshopService.workshopPlacing()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            CampWorkshopService.trackManual(player, level, event.getPos(), event.getPlacedBlock());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDataBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (CampWorkshopService.workshopPlacing() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        var block = event.getPlacedBlock().getBlock();
        for (var hand : InteractionHand.values()) {
            var stack = player.getItemInHand(hand);
            if (
                stack.getItem() instanceof BlockItem item
                    && item.getBlock() == block
                    && stack.has(ADDataComponents.CAMP_BLOCK_DATA.get())
            ) {
                CampContents.applyItemData(level, event.getPos(), stack);
                return;
            }
        }
    }

    private static boolean isModule(BlockEvent.EntityPlaceEvent event) {
        return event.getPlacedBlock().getBlock() instanceof CampModuleBlock;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreakProtect(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (CampPermissionService.adminBypass(player)) {
            return;
        }
        var server = level.getServer();
        var pos = event.getPos();
        String key;
        if (event.getState().getBlock() instanceof CampModuleBlock) {
            var camp = CampManager.moduleAt(server, level.dimension(), pos);
            if (camp.isEmpty()) {
                return;
            }
            key = CampPermissionService.canManage(player, camp.get())
                ? "camp.atomicdawn.module.store_hint"
                : "camp.atomicdawn.error.protected";
        } else {
            var tracked = CampManager.objectAt(server, level.dimension(), pos);
            if (tracked.isEmpty()) {
                return;
            }
            key = CampPermissionService.canBuild(player, tracked.get().camp())
                ? "camp.atomicdawn.workshop.use_scrap"
                : "camp.atomicdawn.error.protected_object";
        }
        event.setCanceled(true);
        CampPlacementService.notify(player, Component.translatable(key), ChatFormatting.RED);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var level = player.level();
        var tracked = CampManager.objectAt(level.getServer(), level.dimension(), event.getPos());
        if (tracked.isEmpty()) {
            return;
        }
        var category = CampBuildable.get(level.registryAccess(), tracked.get().object().definition())
            .map(CampBuildable::category)
            .orElse(CampBuildable.Category.DECOR);
        if (CampPermissionService.canUse(player, tracked.get().camp(), category)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        CampPlacementService.notify(
            player,
            Component.translatable("camp.atomicdawn.error.protected_object"),
            ChatFormatting.RED
        );
    }

    private static boolean protectedBlock(ServerLevel level, BlockPos pos) {
        var server = level.getServer();
        return CampManager.objectAt(server, level.dimension(), pos).isPresent()
            || CampManager.moduleAt(server, level.dimension(), pos).isPresent();
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (
            !AtomicDawn.CONFIG.common.settlement.camp.claims.CAMP_PROTECT_EXPLOSIONS || !(event
                .getLevel() instanceof ServerLevel level)
        ) {
            return;
        }
        event.getAffectedBlocks().removeIf(pos -> protectedBlock(level, pos));
    }

    @SubscribeEvent
    public static void onPiston(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        var resolver = event.getStructureHelper();
        if (resolver == null || !resolver.resolve()) {
            return;
        }
        for (var pos : resolver.getToPush()) {
            if (protectedBlock(level, pos)) {
                event.setCanceled(true);
                return;
            }
        }
        for (var pos : resolver.getToDestroy()) {
            if (protectedBlock(level, pos)) {
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var server = player.level().getServer();
        var name = player.getScoreboardName();
        for (var camp : CampManager.owned(server, player.getUUID())) {
            if (!camp.ownerName().equals(name)) {
                CampManager.put(server, camp.withOwnerName(name));
            }
        }
        CampSync.send(player);
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CampSync.send(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CampService.forget(event.getEntity().getUUID());
        CampWorkshopService.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getTickCount() % RESYNC_TICKS == 0 && CampPlacementService.enabled()) {
            CampSync.changed(server);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        CampManager.invalidate();
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CampCommands.register(event.getDispatcher());
    }
}
