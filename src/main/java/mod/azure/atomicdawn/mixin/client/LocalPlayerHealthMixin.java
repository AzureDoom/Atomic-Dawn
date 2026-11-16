package mod.azure.atomicdawn.mixin.client;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mod.azure.atomicdawn.client.radiation.HealthClampClient;
import mod.azure.atomicdawn.util.MixinUtil;

@SuppressWarnings("unused")
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerHealthMixin {

    @Inject(method = "hurtTo(F)V", at = @At("HEAD"), cancellable = true)
    private void atomicdawn$skipClampFlash(float health, CallbackInfo ci) {
        var self = MixinUtil.<LocalPlayer>self(this);
        if (health < self.getHealth() && HealthClampClient.consume(health)) {
            self.setHealth(health);
            ci.cancel();
        }
    }
}
