package mod.azure.atomicdawn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class FissureVentBlock extends Block {

    private static final DustParticleOptions MIST = new DustParticleOptions(0x8BFF4A, 2.2F);

    private static final DustParticleOptions SPARK = new DustParticleOptions(0xD4FF8A, 0.8F);

    public FissureVentBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull RandomSource random
    ) {
        if (!level.getBlockState(pos.above()).isAir()) {
            return;
        }
        for (var i = 0; i < 2; i++) {
            level.addParticle(
                random.nextInt(4) == 0 ? SPARK : MIST,
                pos.getX() + 0.2 + random.nextDouble() * 0.6,
                pos.getY() + 1.05,
                pos.getZ() + 0.2 + random.nextDouble() * 0.6,
                (random.nextDouble() - 0.5) * 0.02,
                0.06 + random.nextDouble() * 0.05,
                (random.nextDouble() - 0.5) * 0.02
            );
        }
        if (random.nextInt(6) == 0) {
            level.addParticle(
                ParticleTypes.CAMPFIRE_COSY_SMOKE,
                pos.getX() + 0.5,
                pos.getY() + 1.1,
                pos.getZ() + 0.5,
                0,
                0.04,
                0
            );
        }
    }
}
