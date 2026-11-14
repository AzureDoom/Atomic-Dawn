package mod.azure.atomicdawn.camp;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CampModuleBlock extends Block {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    private static final VoxelShape NORTH_SOUTH = Block.box(1.0, 0.0, 0.0, 15.0, 12.0, 16.0);

    private static final VoxelShape EAST_WEST = Block.box(0.0, 0.0, 1.0, 16.0, 12.0, 15.0);

    public CampModuleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected @NotNull VoxelShape getShape(
        @NotNull BlockState state,
        @NotNull BlockGetter level,
        @NotNull BlockPos pos,
        @NotNull CollisionContext context
    ) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? NORTH_SOUTH : EAST_WEST;
    }

    @Override
    protected @NotNull BlockState rotate(@NotNull BlockState state, @NotNull Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected @NotNull BlockState mirror(@NotNull BlockState state, @NotNull Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull BlockHitResult hitResult
    ) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            interact(serverLevel, pos, serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }

    private static void interact(ServerLevel level, BlockPos pos, ServerPlayer player) {
        var camp = CampManager.moduleAt(level.getServer(), level.dimension(), pos);
        if (camp.isEmpty()) {
            CampPlacementService.notify(
                player,
                Component.translatable("camp.atomicdawn.module.unregistered"),
                ChatFormatting.GRAY
            );
            return;
        }
        var slot = camp.get();
        if (!CampPermissionService.canManage(player, slot)) {
            CampPlacementService.notify(
                player,
                Component.translatable("camp.atomicdawn.module.visitor", slot.ownerName()),
                ChatFormatting.YELLOW
            );
            return;
        }
        if (player.isSecondaryUseActive()) {
            CampSlotService.store(player, slot);
            return;
        }
        CampPlacementService.notify(
            player,
            Component.translatable(
                "camp.atomicdawn.module.owner",
                slot.budgetUsed(),
                slot.budgetLimit(),
                CampClaim.radius()
            ),
            ChatFormatting.GREEN
        );
    }

    @Override
    public @NotNull BlockState playerWillDestroy(
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState state,
        @NotNull Player player
    ) {
        if (level instanceof ServerLevel serverLevel) {
            CampManager.moduleAt(serverLevel.getServer(), serverLevel.dimension(), pos)
                .ifPresent(camp -> CampPlacementService.dissolve(serverLevel, camp));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
