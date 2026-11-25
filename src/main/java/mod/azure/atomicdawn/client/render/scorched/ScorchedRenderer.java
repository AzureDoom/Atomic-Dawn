package mod.azure.atomicdawn.client.render.scorched;

import mod.azure.azurelib.hitbox.AzHitboxes;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.render.entity.AzEntityRenderer;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import mod.azure.azurelib.render.layer.AzAutoGlowingLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.NonNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.scorched.ScorchedEntity;
import mod.azure.atomicdawn.entity.scorched.ScorchedVariant;

public class ScorchedRenderer extends AzEntityRenderer<ScorchedEntity> {

    public static final Identifier MODEL = AtomicDawn.id("geo/entity/scorched_shared.geo.json");

    private static final String RADIO_PACK = "scorched_radio_pack";

    private static final String RADIO_LIGHT = "scorched_radio_light";

    private static final String OFFICER_CAP = "scorched_officer_cap";

    private static final String HOLIDAY_HAT = "scorched_holiday_hat";

    public ScorchedRenderer(EntityRendererProvider.Context context) {
        super(
            AzEntityRendererConfig.builder(ScorchedRenderer::model, ScorchedRenderer::texture)
                .setAnimatorProvider(ScorchedAnimator::new)
                .addRenderLayer(new AzAutoGlowingLayer<>())
                .addRenderLayer(new ScorchedItemLayer())
                .setPrerenderEntry(render -> {
                    applyVariant(render.animatable(), render.bakedModel());
                    return render;
                })
                .build(),
            context
        );
        this.shadowRadius = 0.5F;
    }

    private static Identifier model(ScorchedEntity scorched) {
        return MODEL;
    }

    private static Identifier texture(ScorchedEntity scorched) {
        return scorched.variant().texture();
    }

    private static void applyVariant(ScorchedEntity scorched, AzBakedModel model) {
        if (model == null) {
            return;
        }
        var variant = scorched.variant();
        var officer = variant == ScorchedVariant.OFFICER;
        hide(model, RADIO_PACK, !officer);
        hide(model, OFFICER_CAP, !officer);
        hide(model, RADIO_LIGHT, !scorched.radioLightOn());
        hide(model, HOLIDAY_HAT, variant != ScorchedVariant.HOLIDAY);
    }

    private static void hide(AzBakedModel model, String name, boolean hidden) {
        var bone = model.getBoneOrNull(name);
        if (bone != null) {
            bone.setHidden(hidden);
        }
    }

    @Override
    protected @NonNull AABB getBoundingBoxForCulling(@NonNull ScorchedEntity entity, float partialTicks) {
        return AzHitboxes.getCullingBox(entity, super.getBoundingBoxForCulling(entity, partialTicks));
    }
}
