package mod.azure.atomicdawn.client.camp;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import mod.azure.atomicdawn.camp.CampSlotService;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.network.CampActionPayload;
import mod.azure.atomicdawn.network.CampRestorePayload;

public final class CampRestoreScreen extends Screen {

    private static final int PANEL_WIDTH = 300;

    private static final int ROW = 11;

    private static final int MAX_ROWS = 10;

    private static final int BUTTON_HEIGHT = 16;

    private final CampRestorePayload payload;

    private final List<Hit> hits = new ArrayList<>();

    private record Hit(
        int x0,
        int y0,
        int x1,
        int y1,
        Runnable action
    ) {

        boolean contains(double x, double y) {
            return x >= x0 && x < x1 && y >= y0 && y < y1;
        }
    }

    private record Group(
        CampSlotService.Reason reason,
        @Nullable Block block
    ) {}

    private record Row(
        Component text,
        boolean terrain
    ) {}

    public CampRestoreScreen(CampRestorePayload payload) {
        super(Component.translatable("camp.atomicdawn.restore.title"));
        this.payload = payload;
    }

    private List<Row> rows() {
        var groups = new LinkedHashMap<Group, Integer>();
        for (var blocker : payload.blockers()) {
            var reason = blocker.reason();
            var block = reason == CampSlotService.Reason.TERRAIN || reason == CampSlotService.Reason.BLOCK
                ? blocker.state().getBlock()
                : null;
            groups.merge(new Group(reason, block), 1, Integer::sum);
        }
        var rows = new ArrayList<Row>();
        groups.entrySet()
            .stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .forEach(entry -> {
                var group = entry.getKey();
                var key = group.reason().name().toLowerCase(Locale.ROOT);
                var name = group.block() != null
                    ? group.block().getName()
                    : Component.translatable("camp.atomicdawn.restore.reason." + key + "_name");
                var text = Component.translatable(
                    "camp.atomicdawn.restore.line",
                    entry.getValue(),
                    name,
                    Component.translatable("camp.atomicdawn.restore.reason." + key)
                );
                rows.add(new Row(text, group.reason() == CampSlotService.Reason.TERRAIN));
            });
        return rows;
    }

    private int left() {
        return width / 2 - PANEL_WIDTH / 2;
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        hits.clear();
        var lines = rows();
        var shown = Math.min(lines.size(), MAX_ROWS);
        var x = left();
        var textWidth = PANEL_WIDTH - 12;
        var summary = font.split(
            Component.translatable("camp.atomicdawn.restore.summary", payload.ready(), payload.blocked()),
            textWidth
        );
        var hint = font.split(Component.translatable("camp.atomicdawn.restore.highlight"), textWidth);
        var height = 22 + summary.size() * ROW + 6 + shown * ROW + (lines.size() > shown ? ROW : 0) + 6
            + hint.size() * ROW + 8 + BUTTON_HEIGHT + 8;
        var y = Math.max(10, this.height / 2 - height / 2);
        graphics.fill(x, y, x + PANEL_WIDTH, y + height, PipBoyTheme.withAlpha(PipBoyTheme.background(), 0xE8));
        graphics.fill(x, y, x + PANEL_WIDTH, y + 1, PipBoyTheme.border());
        graphics.fill(x, y + height - 1, x + PANEL_WIDTH, y + height, PipBoyTheme.border());
        graphics.text(font, title, x + 6, y + 6, PipBoyTheme.text(), false);
        var lineY = y + 22;
        for (var line : summary) {
            graphics.text(font, line, x + 6, lineY, PipBoyTheme.text(), false);
            lineY += ROW;
        }
        lineY += 6;
        for (var i = 0; i < shown; i++) {
            var row = lines.get(i);
            var text = font.plainSubstrByWidth(row.text().getString(), textWidth);
            graphics.text(font, text, x + 6, lineY, row.terrain() ? PipBoyTheme.warn() : PipBoyTheme.bad(), false);
            lineY += ROW;
        }
        if (lines.size() > shown) {
            graphics.text(
                font,
                Component.translatable("camp.atomicdawn.restore.more", lines.size() - shown),
                x + 6,
                lineY,
                PipBoyTheme.dim(),
                false
            );
            lineY += ROW;
        }
        lineY += 6;
        for (var line : hint) {
            graphics.text(font, line, x + 6, lineY, PipBoyTheme.dim(), false);
            lineY += ROW;
        }
        lineY += 8;
        var buttonX = x + 6;
        var clearable = payload.clearable();
        var terrainCount = payload.blockers().size();
        buttonX = button(
            graphics,
            buttonX,
            lineY,
            Component.translatable("camp.atomicdawn.restore.clear", terrainCount),
            clearable,
            mouseX,
            mouseY,
            () -> act(CampActionPayload.Action.CLEAR_REBUILD, 0)
        );
        buttonX = button(
            graphics,
            buttonX,
            lineY,
            Component.translatable("camp.atomicdawn.restore.partial", payload.ready()),
            payload.ready() > 0,
            mouseX,
            mouseY,
            () -> act(CampActionPayload.Action.REBUILD, 1)
        );
        button(
            graphics,
            buttonX,
            lineY,
            Component.translatable("camp.atomicdawn.restore.later"),
            true,
            mouseX,
            mouseY,
            this::onClose
        );
    }

    private void act(CampActionPayload.Action action, int value) {
        CampClientState.send(action, payload.campId(), value);
        onClose();
    }

    private int button(
        GuiGraphicsExtractor graphics,
        int x,
        int y,
        Component label,
        boolean enabled,
        int mouseX,
        int mouseY,
        Runnable action
    ) {
        var w = font.width(label) + 8;
        var hovered = enabled && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + BUTTON_HEIGHT;
        graphics.fill(x, y, x + w, y + BUTTON_HEIGHT, hovered ? PipBoyTheme.highlight() : PipBoyTheme.panel());
        graphics.text(font, label, x + 4, y + 4, enabled ? PipBoyTheme.text() : PipBoyTheme.disabled(), false);
        if (enabled) {
            hits.add(new Hit(x, y, x + w, y + BUTTON_HEIGHT, action));
        }
        return x + w + 4;
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            for (var hit : List.copyOf(hits)) {
                if (hit.contains(event.x(), event.y())) {
                    hit.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
