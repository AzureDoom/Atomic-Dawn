package mod.azure.atomicdawn.client.pipboy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.powerarmor.PowerArmorFirstPerson;
import mod.azure.atomicdawn.config.ADConfig.Client.PipBoy.WornDevice.PipBoyFirstPersonMode;
import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class PipBoyFirstPerson {

    private static final PlayerModel[][] BODY = new PlayerModel[2][2];

    private static final PlayerModel[][] SCREEN = new PlayerModel[2][2];

    private static final PlayerModel[] SKIN = new PlayerModel[2];

    private PipBoyFirstPerson() {}

    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        var player = Minecraft.getInstance().player;

        if (player == null) {
            return;
        }

        var arm = firstPersonArm(player);
        if (arm != event.getArm()) {
            return;
        }

        submitDevice(
            event.getPoseStack(),
            event.getSubmitNodeCollector(),
            event.getLightCoords(),
            slim(player),
            arm
        );
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (
            event.getHand() != InteractionHand.OFF_HAND
                || !event.getItemStack().isEmpty()
                || AtomicDawn.CONFIG.client.pipboy.wornDevice.PIPBOY_ARM_FIRST_PERSON != PipBoyFirstPersonMode.UNARMED_BOTH_ARMS
        ) {
            return;
        }
        var player = Minecraft.getInstance().player;
        if (player == null || !player.getMainHandItem().isEmpty()) {
            return;
        }
        var arm = firstPersonArm(player);
        if (arm == null || arm != player.getMainArm().getOpposite()) {
            return;
        }
        var pose = event.getPoseStack();
        var collector = event.getSubmitNodeCollector();
        var light = event.getPackedLight();
        var slim = slim(player);
        pose.pushPose();
        applyBareArmPose(pose, event.getEquipProgress(), event.getSwingProgress(), arm);
        submitSkinArm(pose, collector, light, player, slim, arm);
        submitDevice(pose, collector, light, slim, arm);
        pose.popPose();
    }

    @Nullable
    private static HumanoidArm firstPersonArm(AbstractClientPlayer player) {
        if (AtomicDawn.CONFIG.client.pipboy.wornDevice.PIPBOY_ARM_FIRST_PERSON == PipBoyFirstPersonMode.OFF) {
            return null;
        }

        if (PowerArmorFirstPerson.active() || player.getVehicle() instanceof PowerArmorEntity) {
            return null;
        }
        return PipBoyArmLayer.deviceArm(player, player.getMainArm(), player.isInvisible());
    }

    private static boolean slim(AbstractClientPlayer player) {
        return player.getSkin().model() == PlayerModelType.SLIM;
    }

    private static void applyBareArmPose(PoseStack pose, float equipProgress, float swingProgress, HumanoidArm arm) {
        var side = arm == HumanoidArm.RIGHT ? 1F : -1F;
        var root = Mth.sqrt(swingProgress);
        var dx = -0.3F * Mth.sin(root * Mth.PI);
        var dy = 0.4F * Mth.sin(root * Mth.PI * 2F);
        var dz = -0.4F * Mth.sin(swingProgress * Mth.PI);
        pose.translate(side * (dx + 0.64F), dy - 0.6F + equipProgress * -0.6F, dz - 0.72F);
        pose.rotate(Axis.YP.rotationDegrees(side * 45F));
        var swingSq = Mth.sin(swingProgress * swingProgress * Mth.PI);
        var swingRoot = Mth.sin(root * Mth.PI);
        pose.rotate(Axis.YP.rotationDegrees(side * swingRoot * 70F));
        pose.rotate(Axis.ZP.rotationDegrees(side * swingSq * -20F));
        pose.translate(side * -1F, 3.6F, 3.5F);
        pose.rotate(Axis.ZP.rotationDegrees(side * 120F));
        pose.rotate(Axis.XP.rotationDegrees(200F));
        pose.rotate(Axis.YP.rotationDegrees(side * -135F));
        pose.translate(side * 5.6F, 0F, 0F);
    }

    private static ModelPart posedArm(PlayerModel model, HumanoidArm arm) {
        var part = PipBoyArmLayer.arm(model, arm);
        part.resetPose();
        part.visible = true;
        part.zRot = arm == HumanoidArm.RIGHT ? 0.1F : -0.1F;
        return part;
    }

    private static void submitDevice(
        PoseStack pose,
        SubmitNodeCollector collector,
        int light,
        boolean slim,
        HumanoidArm arm
    ) {
        var s = slim ? 1 : 0;
        var a = arm.ordinal();
        if (BODY[s][a] == null) {
            BODY[s][a] = PipBoyArmLayer.bakeDevice(PipBoyArmLayer.BODY_INFLATE, slim, arm);
            SCREEN[s][a] = PipBoyArmLayer.bakeDevice(PipBoyArmLayer.SCREEN_INFLATE, slim, arm);
        }
        collector.submitModelPart(
            posedArm(BODY[s][a], arm),
            pose,
            RenderTypes.entityCutout(PipBoyArmLayer.TEXTURE),
            light,
            OverlayTexture.NO_OVERLAY,
            null
        );
        if (AtomicDawn.CONFIG.client.pipboy.wornDevice.PIPBOY_ARM_SCREEN_GLOW) {
            collector.submitModelPart(
                posedArm(SCREEN[s][a], arm),
                pose,
                RenderTypes.entityCutout(PipBoyArmLayer.SCREEN_TEXTURE),
                PipBoyArmLayer.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                null,
                PipBoyArmLayer.screenColor(),
                0
            );
        }
    }

    private static void submitSkinArm(
        PoseStack pose,
        SubmitNodeCollector collector,
        int light,
        AbstractClientPlayer player,
        boolean slim,
        HumanoidArm arm
    ) {
        var s = slim ? 1 : 0;
        if (SKIN[s] == null) {
            var mesh = PlayerModel.createMesh(CubeDeformation.NONE, slim);
            SKIN[s] = new PlayerModel(LayerDefinition.create(mesh, 64, 64).bakeRoot(), slim);
        }
        var model = SKIN[s];
        var right = arm == HumanoidArm.RIGHT;
        (right ? model.rightSleeve : model.leftSleeve).visible = player.isModelPartShown(
            right ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE
        );
        collector.submitModelPart(
            posedArm(model, arm),
            pose,
            RenderTypes.entityTranslucent(player.getSkin().body().texturePath()),
            light,
            OverlayTexture.NO_OVERLAY,
            null
        );
    }
}
