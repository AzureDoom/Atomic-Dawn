package mod.azure.atomicdawn.client.render.scorched;

import mod.azure.azurelib.animation.AzAnimatorConfig;
import mod.azure.azurelib.animation.controller.AzAnimationController;
import mod.azure.azurelib.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.animation.impl.AzEntityAnimator;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.scorched.ScorchedAnimations;
import mod.azure.atomicdawn.entity.scorched.ScorchedEntity;

public class ScorchedAnimator extends AzEntityAnimator<ScorchedEntity> {

    public static final Identifier ANIMATIONS = AtomicDawn.id("animations/entity/scorched.animation.json");

    public ScorchedAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerControllers(AzAnimationControllerContainer<ScorchedEntity> container) {
        container.add(AzAnimationController.builder(this, ScorchedAnimations.BASE).setTransitionLength(5).build());
        container.add(AzAnimationController.builder(this, ScorchedAnimations.ARMS).setTransitionLength(4).build());
        container.add(AzAnimationController.builder(this, ScorchedAnimations.ACTION).setTransitionLength(2).build());
    }

    @Override
    public @NotNull Identifier getAnimationLocation(ScorchedEntity animatable) {
        return ANIMATIONS;
    }
}
