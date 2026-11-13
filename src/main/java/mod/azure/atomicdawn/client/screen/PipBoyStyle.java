package mod.azure.atomicdawn.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import mod.azure.atomicdawn.client.pipboy.PipBoyDraw;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.client.pipboy.Rect;

public final class PipBoyStyle {

    private enum Role {
        TAB,
        ROW
    }

    private static final Map<AbstractButton, Role> ROLES = Collections.synchronizedMap(new WeakHashMap<>());

    private PipBoyStyle() {}

    public static int frame() {
        return PipBoyTheme.border();
    }

    public static int background() {
        return PipBoyTheme.withAlpha(PipBoyTheme.glass(), 0xF2);
    }

    public static int panel() {
        return PipBoyTheme.panel();
    }

    public static int slotEdge() {
        return PipBoyTheme.faint();
    }

    public static int slotBackground() {
        return PipBoyTheme.glass();
    }

    public static int text() {
        return PipBoyTheme.text();
    }

    public static int dim() {
        return PipBoyTheme.dim();
    }

    public static int good() {
        return PipBoyTheme.text();
    }

    public static int bad() {
        return PipBoyTheme.bad();
    }

    public static int highlight() {
        return PipBoyTheme.warn();
    }

    public static void window(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        var rect = new Rect(x, y, width, height);
        PipBoyDraw.fill(graphics, rect.inset(-1), frame());
        PipBoyDraw.fill(graphics, rect, background());
        PipBoyDraw.fill(graphics, rect.inset(Math.max(4, height / 6)), PipBoyTheme.phosphor(0.035F, 0x50));
    }

    public static void panel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        var rect = new Rect(x, y, width, height);
        PipBoyDraw.fill(graphics, rect, panel());
        PipBoyDraw.outline(graphics, rect, PipBoyTheme.faint());
    }

    public static void slot(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, slotEdge());
        graphics.fill(x, y, x + 16, y + 16, slotBackground());
    }

    public static void title(GuiGraphicsExtractor graphics, Font font, Component title, int x, int y, int width) {
        graphics.text(font, title, x, y, text(), false);
        graphics.fill(x, y + font.lineHeight + 1, x + width, y + font.lineHeight + 2, PipBoyTheme.border());
    }

    public static void crt(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        var scan = PipBoyTheme.scanline();
        if ((scan >>> 24) > 0) {
            for (var line = y; line < y + height; line += 3) {
                graphics.fill(x, line, x + width, line + 1, scan);
            }
        }
        PipBoyDraw.vignette(graphics, new Rect(x, y, width, height), Math.max(4, height / 20), 0x60);
    }

    public static <T extends AbstractButton> T tab(T button) {
        ROLES.put(button, Role.TAB);
        return button;
    }

    public static <T extends AbstractButton> T row(T button) {
        ROLES.put(button, Role.ROW);
        return button;
    }

    public static void skinButtons(
        GuiGraphicsExtractor graphics,
        Font font,
        List<? extends GuiEventListener> children,
        int originX,
        int originY
    ) {
        var rowScale = new HashMap<Integer, Float>();
        for (var child : children) {
            if (child instanceof AbstractButton button && button.visible && ROLES.get(button) == Role.TAB) {
                var w = font.width(button.getMessage());
                var scale = w <= 0 ? 1F : Math.min(1F, (button.getWidth() - 4F) / w);
                rowScale.merge(button.getY(), scale, Math::min);
            }
        }
        for (var child : children) {
            if (!(child instanceof AbstractButton button) || !button.visible) {
                continue;
            }
            var rect = new Rect(
                button.getX() - originX,
                button.getY() - originY,
                button.getWidth(),
                button.getHeight()
            );
            var hovered = button.active && (button.isHovered() || button.isFocused());
            var role = ROLES.get(button);
            var label = button.getMessage();
            var textY = rect.y() + (rect.height() - 8) / 2 + (rect.height() % 2 == 0 ? 0 : 1);
            if (role == Role.TAB) {
                var selected = !button.active;
                PipBoyDraw.fill(graphics, rect, hovered ? PipBoyTheme.text() : PipBoyTheme.background());
                var color = hovered ? PipBoyTheme.ink() : selected ? PipBoyTheme.text() : PipBoyTheme.dim();
                var scale = rowScale.getOrDefault(button.getY(), 1F);
                var w = Math.round(font.width(label) * scale);
                var x = rect.x() + (rect.width() - w) / 2;
                scaled(graphics, font, label, x, textY, scale, color);
                if (selected) {
                    graphics.fill(x - 1, rect.bottom() - 1, x + w + 1, rect.bottom(), PipBoyTheme.text());
                }
            } else if (role == Role.ROW) {
                var selected = !button.active;
                int color;
                if (hovered) {
                    PipBoyDraw.fill(graphics, rect, PipBoyTheme.text());
                    color = PipBoyTheme.ink();
                } else if (selected) {
                    PipBoyDraw.fill(graphics, rect, PipBoyTheme.selection());
                    graphics.fill(rect.x(), rect.y(), rect.x() + 1, rect.bottom(), PipBoyTheme.text());
                    color = PipBoyTheme.text();
                } else {
                    PipBoyDraw.fill(graphics, rect, PipBoyTheme.panel());
                    color = PipBoyTheme.text();
                }
                fitted(graphics, font, label, rect.x() + 3, textY, rect.width() - 5, color);
            } else {
                int color;
                if (hovered) {
                    PipBoyDraw.fill(graphics, rect, PipBoyTheme.text());
                    color = PipBoyTheme.ink();
                } else {
                    PipBoyDraw.fill(graphics, rect, PipBoyTheme.panel());
                    PipBoyDraw.outline(graphics, rect, button.active ? PipBoyTheme.border() : PipBoyTheme.faint());
                    color = button.active ? PipBoyTheme.text() : PipBoyTheme.disabled();
                }
                var w = Math.min(font.width(label), rect.width() - 4);
                fitted(graphics, font, label, rect.x() + (rect.width() - w) / 2, textY, rect.width() - 4, color);
            }
        }
    }

    private static void scaled(
        GuiGraphicsExtractor graphics,
        Font font,
        Component text,
        int x,
        int y,
        float scale,
        int color
    ) {
        if (scale >= 1F) {
            graphics.text(font, text, x, y, color, false);
            return;
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y + (1 - scale) * 4);
        graphics.pose().scale(scale, scale);
        graphics.text(font, text, 0, 0, color, false);
        graphics.pose().popMatrix();
    }

    public static void fitted(
        GuiGraphicsExtractor graphics,
        Font font,
        Component text,
        int x,
        int y,
        int maxWidth,
        int color
    ) {
        var w = font.width(text);
        if (w <= maxWidth || w == 0) {
            graphics.text(font, text, x, y, color, false);
            return;
        }
        var scale = Math.max(0.01F, (float) maxWidth / w);
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y + (1 - scale) * 4);
        graphics.pose().scale(scale, scale);
        graphics.text(font, text, 0, 0, color, false);
        graphics.pose().popMatrix();
    }
}
