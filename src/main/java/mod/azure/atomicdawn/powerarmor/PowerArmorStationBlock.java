package mod.azure.atomicdawn.powerarmor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import mod.azure.atomicdawn.util.PlayerItems;

public class PowerArmorStationBlock extends BaseEntityBlock {

    public static final int SIZE = 3;

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public static final IntegerProperty COLUMN = IntegerProperty.create("column", 0, SIZE - 1);

    public static final IntegerProperty ROW = IntegerProperty.create("row", 0, SIZE - 1);

    public static final IntegerProperty LAYER = IntegerProperty.create("layer", 0, SIZE - 1);

    private static final double PLATFORM_HEIGHT = 0.0;

    private static final double[][] MODEL_BOXES = {
        { -0.0948, 0.0074, 0.3076, 0.2052, 0.1574, 1.8826 },
        { -0.0573, 0.0449, 2.3326, 0.1677, 2.4074, 2.5576 },
        { -1.2573, 0.0449, 2.3326, -1.0323, 2.4074, 2.5576 },
        { -0.0666, 1.9737, 1.8167, 0.1583, 2.68, 2.6134 },
        { -1.2479, 1.9737, 1.8167, -1.0229, 2.68, 2.6134 },
        { -0.0573, 2.3214, 1.4511, 0.0927, 2.8189, 2.0104 },
        { -1.1854, 2.3214, 1.4324, -1.0354, 2.8189, 1.9917 },
        { -0.0479, 2.5089, 1.0011, 0.0646, 3.0064, 1.5604 },
        { -1.1823, 2.5089, 0.9824, -1.0698, 3.0064, 1.5417 },
        { -0.0479, 2.3137, 1.6785, 0.1021, 2.5752, 1.9285 },
        { -1.201, 2.3137, 1.6785, -1.051, 2.5752, 1.9285 },
        { -0.0104, 1.3093, 1.7909, 0.0646, 2.4753, 2.7936 },
        { -0.0479, 1.1794, 2.166, 0.1021, 1.4409, 2.416 },
        { -1.1635, 1.3093, 1.7909, -1.0885, 2.4753, 2.7936 },
        { -1.201, 1.1794, 2.166, -1.051, 1.4409, 2.416 },
        { -1.1858, 0.4314, 2.3795, 0.0376, 1.4885, 2.5295 },
        { -1.1483, 0, 2.3607, 0.0751, 1.0385, 2.5107 },
        { -0.0198, 0.0393, 1.5787, 0.1302, 1.0609, 2.4576 },
        { -1.2198, 0.0393, 1.5787, -1.0698, 1.0609, 2.4576 },
        { -1.2948, 0.0074, 0.3076, -0.9948, 0.1574, 1.8826 },
        { -1.2948, 0.0074, 1.8826, -0.9948, 0.1574, 2.5951 },
        { -0.0948, 0.0074, 1.8826, 0.2052, 0.1574, 2.5951 } };

    private static final Map<Direction, VoxelShape[]> SHAPES = shapes();

    public PowerArmorStationBlock(Properties properties) {
        super(properties);
        registerDefaultState(
            stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(COLUMN, 0)
                .setValue(ROW, 0)
                .setValue(LAYER, 0)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(FACING, COLUMN, ROW, LAYER);
    }

    public static boolean isMaster(BlockState state) {
        return state.getValue(COLUMN) == 0 && state.getValue(ROW) == 0 && state.getValue(LAYER) == 0;
    }

    public static Direction right(Direction facing) {
        return facing.getCounterClockWise();
    }

    public static BlockPos cellPos(BlockPos master, Direction facing, int column, int row, int layer) {
        return master.relative(right(facing), column).above(row).relative(facing.getOpposite(), layer);
    }

    public static BlockPos masterPos(BlockPos pos, BlockState state) {
        var facing = state.getValue(FACING);
        return pos.relative(right(facing), -state.getValue(COLUMN))
            .below(state.getValue(ROW))
            .relative(facing.getOpposite(), -state.getValue(LAYER));
    }

    public static boolean isStationMaster(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.getBlock() instanceof PowerArmorStationBlock && isMaster(state);
    }

    public static Vec3 dockPosition(BlockPos master, Direction facing) {
        var cell = cellPos(master, facing, 1, 0, 1);
        return new Vec3(cell.getX() + 0.5, cell.getY() + PLATFORM_HEIGHT, cell.getZ() + 0.5);
    }

    public static AABB bounds(BlockPos master, Direction facing) {
        var far = cellPos(master, facing, SIZE - 1, SIZE - 1, SIZE - 1);
        return new AABB(master).minmax(new AABB(far));
    }

    @Nullable
    public static PowerArmorEntity docked(Level level, BlockPos master) {
        var state = level.getBlockState(master);
        if (!(state.getBlock() instanceof PowerArmorStationBlock)) {
            return null;
        }
        var facing = state.getValue(FACING);
        var box = bounds(master, facing).inflate(0.5);
        PowerArmorEntity stray = null;
        for (var entity : level.getEntitiesOfClass(PowerArmorEntity.class, box)) {
            if (entity.isDocked() && master.equals(entity.stationPos())) {
                return entity;
            }
            if (!entity.isDocked() && entity.getPassengers().isEmpty() && stray == null) {
                stray = entity;
            }
        }
        if (stray != null && !level.isClientSide()) {
            stray.dock(master, dockPosition(master, facing), facing);
        }
        return stray;
    }

    public static void openMenu(ServerPlayer player, PowerArmorEntity armor) {
        player.openMenu(
            new SimpleMenuProvider(
                (id, inventory, _) -> new PowerArmorStationMenu(id, inventory, armor),
                Component.translatable("container.atomicdawn.power_armor_station")
            ),
            buffer -> buffer.writeVarInt(armor.getId())
        );
    }

    public static InteractionResult dockItem(ServerLevel level, BlockPos clicked, ItemStack stack, Player player) {
        var state = level.getBlockState(clicked);
        var master = masterPos(clicked, state);
        if (docked(level, master) != null) {
            player.sendSystemMessage(Component.translatable("power_armor.atomicdawn.station_occupied"));
            return InteractionResult.FAIL;
        }
        var entity = PowerArmorChassisItem.create(level, stack);
        if (entity == null) {
            return InteractionResult.FAIL;
        }
        var facing = state.getValue(FACING);
        entity.dock(master, dockPosition(master, facing), facing);
        level.addFreshEntity(entity);
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected @NotNull InteractionResult useItemOn(
        @NotNull ItemStack stack,
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull InteractionHand hand,
        @NotNull BlockHitResult hit
    ) {
        if (stack.getItem() instanceof PowerArmorChassisItem) {
            if (level instanceof ServerLevel serverLevel) {
                return dockItem(serverLevel, pos, stack, player);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull BlockHitResult hit
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        var master = masterPos(pos, state);
        var facing = state.getValue(FACING);
        if (serverPlayer.getVehicle() instanceof PowerArmorEntity riding) {
            if (docked(level, master) != null) {
                serverPlayer.sendSystemMessage(
                    Component.translatable("power_armor.atomicdawn.station_occupied")
                );
                return InteractionResult.FAIL;
            }
            riding.dock(master, dockPosition(master, facing), facing);
            return InteractionResult.SUCCESS;
        }
        var armor = docked(level, master);
        if (armor == null) {
            serverPlayer.sendSystemMessage(Component.translatable("power_armor.atomicdawn.station_empty"));
            return InteractionResult.SUCCESS;
        }
        if (serverPlayer.isSecondaryUseActive() && armor.getPassengers().isEmpty()) {
            armor.undock();
            PlayerItems.give(serverPlayer, PowerArmorChassisItem.fromEntity(armor));
            armor.discard();
            serverPlayer.sendSystemMessage(
                Component.translatable("power_armor.atomicdawn.station_picked_up"),
                true
            );
            return InteractionResult.SUCCESS;
        }
        openMenu(serverPlayer, armor);
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        var facing = context.getHorizontalDirection().getOpposite();
        var clicked = context.getClickedPos();
        for (var column = 0; column < SIZE; column++) {
            var master = clicked.relative(right(facing), -column);
            if (canOccupy(context.getLevel(), master, facing, context)) {
                return defaultBlockState().setValue(FACING, facing).setValue(COLUMN, column);
            }
        }
        return null;
    }

    private boolean canOccupy(Level level, BlockPos master, Direction facing, BlockPlaceContext context) {
        var player = context.getPlayer();
        var collision = player == null ? CollisionContext.empty() : CollisionContext.of(player);
        for (var layer = 0; layer < SIZE; layer++) {
            for (var row = 0; row < SIZE; row++) {
                for (var column = 0; column < SIZE; column++) {
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
                    var cellState = cell(defaultBlockState().setValue(FACING, facing), column, row, layer);
                    if (!level.isUnobstructed(cellState, pos, collision)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static BlockState cell(BlockState state, int column, int row, int layer) {
        return state.setValue(COLUMN, column).setValue(ROW, row).setValue(LAYER, layer);
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
        for (var layer = 0; layer < SIZE; layer++) {
            for (var row = 0; row < SIZE; row++) {
                for (var column = 0; column < SIZE; column++) {
                    var cell = cellPos(master, facing, column, row, layer);
                    if (!cell.equals(pos)) {
                        level.setBlock(cell, cell(state, column, row, layer), Block.UPDATE_ALL);
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
            for (var layer = 0; layer < SIZE; layer++) {
                for (var row = 0; row < SIZE; row++) {
                    for (var column = 0; column < SIZE; column++) {
                        var cell = cellPos(master, facing, column, row, layer);
                        if (cell.equals(pos)) {
                            continue;
                        }
                        var cellState = level.getBlockState(cell);
                        if (
                            cellState.is(this)
                                && cellState.getValue(FACING) == facing
                                && cellState.getValue(COLUMN) == column
                                && cellState.getValue(ROW) == row
                                && cellState.getValue(LAYER) == layer
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
        var column = state.getValue(COLUMN);
        var row = state.getValue(ROW);
        var layer = state.getValue(LAYER);
        int nColumn = column;
        int nRow = row;
        int nLayer = layer;
        if (direction == right(facing)) {
            nColumn++;
        } else if (direction == right(facing).getOpposite()) {
            nColumn--;
        } else if (direction == Direction.UP) {
            nRow++;
        } else if (direction == Direction.DOWN) {
            nRow--;
        } else if (direction == facing.getOpposite()) {
            nLayer++;
        } else {
            nLayer--;
        }
        var neighborIsCell = nColumn >= 0 && nColumn < SIZE && nRow >= 0 && nRow < SIZE && nLayer >= 0 && nLayer < SIZE;
        if (neighborIsCell) {
            var intact = neighborState.is(this)
                && neighborState.getValue(FACING) == facing
                && neighborState.getValue(COLUMN) == nColumn
                && neighborState.getValue(ROW) == nRow
                && neighborState.getValue(LAYER) == nLayer;
            if (!intact) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return isMaster(state) ? new PowerArmorStationBlockEntity(pos, state) : null;
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
        var index = index(state.getValue(COLUMN), state.getValue(ROW), state.getValue(LAYER));
        return SHAPES.get(state.getValue(FACING))[index];
    }

    @Override
    protected @NotNull VoxelShape getCollisionShape(
        @NotNull BlockState state,
        @NotNull BlockGetter level,
        @NotNull BlockPos pos,
        @NotNull CollisionContext context
    ) {
        if (
            context instanceof EntityCollisionContext entityContext
                && entityContext.getEntity() instanceof PowerArmorEntity
        ) {
            return Shapes.empty();
        }
        return getShape(state, level, pos, context);
    }

    private static int index(int column, int row, int layer) {
        return (layer * SIZE + row) * SIZE + column;
    }

    private static Map<Direction, VoxelShape[]> shapes() {
        var map = new EnumMap<Direction, VoxelShape[]>(Direction.class);
        for (var facing : Direction.Plane.HORIZONTAL) {
            var shapes = new VoxelShape[SIZE * SIZE * SIZE];
            for (var layer = 0; layer < SIZE; layer++) {
                for (var row = 0; row < SIZE; row++) {
                    for (var column = 0; column < SIZE; column++) {
                        shapes[index(column, row, layer)] = cellShape(facing, column, row, layer);
                    }
                }
            }
            map.put(facing, shapes);
        }
        return map;
    }

    private static VoxelShape cellShape(Direction facing, int column, int row, int layer) {
        var cellX = -column;
        var shape = Shapes.empty();
        for (var box : MODEL_BOXES) {
            var minX = Math.max(box[0], cellX) - cellX;
            var minY = Math.max(box[1], row) - row;
            var minZ = Math.max(box[2], layer) - layer;
            var maxX = Math.min(box[3], cellX + 1) - cellX;
            var maxY = Math.min(box[4], row + 1) - row;
            var maxZ = Math.min(box[5], layer + 1) - layer;
            if (maxX - minX < 0.001 || maxY - minY < 0.001 || maxZ - minZ < 0.001) {
                continue;
            }
            shape = Shapes.or(shape, rotated(facing, minX, minY, minZ, maxX, maxY, maxZ));
        }
        return shape.optimize();
    }

    private static VoxelShape rotated(
        Direction facing,
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ
    ) {
        return switch (facing) {
            case EAST -> Shapes.box(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX);
            case SOUTH -> Shapes.box(1 - maxX, minY, 1 - maxZ, 1 - minX, maxY, 1 - minZ);
            case WEST -> Shapes.box(minZ, minY, 1 - maxX, maxZ, maxY, 1 - minX);
            default -> Shapes.box(minX, minY, minZ, maxX, maxY, maxZ);
        };
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
