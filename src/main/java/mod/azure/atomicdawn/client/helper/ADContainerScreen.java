package mod.azure.atomicdawn.client.helper;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;

public abstract class ADContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    protected ADContainerScreen(T menu, Inventory inventory, Component title, int width, int height) {
        super(menu, inventory, title, width, height);
    }

    @Override
    public final void extractBackground(
        @NotNull GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        drawBackground(ADGuiGraphics.of(graphics), mouseX, mouseY, partialTick);
    }

    protected void drawBackground(ADGuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics.raw(), mouseX, mouseY, partialTick);
    }

    @Override
    protected final void extractLabels(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        drawLabels(ADGuiGraphics.of(graphics), mouseX, mouseY);
    }

    protected void drawLabels(ADGuiGraphics graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics.raw(), mouseX, mouseY);
    }

    @Override
    public final boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        return mouseClicked(ADGuiInput.mouse(event), doubleClick);
    }

    public boolean mouseClicked(ADGuiInput.Mouse event, boolean doubleClick) {
        return super.mouseClicked(event.raw, doubleClick);
    }

    @Override
    public final boolean mouseReleased(@NotNull MouseButtonEvent event) {
        return mouseReleased(ADGuiInput.mouse(event));
    }

    public boolean mouseReleased(ADGuiInput.Mouse event) {
        return super.mouseReleased(event.raw);
    }

    @Override
    public final boolean keyPressed(@NotNull KeyEvent event) {
        return keyPressed(ADGuiInput.key(event));
    }

    public boolean keyPressed(ADGuiInput.Key event) {
        return super.keyPressed(event.raw);
    }
}
