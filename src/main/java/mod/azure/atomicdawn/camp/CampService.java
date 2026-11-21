package mod.azure.atomicdawn.camp;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

import mod.azure.atomicdawn.network.CampActionPayload;
import mod.azure.atomicdawn.pipboy.location.LocationService;

public final class CampService {

    private static final long ACTION_COOLDOWN = 10L;

    private static final int WAYPOINT_COLOR = 0x7CFF4A;

    private static final Map<UUID, Long> LAST_ACTION = new ConcurrentHashMap<>();

    private CampService() {}

    public static void handle(ServerPlayer player, CampActionPayload.Action action, UUID campId, int value) {
        if (action == CampActionPayload.Action.MODULE_FACING) {
            CampModuleFacing.set(player.getUUID(), Direction.from2DDataValue(value));
            return;
        }
        var server = player.level().getServer();
        var now = server.overworld().getGameTime();
        var last = LAST_ACTION.get(player.getUUID());
        if (action != CampActionPayload.Action.REQUEST && last != null && now - last < ACTION_COOLDOWN) {
            return;
        }
        LAST_ACTION.put(player.getUUID(), now);
        switch (action) {
            case REQUEST -> CampSync.send(player);
            case SELECT -> CampSlotService.select(player, value);
            case STORE -> owned(player, campId).ifPresentOrElse(
                camp -> CampSlotService.store(player, camp),
                () -> CampSync.send(player)
            );
            case REBUILD -> owned(player, campId).ifPresent(camp -> {
                if (value != 0) {
                    CampRestoreService.partial(player, camp);
                } else {
                    CampRestoreService.offer(player, camp, false);
                }
            });
            case CLEAR_REBUILD -> owned(player, campId).ifPresent(
                camp -> CampRestoreService.clearAndRestore(player, camp)
            );
            case DELETE -> owned(player, campId).ifPresent(camp -> CampSlotService.delete(player, camp));
            case PIN -> owned(player, campId).ifPresent(camp -> pin(player, camp));
            case TOGGLE_ACCESS -> updateAccess(player, access -> access.toggled(CampAccess.Flag.byIndex(value)));
            case UNTRUST -> updateAccess(player, access -> access.withoutTrusted(campId));
        }
    }

    private static void updateAccess(ServerPlayer player, UnaryOperator<CampAccess> change) {
        var server = player.level().getServer();
        CampSync.accessTarget(server, player)
            .ifPresent(camp -> CampManager.put(server, camp.withAccess(change.apply(camp.access()))));
        CampSync.changed(server);
    }

    public static void trust(ServerPlayer player, String name) {
        var server = player.level().getServer();
        var target = server.getPlayerList().getPlayerByName(name.trim());
        if (target == null || target == player) {
            CampPlacementService.notify(
                player,
                Component.translatable("camp.atomicdawn.access.unknown_player", name.trim()),
                ChatFormatting.RED
            );
            return;
        }
        var camp = CampSync.accessTarget(server, player);
        if (camp.isEmpty()) {
            CampPlacementService.notify(
                player,
                Component.translatable("camp.atomicdawn.workshop.error.no_camp"),
                ChatFormatting.RED
            );
            return;
        }
        if (camp.get().access().trusted().size() >= CampAccess.MAX_TRUSTED) {
            CampPlacementService.notify(
                player,
                Component.translatable("camp.atomicdawn.access.full", CampAccess.MAX_TRUSTED),
                ChatFormatting.RED
            );
            return;
        }
        updateAccess(player, access -> access.withTrusted(target.getUUID(), target.getScoreboardName()));
        CampPlacementService.notify(
            player,
            Component.translatable("camp.atomicdawn.access.trusted", target.getScoreboardName()),
            ChatFormatting.GREEN
        );
    }

    private static Optional<CampSlot> owned(ServerPlayer player, UUID campId) {
        return CampManager.get(player.level().getServer(), campId).filter(camp -> camp.ownedBy(player.getUUID()));
    }

    private static void pin(ServerPlayer player, CampSlot camp) {
        if (!camp.in(player.level().dimension())) {
            CampPlacementService.notify(
                player,
                Component.translatable("camp.atomicdawn.error.pin_dimension"),
                ChatFormatting.RED
            );
            return;
        }
        LocationService.addWaypoint(player, camp.origin(), "C.A.M.P.", WAYPOINT_COLOR);
        CampPlacementService.notify(player, Component.translatable("camp.atomicdawn.pinned"), ChatFormatting.GREEN);
    }

    public static void forget(UUID player) {
        LAST_ACTION.remove(player);
        CampModuleFacing.forget(player);
    }
}
