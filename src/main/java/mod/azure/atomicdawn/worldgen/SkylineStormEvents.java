package mod.azure.atomicdawn.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import mod.azure.atomicdawn.AtomicDawn;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID)
public final class SkylineStormEvents {

    private static final int CHECK_INTERVAL_TICKS = 100;

    private static final int STRIKE_CHANCE_DENOMINATOR = 5;

    private static final int MIN_DISTANCE = 24;

    private static final int EXTRA_DISTANCE = 48;

    private SkylineStormEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity().level() instanceof ServerLevel level))
            return;
        var player = event.getEntity();
        if (player.tickCount % CHECK_INTERVAL_TICKS != 0)
            return;
        if (!level.getBiome(player.blockPosition()).is(ADBiomes.SKYLINE_VALLEY))
            return;
        if (level.getRandom().nextInt(STRIKE_CHANCE_DENOMINATOR) != 0)
            return;

        double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
        int radius = MIN_DISTANCE + level.getRandom().nextInt(EXTRA_DISTANCE + 1);
        int x = player.getBlockX() + (int) Math.round(Math.cos(angle) * radius);
        int z = player.getBlockZ() + (int) Math.round(Math.sin(angle) * radius);
        BlockPos probe = new BlockPos(x, player.getBlockY(), z);
        if (!hasChunkAt(level, probe))
            return;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos strikePos = new BlockPos(x, y, z);
        if (!level.getBiome(strikePos).is(ADBiomes.SKYLINE_VALLEY))
            return;

        LightningBolt lightning = new LightningBolt(EntityTypes.LIGHTNING_BOLT, level);
        lightning.setPos(x + 0.5, y, z + 0.5);
        lightning.setVisualOnly(true);
        level.addFreshEntity(lightning);
    }

    public static boolean hasChunkAt(LevelReader level, BlockPos pos) {
        return level.getChunk(
            SectionPos.blockToSectionCoord(pos.getX()),
            SectionPos.blockToSectionCoord(pos.getZ()),
            ChunkStatus.FULL,
            false
        ) != null;
    }
}
