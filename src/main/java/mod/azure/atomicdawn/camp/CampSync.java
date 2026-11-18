package mod.azure.atomicdawn.camp;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import mod.azure.atomicdawn.network.ADNetwork;
import mod.azure.atomicdawn.network.CampSyncPayload;

public final class CampSync {

    private static final long NEARBY_RANGE_SQR = 512L * 512L;

    private CampSync() {}

    private static CampSyncPayload.CampView view(ServerPlayer player, CampSlot camp) {
        var canBuild = CampPermissionService.canBuild(player, camp);
        List<CampSyncPayload.ObjectView> objects = canBuild
            ? camp.objects()
                .stream()
                .filter(CampObject::deployed)
                .map(
                    object -> new CampSyncPayload.ObjectView(object.definition(), object.pos(), object.parts())
                )
                .toList()
            : List.of();
        return new CampSyncPayload.CampView(
            camp.campId(),
            camp.ownerName(),
            camp.ownedBy(player.getUUID()),
            canBuild,
            camp.access().mapIcon(),
            camp.dimension().identifier().toString(),
            camp.origin(),
            camp.slotIndex(),
            camp.budgetUsed(),
            camp.budgetLimit(),
            objects
        );
    }

    private static CampSyncPayload.AccessView access(CampSlot camp) {
        var flags = 0;
        for (var flag : CampAccess.Flag.values()) {
            if (camp.access().get(flag)) {
                flags |= 1 << flag.ordinal();
            }
        }
        var trusted = camp.access()
            .trusted()
            .entrySet()
            .stream()
            .sorted(Map.Entry.comparingByValue())
            .map(entry -> new CampSyncPayload.TrustedView(entry.getKey(), entry.getValue()))
            .toList();
        return new CampSyncPayload.AccessView(trusted, flags);
    }

    public static void send(ServerPlayer player) {
        var level = player.level();
        var server = level.getServer();
        for (var camp : CampManager.owned(server, player.getUUID())) {
            var campLevel = server.getLevel(camp.dimension());
            if (campLevel != null && camp.deployed()) {
                CampWorkshopService.prune(campLevel, camp);
            }
        }
        var owned = CampManager.owned(server, player.getUUID());
        var deployed = owned.stream().filter(CampSlot::deployed).findFirst();
        var here = player.blockPosition();
        var nearby = CampManager.inDimension(server, level.dimension())
            .stream()
            .filter(camp -> !camp.ownedBy(player.getUUID()))
            .filter(camp -> CampClaim.horizontalDistanceSqr(camp.origin(), here) <= NEARBY_RANGE_SQR)
            .sorted(Comparator.comparingLong(camp -> CampClaim.horizontalDistanceSqr(camp.origin(), here)))
            .limit(CampSyncPayload.MAX_NEARBY)
            .map(camp -> view(player, camp))
            .toList();
        ADNetwork.sendTo(
            player,
            new CampSyncPayload(
                CampPlacementService.enabled(),
                deployed.map(camp -> view(player, camp)),
                owned.size(),
                CampPlacementService.maxSlots(),
                CampClaim.radius(),
                CampClaim.up(),
                CampClaim.down(),
                CampClaim.spacing(),
                CampPlacementService.dimensionAllowed(level),
                nearby,
                owned.stream()
                    .map(
                        camp -> new CampSyncPayload.SlotView(
                            camp.campId(),
                            camp.slotIndex(),
                            camp.deployed(),
                            camp.dimension().identifier().toString(),
                            camp.origin(),
                            camp.budgetUsed(),
                            camp.budgetLimit(),
                            camp.objects().size(),
                            camp.storedCount()
                        )
                    )
                    .toList(),
                CampPlacementService.selectedSlot(server, player.getUUID()),
                deployed.map(CampSync::access).or(() -> owned.stream().findFirst().map(CampSync::access)),
                blueprint(player)
            )
        );
    }

    private static List<CampSyncPayload.BlueprintPart> blueprint(ServerPlayer player) {
        var server = player.level().getServer();
        var selected = CampManager.inSlot(
            server,
            player.getUUID(),
            CampPlacementService.selectedSlot(server, player.getUUID())
        );
        if (selected.isEmpty() || selected.get().deployed()) {
            return List.of();
        }
        var parts = new ArrayList<CampSyncPayload.BlueprintPart>();
        for (var object : selected.get().objects()) {
            if (!object.stored()) {
                continue;
            }
            for (var snapshot : object.snapshot()) {
                if (parts.size() >= CampSyncPayload.MAX_BLUEPRINT) {
                    return parts;
                }
                snapshot.state()
                    .filter(state -> !state.isAir())
                    .ifPresent(state -> parts.add(new CampSyncPayload.BlueprintPart(snapshot.offset(), state)));
            }
        }
        return parts;
    }

    public static void changed(MinecraftServer server) {
        for (var player : server.getPlayerList().getPlayers()) {
            send(player);
        }
    }

    public static Optional<CampSlot> accessTarget(MinecraftServer server, ServerPlayer player) {
        var owned = CampManager.owned(server, player.getUUID());
        return owned.stream().filter(CampSlot::deployed).findFirst().or(() -> owned.stream().findFirst());
    }
}
