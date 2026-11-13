package mod.azure.atomicdawn.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.crafting.AmmoContainerMenu;

public class AmmoContainerScreen extends AbstractContainerScreen<AmmoContainerMenu> {

    public AmmoContainerScreen(AmmoContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, AmmoContainerMenu.WIDTH, AmmoContainerMenu.HEIGHT);
        titleLabelX = 8;
        titleLabelY = 5;
        inventoryLabelX = 8;
        inventoryLabelY = 90;
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        PipBoyStyle.window(graphics, x, y, imageWidth, imageHeight);
        for (var slot : menu.slots) {
            PipBoyStyle.slot(graphics, x + slot.x, y + slot.y);
        }
        PipBoyStyle.crt(graphics, x, y, imageWidth, imageHeight);
    }

    @Override
    protected void extractLabels(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, PipBoyStyle.text(), false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, PipBoyStyle.dim(), false);
    }
}
