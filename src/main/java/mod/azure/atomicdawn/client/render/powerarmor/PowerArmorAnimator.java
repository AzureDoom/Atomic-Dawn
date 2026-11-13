package mod.azure.atomicdawn.client.render.powerarmor;

import mod.azure.azurelib.animation.AzAnimatorConfig;
import mod.azure.azurelib.animation.controller.AzAnimationController;
import mod.azure.azurelib.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.animation.impl.AzEntityAnimator;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.powerarmor.PowerArmorAnimations;
import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;

public class PowerArmorAnimator extends AzEntityAnimator<PowerArmorEntity> {

    public static final Identifier ANIMATIONS = AtomicDawn.id("animations/entity/power_armor.animation.json");

    public PowerArmorAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerControllers(AzAnimationControllerContainer<PowerArmorEntity> container) {
        container.add(AzAnimationController.builder(this, PowerArmorAnimations.BASE).setTransitionLength(4).build());
        container.add(AzAnimationController.builder(this, PowerArmorAnimations.ACTION).setTransitionLength(2).build());
    }

    @Override
    public @NotNull Identifier getAnimationLocation(PowerArmorEntity animatable) {
        return ANIMATIONS;
    }
}
