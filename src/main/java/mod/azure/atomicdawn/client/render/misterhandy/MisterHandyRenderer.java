package mod.azure.atomicdawn.client.render.misterhandy;

import mod.azure.azurelib.hitbox.AzHitboxes;
import mod.azure.azurelib.render.entity.AzEntityRenderer;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.NonNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.misterhandy.MisterHandyEntity;

public class MisterHandyRenderer extends AzEntityRenderer<MisterHandyEntity> {

    private static final Identifier MODEL = AtomicDawn.id("geo/entity/mister_handy.geo.json");

    private static final Identifier TEXTURE = AtomicDawn.id("textures/entity/mister_handy/mister_handy.png");

    public MisterHandyRenderer(EntityRendererProvider.Context context) {
        super(
            AzEntityRendererConfig.builder(MisterHandyRenderer::model, MisterHandyRenderer::texture)
                .setAnimatorProvider(MisterHandyAnimator::new)
                .setDeathMaxRotation(0F)
                .setShadowRadius(0.6F)
                .build(),
            context
        );
    }

    private static Identifier model(MisterHandyEntity entity) {
        return MODEL;
    }

    private static Identifier texture(MisterHandyEntity entity) {
        return TEXTURE;
    }

    @Override
    protected @NonNull AABB getBoundingBoxForCulling(@NonNull MisterHandyEntity entity, float partialTicks) {
        return AzHitboxes.getCullingBox(entity, super.getBoundingBoxForCulling(entity, partialTicks));
    }
}
