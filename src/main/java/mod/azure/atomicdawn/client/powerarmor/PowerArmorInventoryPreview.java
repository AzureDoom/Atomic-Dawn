package mod.azure.atomicdawn.client.powerarmor;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;

import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;

public final class PowerArmorInventoryPreview {

    private static boolean rendering;

    private PowerArmorInventoryPreview() {}

    public static boolean render(
        GuiGraphicsExtractor graphics,
        int x0,
        int y0,
        int x1,
        int y1,
        int scale,
        float yOffset,
        float mouseX,
        float mouseY,
        LivingEntity pilot,
        PowerArmorEntity armor
    ) {
        if (rendering) {
            return false;
        }
        rendering = true;
        try {
            var pilotHeight = pilot.getBbHeight();
            var armorHeight = Math.max(pilotHeight, armor.getBbHeight());
            var lift = (float) (pilot.getY() - armor.getY());
            var fitted = Math.max(1, Math.round(scale * pilotHeight / armorHeight));
            var angleX = (float) Math.atan(((x0 + x1) / 2F - mouseX) / 40F);
            var angleY = (float) Math.atan(((y0 + y1) / 2F - mouseY) / 40F);
            var pilotOffset = (armor.getBbHeight() / 2F - lift - pilotHeight / 2F) / pilot.getScale() + yOffset;
            if (!pilot.isInvisible()) {
                InventoryScreen.renderEntityInInventoryFollowsAngle(
                    graphics,
                    x0,
                    y0,
                    x1,
                    y1,
                    fitted,
                    pilotOffset,
                    angleX,
                    angleY,
                    pilot
                );
            }
            InventoryScreen.renderEntityInInventoryFollowsAngle(
                graphics,
                x0,
                y0,
                x1,
                y1,
                fitted,
                yOffset,
                angleX,
                angleY,
                armor
            );
            return true;
        } finally {
            rendering = false;
        }
    }
}
