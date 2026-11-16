package mod.azure.atomicdawn.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mod.azure.atomicdawn.client.render.powerarmor.PowerArmorRenderer;
import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;
import mod.azure.atomicdawn.util.MixinUtil;

@SuppressWarnings("unused")
@Mixin(PlayerModel.class)
public abstract class PowerArmorPlayerModelMixin {

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void atomicdawn$posePowerArmorRider(AvatarRenderState state, CallbackInfo ci) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null)
            return;
        var entity = minecraft.level.getEntity(state.id);
        if (!(entity instanceof Player player))
            return;
        if (!(player.getVehicle() instanceof PowerArmorEntity armor))
            return;
        if (!(minecraft.getEntityRenderDispatcher().getRenderer(armor) instanceof PowerArmorRenderer renderer))
            return;

        float partialTick = Math.clamp(state.ageInTicks - player.tickCount, 0F, 1F);
        var pose = renderer.sampleRiderPose(armor, partialTick);
        var self = MixinUtil.<PlayerModel>self(this);
        if (pose != null)
            pose.apply(self);
    }
}
