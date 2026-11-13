package mod.azure.atomicdawn.client.animation;

import mod.azure.azurelib.animation.AzAnimatorConfig;
import mod.azure.azurelib.animation.controller.AzAnimationController;
import mod.azure.azurelib.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.animation.impl.AzItemAnimator;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.weapon.action.WeaponAnimations;

public class WeaponAnimator extends AzItemAnimator {

    private final Identifier animationLocation;

    public WeaponAnimator(Identifier animationLocation) {
        super(AzAnimatorConfig.defaultConfig());
        this.animationLocation = animationLocation;
    }

    @Override
    public void registerControllers(AzAnimationControllerContainer<ItemStack> container) {
        container.add(
            AzAnimationController.builder(this, WeaponAnimations.CONTROLLER)
                .setTransitionLength(2)
                .build()
        );
    }

    @Override
    public @NotNull Identifier getAnimationLocation(ItemStack animatable) {
        return animationLocation;
    }
}
