package mod.azure.atomicdawn.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mod.azure.atomicdawn.client.powerarmor.TargetingHud;

@SuppressWarnings("unused")
@Mixin(Minecraft.class)
public abstract class TargetingHudMixin {

    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void atomicdawn$targetingHud(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (TargetingHud.highlights(entity)) {
            cir.setReturnValue(true);
        }
    }
}
