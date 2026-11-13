package mod.azure.atomicdawn.pipboy.bestiary;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import mod.azure.atomicdawn.pipboy.PipBoyNotifications;
import mod.azure.atomicdawn.registry.ADAttachments;

public final class BestiaryService {

    private BestiaryService() {}

    public static BestiaryLog log(ServerPlayer player) {
        return player.getData(ADAttachments.BESTIARY.get());
    }

    public static void encounter(ServerPlayer player, LivingEntity entity) {
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        var log = log(player);
        if (log.encountered(id) || log.kills().size() >= BestiaryLog.MAX_ENTRIES) {
            return;
        }
        if (BestiaryEntry.forEntity(player.level().registryAccess(), id).isEmpty()) {
            return;
        }
        player.setData(ADAttachments.BESTIARY.get(), log.with(id, 0));
        PipBoyNotifications.send(
            player,
            PipBoyNotifications.Kind.BESTIARY_ENCOUNTERED,
            0,
            entity.getType().getDescriptionId()
        );
    }

    public static void recordKill(ServerPlayer player, LivingEntity entity) {
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        var entry = BestiaryEntry.forEntity(player.level().registryAccess(), id).orElse(null);
        if (entry == null) {
            return;
        }
        var log = log(player);
        if (!log.encountered(id) && log.kills().size() >= BestiaryLog.MAX_ENTRIES) {
            return;
        }
        var kills = log.killCount(id) + 1;
        player.setData(ADAttachments.BESTIARY.get(), log.with(id, kills));
        if (kills == entry.value().studyKills()) {
            PipBoyNotifications.send(
                player,
                PipBoyNotifications.Kind.BESTIARY_STUDIED,
                0,
                entity.getType().getDescriptionId()
            );
        }
    }
}
