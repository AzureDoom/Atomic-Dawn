package mod.azure.atomicdawn.radiation;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.worldgen.ADBiomes;

public final class RadStormService {

    private RadStormService() {}

    public static boolean enabled() {
        return AtomicDawn.CONFIG.environmentalRadiation.RAD_STORMS_ENABLED;
    }

    public static RadStorm state(ServerLevel level) {
        return level.hasData(ADAttachments.LEVEL_RAD_STORM.get())
            ? level.getData(ADAttachments.LEVEL_RAD_STORM.get())
            : RadStorm.NONE;
    }

    public static boolean active(ServerLevel level) {
        return enabled() && state(level).active() && level.isRaining();
    }

    public static void tick(ServerLevel level) {
        if (!level.dimensionType().hasSkyLight()) {
            return;
        }
        var state = state(level);
        var raining = level.isRaining();
        if (raining == state.raining()) {
            return;
        }
        var active = raining && enabled() && level.getRandom()
            .nextDouble() < AtomicDawn.CONFIG.environmentalRadiation.RAD_STORM_CHANCE;
        level.setData(ADAttachments.LEVEL_RAD_STORM.get(), new RadStorm(raining, active));
        if (!active) {
            return;
        }
        for (var player : level.players()) {
            if (level.getBiome(player.blockPosition()).is(ADBiomes.WASTELAND)) {
                player.sendSystemMessage(
                    Component.translatable("message.atomicdawn.rad_storm").withStyle(ChatFormatting.GREEN),
                    true
                );
            }
        }
    }

    public static void sync(Player player) {
        var active = player.level() instanceof ServerLevel level && active(level);
        if (player.getData(ADAttachments.RAD_STORM.get()) != active) {
            player.setData(ADAttachments.RAD_STORM.get(), active);
        }
    }

    public static boolean exposed(ServerLevel level, Player player) {
        if (!active(level) || !level.getBiome(player.blockPosition()).is(ADBiomes.WASTELAND)) {
            return false;
        }
        return level.canSeeSky(BlockPos.containing(player.getEyePosition()));
    }

    public static float radiation(ServerLevel level, Player player) {
        return exposed(level, player)
            ? (float) AtomicDawn.CONFIG.environmentalRadiation.RAD_STORM_RADS_PER_SECOND
            : 0F;
    }
}
