package mod.azure.atomicdawn.client.pipboy.tab;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.client.pipboy.PipBoyScreen;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.client.pipboy.Rect;

final class NotesPage {

    private static final String SCROLL = "data.notes";

    private static int selectedSlot = -1;

    private static int page;

    private NotesPage() {}

    record Note(
        int slot,
        ItemStack stack,
        Component title,
        String author,
        List<Component> pages
    ) {}

    static List<Note> notes(LocalPlayer player) {
        var filtered = Minecraft.getInstance().isTextFilteringEnabled();
        var notes = new ArrayList<Note>();
        var inventory = player.getInventory();
        for (var slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            var written = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
            if (written != null) {
                notes.add(
                    new Note(
                        slot,
                        stack,
                        Component.literal(written.title().get(filtered)),
                        written.author(),
                        written.getPages(filtered)
                    )
                );
                continue;
            }
            var writable = stack.get(DataComponents.WRITABLE_BOOK_CONTENT);
            if (writable != null) {
                var pages = new ArrayList<Component>();
                for (var text : writable.pages()) {
                    pages.add(Component.literal(text.get(filtered)));
                }
                var title = stack.has(DataComponents.CUSTOM_NAME)
                    ? stack.getHoverName()
                    : Component.translatable("pipboy.atomicdawn.notes.untitled");
                notes.add(new Note(slot, stack, title, "", pages));
            }
        }
        return notes;
    }

    private static Note selected(List<Note> notes) {
        for (var note : notes) {
            if (note.slot() == selectedSlot) {
                return note;
            }
        }
        return null;
    }

    static boolean scroll(int direction, Rect area) {
        var player = Minecraft.getInstance().player;
        return player != null && PageList.scroll(
            SCROLL,
            direction,
            notes(player).size(),
            PageList.rows(PageList.list(area, false))
        );
    }

    static void init(PipBoyScreen screen, Rect area) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        var list = PageList.list(area, false);
        var notes = notes(player);
        var rows = PageList.rows(list);
        var first = PageList.first(SCROLL, notes.size(), rows);
        for (var r = 0; r < rows && first + r < notes.size(); r++) {
            var note = notes.get(first + r);
            screen.row(
                note.title(),
                list.x(),
                list.y() + r * PageList.ROW,
                list.width(),
                PageList.ROW - 1,
                note.slot() == selectedSlot,
                () -> {
                    selectedSlot = note.slot();
                    page = 0;
                    screen.refresh();
                }
            );
        }
        var note = selected(notes);
        if (note == null || note.pages().size() <= 1) {
            return;
        }
        var detail = PageList.detail(area);
        var y = detail.bottom() - 13;
        screen.button(Component.literal("<"), detail.x(), y, 16, 13, page > 0, () -> {
            page--;
            screen.refresh();
        });
        screen.button(Component.literal(">"), detail.right() - 16, y, 16, 13, page < note.pages().size() - 1, () -> {
            page++;
            screen.refresh();
        });
    }

    static void render(ADGuiGraphics graphics, PipBoyScreen screen, Rect area) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        var list = PageList.list(area, false);
        var detail = PageList.detail(area);
        var notes = notes(player);
        PageList.decorate(graphics, list, detail, SCROLL, notes.size());
        if (notes.isEmpty()) {
            screen.wrapped(
                graphics,
                Component.translatable("pipboy.atomicdawn.data.notes.empty"),
                list.x() + 2,
                list.y() + 3,
                list.width() - 4,
                list.bottom(),
                PipBoyTheme.dim()
            );
        }
        var note = selected(notes);
        if (note == null) {
            screen.wrapped(
                graphics,
                Component.translatable("pipboy.atomicdawn.notes.select"),
                detail.x(),
                detail.y() + 2,
                detail.width(),
                detail.bottom(),
                PipBoyTheme.dim()
            );
            return;
        }
        var x = detail.x();
        var y = detail.y() + 1;
        screen.fitted(graphics, note.title(), x, y, detail.width(), PipBoyTheme.text());
        y += 11;
        if (!note.author().isEmpty()) {
            screen.text(
                graphics,
                Component.translatable("pipboy.atomicdawn.notes.author", note.author()),
                x,
                y,
                PipBoyTheme.dim()
            );
            y += 11;
        }
        var pages = note.pages();
        if (pages.isEmpty()) {
            screen.text(graphics, Component.translatable("pipboy.atomicdawn.notes.blank"), x, y + 2, PipBoyTheme.dim());
            return;
        }
        page = Math.clamp(page, 0, pages.size() - 1);
        graphics.fill(x, y + 1, detail.right(), y + 2, PipBoyTheme.faint());
        screen.wrapped(graphics, pages.get(page), x, y + 5, detail.width(), detail.bottom() - 16, PipBoyTheme.text());
        var counter = Component.translatable("pipboy.atomicdawn.notes.page", page + 1, pages.size());
        screen.text(
            graphics,
            counter,
            x + (detail.width() - screen.font().width(counter)) / 2,
            detail.bottom() - 10,
            PipBoyTheme.dim()
        );
    }
}
