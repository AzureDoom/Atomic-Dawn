package mod.azure.atomicdawn.client.network;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

import mod.azure.atomicdawn.client.ClientWeaponState;
import mod.azure.atomicdawn.client.camp.CampClientState;
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
import mod.azure.atomicdawn.client.vats.VatsClient;
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
import mod.azure.atomicdawn.network.VatsShotPayload;
import mod.azure.atomicdawn.network.WorkbenchResultPayload;

public final class ClientPayloadHandlers {

    private ClientPayloadHandlers() {}

    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(
            ReloadStatusPayload.TYPE,
            (payload, _) -> ClientWeaponState.onReloadStatus(payload.durationTicks())
        );
        event.register(VatsShotPayload.TYPE, (payload, _) -> VatsClient.onShot(payload));
        event.register(SpecialResultPayload.TYPE, (payload, _) -> PipBoyClient.onResult(payload));
        event.register(HolotapePlayPayload.TYPE, (payload, _) -> HolotapePlayer.play(payload.holotape()));
        event.register(PowerArmorLandingPayload.TYPE, (payload, _) -> PowerArmorClient.onLanding(payload));
        event.register(PipBoyStatsPayload.TYPE, (payload, _) -> PipBoyClient.onStats(payload));
        event.register(PipBoyNotificationPayload.TYPE, (payload, _) -> PipBoyClient.onNotification(payload));
        event.register(NukeExplosionPayload.TYPE, (payload, _) -> NukeEffects.onDetonation(payload));
        event.register(PartySyncPayload.TYPE, (payload, _) -> PartyClient.onSync(payload));
        event.register(GhoulPromptPayload.TYPE, (payload, _) -> GhoulClient.onPrompt(payload));
        event.register(HealthClampPayload.TYPE, (payload, _) -> HealthClampClient.expect(payload.health()));
        event.register(NpcDialoguePayload.TYPE, (payload, _) -> NpcClient.onDialogue(payload));
        event.register(NpcEditorPayload.TYPE, (payload, _) -> NpcClient.onEditor(payload));
        event.register(NpcPickerPayload.TYPE, (payload, _) -> NpcClient.onPicker(payload));
        event.register(LockpickOpenPayload.TYPE, (payload, _) -> LockpickClient.onOpen(payload));
        event.register(LockpickStatePayload.TYPE, (payload, _) -> LockpickClient.onState(payload));
        event.register(CampSyncPayload.TYPE, (payload, _) -> CampClientState.accept(payload));
        event.register(
            WorkbenchResultPayload.TYPE,
            (payload, _) -> {
                var screen = Minecraft.getInstance().gui.screen();
                if (screen instanceof WorkbenchScreen workbench) {
                    workbench.showResult(payload.result());
                } else if (screen instanceof TinkersBenchScreen tinkers) {
                    tinkers.showResult(payload.result());
                }
            }
        );
    }
}
