package mod.azure.atomicdawn.client.render.brahmin;

import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.render.entity.AzEntityRenderer;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.brahmin.BrahminEntity;

public class BrahminRenderer extends AzEntityRenderer<BrahminEntity> {

    public static final Identifier MODEL = AtomicDawn.id("geo/entity/brahmin.geo.json");

    public static final Identifier TEXTURE = AtomicDawn.id("textures/entity/brahmin/brahmin.png");

    private static final float BABY_SCALE = 0.55F;

    public BrahminRenderer(EntityRendererProvider.Context context) {
        super(
            AzEntityRendererConfig.builder(BrahminRenderer::model, BrahminRenderer::texture)
                .setAnimatorProvider(BrahminAnimator::new)
                .setPrerenderEntry(render -> {
                    scaleForAge(render.animatable(), render.bakedModel());
                    return render;
                })
                .build(),
            context
        );
        this.shadowRadius = 0.7F;
    }

    private static Identifier model(BrahminEntity brahmin) {
        return MODEL;
    }

    private static Identifier texture(BrahminEntity brahmin) {
        return TEXTURE;
    }

    private static void scaleForAge(BrahminEntity brahmin, AzBakedModel model) {
        if (model == null) {
            return;
        }
        var root = model.getBoneOrNull("root");
        if (root != null) {
            var scale = brahmin.isBaby() ? BABY_SCALE : 1F;
            root.setScaleX(scale);
            root.setScaleY(scale);
            root.setScaleZ(scale);
        }
    }
}
