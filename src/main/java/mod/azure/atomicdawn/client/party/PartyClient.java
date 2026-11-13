package mod.azure.atomicdawn.client.party;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import java.util.List;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.network.PartySyncPayload;
import mod.azure.atomicdawn.party.PartyService;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class PartyClient {

    private static PartySyncPayload party = PartySyncPayload.NONE;

    private PartyClient() {}

    public static void onSync(PartySyncPayload payload) {
        party = payload;
        PartyService.clientHasOnlineMates = others().stream().anyMatch(PartySyncPayload.Member::online);
    }

    public static boolean inParty() {
        return !party.members().isEmpty();
    }

    public static int maxSize() {
        return party.maxSize();
    }

    public static List<PartySyncPayload.Member> members() {
        return party.members();
    }

    public static List<PartySyncPayload.Member> others() {
        var player = Minecraft.getInstance().player;
        return player == null
            ? party.members()
            : party.members().stream().filter(member -> !member.id().equals(player.getUUID())).toList();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        party = PartySyncPayload.NONE;
        PartyService.clientHasOnlineMates = false;
    }
}
