package mod.azure.atomicdawn.client.render.scorchbeast;

import mod.azure.azurelib.hitbox.AzHitboxes;
import mod.azure.azurelib.render.entity.AzEntityRenderer;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import mod.azure.azurelib.render.layer.AzAutoGlowingLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.NonNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.scorchbeast.ScorchbeastEntity;

public class ScorchbeastRenderer extends AzEntityRenderer<ScorchbeastEntity> {

    private static final Identifier MODEL = AtomicDawn.id("geo/entity/scorchbeast.geo.json");

    private static final Identifier TEXTURE = AtomicDawn.id("textures/entity/scorchbeast/scorchbeast.png");

    public ScorchbeastRenderer(EntityRendererProvider.Context context) {
        super(
            AzEntityRendererConfig.builder(ScorchbeastRenderer::model, ScorchbeastRenderer::texture)
                .setAnimatorProvider(ScorchbeastAnimator::new)
                .addRenderLayer(new AzAutoGlowingLayer<>())
                .setShadowRadius(2.5F)
                .build(),
            context
        );
    }

    private static Identifier model(ScorchbeastEntity entity) {
        return MODEL;
    }

    private static Identifier texture(ScorchbeastEntity entity) {
        return TEXTURE;
    }

    @Override
    protected @NonNull AABB getBoundingBoxForCulling(@NonNull ScorchbeastEntity entity, float partialTicks) {
        return AzHitboxes.getCullingBox(entity, super.getBoundingBoxForCulling(entity, partialTicks)).inflate(8.0);
    }
}
