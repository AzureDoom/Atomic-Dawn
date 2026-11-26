package mod.azure.atomicdawn.client.helper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;

public final class ADGuiRenderUtil {

    private ADGuiRenderUtil() {}

    public static boolean isHudHidden() {
        return Minecraft.getInstance().gameRenderer.gameRenderState().guiRenderState.isHudHidden;
    }

    public static int guiWidth() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    public static int guiHeight() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    public static void entityPreview(
        ADGuiGraphics graphics,
        int x0,
        int y0,
        int x1,
        int y1,
        int scale,
        float yOffset,
        float mouseX,
        float mouseY,
        LivingEntity entity
    ) {
        InventoryScreen.renderEntityInInventoryFollowsAngle(
            graphics.raw(),
            x0,
            y0,
            x1,
            y1,
            scale,
            yOffset,
            mouseX,
            mouseY,
            entity
        );
    }
}
