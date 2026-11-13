package mod.azure.atomicdawn.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import mod.azure.atomicdawn.network.ADNetwork;
import mod.azure.atomicdawn.network.HealthClampPayload;

public final class HealthClamp {

    private HealthClamp() {}

    public static void clampToMax(Player player) {
        var max = player.getMaxHealth();
        if (!player.isAlive() || player.getHealth() <= max) {
            return;
        }
        var clamped = Math.max(1F, max);
        if (player instanceof ServerPlayer serverPlayer) {
            ADNetwork.sendTo(serverPlayer, new HealthClampPayload(clamped));
        }
        player.setHealth(clamped);
    }
}
