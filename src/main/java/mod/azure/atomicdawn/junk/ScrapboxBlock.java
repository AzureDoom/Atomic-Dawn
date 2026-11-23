package mod.azure.atomicdawn.junk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.network.ADNetwork;
import mod.azure.atomicdawn.network.StorageOpenPayload;

public class ScrapboxBlock extends Block {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE = Block.box(0.5, 0, 0.5, 15.5, 16, 15.5);

    public ScrapboxBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
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
    protected @NotNull InteractionResult useItemOn(
        @NotNull ItemStack stack,
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull InteractionHand hand,
        @NotNull BlockHitResult hitResult
    ) {
        if (!ScrapRecipes.storable(level.registryAccess(), stack)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            ScrapboxService.depositHeld(serverPlayer, pos, hand);
            ADNetwork.sendTo(serverPlayer, new StorageOpenPayload(StorageOpenPayload.Mode.SCRAPBOX, pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull BlockHitResult hitResult
    ) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (player.isSecondaryUseActive()) {
                ScrapboxService.depositAll(serverPlayer, pos);
            } else {
                ADNetwork.sendTo(serverPlayer, new StorageOpenPayload(StorageOpenPayload.Mode.SCRAPBOX, pos));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
