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

    public static final int ROLL_SIGN = 1;

    public static final int ANIMATION_TICKS = 80;

    public static final int PULL_TICKS = 20;

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

    public static boolean isCorner(BlockState state) {
        int column = state.getValue(COLUMN);
        int row = state.getValue(ROW);
        return (column == 0 || column == SIZE - 1) && (row == 0 || row == SIZE - 1);
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

    public static int inward(boolean opening, int tick) {
        return opening ? (tick >= PULL_TICKS / 2 ? 1 : 0) : (tick < ANIMATION_TICKS - PULL_TICKS / 2 ? 1 : 0);
    }

    public static int lateral(boolean opening, int tick) {
        var rollTicks = ANIMATION_TICKS - PULL_TICKS;
        var progress = opening ? tick - PULL_TICKS : rollTicks - tick;
        return Mth.clamp(Math.round(progress * (float) SIZE / rollTicks), 0, SIZE);
    }

    public static List<BlockPos> doorCells(BlockPos master, Direction facing, int inward, int lateral) {
        var cells = new ArrayList<BlockPos>(SIZE * SIZE);
        for (var row = 0; row < SIZE; row++) {
            for (var column = 0; column < SIZE; column++) {
                cells.add(
                    master.relative(right(facing), ROLL_SIGN * (column - CENTER + lateral))
                        .relative(facing.getOpposite(), inward)
                        .above(row)
                );
            }
        }
        return cells;
    }

    public static Set<BlockPos> sweptCells(BlockPos master, Direction facing) {
        var cells = new HashSet<BlockPos>();
        for (var lateral = 0; lateral <= SIZE; lateral++) {
            cells.addAll(doorCells(master, facing, 1, lateral));
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
        return !state.getValue(OPEN) || isCorner(state) ? Shapes.block() : Shapes.empty();
    }

    @Override
    protected @NotNull VoxelShape getCollisionShape(
        @NotNull BlockState state,
        @NotNull BlockGetter level,
        @NotNull BlockPos pos,
        @NotNull CollisionContext context
    ) {
        return !state.getValue(OPEN) || isCorner(state) ? Shapes.block() : Shapes.empty();
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
