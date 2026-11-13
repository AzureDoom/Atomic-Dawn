package mod.azure.atomicdawn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class ArmorBenchBlock extends MultiblockBenchBlock {

    public ArmorBenchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected BenchBlockEntity newMasterEntity(BlockPos pos, BlockState state) {
        return new ArmorBenchBlockEntity(pos, state);
    }
}
