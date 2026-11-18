package mod.azure.atomicdawn.client.camp;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import mod.azure.atomicdawn.camp.CampSlotService;
import mod.azure.atomicdawn.network.CampRestorePayload;

public final class CampRestoreClient {

    private static final long HIGHLIGHT_TICKS = 2400L;

    @Nullable
    private static CampRestorePayload current;

    private static long expiresAt;

    private CampRestoreClient() {}

    public static void accept(CampRestorePayload payload) {
        var minecraft = Minecraft.getInstance();
        if (!payload.open()) {
            current = null;
            if (minecraft.gui.screen() instanceof CampRestoreScreen screen) {
                screen.onClose();
            }
            return;
        }
        current = payload;
        var level = minecraft.level;
        expiresAt = level == null ? 0L : level.getGameTime() + HIGHLIGHT_TICKS;
        minecraft.gui.setScreen(new CampRestoreScreen(payload));
    }

    public static @Nullable CampRestorePayload current() {
        var level = Minecraft.getInstance().level;
        if (current != null && (level == null || level.getGameTime() > expiresAt)) {
            current = null;
        }
        return current;
    }

    public static List<CampSlotService.Blocker> blockers() {
        var payload = current();
        return payload == null ? List.of() : payload.blockers();
    }

    public static void clear() {
        current = null;
    }
}
