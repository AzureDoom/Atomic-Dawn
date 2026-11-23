package mod.azure.atomicdawn.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.client.pipboy.Rect;
import mod.azure.atomicdawn.npc.NpcGearMenu;

public class NpcGearScreen extends AbstractContainerScreen<NpcGearMenu> {

    private static final int PREVIEW_X = 30;

    private static final int PREVIEW_Y = 18;

    private static final int PREVIEW_W = 116;

    private static final int PREVIEW_H = 76;

    public NpcGearScreen(NpcGearMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, NpcGearMenu.WIDTH, NpcGearMenu.HEIGHT);
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = NpcGearMenu.INVENTORY_Y - 11;
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        PipBoyStyle.window(graphics, x, y, imageWidth, imageHeight);
        var preview = new Rect(x + PREVIEW_X, y + PREVIEW_Y, PREVIEW_W, PREVIEW_H);
        PipBoyStyle.panel(graphics, preview.x(), preview.y(), preview.width(), preview.height());
        var npc = menu.npc();
        if (npc != null) {
            var bodyRot = npc.yBodyRot;
            var bodyRotO = npc.yBodyRotO;
            var yRot = npc.getYRot();
            var yRotO = npc.yRotO;
            var xRot = npc.getXRot();
            var xRotO = npc.xRotO;
            var headRot = npc.yHeadRot;
            var headRotO = npc.yHeadRotO;
            var yaw = 200F;
            npc.yBodyRot = yaw;
            npc.yBodyRotO = yaw;
            npc.setYRot(yaw);
            npc.yRotO = yaw;
            npc.setXRot(0F);
            npc.xRotO = 0F;
            npc.yHeadRot = yaw;
            npc.yHeadRotO = yaw;
            InventoryScreen.renderEntityInInventoryFollowsAngle(
                graphics,
                preview.x(),
                preview.y() + 2,
                preview.right(),
                preview.bottom() - 2,
                32,
                0.0F,
                0.0F,
                0.0F,
                npc
            );
            npc.yBodyRot = bodyRot;
            npc.yBodyRotO = bodyRotO;
            npc.setYRot(yRot);
            npc.yRotO = yRotO;
            npc.setXRot(xRot);
            npc.xRotO = xRotO;
            npc.yHeadRot = headRot;
            npc.yHeadRotO = headRotO;
        }
        graphics.fill(
            x + 8,
            y + NpcGearMenu.INVENTORY_Y - 14,
            x + imageWidth - 8,
            y + NpcGearMenu.INVENTORY_Y - 13,
            PipBoyTheme.border()
        );
        for (var i = 0; i < menu.slots.size(); i++) {
            var slot = menu.slots.get(i);
            PipBoyStyle.slot(graphics, x + slot.x, y + slot.y);
            if (i < NpcGearMenu.SLOTS.length && !slot.hasItem()) {
                var glyph = Component.translatable(glyph(NpcGearMenu.SLOTS[i]));
                graphics.text(
                    font,
                    glyph,
                    x + slot.x + 8 - font.width(glyph) / 2,
                    y + slot.y + 4,
                    PipBoyTheme.faint(),
                    false
                );
            }
        }
        PipBoyStyle.crt(graphics, x, y, imageWidth, imageHeight);
    }

    private static String glyph(EquipmentSlot slot) {
        return "npc.atomicdawn.gear.slot." + slot.getName().toLowerCase(Locale.ROOT);
    }

    @Override
    protected void extractLabels(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, PipBoyStyle.text(), false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, PipBoyStyle.dim(), false);
    }
}
