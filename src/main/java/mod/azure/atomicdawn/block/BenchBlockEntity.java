package mod.azure.atomicdawn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("unused")
public abstract class BenchBlockEntity extends BlockEntity implements MenuProvider {

    private static final int EVENT_VIEWERS = 1;

    private int viewers;

    protected BenchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean isInUse() {
        return viewers > 0;
    }

    public void startOpen() {
        setViewers(viewers + 1);
    }

    public void stopOpen() {
        setViewers(Math.max(0, viewers - 1));
    }

    private void setViewers(int count) {
        viewers = count;
        if (level != null && !level.isClientSide()) {
            level.blockEvent(worldPosition, getBlockState().getBlock(), EVENT_VIEWERS, viewers);
        }
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == EVENT_VIEWERS) {
            viewers = param;
            return true;
        }
        return super.triggerEvent(id, param);
    }
}
