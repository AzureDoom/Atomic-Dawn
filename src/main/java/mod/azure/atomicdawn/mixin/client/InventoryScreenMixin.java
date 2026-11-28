package mod.azure.atomicdawn.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mod.azure.atomicdawn.client.powerarmor.PowerArmorInventoryPreview;
import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;

@SuppressWarnings("unused")
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {

    @Inject(
        method = "extractEntityInInventoryFollowsMouse",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void atomicdawn$previewPowerArmor(
        GuiGraphicsExtractor graphics,
        int x0,
        int y0,
        int x1,
        int y1,
        int scale,
        float yOffset,
        float mouseX,
        float mouseY,
        LivingEntity entity,
        CallbackInfo ci
    ) {
        if (
            entity != Minecraft.getInstance().player
                || !(entity.getVehicle() instanceof PowerArmorEntity armor)
        ) {
            return;
        }

        var rendered = PowerArmorInventoryPreview.render(
            graphics,
            x0,
            y0,
            x1,
            y1,
            scale,
            yOffset,
            mouseX,
            mouseY,
            entity,
            armor
        );

        if (rendered) {
            ci.cancel();
        }
    }
}
