package mod.azure.atomicdawn.client.ghoul;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public final class GhoulPlayerRenderLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private static final float SKIN_INFLATE = 0.02F;

    private static final float GLOW_INFLATE = 0.06F;

    private static final int FULL_BRIGHT = 0xF000F0;

    private static final int SKIN_ORDER = 1;

    private static final int GLOW_ORDER = 2;

    private final PlayerModel skinModel;

    private final PlayerModel glowModel;

    public GhoulPlayerRenderLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, boolean slim) {
        super(parent);
        this.skinModel = bake(SKIN_INFLATE, slim);
        this.glowModel = bake(GLOW_INFLATE, slim);
    }

    private static PlayerModel bake(float inflate, boolean slim) {
        var mesh = PlayerModel.createMesh(new CubeDeformation(inflate), slim);
        return new PlayerModel(LayerDefinition.create(mesh, 64, 64).bakeRoot(), slim);
    }

    @Override
    public void submit(
        @NotNull PoseStack poseStack,
        @NotNull SubmitNodeCollector collector,
        int packedLight,
        @NotNull AvatarRenderState state,
        float yRot,
        float xRot
    ) {
        if (state.isInvisible) {
            return;
        }
        var appearance = GhoulClient.appearance(state.id);
        if (appearance == null) {
            return;
        }
        skinModel.setupAnim(state);
        renderColoredCutoutModel(
            skinModel,
            GhoulClient.skin(appearance.variant()),
            poseStack,
            collector,
            packedLight,
            state,
            -1,
            SKIN_ORDER
        );
        if (!appearance.fullGlow()) {
            return;
        }
        var pulse = 0.75F + 0.25F * Mth.sin(state.ageInTicks * 0.15F);
        var green = Math.round(255 * pulse);
        var color = 0xFF000000 | (Math.round(green * 0.55F) << 16) | (green << 8) | Math.round(green * 0.35F);
        glowModel.setupAnim(state);
        renderColoredCutoutModel(
            glowModel,
            GhoulClient.GLOW_TEXTURE,
            poseStack,
            collector,
            FULL_BRIGHT,
            state,
            color,
            GLOW_ORDER
        );
    }
}
