package mod.azure.atomicdawn.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public final class MedicalClientExtensions implements IClientItemExtensions {

    public static final MedicalClientExtensions STIMPAK = new MedicalClientExtensions(false);

    public static final MedicalClientExtensions DOCTORS_BAG = new MedicalClientExtensions(true);

    private final boolean bag;

    private MedicalClientExtensions(boolean bag) {
        this.bag = bag;
    }

    @Override
    public HumanoidModel.@Nullable ArmPose getArmPose(
        @NonNull LivingEntity entity,
        @NonNull InteractionHand hand,
        @NonNull ItemStack stack
    ) {
        if (!entity.isUsingItem() || entity.getUsedItemHand() != hand) {
            return null;
        }
        return bag ? HumanoidModel.ArmPose.BRUSH : HumanoidModel.ArmPose.CROSSBOW_CHARGE;
    }

    @Override
    public boolean applyForgeHandTransform(
        @NonNull PoseStack poseStack,
        @NonNull PlayerRenderState playerRenderState,
        @NonNull HumanoidArm arm,
        @NonNull ItemStack itemInHand,
        float partialTick,
        float equipProcess,
        float swingProcess
    ) {
        LocalPlayer player = Minecraft.getInstance().player;

        if (
            player == null
                || !player.isUsingItem()
                || player.getUseItem() != itemInHand
        ) {
            return false;
        }

        float side = arm == HumanoidArm.RIGHT ? 1F : -1F;

        int total = Math.max(1, itemInHand.getUseDuration(player));
        float used = total - (player.getUseItemRemainingTicks() - partialTick + 1F);
        float progress = Mth.clamp(used / total, 0F, 1F);

        poseStack.translate(side * 0.56F, -0.52F, -0.72F);

        if (bag) {
            rummage(poseStack, side, progress, used);
        } else {
            inject(poseStack, side, progress);
        }

        return true;
    }

    private static void inject(PoseStack poseStack, float side, float progress) {
        var aim = smooth(Math.min(1F, progress / 0.55F));
        poseStack.translate(-side * 0.38F * aim, 0.12F * aim, 0.12F * aim);
        poseStack.rotate(Axis.YP.rotationDegrees(-side * 45F * aim));
        poseStack.rotate(Axis.ZP.rotationDegrees(-side * 30F * aim));
        poseStack.rotate(Axis.XP.rotationDegrees(-20F * aim));
        if (progress > 0.55F) {
            var jab = Mth.sin((progress - 0.55F) / 0.45F * Mth.PI);
            poseStack.translate(-side * 0.14F * jab, -0.1F * jab, -0.04F * jab);
        }
    }

    private static void rummage(PoseStack poseStack, float side, float progress, float used) {
        var lift = smooth(Math.min(1F, progress / 0.2F));
        var lower = progress > 0.85F ? smooth((progress - 0.85F) / 0.15F) : 0F;
        var amount = lift * (1F - lower);
        poseStack.translate(-side * 0.22F * amount, 0.1F * amount, 0.08F * amount);
        poseStack.rotate(Axis.XP.rotationDegrees(-25F * amount));
        poseStack.rotate(Axis.YP.rotationDegrees(-side * 15F * amount));
        var shake = Mth.sin(used * 0.9F) * 0.035F * amount;
        var tilt = Mth.cos(used * 0.6F) * 6F * amount;
        poseStack.translate(shake, Math.abs(shake), 0F);
        poseStack.rotate(Axis.ZP.rotationDegrees(tilt));
    }

    private static float smooth(float value) {
        var clamped = Mth.clamp(value, 0F, 1F);
        return clamped * clamped * (3F - 2F * clamped);
    }
}
