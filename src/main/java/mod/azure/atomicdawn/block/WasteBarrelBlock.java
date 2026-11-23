package mod.azure.atomicdawn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class WasteBarrelBlock extends Block {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE = Block.box(1.5, 0, 1.5, 14.5, 16, 14.5);

    private static final DustParticleOptions OOZE = new DustParticleOptions(0x7CFF3A, 0.9F);

    private final boolean leaking;

    public WasteBarrelBlock(Properties properties, boolean leaking) {
        super(properties);
        this.leaking = leaking;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public boolean leaking() {
        return leaking;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected @NotNull BlockState rotate(@NotNull BlockState state, @NotNull Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected @NotNull BlockState mirror(@NotNull BlockState state, @NotNull Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
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
    public void animateTick(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull RandomSource random
    ) {
        if (!leaking || random.nextInt(3) != 0) {
            return;
        }
        var facing = state.getValue(FACING);
        var x = pos.getX() + 0.5 + facing.getStepX() * 0.42 + (random.nextDouble() - 0.5) * 0.3;
        var z = pos.getZ() + 0.5 + facing.getStepZ() * 0.42 + (random.nextDouble() - 0.5) * 0.3;
        var y = pos.getY() + 0.3 + random.nextDouble() * 0.6;
        level.addParticle(OOZE, x, y, z, 0, -0.04, 0);
        if (random.nextInt(4) == 0) {
            level.addParticle(
                OOZE,
                pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.5,
                pos.getY() + 1.05,
                pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.5,
                0,
                0.03,
                0
            );
        }
    }
}
