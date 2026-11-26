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
import mod.azure.atomicdawn.helper.ADWorldgenUtil;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID)
public final class SkylineStormEvents {

    private SkylineStormEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity().level() instanceof ServerLevel level))
            return;
        var player = event.getEntity();
        if (player.tickCount % 100 != 0)
            return;
        if (!ADWorldgenUtil.biomeIs(level, player.blockPosition(), ADBiomes.SKYLINE_VALLEY))
            return;
        if (level.getRandom().nextInt(5) != 0)
            return;

        double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
        int radius = 24 + level.getRandom().nextInt(48 + 1);
        int x = player.getBlockX() + (int) Math.round(Math.cos(angle) * radius);
        int z = player.getBlockZ() + (int) Math.round(Math.sin(angle) * radius);
        BlockPos probe = new BlockPos(x, player.getBlockY(), z);
        if (!hasChunkAt(level, probe))
            return;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos strikePos = new BlockPos(x, y, z);
        if (!ADWorldgenUtil.biomeIs(level, strikePos, ADBiomes.SKYLINE_VALLEY))
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
