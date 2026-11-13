package mod.azure.atomicdawn.client.pipboy;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class PipBoyButton {

    public enum Style {

        BUTTON,

        ROW,

        OPTION
    }

    private final Component label;

    private final Rect rect;

    private final boolean active;

    private final boolean selected;

    private final Style style;

    @Nullable
    private final Component tooltip;

    private final Runnable action;

    public PipBoyButton(
        Component label,
        Rect rect,
        boolean active,
        boolean selected,
        Style style,
        @Nullable Component tooltip,
        Runnable action
    ) {
        this.label = label;
        this.rect = rect;
        this.active = active;
        this.selected = selected;
        this.style = style;
        this.tooltip = tooltip;
        this.action = action;
    }

    public Rect rect() {
        return rect;
    }

    public boolean active() {
        return active;
    }

    @Nullable
    public Component tooltip() {
        return tooltip;
    }

    public void press() {
        action.run();
    }

    public boolean hovered(double mouseX, double mouseY) {
        return rect.contains(mouseX, mouseY);
    }

    public void render(GuiGraphicsExtractor graphics, PipBoyScreen screen, int mouseX, int mouseY) {
        var hovered = hovered(mouseX, mouseY) && active;
        var font = screen.font();
        var textY = rect.y() + (rect.height() - 8) / 2 + (rect.height() % 2 == 0 ? 0 : 1);
        switch (style) {
            case ROW -> {
                int color;
                if (hovered) {
                    PipBoyDraw.fill(graphics, rect, PipBoyTheme.text());
                    color = PipBoyTheme.ink();
                } else if (selected) {
                    PipBoyDraw.fill(graphics, rect, PipBoyTheme.selection());
                    graphics.fill(rect.x(), rect.y(), rect.x() + 1, rect.bottom(), PipBoyTheme.text());
                    color = PipBoyTheme.text();
                } else {
                    color = PipBoyTheme.text();
                }
                screen.fitted(graphics, label, rect.x() + 3, textY, rect.width() - 5, color);
            }
            case OPTION -> {
                int color;
                if (hovered) {
                    PipBoyDraw.fill(graphics, rect, PipBoyTheme.text());
                    color = PipBoyTheme.ink();
                } else {
                    color = selected ? PipBoyTheme.text() : PipBoyTheme.dim();
                }
                var w = Math.min(font.width(label), rect.width() - 4);
                var x = rect.x() + (rect.width() - w) / 2;
                screen.fitted(graphics, label, x, textY, rect.width() - 4, color);
                if (selected && !hovered) {
                    graphics.fill(x - 1, rect.bottom() - 1, x + w + 1, rect.bottom(), PipBoyTheme.text());
                }
            }
            default -> {
                int color;
                if (hovered) {
                    PipBoyDraw.fill(graphics, rect, PipBoyTheme.text());
                    color = PipBoyTheme.ink();
                } else {
                    PipBoyDraw.fill(graphics, rect, PipBoyTheme.panel());
                    PipBoyDraw.outline(graphics, rect, active ? PipBoyTheme.border() : PipBoyTheme.faint());
                    color = active ? PipBoyTheme.text() : PipBoyTheme.disabled();
                }
                var w = Math.min(font.width(label), rect.width() - 4);
                screen.fitted(graphics, label, rect.x() + (rect.width() - w) / 2, textY, rect.width() - 4, color);
            }
        }
    }
}
