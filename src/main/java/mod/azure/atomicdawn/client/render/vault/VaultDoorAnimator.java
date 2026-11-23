package mod.azure.atomicdawn.client.render.vault;

import mod.azure.azurelib.animation.AzAnimatorConfig;
import mod.azure.azurelib.animation.controller.AzAnimationController;
import mod.azure.azurelib.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.animation.impl.AzBlockAnimator;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.vault.VaultDoorAnimations;
import mod.azure.atomicdawn.vault.VaultDoorBlockEntity;

public class VaultDoorAnimator extends AzBlockAnimator<VaultDoorBlockEntity> {

    public static final Identifier ANIMATIONS = AtomicDawn.id("animations/block/vault_door.animation.json");

    public VaultDoorAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerControllers(AzAnimationControllerContainer<VaultDoorBlockEntity> container) {
        container.add(AzAnimationController.builder(this, VaultDoorAnimations.BASE).setTransitionLength(0).build());
    }

    @Override
    public @NotNull Identifier getAnimationLocation(VaultDoorBlockEntity animatable) {
        return ANIMATIONS;
    }
}
