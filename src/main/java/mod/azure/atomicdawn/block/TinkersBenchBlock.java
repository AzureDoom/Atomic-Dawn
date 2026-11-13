package mod.azure.atomicdawn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class TinkersBenchBlock extends MultiblockBenchBlock {

    public TinkersBenchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected BenchBlockEntity newMasterEntity(BlockPos pos, BlockState state) {
        return new TinkersBenchBlockEntity(pos, state);
    }
}
