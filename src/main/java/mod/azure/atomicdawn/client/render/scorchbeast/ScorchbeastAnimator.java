package mod.azure.atomicdawn.client.render.scorchbeast;

import mod.azure.azurelib.animation.AzAnimatorConfig;
import mod.azure.azurelib.animation.controller.AzAnimationController;
import mod.azure.azurelib.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.animation.impl.AzEntityAnimator;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.scorchbeast.ScorchbeastAnimations;
import mod.azure.atomicdawn.entity.scorchbeast.ScorchbeastEntity;

public class ScorchbeastAnimator extends AzEntityAnimator<ScorchbeastEntity> {

    private static final Identifier ANIMATIONS = AtomicDawn.id("animations/entity/scorchbeast.animation.json");

    public ScorchbeastAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerControllers(AzAnimationControllerContainer<ScorchbeastEntity> container) {
        container.add(
            AzAnimationController.builder(this, ScorchbeastAnimations.BASE).setTransitionLength(8).build()
        );
        container.add(
            AzAnimationController.builder(this, ScorchbeastAnimations.ACTION).setTransitionLength(4).build()
        );
    }

    @Override
    public @NotNull Identifier getAnimationLocation(ScorchbeastEntity animatable) {
        return ANIMATIONS;
    }
}
