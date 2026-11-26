package mod.azure.atomicdawn.client.pipboy;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.config.ADColors;
import mod.azure.atomicdawn.pipboy.PipBoyService;
import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;
import mod.azure.atomicdawn.powerarmor.PowerArmorSlot;

public final class PipBoyArmLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    public static final Identifier TEXTURE = AtomicDawn.id("textures/entity/pipboy/pipboy_arm.png");

    public static final Identifier SCREEN_TEXTURE = AtomicDawn.id("textures/entity/pipboy/pipboy_arm_screen.png");

    static final float BODY_INFLATE = 0.05F;

    static final float SCREEN_INFLATE = 0.08F;

    static final int FULL_BRIGHT = 0xF000F0;

    private static final String PART = "atomicdawn_pipboy";

    private static final int TEX_WIDTH = 32;

    private static final int TEX_HEIGHT = 32;

    private static final int BODY_ORDER = 3;

    private static final int SCREEN_ORDER = 4;

    private final PlayerModel[] bodyModels = new PlayerModel[2];

    private final PlayerModel[] screenModels = new PlayerModel[2];

    public static final TagKey<Item> HIDES_PIPBOY = TagKey.create(Registries.ITEM, AtomicDawn.id("hides_pipboy"));

    public PipBoyArmLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, boolean slim) {
        super(parent);
        for (var arm : HumanoidArm.values()) {
            bodyModels[arm.ordinal()] = bakeDevice(BODY_INFLATE, slim, arm);
            screenModels[arm.ordinal()] = bakeDevice(SCREEN_INFLATE, slim, arm);
        }
    }

    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (var model : event.getSkins()) {
            AvatarRenderer<AbstractClientPlayer> renderer = event.getPlayerRenderer(model);
            if (renderer != null) {
                renderer.addLayer(new PipBoyArmLayer(renderer, model == PlayerModelType.SLIM));
            }
        }
    }

    static PlayerModel bakeDevice(float inflate, boolean slim, HumanoidArm side) {
        var mesh = PlayerModel.createMesh(CubeDeformation.NONE, slim);
        addDevice(mesh.getRoot().getChild(side == HumanoidArm.RIGHT ? "right_arm" : "left_arm"), side, inflate, slim);
        var model = new PlayerModel(LayerDefinition.create(mesh, TEX_WIDTH, TEX_HEIGHT).bakeRoot(), slim);
        model.root().getAllParts().forEach(part -> part.skipDraw = true);
        arm(model, side).getChild(PART).skipDraw = false;
        return model;
    }

    static ModelPart arm(PlayerModel model, HumanoidArm side) {
        return side == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
    }

    static int screenColor() {
        return ADColors.argb(AtomicDawn.CONFIG.client.pipboy.display.PIPBOY_COLOR, 0xFF1AFF80) | 0xFF000000;
    }

    private static void addDevice(PartDefinition arm, HumanoidArm side, float inflate, boolean slim) {
        var right = side == HumanoidArm.RIGHT;
        var centerX = slim ? (right ? -0.5F : 0.5F) : (right ? -1F : 1F);
        var cuffMinX = centerX - 2.5F;
        var housingMinX = right ? cuffMinX - 1F : cuffMinX + 5F;
        var deformation = new CubeDeformation(inflate);
        arm.addOrReplaceChild(
            PART,
            CubeListBuilder.create()
                .mirror(!right)
                .texOffs(0, 0)
                .addBox(cuffMinX, 4F, -2.5F, 5F, 5F, 5F, deformation)
                .texOffs(0, 12)
                .addBox(housingMinX, 4.5F, -2F, 1F, 4F, 4F, deformation),
            PartPose.ZERO
        );
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
        var level = Minecraft.getInstance().level;
        var player = level == null ? null : level.getEntity(state.id) instanceof Player p ? p : null;
        var arm = deviceArm(player, state.mainArm, state.isInvisible);
        if (arm == null || state.chestEquipment.is(HIDES_PIPBOY)) {
            return;
        }
        var body = bodyModels[arm.ordinal()];
        body.setupAnim(state);
        renderColoredCutoutModel(body, TEXTURE, poseStack, collector, packedLight, state, -1, BODY_ORDER);
        if (AtomicDawn.CONFIG.client.pipboy.wornDevice.PIPBOY_ARM_SCREEN_GLOW) {
            var screen = screenModels[arm.ordinal()];
            screen.setupAnim(state);
            renderColoredCutoutModel(
                screen,
                SCREEN_TEXTURE,
                poseStack,
                collector,
                FULL_BRIGHT,
                state,
                screenColor(),
                SCREEN_ORDER
            );
        }
    }

    @Nullable
    static HumanoidArm deviceArm(@Nullable Player player, HumanoidArm mainArm, boolean invisible) {
        var config = AtomicDawn.CONFIG;
        if (
            invisible || !config.common.pipboy.general.PIPBOY_ENABLED
                || !config.client.pipboy.wornDevice.PIPBOY_ARM_DEVICE
        ) {
            return null;
        }
        var arm = switch (config.client.pipboy.wornDevice.PIPBOY_ARM) {
            case LEFT -> HumanoidArm.LEFT;
            case RIGHT -> HumanoidArm.RIGHT;
            case MAIN_HAND -> mainArm;
            case OFF_HAND -> mainArm.getOpposite();
        };
        if (player == null) {
            return arm;
        }

        if (player == Minecraft.getInstance().player && !PipBoyService.data(player).initialized()) {
            return null;
        }

        if (player.getVehicle() instanceof PowerArmorEntity armor) {
            var slot = arm == HumanoidArm.LEFT ? PowerArmorSlot.LEFT_ARM : PowerArmorSlot.RIGHT_ARM;
            if (!armor.getPiece(slot).isEmpty()) {
                return null;
            }
        }
        return arm;
    }
}
