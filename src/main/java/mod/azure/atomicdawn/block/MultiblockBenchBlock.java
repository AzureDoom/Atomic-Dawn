package mod.azure.atomicdawn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public abstract class MultiblockBenchBlock extends BaseEntityBlock {

    public static final int WIDTH = 2;

    public static final int HEIGHT = 2;

    public static final int DEPTH = 1;

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    @Nullable
    public static final IntegerProperty COLUMN = WIDTH > 1 ? IntegerProperty.create("column", 0, WIDTH - 1) : null;

    @Nullable
    public static final IntegerProperty ROW = HEIGHT > 1 ? IntegerProperty.create("row", 0, HEIGHT - 1) : null;

    @Nullable
    public static final IntegerProperty LAYER = DEPTH > 1 ? IntegerProperty.create("layer", 0, DEPTH - 1) : null;

    private static final VoxelShape TABLE = Block.box(0, 0, 0, 16, 16, 16);

    private static final Map<Direction, VoxelShape> BACK_PANEL = rotations(Block.box(0, 0, 12, 16, 16, 16));

    protected MultiblockBenchBlock(Properties properties) {
        super(properties);
        var state = this.stateDefinition.any().setValue(FACING, Direction.NORTH);
        state = setCell(state, 0, 0, 0);
        this.registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(FACING);
        if (COLUMN != null) {
            builder.add(COLUMN);
        }
        if (ROW != null) {
            builder.add(ROW);
        }
        if (LAYER != null) {
            builder.add(LAYER);
        }
    }

    public static int column(BlockState state) {
        return COLUMN == null ? 0 : state.getValue(COLUMN);
    }

    public static int row(BlockState state) {
        return ROW == null ? 0 : state.getValue(ROW);
    }

    public static int layer(BlockState state) {
        return LAYER == null ? 0 : state.getValue(LAYER);
    }

    public static boolean isMaster(BlockState state) {
        return column(state) == 0 && row(state) == 0 && layer(state) == 0;
    }

    private static BlockState setCell(BlockState state, int column, int row, int layer) {
        if (COLUMN != null) {
            state = state.setValue(COLUMN, column);
        }
        if (ROW != null) {
            state = state.setValue(ROW, row);
        }
        if (LAYER != null) {
            state = state.setValue(LAYER, layer);
        }
        return state;
    }

    public static Direction right(Direction facing) {
        return facing.getCounterClockWise();
    }

    public static BlockPos cellPos(BlockPos master, Direction facing, int column, int row, int layer) {
        return master.relative(right(facing), column).above(row).relative(facing.getOpposite(), layer);
    }

    public static BlockPos masterPos(BlockPos pos, BlockState state) {
        var facing = state.getValue(FACING);
        return pos.relative(right(facing), -column(state))
            .below(row(state))
            .relative(facing.getOpposite(), -layer(state));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        var facing = context.getHorizontalDirection().getOpposite();
        var clicked = context.getClickedPos();
        var level = context.getLevel();

        for (var column = 0; column < WIDTH; column++) {
            var master = clicked.relative(right(facing), -column);
            if (canOccupy(level, master, facing, context)) {
                return setCell(defaultBlockState().setValue(FACING, facing), column, 0, 0);
            }
        }
        return null;
    }

    private boolean canOccupy(Level level, BlockPos master, Direction facing, BlockPlaceContext context) {
        var player = context.getPlayer();
        var collision = player == null ? CollisionContext.empty() : CollisionContext.of(player);
        for (var layer = 0; layer < DEPTH; layer++) {
            for (var row = 0; row < HEIGHT; row++) {
                for (var column = 0; column < WIDTH; column++) {
                    var pos = cellPos(master, facing, column, row, layer);
                    if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
                        return false;
                    }
                    if (pos.equals(context.getClickedPos())) {
                        continue;
                    }
                    if (!level.getBlockState(pos).canBeReplaced(context)) {
                        return false;
                    }
                    var cellState = setCell(defaultBlockState().setValue(FACING, facing), column, row, layer);
                    if (!level.isUnobstructed(cellState, pos, collision)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void setPlacedBy(
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState state,
        @Nullable LivingEntity placer,
        @NotNull ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) {
            return;
        }
        var facing = state.getValue(FACING);
        var master = masterPos(pos, state);
        for (var layer = 0; layer < DEPTH; layer++) {
            for (var row = 0; row < HEIGHT; row++) {
                for (var column = 0; column < WIDTH; column++) {
                    var cell = cellPos(master, facing, column, row, layer);
                    if (!cell.equals(pos)) {
                        level.setBlock(cell, setCell(state, column, row, layer), Block.UPDATE_ALL);
                    }
                }
            }
        }
    }

    @Override
    public @NotNull BlockState playerWillDestroy(
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState state,
        @NotNull Player player
    ) {
        if (!level.isClientSide()) {
            var master = masterPos(pos, state);
            var facing = state.getValue(FACING);
            var masterState = level.getBlockState(master);
            if (
                !isMaster(state)
                    && !player.preventsBlockDrops()
                    && masterState.is(this)
                    && isMaster(masterState)
                    && player.hasCorrectToolForDrops(masterState, level, master)
            ) {
                Block.dropResources(
                    masterState,
                    level,
                    master,
                    level.getBlockEntity(master),
                    player,
                    player.getMainHandItem()
                );
            }
            for (var layer = 0; layer < DEPTH; layer++) {
                for (var row = 0; row < HEIGHT; row++) {
                    for (var column = 0; column < WIDTH; column++) {
                        var cell = cellPos(master, facing, column, row, layer);
                        if (cell.equals(pos)) {
                            continue;
                        }
                        var cellState = level.getBlockState(cell);
                        if (
                            cellState.is(this)
                                && cellState.getValue(FACING) == facing
                                && column(cellState) == column
                                && row(cellState) == row
                                && layer(cellState) == layer
                        ) {
                            level.setBlock(
                                cell,
                                Blocks.AIR.defaultBlockState(),
                                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE
                            );
                            level.levelEvent(2001, cell, Block.getId(cellState));
                        }
                    }
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected @NotNull List<ItemStack> getDrops(@NotNull BlockState state, LootParams.@NotNull Builder params) {
        return isMaster(state) ? super.getDrops(state, params) : List.of();
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
        var facing = state.getValue(FACING);
        var column = column(state);
        var row = row(state);
        var layer = layer(state);

        var neighborColumn = column;
        var neighborRow = row;
        var neighborLayer = layer;
        if (direction == right(facing)) {
            neighborColumn++;
        } else if (direction == right(facing).getOpposite()) {
            neighborColumn--;
        } else if (direction == Direction.UP) {
            neighborRow++;
        } else if (direction == Direction.DOWN) {
            neighborRow--;
        } else if (direction == facing.getOpposite()) {
            neighborLayer++;
        } else {
            neighborLayer--;
        }

        var neighborIsCell = neighborColumn >= 0 && neighborColumn < WIDTH
            && neighborRow >= 0 && neighborRow < HEIGHT
            && neighborLayer >= 0 && neighborLayer < DEPTH;

        if (neighborIsCell) {
            var intact = neighborState.is(this)
                && neighborState.getValue(FACING) == facing
                && column(neighborState) == neighborColumn
                && row(neighborState) == neighborRow
                && layer(neighborState) == neighborLayer;
            if (!intact) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull BlockHitResult hitResult
    ) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            var master = masterPos(pos, state);
            if (level.getBlockEntity(master) instanceof BenchBlockEntity bench) {
                serverPlayer.openMenu(bench, master);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return isMaster(state) ? newMasterEntity(pos, state) : null;
    }

    protected abstract BenchBlockEntity newMasterEntity(BlockPos pos, BlockState state);

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
        if (row(state) == 0) {
            return TABLE;
        }
        return layer(state) == DEPTH - 1 ? BACK_PANEL.get(state.getValue(FACING)) : Shapes.block();
    }

    @Override
    protected @NotNull BlockState rotate(@NotNull BlockState state, @NotNull Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected @NotNull BlockState mirror(@NotNull BlockState state, @NotNull Mirror mirror) {
        return state;
    }

    private static Map<Direction, VoxelShape> rotations(VoxelShape north) {
        var map = new EnumMap<Direction, VoxelShape>(Direction.class);
        var bounds = north.bounds();
        for (var facing : Direction.Plane.HORIZONTAL) {
            var minX = bounds.minX;
            var minZ = bounds.minZ;
            var maxX = bounds.maxX;
            var maxZ = bounds.maxZ;
            var steps = switch (facing) {
                case EAST -> 1;
                case SOUTH -> 2;
                case WEST -> 3;
                default -> 0;
            };
            for (var i = 0; i < steps; i++) {
                var nMinX = 1 - maxZ;
                var nMaxX = 1 - minZ;
                minZ = minX;
                maxZ = maxX;
                minX = nMinX;
                maxX = nMaxX;
            }
            map.put(facing, Shapes.box(minX, bounds.minY, minZ, maxX, bounds.maxY, maxZ));
        }
        return map;
    }
}
