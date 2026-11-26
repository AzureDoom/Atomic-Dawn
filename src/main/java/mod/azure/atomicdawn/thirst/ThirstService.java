package mod.azure.atomicdawn.thirst;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.weapon.action.VatsActions;

public final class ThirstService {

    private ThirstService() {}

    public static boolean enabled() {
        return AtomicDawn.CONFIG.common.survival.thirst.hydration.THIRST_ENABLED;
    }

    public static float thirst(Player player) {
        return player.getData(ADAttachments.THIRST.get()).value();
    }

    public static boolean dehydrated(Player player) {
        return enabled() && !player.isCreative() && !player.isSpectator() && thirst(player) <= 0F;
    }

    public static float apMultiplier(Player player) {
        return dehydrated(player)
            ? (float) AtomicDawn.CONFIG.common.survival.thirst.hydration.DEHYDRATED_AP_MULTIPLIER
            : 1F;
    }

    public static void set(Player player, float value) {
        var next = new Thirst(value);
        if (Math.abs(next.value() - thirst(player)) < 1.0E-3F) {
            return;
        }
        player.setData(ADAttachments.THIRST.get(), next);
    }

    public static void drink(Player player, double amount) {
        if (!enabled() || amount <= 0) {
            return;
        }
        set(player, thirst(player) + (float) amount);
    }

    public static void tick(ServerPlayer player) {
        if (!enabled() || player.isCreative() || player.isSpectator() || !player.isAlive()) {
            return;
        }
        if (player.level().getDifficulty() == Difficulty.PEACEFUL) {
            if (thirst(player) < Thirst.MAX) {
                set(player, thirst(player) + 1F);
            }
            return;
        }
        var config = AtomicDawn.CONFIG.common.survival.thirst.hydration;
        var drain = (float) config.THIRST_DRAIN_PER_SECOND;
        if (player.isSprinting()) {
            drain *= (float) config.THIRST_SPRINT_MULTIPLIER;
        }
        if (drain > 0F) {
            set(player, thirst(player) - drain);
        }
        if (dehydrated(player)) {
            var ap = VatsActions.actionPoints(player);
            var max = VatsActions.maxActionPoints(player);
            if (ap.points() > max) {
                player.setData(ADAttachments.ACTION_POINTS.get(), ap.withPoints(ap.points(), max));
            }
        }
    }
}
