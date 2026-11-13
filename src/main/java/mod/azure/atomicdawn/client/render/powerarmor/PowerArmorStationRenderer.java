package mod.azure.atomicdawn.client.render.powerarmor;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.azure.azurelib.render.AzLayerRenderer;
import mod.azure.azurelib.render.block.AzBlockEntityModelRenderer;
import mod.azure.azurelib.render.block.AzBlockEntityRenderer;
import mod.azure.azurelib.render.block.AzBlockEntityRendererConfig;
import mod.azure.azurelib.render.block.AzBlockEntityRendererPipeline;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.powerarmor.PowerArmorStationBlock;
import mod.azure.atomicdawn.powerarmor.PowerArmorStationBlockEntity;

public class PowerArmorStationRenderer extends AzBlockEntityRenderer<PowerArmorStationBlockEntity> {

    public static final Identifier MODEL = AtomicDawn.id("geo/block/power_armor_station.geo.json");

    public static final Identifier TEXTURE = AtomicDawn.id("textures/block/power_armor_station.png");

    public PowerArmorStationRenderer() {
        super(
            AzBlockEntityRendererConfig.<PowerArmorStationBlockEntity>builder(MODEL, TEXTURE)
                .setModelRenderer(
                    (pipeline, layerRenderer) -> new ModelRenderer(
                        (AzBlockEntityRendererPipeline<PowerArmorStationBlockEntity>) pipeline,
                        layerRenderer
                    )
                )
                .build()
        );
    }

    @Override
    public @NotNull AABB getRenderBoundingBox(PowerArmorStationBlockEntity station) {
        var state = station.getBlockState();
        if (!state.hasProperty(PowerArmorStationBlock.FACING)) {
            return new AABB(station.getBlockPos());
        }
        return PowerArmorStationBlock.bounds(station.getBlockPos(), state.getValue(PowerArmorStationBlock.FACING))
            .inflate(0.5);
    }

    private static final class ModelRenderer extends AzBlockEntityModelRenderer<PowerArmorStationBlockEntity> {

        private static final double CENTER = (PowerArmorStationBlock.SIZE - 1) / 2.0;

        ModelRenderer(
            AzBlockEntityRendererPipeline<PowerArmorStationBlockEntity> pipeline,
            AzLayerRenderer<Long, PowerArmorStationBlockEntity> layerRenderer
        ) {
            super(pipeline, layerRenderer);
        }

        @Override
        protected void rotateBlock(Direction facing, PoseStack poseStack) {
            super.rotateBlock(facing, poseStack);
            poseStack.translate(-CENTER, 0, CENTER);
        }
    }
}
