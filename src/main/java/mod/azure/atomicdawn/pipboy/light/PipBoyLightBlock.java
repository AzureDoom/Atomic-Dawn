package mod.azure.atomicdawn.pipboy.light;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class PipBoyLightBlock extends Block implements SimpleWaterloggedBlock {

    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL;

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final int CHECK_TICKS = 10;

    public PipBoyLightBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 15).setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(LEVEL, WATERLOGGED);
    }

    @Override
    protected void onPlace(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState oldState,
        boolean movedByPiston
    ) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, CHECK_TICKS);
        }
    }

    @Override
    protected void tick(
        @NotNull BlockState state,
        @NotNull ServerLevel level,
        @NotNull BlockPos pos,
        @NotNull RandomSource random
    ) {
        if (PipBoyLightService.claims(level, pos)) {
            level.scheduleTick(pos, this, CHECK_TICKS);
        } else {
            level.setBlock(pos, emptied(state), Block.UPDATE_ALL);
        }
    }

    public static BlockState emptied(BlockState state) {
        return state.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected @NotNull BlockState updateShape(
        @NotNull BlockState state,
        @NotNull LevelReader level,
        @NotNull ScheduledTickAccess scheduledTickAccess,
        @NotNull BlockPos pos,
        @NotNull Direction direction,
        @NotNull BlockPos neighborPos,
        @NotNull BlockState neighborState,
        @NotNull RandomSource random
    ) {
        if (state.getValue(WATERLOGGED)) {
            scheduledTickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected @NotNull FluidState getFluidState(@NotNull BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected @NotNull VoxelShape getShape(
        @NotNull BlockState state,
        @NotNull BlockGetter level,
        @NotNull BlockPos pos,
        @NotNull CollisionContext context
    ) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(@NotNull BlockState state) {
        return true;
    }
}
