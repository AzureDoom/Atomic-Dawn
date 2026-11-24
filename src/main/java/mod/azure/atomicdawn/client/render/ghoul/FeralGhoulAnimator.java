package mod.azure.atomicdawn.client.render.ghoul;

import mod.azure.azurelib.animation.AzAnimatorConfig;
import mod.azure.azurelib.animation.controller.AzAnimationController;
import mod.azure.azurelib.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.animation.impl.AzEntityAnimator;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.ghoul.FeralGhoulAnimations;
import mod.azure.atomicdawn.entity.ghoul.FeralGhoulEntity;

public class FeralGhoulAnimator extends AzEntityAnimator<FeralGhoulEntity> {

    public static final Identifier ANIMATIONS = AtomicDawn.id("animations/entity/feral_ghoul.animation.json");

    public FeralGhoulAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerControllers(AzAnimationControllerContainer<FeralGhoulEntity> container) {
        container.add(AzAnimationController.builder(this, FeralGhoulAnimations.BASE).setTransitionLength(5).build());
        container.add(AzAnimationController.builder(this, FeralGhoulAnimations.ACTION).setTransitionLength(2).build());
    }

    @Override
    public @NotNull Identifier getAnimationLocation(FeralGhoulEntity animatable) {
        return ANIMATIONS;
    }
}
