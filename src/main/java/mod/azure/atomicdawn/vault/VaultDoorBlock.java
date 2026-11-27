package mod.azure.atomicdawn.vault;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mod.azure.atomicdawn.registry.ADBlockEntities;

public class VaultDoorBlock extends BaseEntityBlock {

    public static final int SIZE = 5;

    public static final int CENTER = SIZE / 2;

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public static final IntegerProperty COLUMN = IntegerProperty.create("column", 0, SIZE - 1);

    public static final IntegerProperty ROW = IntegerProperty.create("row", 0, SIZE - 1);

    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public static final int ANIMATION_TICKS = 160;

    public static final int UNLOCK_TICKS = 30;

    public static final int PULL_TICKS = 60;

    public static final int ROLL_TICKS = ANIMATION_TICKS - UNLOCK_TICKS - PULL_TICKS;

    public static final float PULL_DEPTH = 24.0F;

    public static final float DOOR_HALF_DEPTH = 10.0F;

    public static final float FRAME_HALF_DEPTH = 8.0F;

    private static final double BORE_RADIUS = 34.5;

    private static final double BORE_CENTER = 41.0;

    private static final int SHAPE_STEP = 2;

    private static final VoxelShape[] FRAME_SHAPES = buildFrameShapes();

    public VaultDoorBlock(Properties properties) {
        super(properties);
        registerDefaultState(
            stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(COLUMN, CENTER)
                .setValue(ROW, 0)
                .setValue(OPEN, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(FACING, COLUMN, ROW, OPEN);
    }

    public static boolean isMaster(BlockState state) {
        return state.getValue(COLUMN) == CENTER && state.getValue(ROW) == 0;
    }

    public static Direction right(Direction facing) {
        return facing.getCounterClockWise();
    }

    public static BlockPos cellPos(BlockPos master, Direction facing, int column, int row) {
        return master.relative(right(facing), column - CENTER).above(row);
    }

    public static BlockPos masterPos(BlockPos pos, BlockState state) {
        return pos.relative(right(state.getValue(FACING)), CENTER - state.getValue(COLUMN)).below(state.getValue(ROW));
    }

    private static int shapeIndex(Direction facing, int column, int row) {
        return (facing.get2DDataValue() * SIZE + row) * SIZE + column;
    }

    private static VoxelShape[] buildFrameShapes() {
        var shapes = new VoxelShape[4 * SIZE * SIZE];
        for (var facing : Direction.Plane.HORIZONTAL) {
            for (var row = 0; row < SIZE; row++) {
                for (var column = 0; column < SIZE; column++) {
                    shapes[shapeIndex(facing, column, row)] = buildFrameShape(facing, column, row);
                }
            }
        }
        return shapes;
    }

    private static double boreHalfWidth(double y) {
        var dy = y - BORE_CENTER;
        var circle = Math.abs(dy) < BORE_RADIUS ? Math.sqrt(BORE_RADIUS * BORE_RADIUS - dy * dy) : 0.0;
        return y < BORE_CENTER ? Math.max(circle, 16.0) : circle;
    }

    private static VoxelShape buildFrameShape(Direction facing, int column, int row) {
        var shape = Shapes.empty();
        var left = (column - CENTER) * 16.0 - 8.0;
        var bottom = row * 16.0;
        var half = SIZE * 8.0;
        for (var band = 0; band < 16; band += SHAPE_STEP) {
            var open = Math.min(boreHalfWidth(bottom + band), boreHalfWidth(bottom + band + SHAPE_STEP));
            shape = Shapes.or(shape, span(facing, left, -half, -open, band, band + SHAPE_STEP, 0.0, 16.0));
            shape = Shapes.or(shape, span(facing, left, open, half, band, band + SHAPE_STEP, 0.0, 16.0));
        }
        if (row == 0) {
            shape = Shapes.or(
                shape,
                span(
                    facing,
                    left,
                    -21.0,
                    21.0,
                    0.0,
                    8.0,
                    2.0,
                    14.0
                )
            );
        }
        return shape.optimize();
    }

    private static VoxelShape span(
        Direction facing,
        double left,
        double from,
        double to,
        double minY,
        double maxY,
        double minDepth,
        double maxDepth
    ) {
        var start = Math.max(from, left) - left;
        var end = Math.min(to, left + 16.0) - left;
        if (end - start <= 1.0E-4) {
            return Shapes.empty();
        }
        var right = right(facing);
        var positive = right.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        var min = positive ? start : 16.0 - end;
        var max = positive ? end : 16.0 - start;
        return right.getAxis() == Direction.Axis.X
            ? Block.box(min, minY, minDepth, max, maxY, maxDepth)
            : Block.box(minDepth, minY, min, maxDepth, maxY, max);
    }

    public static VoxelShape frameShape(BlockState state) {
        return FRAME_SHAPES[shapeIndex(state.getValue(FACING), state.getValue(COLUMN), state.getValue(ROW))];
    }

    public static AABB renderBounds(BlockPos master) {
        return new AABB(master).inflate(SIZE + 3, 0, SIZE + 3).expandTowards(0, SIZE + 1, 0);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        var facing = context.getHorizontalDirection().getOpposite();
        var master = context.getClickedPos();
        var level = context.getLevel();
        for (var row = 0; row < SIZE; row++) {
            for (var column = 0; column < SIZE; column++) {
                var pos = cellPos(master, facing, column, row);
                if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
                    return null;
                }
                if (!pos.equals(master) && !level.getBlockState(pos).canBeReplaced(context)) {
                    return null;
                }
            }
        }
        return defaultBlockState().setValue(FACING, facing);
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
        for (var row = 0; row < SIZE; row++) {
            for (var column = 0; column < SIZE; column++) {
                var cell = cellPos(pos, facing, column, row);
                if (!cell.equals(pos)) {
                    level.setBlock(cell, state.setValue(COLUMN, column).setValue(ROW, row), Block.UPDATE_ALL);
                }
            }
        }
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull BlockHitResult hit
    ) {
        if (level instanceof ServerLevel serverLevel) {
            toggle(serverLevel, masterPos(pos, state));
        }
        return InteractionResult.SUCCESS;
    }

    public static void toggle(ServerLevel level, BlockPos master) {
        var masterState = level.getBlockState(master);
        if (!(masterState.getBlock() instanceof VaultDoorBlock) || !isMaster(masterState)) {
            return;
        }
        if (
            !(level.getBlockEntity(master) instanceof VaultDoorBlockEntity door) || !door.tryToggle(level.getGameTime())
        ) {
            level.playSound(
                null,
                master.above(CENTER),
                SoundEvents.STONE_BUTTON_CLICK_OFF,
                SoundSource.BLOCKS,
                0.8F,
                0.6F
            );
            return;
        }
        var opening = !masterState.getValue(OPEN);
        door.start(opening);
        level.blockEvent(master, masterState.getBlock(), VaultDoorBlockEntity.EVENT_ANIMATE, opening ? 1 : 0);
        var center = master.above(CENTER);
        level.playSound(null, center, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.6F, 0.55F);
        level.playSound(null, center, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.5F, 0.45F);
        level.playSound(
            null,
            center,
            opening ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE,
            SoundSource.BLOCKS,
            1.5F,
            0.5F
        );
        level.scheduleTick(master, masterState.getBlock(), ANIMATION_TICKS);
    }

    public static void setOpen(Level level, BlockPos master, Direction facing, boolean open) {
        for (var row = 0; row < SIZE; row++) {
            for (var column = 0; column < SIZE; column++) {
                var cell = cellPos(master, facing, column, row);
                var cellState = level.getBlockState(cell);
                if (
                    cellState.getBlock() instanceof VaultDoorBlock
                        && cellState.getValue(FACING) == facing
                        && cellState.getValue(OPEN) != open
                ) {
                    level.setBlock(cell, cellState.setValue(OPEN, open), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private static int timeline(boolean opening, int tick) {
        return Mth.clamp(opening ? tick : ANIMATION_TICKS - tick, 0, ANIMATION_TICKS);
    }

    public static float pullOffset(boolean opening, int tick) {
        var progress = (timeline(opening, tick) - UNLOCK_TICKS) / (float) PULL_TICKS;
        return PULL_DEPTH * Mth.clamp(progress, 0.0F, 1.0F);
    }

    public static int lateral(boolean opening, int tick) {
        var progress = (timeline(opening, tick) - UNLOCK_TICKS - PULL_TICKS) / (float) ROLL_TICKS;
        return Mth.clamp(Math.round(progress * SIZE), 0, SIZE);
    }

    public static boolean passable(float offset) {
        return offset - DOOR_HALF_DEPTH >= FRAME_HALF_DEPTH;
    }

    private static float depthOverlap(int depth, float offset) {
        var low = Math.max(depth * 16.0F - FRAME_HALF_DEPTH, offset - DOOR_HALF_DEPTH);
        var high = Math.min(depth * 16.0F + FRAME_HALF_DEPTH, offset + DOOR_HALF_DEPTH);
        return high - low;
    }

    private static void addLayer(
        Collection<BlockPos> cells,
        BlockPos master,
        Direction facing,
        int depth,
        int lateral
    ) {
        for (var row = 0; row < SIZE; row++) {
            for (var column = 0; column < SIZE; column++) {
                cells.add(
                    master.relative(right(facing), (column - CENTER + lateral))
                        .relative(facing.getOpposite(), depth)
                        .above(row)
                );
            }
        }
    }

    public static List<BlockPos> doorCells(BlockPos master, Direction facing, float offset, int lateral) {
        var cells = new ArrayList<BlockPos>(5 * 5 * 2);
        for (var depth = 1; depth <= 2; depth++) {
            if (depthOverlap(depth, offset) >= 4.0F) {
                addLayer(cells, master, facing, depth, lateral);
            }
        }
        return cells;
    }

    public static Set<BlockPos> sweptCells(BlockPos master, Direction facing) {
        var cells = new HashSet<BlockPos>();
        for (var lateral = 0; lateral <= 5; lateral++) {
            for (var depth = 1; depth <= 2; depth++) {
                addLayer(cells, master, facing, depth, lateral);
            }
        }
        return cells;
    }

    @Override
    protected void tick(
        @NotNull BlockState state,
        @NotNull ServerLevel level,
        @NotNull BlockPos pos,
        @NotNull RandomSource random
    ) {
        if (!isMaster(state)) {
            return;
        }
        var center = pos.above(CENTER);
        level.playSound(null, center, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.2F, 0.7F);
        level.playSound(null, center, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.5F, 0.4F);
    }

    @Override
    protected @NotNull VoxelShape getShape(
        @NotNull BlockState state,
        @NotNull BlockGetter level,
        @NotNull BlockPos pos,
        @NotNull CollisionContext context
    ) {
        return state.getValue(OPEN) ? frameShape(state) : Shapes.block();
    }

    @Override
    protected @NotNull VoxelShape getCollisionShape(
        @NotNull BlockState state,
        @NotNull BlockGetter level,
        @NotNull BlockPos pos,
        @NotNull CollisionContext context
    ) {
        return state.getValue(OPEN) ? frameShape(state) : Shapes.block();
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
            if (level.getBlockEntity(master) instanceof VaultDoorBlockEntity door) {
                door.clearBarriers(level);
            }
            VaultDoorBlockEntity.sweep(level, master, facing);
            for (var row = 0; row < SIZE; row++) {
                for (var column = 0; column < SIZE; column++) {
                    var cell = cellPos(master, facing, column, row);
                    if (!cell.equals(pos) && level.getBlockState(cell).getBlock() instanceof VaultDoorBlock) {
                        level.setBlock(cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
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
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return isMaster(state) ? new VaultDoorBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
        @NotNull Level level,
        @NotNull BlockState state,
        @NotNull BlockEntityType<T> type
    ) {
        if (!isMaster(state)) {
            return null;
        }
        return level.isClientSide()
            ? createTickerHelper(type, ADBlockEntities.VAULT_DOOR.get(), VaultDoorBlockEntity::clientTick)
            : createTickerHelper(type, ADBlockEntities.VAULT_DOOR.get(), VaultDoorBlockEntity::serverTick);
    }

    @Override
    protected @NotNull BlockState rotate(@NotNull BlockState state, @NotNull Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected @NotNull BlockState mirror(@NotNull BlockState state, @NotNull Mirror mirror) {
        return state;
    }
}
