package mod.azure.atomicdawn.client.render.supermutant;

import mod.azure.azurelib.animation.AzAnimatorConfig;
import mod.azure.azurelib.animation.controller.AzAnimationController;
import mod.azure.azurelib.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.animation.impl.AzEntityAnimator;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.entity.supermutant.SuperMutantAnimations;
import mod.azure.atomicdawn.entity.supermutant.SuperMutantEntity;

public class SuperMutantAnimator extends AzEntityAnimator<SuperMutantEntity> {

    public SuperMutantAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerControllers(AzAnimationControllerContainer<SuperMutantEntity> container) {
        container.add(
            AzAnimationController.builder(this, SuperMutantAnimations.BASE).setTransitionLength(6).build()
        );
        container.add(
            AzAnimationController.builder(this, SuperMutantAnimations.ARMS).setTransitionLength(5).build()
        );
        container.add(
            AzAnimationController.builder(this, SuperMutantAnimations.ACTION).setTransitionLength(3).build()
        );
    }

    @Override
    public @NotNull Identifier getAnimationLocation(SuperMutantEntity animatable) {
        return animatable.variant().animations();
    }
}
