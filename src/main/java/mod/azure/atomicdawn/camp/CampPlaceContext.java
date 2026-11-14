package mod.azure.atomicdawn.camp;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class CampPlaceContext extends BlockPlaceContext {

    private final Direction facing;

    public CampPlaceContext(Player player, ItemStack stack, BlockPos pos, Direction facing) {
        super(
            player,
            InteractionHand.MAIN_HAND,
            stack,
            new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, pos, false)
        );
        this.facing = facing;
    }

    @Override
    public @NotNull Direction getHorizontalDirection() {
        return facing;
    }

    @Override
    public float getRotation() {
        return facing.toYRot();
    }

    @Override
    public @NotNull Direction getNearestLookingDirection() {
        return facing;
    }

    @Override
    public Direction @NotNull [] getNearestLookingDirections() {
        return new Direction[] {
            facing,
            Direction.DOWN,
            facing.getClockWise(),
            facing.getCounterClockWise(),
            Direction.UP,
            facing.getOpposite() };
    }
}
