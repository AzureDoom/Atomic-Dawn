package mod.azure.atomicdawn.client.helper;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

public final class ADGuiGraphics {

    private final GuiGraphicsExtractor raw;

    private ADGuiGraphics(GuiGraphicsExtractor raw) {
        this.raw = raw;
    }

    public static ADGuiGraphics of(GuiGraphicsExtractor raw) {
        return new ADGuiGraphics(raw);
    }

    public GuiGraphicsExtractor raw() {
        return raw;
    }

    public void fill(int x0, int y0, int x1, int y1, int color) {
        raw.fill(x0, y0, x1, y1, color);
    }

    public void text(Font font, Component text, int x, int y, int color, boolean shadow) {
        raw.text(font, text, x, y, color, shadow);
    }

    public void text(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        raw.text(font, text, x, y, color, shadow);
    }

    public void text(Font font, String text, int x, int y, int color, boolean shadow) {
        this.text(font, Component.literal(text), x, y, color, shadow);
    }

    public void item(ItemStack stack, int x, int y) {
        raw.item(stack, x, y);
    }

    public void blit(
        Identifier texture,
        int x,
        int y,
        float u,
        float v,
        int width,
        int height,
        int textureWidth,
        int textureHeight
    ) {
        raw.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    public void push() {
        raw.pose().pushMatrix();
    }

    public void pop() {
        raw.pose().popMatrix();
    }

    public void translate(float x, float y) {
        raw.pose().translate(x, y);
    }

    public void scale(float x, float y) {
        raw.pose().scale(x, y);
    }

    public void rotate(float radians) {
        raw.pose().rotate(radians);
    }
}
