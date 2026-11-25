package mod.azure.atomicdawn.client.render.supermutant;

import mod.azure.azurelib.hitbox.AzHitboxes;
import mod.azure.azurelib.render.entity.AzEntityRenderer;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.NonNull;

import mod.azure.atomicdawn.entity.supermutant.SuperMutantEntity;

public class SuperMutantRenderer extends AzEntityRenderer<SuperMutantEntity> {

    public SuperMutantRenderer(EntityRendererProvider.Context context) {
        super(
            AzEntityRendererConfig.builder(SuperMutantRenderer::model, SuperMutantRenderer::texture)
                .setAnimatorProvider(SuperMutantAnimator::new)
                .addRenderLayer(new SuperMutantItemLayer())
                .setDeathMaxRotation(0F)
                .setShadowRadius(0.9F)
                .build(),
            context
        );
    }

    private static Identifier model(SuperMutantEntity mutant) {
        return mutant.variant().model();
    }

    private static Identifier texture(SuperMutantEntity mutant) {
        return mutant.variant().texture();
    }

    @Override
    protected @NonNull AABB getBoundingBoxForCulling(@NonNull SuperMutantEntity entity, float partialTicks) {
        return AzHitboxes.getCullingBox(entity, super.getBoundingBoxForCulling(entity, partialTicks));
    }
}
