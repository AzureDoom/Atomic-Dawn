package mod.azure.atomicdawn.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mod.azure.azurelib.render.AzLayerRenderer;
import mod.azure.azurelib.render.entity.AzEntityModelRenderer;
import mod.azure.azurelib.render.entity.AzEntityRenderer;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import mod.azure.azurelib.render.entity.AzEntityRendererPipeline;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.UUID;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.ExplosiveProjectileEntity;

public class MiniNukeRenderer extends AzEntityRenderer<ExplosiveProjectileEntity> {

    public static final Identifier MODEL = AtomicDawn.id("geo/entity/mini_nuke.geo.json");

    public static final Identifier TEXTURE = AtomicDawn.id("textures/entity/mini_nuke.png");

    public MiniNukeRenderer(EntityRendererProvider.Context context) {
        super(
            AzEntityRendererConfig.<ExplosiveProjectileEntity>builder(MODEL, TEXTURE)
                .setModelRenderer(
                    (pipeline, layerRenderer) -> new ModelRenderer(
                        (AzEntityRendererPipeline<ExplosiveProjectileEntity>) pipeline,
                        layerRenderer
                    )
                )
                .build(),
            context
        );
    }

    private static final class ModelRenderer extends AzEntityModelRenderer<ExplosiveProjectileEntity> {

        ModelRenderer(
            AzEntityRendererPipeline<ExplosiveProjectileEntity> pipeline,
            AzLayerRenderer<UUID, ExplosiveProjectileEntity> layerRenderer
        ) {
            super(pipeline, layerRenderer);
        }

        @Override
        protected void applyRotations(
            ExplosiveProjectileEntity animatable,
            PoseStack poseStack,
            float ageInTicks,
            float rotationYaw,
            float partialTick,
            float nativeScale
        ) {
            var yaw = Mth.rotLerp(partialTick, animatable.yRotO, animatable.getYRot());
            var pitch = Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot());
            poseStack.translate(0, 0.25, 0);
            poseStack.rotate(Axis.YP.rotationDegrees(180F - yaw));
            poseStack.rotate(Axis.XP.rotationDegrees(-pitch));
        }
    }
}
