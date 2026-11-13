package mod.azure.atomicdawn.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import mod.azure.atomicdawn.weapon.action.WeaponActions;

public final class WeaponClientExtensions implements IClientItemExtensions {

    public static final WeaponClientExtensions INSTANCE = new WeaponClientExtensions();

    private WeaponClientExtensions() {}

    @Override
    public HumanoidModel.@Nullable ArmPose getArmPose(
        @NonNull LivingEntity entity,
        @NonNull InteractionHand hand,
        @NonNull ItemStack stack
    ) {
        if (hand != InteractionHand.MAIN_HAND) {
            return null;
        }
        if (entity instanceof Player player && WeaponActions.isAiming(player, stack)) {
            return HumanoidModel.ArmPose.BOW_AND_ARROW;
        }
        return HumanoidModel.ArmPose.CROSSBOW_HOLD;
    }
}
