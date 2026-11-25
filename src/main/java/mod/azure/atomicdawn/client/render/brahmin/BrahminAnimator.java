package mod.azure.atomicdawn.client.render.brahmin;

import mod.azure.azurelib.animation.AzAnimatorConfig;
import mod.azure.azurelib.animation.controller.AzAnimationController;
import mod.azure.azurelib.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.animation.impl.AzEntityAnimator;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.brahmin.BrahminAnimations;
import mod.azure.atomicdawn.entity.brahmin.BrahminEntity;

public class BrahminAnimator extends AzEntityAnimator<BrahminEntity> {

    public static final Identifier ANIMATIONS = AtomicDawn.id("animations/entity/brahmin.animation.json");

    public BrahminAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerControllers(AzAnimationControllerContainer<BrahminEntity> container) {
        container.add(AzAnimationController.builder(this, BrahminAnimations.BASE).setTransitionLength(5).build());
        container.add(AzAnimationController.builder(this, BrahminAnimations.ACTION).setTransitionLength(3).build());
    }

    @Override
    public @NotNull Identifier getAnimationLocation(BrahminEntity animatable) {
        return ANIMATIONS;
    }
}
