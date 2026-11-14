package mod.azure.atomicdawn.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ADNetwork {

    private static final String PROTOCOL_VERSION = "13";

    private ADNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(
            WeaponActionPayload.TYPE,
            WeaponActionPayload.STREAM_CODEC,
            WeaponActionPayload::handleOnServer
        );
        registrar.playToServer(
            WorkbenchInstallPayload.TYPE,
            WorkbenchInstallPayload.STREAM_CODEC,
            WorkbenchInstallPayload::handleOnServer
        );
        registrar.playToServer(
            WorkbenchActionPayload.TYPE,
            WorkbenchActionPayload.STREAM_CODEC,
            WorkbenchActionPayload::handleOnServer
        );
        registrar.playToServer(
            TinkerCraftPayload.TYPE,
            TinkerCraftPayload.STREAM_CODEC,
            TinkerCraftPayload::handleOnServer
        );
        registrar.playToServer(VatsStatePayload.TYPE, VatsStatePayload.STREAM_CODEC, VatsStatePayload::handleOnServer);
        registrar.playToServer(
            SpecialActionPayload.TYPE,
            SpecialActionPayload.STREAM_CODEC,
            SpecialActionPayload::handleOnServer
        );
        registrar.playToServer(
            PipBoyActionPayload.TYPE,
            PipBoyActionPayload.STREAM_CODEC,
            PipBoyActionPayload::handleOnServer
        );
        registrar.playToServer(
            WaypointActionPayload.TYPE,
            WaypointActionPayload.STREAM_CODEC,
            WaypointActionPayload::handleOnServer
        );
        registrar.playToServer(
            GhoulActionPayload.TYPE,
            GhoulActionPayload.STREAM_CODEC,
            GhoulActionPayload::handleOnServer
        );
        registrar.playToClient(HolotapePlayPayload.TYPE, HolotapePlayPayload.STREAM_CODEC);
        registrar.playToClient(GhoulPromptPayload.TYPE, GhoulPromptPayload.STREAM_CODEC);
        registrar.playToClient(HealthClampPayload.TYPE, HealthClampPayload.STREAM_CODEC);
        registrar.playToClient(NpcDialoguePayload.TYPE, NpcDialoguePayload.STREAM_CODEC);
        registrar.playToClient(NpcEditorPayload.TYPE, NpcEditorPayload.STREAM_CODEC);
        registrar.playToServer(
            NpcDialogueChoicePayload.TYPE,
            NpcDialogueChoicePayload.STREAM_CODEC,
            NpcDialogueChoicePayload::handleOnServer
        );
        registrar.playToServer(NpcEditPayload.TYPE, NpcEditPayload.STREAM_CODEC, NpcEditPayload::handleOnServer);
        registrar.playToClient(NpcPickerPayload.TYPE, NpcPickerPayload.STREAM_CODEC);
        registrar.playToServer(NpcPickPayload.TYPE, NpcPickPayload.STREAM_CODEC, NpcPickPayload::handleOnServer);
        registrar.playToServer(
            PowerArmorActionPayload.TYPE,
            PowerArmorActionPayload.STREAM_CODEC,
            PowerArmorActionPayload::handleOnServer
        );
        registrar.playToServer(
            ArmorBenchActionPayload.TYPE,
            ArmorBenchActionPayload.STREAM_CODEC,
            ArmorBenchActionPayload::handleOnServer
        );
        registrar.playToClient(PowerArmorLandingPayload.TYPE, PowerArmorLandingPayload.STREAM_CODEC);
        registrar.playToClient(SpecialResultPayload.TYPE, SpecialResultPayload.STREAM_CODEC);
        registrar.playToClient(PipBoyStatsPayload.TYPE, PipBoyStatsPayload.STREAM_CODEC);
        registrar.playToClient(PipBoyNotificationPayload.TYPE, PipBoyNotificationPayload.STREAM_CODEC);
        registrar.playToClient(NukeExplosionPayload.TYPE, NukeExplosionPayload.STREAM_CODEC);
        registrar.playToClient(VatsShotPayload.TYPE, VatsShotPayload.STREAM_CODEC);
        registrar.playToClient(ReloadStatusPayload.TYPE, ReloadStatusPayload.STREAM_CODEC);
        registrar.playToClient(WorkbenchResultPayload.TYPE, WorkbenchResultPayload.STREAM_CODEC);
        registrar.playToClient(PartySyncPayload.TYPE, PartySyncPayload.STREAM_CODEC);
        registrar.playToServer(
            LockpickActionPayload.TYPE,
            LockpickActionPayload.STREAM_CODEC,
            LockpickActionPayload::handleOnServer
        );
        registrar.playToClient(LockpickOpenPayload.TYPE, LockpickOpenPayload.STREAM_CODEC);
        registrar.playToClient(LockpickStatePayload.TYPE, LockpickStatePayload.STREAM_CODEC);
    }

    public static void sendTo(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
