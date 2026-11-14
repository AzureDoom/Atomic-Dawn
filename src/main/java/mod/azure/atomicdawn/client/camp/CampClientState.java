package mod.azure.atomicdawn.client.camp;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import mod.azure.atomicdawn.network.CampActionPayload;
import mod.azure.atomicdawn.network.CampSyncPayload;

public final class CampClientState {

    private static final UUID NONE = new UUID(0L, 0L);

    @Nullable
    private static CampSyncPayload state;

    private static int revision;

    private CampClientState() {}

    public static void accept(CampSyncPayload payload) {
        state = payload;
        revision++;
    }

    public static void clear() {
        state = null;
        revision++;
    }

    public static @Nullable CampSyncPayload get() {
        return state;
    }

    public static int revision() {
        return revision;
    }

    public static void request() {
        send(CampActionPayload.Action.REQUEST, NONE, 0);
    }

    public static void send(CampActionPayload.Action action, UUID campId, int value) {
        if (Minecraft.getInstance().getConnection() != null) {
            ClientPacketDistributor.sendToServer(new CampActionPayload(action, campId, value));
        }
    }

    public static boolean canDeployAt(BlockPos pos) {
        var current = state;
        if (current == null || !current.enabled() || !current.dimensionAllowed()) {
            return false;
        }
        if (current.camp().isPresent()) {
            return false;
        }
        var selectedExists = current.slots().stream().anyMatch(slot -> slot.slotIndex() == current.selectedSlot());
        if (!selectedExists && current.slotsUsed() >= current.slotsMax()) {
            return false;
        }
        var spacing = (long) current.spacing();
        for (var other : current.nearby()) {
            long dx = other.origin().getX() - pos.getX();
            long dz = other.origin().getZ() - pos.getZ();
            if (dx * dx + dz * dz < spacing * spacing) {
                return false;
            }
        }
        return true;
    }
}
