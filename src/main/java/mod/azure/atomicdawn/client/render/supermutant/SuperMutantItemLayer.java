package mod.azure.atomicdawn.client.render.supermutant;

import com.mojang.math.Axis;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import mod.azure.azurelib.render.layer.AzBlockAndItemLayer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

import mod.azure.atomicdawn.entity.supermutant.SuperMutantEntity;

public class SuperMutantItemLayer extends AzBlockAndItemLayer<UUID, SuperMutantEntity> {

    public static final String RIGHT_HAND = "right_hand_item";

    public static final String LEFT_HAND = "left_hand_item";

    private static final float SCALE = 1.4F;

    @Override
    public ItemStack itemStackForBone(AzBone bone, SuperMutantEntity animatable) {
        if (RIGHT_HAND.equals(bone.getName())) {
            var stack = animatable.getMainHandItem();
            return stack.isEmpty() ? null : stack;
        }
        if (LEFT_HAND.equals(bone.getName())) {
            var stack = animatable.getOffhandItem();
            return stack.isEmpty() ? null : stack;
        }
        return null;
    }

    @Override
    protected ItemDisplayContext getTransformTypeForStack(
        AzBone bone,
        ItemStack stack,
        SuperMutantEntity animatable
    ) {
        return LEFT_HAND.equals(bone.getName())
            ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND
            : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    @Override
    protected void renderItemForBone(
        AzRendererPipelineContext<UUID, SuperMutantEntity> context,
        AzBone bone,
        ItemStack itemStack,
        SuperMutantEntity animatable
    ) {
        var poseStack = context.poseStack();
        poseStack.pushPose();
        poseStack.rotate(Axis.XP.rotationDegrees(-90F));
        poseStack.scale(SCALE, SCALE, SCALE);
        super.renderItemForBone(context, bone, itemStack, animatable);
        poseStack.popPose();
    }
}
