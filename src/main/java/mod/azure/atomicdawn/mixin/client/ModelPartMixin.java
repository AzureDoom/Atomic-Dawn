package mod.azure.atomicdawn.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mod.azure.atomicdawn.client.powerarmor.PowerArmorFirstPerson;
import mod.azure.atomicdawn.util.MixinUtil;

@SuppressWarnings("unused")
@Mixin(ModelPart.class)
public abstract class ModelPartMixin {

    @Inject(
        method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void atomicdawn$hidePowerArmorPilotArms(
        PoseStack poseStack,
        VertexConsumer buffer,
        int light,
        int overlay,
        int color,
        CallbackInfo ci
    ) {
        var self = MixinUtil.<ModelPart>self(this);
        if (PowerArmorFirstPerson.suppresses(self)) {
            ci.cancel();
        }
    }
}
