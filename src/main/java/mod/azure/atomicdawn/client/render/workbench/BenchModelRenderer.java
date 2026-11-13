package mod.azure.atomicdawn.client.render.workbench;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.azure.azurelib.render.AzLayerRenderer;
import mod.azure.azurelib.render.block.AzBlockEntityModelRenderer;
import mod.azure.azurelib.render.block.AzBlockEntityRendererPipeline;
import net.minecraft.core.Direction;

import mod.azure.atomicdawn.block.BenchBlockEntity;
import mod.azure.atomicdawn.block.MultiblockBenchBlock;

public class BenchModelRenderer<T extends BenchBlockEntity> extends AzBlockEntityModelRenderer<T> {

    private static final double CENTER_X = -(MultiblockBenchBlock.WIDTH - 1) / 2.0;

    private static final double CENTER_Z = (MultiblockBenchBlock.DEPTH - 1) / 2.0;

    public BenchModelRenderer(AzBlockEntityRendererPipeline<T> pipeline, AzLayerRenderer<Long, T> layerRenderer) {
        super(pipeline, layerRenderer);
    }

    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        super.rotateBlock(facing, poseStack);
        poseStack.translate(CENTER_X, 0, CENTER_Z);
    }
}
