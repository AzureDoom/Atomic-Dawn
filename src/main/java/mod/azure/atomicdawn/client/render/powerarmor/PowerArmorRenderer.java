package mod.azure.atomicdawn.client.render.powerarmor;

import mod.azure.azurelib.render.entity.AzEntityRenderer;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;

public class PowerArmorRenderer extends AzEntityRenderer<PowerArmorEntity> {

    public static final Identifier MODEL = AtomicDawn.id("geo/entity/power_armor.geo.json");

    public PowerArmorRenderer(EntityRendererProvider.Context context) {
        super(
            AzEntityRendererConfig.builder(ignored -> MODEL, PowerArmorTextures::texture)
                .setAnimatorProvider(PowerArmorAnimator::new)
                .setPrerenderEntry(render -> {
                    PowerArmorModelParts.apply(render.animatable(), render.bakedModel());
                    return render;
                })
                .build(),
            context
        );
    }

    public PowerArmorRiderPose sampleRiderPose(PowerArmorEntity armor, float partialTick) {
        poseOffscreen(armor, partialTick);
        var model = provider.provideBakedModel(armor, armor);
        return model == null ? null : PowerArmorRiderPose.capture(model);
    }

    @Override
    public boolean shouldRender(
        @NonNull PowerArmorEntity entity,
        @NonNull Frustum culler,
        double camX,
        double camY,
        double camZ,
        float partialTicks
    ) {
        if (PowerArmorModelParts.isFirstPersonRider(entity)) {
            return false;
        }

        var player = Minecraft.getInstance().player;
        if (entity.isInvisible() && (player == null || (!entity.hasPassenger(player) && !player.isSpectator()))) {
            return false;
        }
        return super.shouldRender(entity, culler, camX, camY, camZ, partialTicks);
    }

    @Override
    protected boolean shouldShowName(@NotNull PowerArmorEntity entity, double distanceToCameraSq) {
        return entity.hasCustomName() && entity.isCustomNameVisible();
    }
}
