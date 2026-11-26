package mod.azure.atomicdawn.client.network;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

import mod.azure.atomicdawn.client.ClientWeaponState;
import mod.azure.atomicdawn.client.camp.CampClientState;
import mod.azure.atomicdawn.client.camp.CampRestoreClient;
import mod.azure.atomicdawn.client.effect.NukeEffects;
import mod.azure.atomicdawn.client.ghoul.GhoulClient;
import mod.azure.atomicdawn.client.lock.LockpickClient;
import mod.azure.atomicdawn.client.npc.NpcClient;
import mod.azure.atomicdawn.client.party.PartyClient;
import mod.azure.atomicdawn.client.pipboy.PipBoyClient;
import mod.azure.atomicdawn.client.pipboy.radio.HolotapePlayer;
import mod.azure.atomicdawn.client.powerarmor.PowerArmorClient;
import mod.azure.atomicdawn.client.radiation.HealthClampClient;
import mod.azure.atomicdawn.client.screen.TinkersBenchScreen;
import mod.azure.atomicdawn.client.screen.WorkbenchScreen;
import mod.azure.atomicdawn.client.storage.StorageClient;
import mod.azure.atomicdawn.client.vats.VatsClient;
import mod.azure.atomicdawn.network.CampRestorePayload;
import mod.azure.atomicdawn.network.CampSyncPayload;
import mod.azure.atomicdawn.network.GhoulPromptPayload;
import mod.azure.atomicdawn.network.HealthClampPayload;
import mod.azure.atomicdawn.network.HolotapePlayPayload;
import mod.azure.atomicdawn.network.LockpickOpenPayload;
import mod.azure.atomicdawn.network.LockpickStatePayload;
import mod.azure.atomicdawn.network.NpcDialoguePayload;
import mod.azure.atomicdawn.network.NpcEditorPayload;
import mod.azure.atomicdawn.network.NpcPickerPayload;
import mod.azure.atomicdawn.network.NukeExplosionPayload;
import mod.azure.atomicdawn.network.PartySyncPayload;
import mod.azure.atomicdawn.network.PipBoyNotificationPayload;
import mod.azure.atomicdawn.network.PipBoyStatsPayload;
import mod.azure.atomicdawn.network.PowerArmorLandingPayload;
import mod.azure.atomicdawn.network.ReloadStatusPayload;
import mod.azure.atomicdawn.network.SpecialResultPayload;
import mod.azure.atomicdawn.network.StorageOpenPayload;
import mod.azure.atomicdawn.network.VatsShotPayload;
import mod.azure.atomicdawn.network.WorkbenchResultPayload;

public final class ClientPayloadHandlers {

    private ClientPayloadHandlers() {}

    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(
            ReloadStatusPayload.TYPE,
            (payload, ignored) -> ClientWeaponState.onReloadStatus(payload.durationTicks())
        );
        event.register(VatsShotPayload.TYPE, (payload, ignored) -> VatsClient.onShot(payload));
        event.register(SpecialResultPayload.TYPE, (payload, ignored) -> PipBoyClient.onResult(payload));
        event.register(HolotapePlayPayload.TYPE, (payload, ignored) -> HolotapePlayer.play(payload.holotape()));
        event.register(PowerArmorLandingPayload.TYPE, (payload, ignored) -> PowerArmorClient.onLanding(payload));
        event.register(PipBoyStatsPayload.TYPE, (payload, ignored) -> PipBoyClient.onStats(payload));
        event.register(PipBoyNotificationPayload.TYPE, (payload, ignored) -> PipBoyClient.onNotification(payload));
        event.register(NukeExplosionPayload.TYPE, (payload, ignored) -> NukeEffects.onDetonation(payload));
        event.register(PartySyncPayload.TYPE, (payload, ignored) -> PartyClient.onSync(payload));
        event.register(GhoulPromptPayload.TYPE, (payload, ignored) -> GhoulClient.onPrompt(payload));
        event.register(HealthClampPayload.TYPE, (payload, ignored) -> HealthClampClient.expect(payload.health()));
        event.register(NpcDialoguePayload.TYPE, (payload, ignored) -> NpcClient.onDialogue(payload));
        event.register(NpcEditorPayload.TYPE, (payload, ignored) -> NpcClient.onEditor(payload));
        event.register(NpcPickerPayload.TYPE, (payload, ignored) -> NpcClient.onPicker(payload));
        event.register(LockpickOpenPayload.TYPE, (payload, ignored) -> LockpickClient.onOpen(payload));
        event.register(LockpickStatePayload.TYPE, (payload, ignored) -> LockpickClient.onState(payload));
        event.register(CampSyncPayload.TYPE, (payload, ignored) -> CampClientState.accept(payload));
        event.register(CampRestorePayload.TYPE, (payload, ignored) -> CampRestoreClient.accept(payload));
        event.register(
            WorkbenchResultPayload.TYPE,
            (payload, ignored) -> {
                var screen = Minecraft.getInstance().gui.screen();
                if (screen instanceof WorkbenchScreen workbench) {
                    workbench.showResult(payload.result());
                } else if (screen instanceof TinkersBenchScreen tinkers) {
                    tinkers.showResult(payload.result());
                }
            }
        );
        event.register(StorageOpenPayload.TYPE, (payload, ignored) -> StorageClient.onOpen(payload));
    }
}
