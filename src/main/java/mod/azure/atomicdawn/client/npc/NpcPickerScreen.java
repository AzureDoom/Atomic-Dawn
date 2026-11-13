package mod.azure.atomicdawn.client.npc;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.network.NpcPickPayload;
import mod.azure.atomicdawn.network.NpcPickerPayload;

public final class NpcPickerScreen extends Screen {

    private static final int ROW = 14;

    private static final int FIELD_H = 18;

    private final List<NpcPickerPayload.Entry> entries;

    private final long openedAt = System.currentTimeMillis();

    private String filter = "";

    private int scroll;

    private int left;

    private int top;

    private int panelW;

    private int panelBottom;

    private int listY;

    private int listRows;

    public NpcPickerScreen(List<NpcPickerPayload.Entry> entries) {
        super(Component.translatable("npc.atomicdawn.picker.title"));
        this.entries = entries;
    }

    @Override
    protected void init() {
        panelW = Math.min(280, width - 16);
        left = (width - panelW) / 2;
        top = 16;
        panelBottom = height - 16;
        var search = new EditBox(font, left + 6, top + 18, panelW - 12, FIELD_H, Component.empty());
        search.setHint(Component.translatable("npc.atomicdawn.picker.search"));
        search.setValue(filter);
        search.setResponder(value -> {
            filter = value.toLowerCase(Locale.ROOT);
            scroll = 0;
        });
        addRenderableWidget(search);
        listY = top + 18 + FIELD_H + 4;
        listRows = Math.max(1, (panelBottom - FIELD_H - 10 - listY) / ROW);
        addRenderableWidget(
            Button.builder(Component.translatable("npc.atomicdawn.editor.cancel"), _ -> onClose())
                .pos(left + 6, panelBottom - FIELD_H - 4)
                .size(panelW - 12, FIELD_H)
                .build()
        );
        setInitialFocus(search);
    }

    private List<NpcPickerPayload.Entry> visible() {
        if (filter.isEmpty()) {
            return entries;
        }
        return entries.stream().filter(entry -> entry.name().toLowerCase(Locale.ROOT).contains(filter)).toList();
    }

    /** Server sent seconds at open time; count down locally so the list doesn't look frozen. */
    private int remaining(NpcPickerPayload.Entry entry) {
        var elapsed = (int) ((System.currentTimeMillis() - openedAt) / 1000L);
        return Math.max(0, entry.respawnSeconds() - elapsed);
    }

    private int rowAt(double x, double y) {
        if (x < left + 6 || x >= left + panelW - 6 || y < listY || y >= listY + listRows * ROW) {
            return -1;
        }
        var index = (int) ((y - listY) / ROW) + scroll;
        return index < visible().size() ? index : -1;
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        var index = rowAt(event.x(), event.y());
        if (index >= 0) {
            ClientPacketDistributor.sendToServer(new NpcPickPayload(visible().get(index).entityId()));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        var max = Math.max(0, visible().size() - listRows);
        scroll = Math.clamp(scroll - (int) Math.signum(scrollY) * 3L, 0, max);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(left - 1, top - 1, left + panelW + 1, panelBottom + 1, PipBoyTheme.border());
        graphics.fill(left, top, left + panelW, panelBottom, PipBoyTheme.withAlpha(PipBoyTheme.background(), 0xE8));
        graphics.text(font, title, left + 6, top + 5, PipBoyTheme.text(), false);

        var list = visible();
        var rowW = panelW - 12;
        for (var i = 0; i < listRows; i++) {
            var index = scroll + i;
            if (index >= list.size()) {
                break;
            }
            var entry = list.get(index);
            var y = listY + i * ROW;
            if (rowAt(mouseX, mouseY) == index) {
                graphics.fill(left + 6, y, left + 6 + rowW, y + ROW, PipBoyTheme.highlight());
            }
            var remaining = remaining(entry);
            var status = entry.downed()
                ? remaining > 0
                    ? Component.translatable("npc.atomicdawn.picker.respawning", remaining)
                    : Component.translatable("npc.atomicdawn.picker.respawning_soon")
                : Component.translatable("npc.atomicdawn.picker.distance", entry.distance());
            var statusW = font.width(status);
            var name = font.plainSubstrByWidth(entry.name(), rowW - statusW - 12);
            graphics.text(
                font,
                Component.literal(name),
                left + 9,
                y + 3,
                entry.downed() ? PipBoyTheme.faint() : PipBoyTheme.text(),
                false
            );
            graphics.text(
                font,
                status,
                left + 6 + rowW - statusW - 3,
                y + 3,
                entry.downed() ? PipBoyTheme.bad() : PipBoyTheme.faint(),
                false
            );
        }
        if (list.isEmpty()) {
            graphics.text(
                font,
                Component.translatable("npc.atomicdawn.editor.empty"),
                left + 9,
                listY + 3,
                PipBoyTheme.faint(),
                false
            );
        }
    }
}
