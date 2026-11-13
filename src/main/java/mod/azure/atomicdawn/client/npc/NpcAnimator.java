package mod.azure.atomicdawn.client.npc;

import mod.azure.azurelib.animation.AzAnimatorConfig;
import mod.azure.azurelib.animation.controller.AzAnimationController;
import mod.azure.azurelib.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.animation.impl.AzEntityAnimator;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.npc.NpcAnimations;
import mod.azure.atomicdawn.npc.NpcEntity;
import mod.azure.atomicdawn.npc.NpcModelDefinition;
import mod.azure.atomicdawn.npc.NpcModels;

public class NpcAnimator extends AzEntityAnimator<NpcEntity> {

    private static final Identifier MISSING = AtomicDawn.id("animations/entity/npc/missing.animation.json");

    public NpcAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerControllers(AzAnimationControllerContainer<NpcEntity> container) {
        container.add(AzAnimationController.builder(this, NpcAnimations.BASE).setTransitionLength(4).build());
        container.add(AzAnimationController.builder(this, NpcAnimations.ACTION).setTransitionLength(2).build());
    }

    @Override
    public @NotNull Identifier getAnimationLocation(NpcEntity npc) {
        return NpcModels.get(npc.level().registryAccess(), NpcClient.appearance(npc).model())
            .map(NpcModelDefinition::animations)
            .orElse(MISSING);
    }
}
