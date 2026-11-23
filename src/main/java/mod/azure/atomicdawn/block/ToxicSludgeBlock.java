package mod.azure.atomicdawn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class ToxicSludgeBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    private static final DustParticleOptions BUBBLE = new DustParticleOptions(0x9CFF4A, 0.7F);

    public ToxicSludgeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull VoxelShape getShape(
        @NotNull BlockState state,
        @NotNull BlockGetter level,
        @NotNull BlockPos pos,
        @NotNull CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
        var below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected @NotNull BlockState updateShape(
        @NotNull BlockState state,
        @NotNull LevelReader level,
        @NotNull ScheduledTickAccess ticks,
        @NotNull BlockPos pos,
        @NotNull Direction direction,
        @NotNull BlockPos neighborPos,
        @NotNull BlockState neighborState,
        @NotNull RandomSource random
    ) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos)
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public void animateTick(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull RandomSource random
    ) {
        if (random.nextInt(5) != 0) {
            return;
        }
        level.addParticle(
            BUBBLE,
            pos.getX() + random.nextDouble(),
            pos.getY() + 0.1,
            pos.getZ() + random.nextDouble(),
            0,
            0.02,
            0
        );
    }
}
