package mod.azure.atomicdawn.client.render.misterhandy;

import mod.azure.azurelib.animation.AzAnimatorConfig;
import mod.azure.azurelib.animation.controller.AzAnimationController;
import mod.azure.azurelib.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.animation.impl.AzEntityAnimator;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.misterhandy.MisterHandyAnimations;
import mod.azure.atomicdawn.entity.misterhandy.MisterHandyEntity;

public class MisterHandyAnimator extends AzEntityAnimator<MisterHandyEntity> {

    private static final Identifier ANIMATIONS = AtomicDawn.id("animations/entity/mister_handy.animation.json");

    public MisterHandyAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerControllers(AzAnimationControllerContainer<MisterHandyEntity> container) {
        container.add(
            AzAnimationController.builder(this, MisterHandyAnimations.BASE).setTransitionLength(5).build()
        );
        container.add(
            AzAnimationController.builder(this, MisterHandyAnimations.ACTION).setTransitionLength(3).build()
        );
    }

    @Override
    public @NotNull Identifier getAnimationLocation(MisterHandyEntity animatable) {
        return ANIMATIONS;
    }
}
