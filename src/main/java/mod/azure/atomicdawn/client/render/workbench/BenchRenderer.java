package mod.azure.atomicdawn.client.render.workbench;

import mod.azure.azurelib.render.block.AzBlockEntityRenderer;
import mod.azure.azurelib.render.block.AzBlockEntityRendererConfig;
import mod.azure.azurelib.render.block.AzBlockEntityRendererPipeline;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.block.BenchBlockEntity;
import mod.azure.atomicdawn.block.MultiblockBenchBlock;

public class BenchRenderer<T extends BenchBlockEntity> extends AzBlockEntityRenderer<T> {

    public BenchRenderer(Identifier model, Identifier texture) {
        super(
            AzBlockEntityRendererConfig.<T>builder(model, texture)
                .setModelRenderer(
                    (pipeline, layerRenderer) -> new BenchModelRenderer<>(
                        (AzBlockEntityRendererPipeline<T>) pipeline,
                        layerRenderer
                    )
                )
                .build()
        );
    }

    @Override
    public @NotNull AABB getRenderBoundingBox(T bench) {
        var state = bench.getBlockState();
        var master = bench.getBlockPos();
        if (!state.hasProperty(MultiblockBenchBlock.FACING)) {
            return new AABB(master);
        }
        var facing = state.getValue(MultiblockBenchBlock.FACING);
        var far = MultiblockBenchBlock.cellPos(
            master,
            facing,
            MultiblockBenchBlock.WIDTH - 1,
            MultiblockBenchBlock.HEIGHT - 1,
            MultiblockBenchBlock.DEPTH - 1
        );
        return new AABB(master).minmax(new AABB(far)).inflate(0.5);
    }
}
