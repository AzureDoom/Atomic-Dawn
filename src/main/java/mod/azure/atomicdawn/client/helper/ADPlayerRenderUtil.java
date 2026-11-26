package mod.azure.atomicdawn.client.helper;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class ADPlayerRenderUtil {

    private ADPlayerRenderUtil() {}

    @FunctionalInterface
    public interface PlayerLayerFactory {

        RenderLayer<AvatarRenderState, PlayerModel> create(
            RenderLayerParent<AvatarRenderState, PlayerModel> parent,
            boolean slim
        );
    }

    public static boolean isSlim(AbstractClientPlayer player) {
        return player.getSkin().model() == PlayerModelType.SLIM;
    }

    public static Identifier skinTexture(AbstractClientPlayer player) {
        return player.getSkin().body().texturePath();
    }

    public static MeshDefinition playerMesh(CubeDeformation deformation, boolean slim) {
        return PlayerModel.createMesh(deformation, slim);
    }

    public static PlayerModel bakePlayerModel(MeshDefinition mesh, int textureWidth, int textureHeight, boolean slim) {
        return new PlayerModel(LayerDefinition.create(mesh, textureWidth, textureHeight).bakeRoot(), slim);
    }

    public static PlayerModel bakePlayerModel(CubeDeformation deformation, boolean slim) {
        return bakePlayerModel(playerMesh(deformation, slim), 64, 64, slim);
    }

    public static ModelPart arm(PlayerModel model, HumanoidArm side) {
        return side == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
    }

    public static ModelPart sleeve(PlayerModel model, HumanoidArm side) {
        return side == HumanoidArm.RIGHT ? model.rightSleeve : model.leftSleeve;
    }

    public static boolean sleeveShown(AbstractClientPlayer player, HumanoidArm side) {
        return player.isModelPartShown(
            side == HumanoidArm.RIGHT ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE
        );
    }

    public static void submitPart(
        SubmitNodeCollector collector,
        ModelPart part,
        PoseStack pose,
        RenderType type,
        int light
    ) {
        collector.submitModelPart(part, pose, type, light, OverlayTexture.NO_OVERLAY, null);
    }

    public static void submitPart(
        SubmitNodeCollector collector,
        ModelPart part,
        PoseStack pose,
        RenderType type,
        int light,
        int color
    ) {
        collector.submitModelPart(part, pose, type, light, OverlayTexture.NO_OVERLAY, null, color, 0);
    }

    public static void addPlayerLayers(EntityRenderersEvent.AddLayers event, PlayerLayerFactory factory) {
        for (var model : event.getSkins()) {
            AvatarRenderer<AbstractClientPlayer> renderer = event.getPlayerRenderer(model);
            if (renderer != null) {
                renderer.addLayer(factory.create(renderer, model == PlayerModelType.SLIM));
            }
        }
    }
}
