package mod.azure.atomicdawn.client.render.ghoul;

import mod.azure.azurelib.hitbox.AzHitboxes;
import mod.azure.azurelib.render.entity.AzEntityRenderer;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.NonNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.ghoul.FeralGhoulEntity;

public class FeralGhoulRenderer extends AzEntityRenderer<FeralGhoulEntity> {

    public static final Identifier MODEL = AtomicDawn.id("geo/entity/feral_ghoul.geo.json");

    public FeralGhoulRenderer(EntityRendererProvider.Context context) {
        super(
            AzEntityRendererConfig.builder(FeralGhoulRenderer::model, FeralGhoulRenderer::texture)
                .setAnimatorProvider(FeralGhoulAnimator::new)
                .build(),
            context
        );
        this.shadowRadius = 0.5F;
    }

    private static Identifier model(FeralGhoulEntity ghoul) {
        return MODEL;
    }

    private static Identifier texture(FeralGhoulEntity ghoul) {
        return ghoul.variant().texture();
    }

    @Override
    protected @NonNull AABB getBoundingBoxForCulling(@NonNull FeralGhoulEntity entity, float partialTicks) {
        return AzHitboxes.getCullingBox(entity, super.getBoundingBoxForCulling(entity, partialTicks));
    }
}
