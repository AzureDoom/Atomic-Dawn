package mod.azure.atomicdawn.pipboy;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.network.ADNetwork;
import mod.azure.atomicdawn.network.PipBoyStatsPayload;
import mod.azure.atomicdawn.registry.ADAttachments;

public final class PipBoyService {

    private static final Map<UUID, Long> LAST_STATS_REQUEST = new ConcurrentHashMap<>();

    private PipBoyService() {}

    public static PipBoyData data(Player player) {
        return player.getData(ADAttachments.PIPBOY.get());
    }

    public static void ensureDevice(ServerPlayer player) {
        var data = data(player);
        if (data.initialized() || !AtomicDawn.CONFIG.pipboy.PIPBOY_GRANT_ON_FIRST_JOIN) {
            return;
        }
        player.setData(ADAttachments.PIPBOY.get(), data.initializedWith(UUID.randomUUID()));
    }

    public static void toggleFavorite(ServerPlayer player, Identifier item) {
        if (!BuiltInRegistries.ITEM.containsKey(item)) {
            return;
        }
        var data = data(player);
        var favorites = new LinkedHashSet<>(data.favorites());
        if (!favorites.remove(item)) {
            if (favorites.size() >= AtomicDawn.CONFIG.pipboy.PIPBOY_MAX_FAVORITES) {
                return;
            }
            favorites.add(item);
        }
        player.setData(ADAttachments.PIPBOY.get(), data.withFavorites(favorites));
    }

    public static PipBoyStats stats(ServerPlayer player) {
        return player.getData(ADAttachments.PIPBOY_STATS.get());
    }

    public static void recordShot(ServerPlayer player) {
        player.setData(ADAttachments.PIPBOY_STATS.get(), stats(player).withShot());
    }

    public static void recordVats(ServerPlayer player, boolean hit, boolean critical) {
        player.setData(ADAttachments.PIPBOY_STATS.get(), stats(player).withVats(hit, critical));
    }

    public static void recordRads(ServerPlayer player, float rads) {
        if (rads >= 0.5F) {
            player.setData(ADAttachments.PIPBOY_STATS.get(), stats(player).withRads(Math.round(rads)));
        }
    }

    public static void sendStats(ServerPlayer player) {
        var now = player.level().getGameTime();
        var last = LAST_STATS_REQUEST.get(player.getUUID());
        if (last != null && now - last < 20) {
            return;
        }
        LAST_STATS_REQUEST.put(player.getUUID(), now);
        var vanilla = player.getStats();
        var distance = 0L;
        for (
            var stat : new Identifier[] {
                Stats.WALK_ONE_CM,
                Stats.SPRINT_ONE_CM,
                Stats.CROUCH_ONE_CM,
                Stats.SWIM_ONE_CM,
                Stats.WALK_ON_WATER_ONE_CM,
                Stats.WALK_UNDER_WATER_ONE_CM,
                Stats.CLIMB_ONE_CM,
                Stats.FLY_ONE_CM,
                Stats.HORSE_ONE_CM,
                Stats.BOAT_ONE_CM,
                Stats.MINECART_ONE_CM }
        ) {
            distance += vanilla.getValue(Stats.CUSTOM.get(stat));
        }
        var ad = stats(player);
        ADNetwork.sendTo(
            player,
            new PipBoyStatsPayload(
                vanilla.getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)),
                vanilla.getValue(Stats.CUSTOM.get(Stats.DEATHS)),
                vanilla.getValue(Stats.CUSTOM.get(Stats.MOB_KILLS)),
                vanilla.getValue(Stats.CUSTOM.get(Stats.PLAYER_KILLS)),
                distance,
                ad.shotsFired(),
                ad.vatsHits(),
                ad.vatsMisses(),
                ad.criticalHits(),
                ad.radsAbsorbed()
            )
        );
    }

    public static void forget(ServerPlayer player) {
        LAST_STATS_REQUEST.remove(player.getUUID());
    }
}
