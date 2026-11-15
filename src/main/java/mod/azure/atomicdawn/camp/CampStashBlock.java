package mod.azure.atomicdawn.camp;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class CampStashBlock extends Block {

    private static final VoxelShape SHAPE = Shapes.or(
        Block.box(0.65, 0.15, 3.65, 15.35, 6.7, 12.35),
        Block.box(0, 6.3, 3, 16, 9, 13),
        Block.box(2, 5.2, 2.5, 4.4, 7.1, 3.65),
        Block.box(11.6, 5.2, 2.5, 14, 7.1, 3.65)
    );

    public CampStashBlock(Properties properties) {
        super(properties);
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
            open(serverLevel, pos, serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }

    private static void open(ServerLevel level, BlockPos pos, ServerPlayer player) {
        var tracked = CampManager.objectAt(level.getServer(), level.dimension(), pos);
        if (tracked.isEmpty() || !tracked.get().camp().ownedBy(player.getUUID())) {
            CampPlacementService.notify(
                player,
                Component.translatable("camp.atomicdawn.stash.not_yours"),
                ChatFormatting.RED
            );
            return;
        }
        if (player.isSecondaryUseActive()) {
            var moved = CampMaterials.deposit(player);
            CampPlacementService.notify(
                player,
                Component.translatable("camp.atomicdawn.scrapbox.deposited", moved),
                moved > 0 ? ChatFormatting.GREEN : ChatFormatting.GRAY
            );
            return;
        }
        player.openMenu(
            new SimpleMenuProvider(
                (id, inventory, _) -> ChestMenu.sixRows(id, inventory, CampStash.open(player)),
                Component.translatable("container.atomicdawn.camp_stash")
            )
        );
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
}
