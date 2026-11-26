package mod.azure.atomicdawn.pipboy;

import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.network.ADNetwork;
import mod.azure.atomicdawn.network.PipBoyNotificationPayload;

public final class PipBoyNotifications {

    public enum Kind {

        LEVEL_UP,
        PERK_PICKS,
        CARDS_UNEQUIPPED,
        QUEST_STARTED,
        QUEST_UPDATED,
        QUEST_COMPLETED,
        LOCATION_DISCOVERED,
        BESTIARY_ENCOUNTERED,
        BESTIARY_STUDIED,
        HOLOTAPE_ADDED,
        LEGENDARY_SLOT;

        private static final Kind[] VALUES = values();

        public static Kind byIndex(int index) {
            return VALUES[Math.floorMod(index, VALUES.length)];
        }

        public String translationKey() {
            return "pipboy.atomicdawn.notification." + name().toLowerCase(Locale.ROOT);
        }
    }

    private PipBoyNotifications() {}

    public static void send(ServerPlayer player, Kind kind, int value) {
        send(player, kind, value, "");
    }

    public static void send(ServerPlayer player, Kind kind, int value, String detail) {
        if (!AtomicDawn.CONFIG.common.pipboy.general.PIPBOY_NOTIFICATIONS || player.connection == null) {
            return;
        }
        ADNetwork.sendTo(player, new PipBoyNotificationPayload(kind, value, detail));
    }
}
