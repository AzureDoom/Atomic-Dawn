package mod.azure.atomicdawn.client.hud.layout;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;

import mod.azure.atomicdawn.client.pipboy.Rect;

public final class HudEditorScreen extends Screen {

    private static final int COLOR_GRID = 0x30FFFFFF;

    private static final int COLOR_FILL = 0x3034FF6A;

    private static final int COLOR_FILL_SELECTED = 0x5034FF6A;

    private static final int COLOR_OUTLINE = 0xFF34FF6A;

    private static final int COLOR_OUTLINE_SELECTED = 0xFFFFE066;

    private static final int COLOR_INACTIVE = 0xA0A0A0A0;

    private static final int COLOR_HIDDEN = 0xFFFF5050;

    private static final int COLOR_HOTBAR = 0x40FFFFFF;

    private static final int COLOR_TEXT = 0xFFE8FFE8;

    private static final int SNAP = 4;

    private static final float SCALE_STEP = 0.1F;

    private final Map<HudElement, HudPlacement> original;

    @Nullable
    private HudElement selected;

    @Nullable
    private HudElement dragging;

    private double grabX;

    private double grabY;

    private boolean saved;

    public HudEditorScreen() {
        super(Component.translatable("hud_editor.atomicdawn.title"));
        this.original = HudLayout.snapshot();
    }

    @Override
    protected void init() {
        HudLayout.setEditing(true);
        var centerX = width / 2;
        var y = height / 2 + 30;
        addRenderableWidget(
            Button.builder(Component.translatable("hud_editor.atomicdawn.reset_selected"), _ -> resetSelected())
                .pos(centerX - 154, y)
                .size(100, 20)
                .build()
        );
        addRenderableWidget(
            Button.builder(Component.translatable("hud_editor.atomicdawn.reset_all"), _ -> {
                for (var element : HudElement.values()) {
                    HudLayout.set(element, null);
                }
            }).pos(centerX - 50, y).size(100, 20).build()
        );
        addRenderableWidget(
            Button.builder(Component.translatable("hud_editor.atomicdawn.done"), _ -> {
                save();
                onClose();
            }).pos(centerX + 54, y).size(100, 20).build()
        );
        addRenderableWidget(
            Button.builder(Component.translatable("hud_editor.atomicdawn.cancel"), _ -> {
                HudLayout.restore(original);
                saved = true;
                onClose();
            }).pos(centerX - 50, y + 24).size(100, 20).build()
        );
    }

    private void save() {
        HudLayout.save();
        saved = true;
    }

    @Override
    public void removed() {
        if (!saved) {
            HudLayout.save();
            saved = true;
        }
        HudLayout.setEditing(false);
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void resetSelected() {
        if (selected != null) {
            HudLayout.set(selected, null);
        }
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (dragging != null) {
            drag(dragging, mouseX, mouseY);
        }
        graphics.fill(width / 3, 0, width / 3 + 1, height, COLOR_GRID);
        graphics.fill(width * 2 / 3, 0, width * 2 / 3 + 1, height, COLOR_GRID);
        graphics.fill(0, height / 3, width, height / 3 + 1, COLOR_GRID);
        graphics.fill(0, height * 2 / 3, width, height * 2 / 3 + 1, COLOR_GRID);
        outline(graphics, new Rect(width / 2 - 91, height - 22, 182, 22), COLOR_HOTBAR);
        text(
            graphics,
            Component.translatable("hud_editor.atomicdawn.hotbar"),
            width / 2 - 89,
            height - 32,
            COLOR_HOTBAR
        );

        HudElement hovered = null;
        for (var element : HudElement.values()) {
            var rect = HudLayout.screenRect(element);
            var placement = HudLayout.placement(element);
            var hidden = placement != null && placement.hidden();
            var isSelected = element == selected;
            graphics.fill(
                rect.x(),
                rect.y(),
                rect.right(),
                rect.bottom(),
                isSelected ? COLOR_FILL_SELECTED : COLOR_FILL
            );
            var color = hidden
                ? COLOR_HIDDEN
                : isSelected ? COLOR_OUTLINE_SELECTED : HudLayout.active(element) ? COLOR_OUTLINE : COLOR_INACTIVE;
            outline(graphics, rect, color);
            Component label = Component.translatable(element.translationKey());
            if (hidden) {
                label = Component.translatable("hud_editor.atomicdawn.hidden", label);
            }
            var labelY = rect.y() >= 10 ? rect.y() - 10 : rect.bottom() + 2;
            text(graphics, label, rect.x(), labelY, color);
            if (rect.contains(mouseX, mouseY)) {
                hovered = element;
            }
        }

        var help = Component.translatable("hud_editor.atomicdawn.help");
        var title = getTitle();
        var top = height / 2 - 20;
        text(graphics, title, width / 2 - font.width(title) / 2, top, COLOR_TEXT);
        var lines = font.split(help, Math.min(320, width - 20));
        var y = top + 12;
        for (var line : lines) {
            graphics.text(font, line, width / 2 - font.width(line) / 2, y, COLOR_INACTIVE, true);
            y += 10;
        }
        var focus = hovered != null ? hovered : selected;
        if (focus != null) {
            var placement = HudLayout.placement(focus);
            var info = Component.translatable(
                "hud_editor.atomicdawn.info",
                Component.translatable(focus.translationKey()),
                placement == null
                    ? Component.translatable("hud_editor.atomicdawn.default")
                    : Component.literal(placement.anchor().id().replace('_', ' ')),
                String.format(Locale.ROOT, "%.1f", placement == null ? 1F : placement.scale())
            );
            text(graphics, info, width / 2 - font.width(info) / 2, height / 2 + 74, COLOR_TEXT);
        }
    }

    private void text(GuiGraphicsExtractor graphics, Component text, int x, int y, int color) {
        graphics.text(font, text, x, y, color, true);
    }

    private static void outline(GuiGraphicsExtractor graphics, Rect rect, int color) {
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.y() + 1, color);
        graphics.fill(rect.x(), rect.bottom() - 1, rect.right(), rect.bottom(), color);
        graphics.fill(rect.x(), rect.y(), rect.x() + 1, rect.bottom(), color);
        graphics.fill(rect.right() - 1, rect.y(), rect.right(), rect.bottom(), color);
    }

    @Nullable
    private static HudElement elementAt(double x, double y) {
        var elements = HudElement.values();
        for (var i = elements.length - 1; i >= 0; i--) {
            if (HudLayout.screenRect(elements[i]).contains(x, y)) {
                return elements[i];
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        var element = elementAt(event.x(), event.y());
        if (element == null) {
            selected = null;
            return false;
        }
        selected = element;
        if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            var current = current(element);
            HudLayout.set(element, current.withHidden(!current.hidden()));
            return true;
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            var rect = HudLayout.screenRect(element);
            dragging = element;
            grabX = event.x() - rect.x();
            grabY = event.y() - rect.y();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent event) {
        if (dragging != null && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            drag(dragging, event.x(), event.y());
            dragging = null;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        var element = elementAt(mouseX, mouseY);
        if (element == null || scrollY == 0.0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        selected = element;
        var current = current(element);
        var scale = Math.round((current.scale() + (scrollY > 0 ? SCALE_STEP : -SCALE_STEP)) * 10F) / 10F;
        var base = HudLayout.defaultRect(element);
        var rect = HudLayout.screenRect(element);
        var resized = current.withScale(scale);
        var newWidth = base.width() * resized.scale();
        var newHeight = base.height() * resized.scale();
        var centerX = rect.x() + rect.width() / 2F;
        var centerY = rect.y() + rect.height() / 2F;
        HudLayout.set(
            element,
            HudLayout.fromScreen(
                centerX - newWidth / 2F,
                centerY - newHeight / 2F,
                newWidth,
                newHeight,
                resized.scale(),
                resized.hidden(),
                width,
                height
            )
        );
        return true;
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        if (selected != null) {
            var step = shiftDown() ? 10 : 1;
            switch (event.key()) {
                case InputConstants.KEY_LEFT -> {
                    return nudge(selected, -step, 0);
                }
                case InputConstants.KEY_RIGHT -> {
                    return nudge(selected, step, 0);
                }
                case InputConstants.KEY_UP -> {
                    return nudge(selected, 0, -step);
                }
                case InputConstants.KEY_DOWN -> {
                    return nudge(selected, 0, step);
                }
                case InputConstants.KEY_DELETE, InputConstants.KEY_BACKSPACE -> {
                    resetSelected();
                    return true;
                }
                case InputConstants.KEY_H -> {
                    var current = current(selected);
                    HudLayout.set(selected, current.withHidden(!current.hidden()));
                    return true;
                }
                default -> {}
            }
        }
        return super.keyPressed(event);
    }

    private boolean nudge(HudElement element, int dx, int dy) {
        var rect = HudLayout.screenRect(element);
        var current = current(element);
        HudLayout.set(
            element,
            HudLayout.fromScreen(
                rect.x() + dx,
                rect.y() + dy,
                rect.width(),
                rect.height(),
                current.scale(),
                current.hidden(),
                width,
                height
            )
        );
        return true;
    }

    private void drag(HudElement element, double mouseX, double mouseY) {
        var rect = HudLayout.screenRect(element);
        var current = current(element);
        var left = (float) (mouseX - grabX);
        var top = (float) (mouseY - grabY);
        if (!shiftDown()) {
            left = snap(left, rect.width(), width);
            top = snap(top, rect.height(), height);
        }
        left = Math.clamp(left, 0F, Math.max(0F, width - rect.width()));
        top = Math.clamp(top, 0F, Math.max(0F, height - rect.height()));
        HudLayout.set(
            element,
            HudLayout.fromScreen(
                left,
                top,
                rect.width(),
                rect.height(),
                current.scale(),
                current.hidden(),
                width,
                height
            )
        );
    }

    private static float snap(float start, int size, int screen) {
        float[] candidates = { 0F, screen - size, (screen - size) / 2F };
        for (var candidate : candidates) {
            if (Math.abs(start - candidate) <= SNAP) {
                return candidate;
            }
        }
        return start;
    }

    private boolean shiftDown() {
        return InputConstants.isKeyDown(InputConstants.KEY_LSHIFT)
            || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);
    }

    private HudPlacement current(HudElement element) {
        var placement = HudLayout.placement(element);
        if (placement != null) {
            return placement;
        }
        var rect = HudLayout.screenRect(element);
        return HudLayout.fromScreen(rect.x(), rect.y(), rect.width(), rect.height(), 1F, false, width, height);
    }
}
