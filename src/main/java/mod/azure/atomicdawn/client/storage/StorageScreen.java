package mod.azure.atomicdawn.client.storage;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import mod.azure.atomicdawn.camp.CampScrapbox;
import mod.azure.atomicdawn.camp.CampStash;
import mod.azure.atomicdawn.client.pipboy.PipBoyDraw;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.client.pipboy.Rect;
import mod.azure.atomicdawn.client.screen.PipBoyStyle;
import mod.azure.atomicdawn.junk.ScrapRecipes;
import mod.azure.atomicdawn.network.StorageActionPayload;
import mod.azure.atomicdawn.network.StorageOpenPayload;

public final class StorageScreen extends Screen {

    private static final int WIDTH = 236;

    private static final int HEIGHT = 266;

    private static final int ROW_H = 18;

    private static final int ROWS = 5;

    private static final int SLOT = 18;

    private static final double MAX_DISTANCE_SQR = 8.0 * 8.0;

    private final StorageOpenPayload.Mode mode;

    private final BlockPos pos;

    private EditBox search;

    private int scroll;

    private int left;

    private int top;

    private record Entry(
        ItemStack stack,
        long count,
        boolean scrappable,
        int index
    ) {}

    public StorageScreen(StorageOpenPayload.Mode mode, BlockPos pos) {
        super(
            Component.translatable(
                mode == StorageOpenPayload.Mode.STASH ? "screen.atomicdawn.stash.title" : "block.atomicdawn.scrapbox"
            )
        );
        this.mode = mode;
        this.pos = pos;
    }

    private boolean stash() {
        return mode == StorageOpenPayload.Mode.STASH;
    }

    private String key(String suffix) {
        return (stash() ? "screen.atomicdawn.stash." : "screen.atomicdawn.scrapbox.") + suffix;
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = Math.max(2, (height - HEIGHT) / 2);
        var previous = search == null ? "" : search.getValue();
        search = new EditBox(font, left + 8, top + 20, WIDTH - 16, 12, Component.translatable(key("search")));
        search.setHint(Component.translatable(key("search")));
        search.setValue(previous);
        search.setResponder(_ -> scroll = 0);
        addRenderableWidget(search);
    }

    private boolean inRange() {
        return minecraft.player != null && minecraft.level != null && Vec3.atCenterOf(pos)
            .distanceToSqr(minecraft.player.position()) <= MAX_DISTANCE_SQR;
    }

    @Override
    public void tick() {
        super.tick();
        if (!inRange()) {
            onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private List<Entry> entries() {
        var player = minecraft.player;
        var filter = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT).trim();
        var list = new ArrayList<Entry>();
        if (stash()) {
            var entries = CampStash.of(player).entries();
            for (var i = 0; i < entries.size(); i++) {
                var entry = entries.get(i);
                list.add(new Entry(entry.stack(), entry.count(), false, i));
            }
        } else {
            var access = player.level().registryAccess();
            for (var stored : CampScrapbox.of(player).items().entrySet()) {
                var stack = new ItemStack(stored.getKey());
                list.add(new Entry(stack, stored.getValue(), ScrapRecipes.scrappable(access, stack), -1));
            }
        }
        if (!filter.isEmpty()) {
            list.removeIf(entry -> !entry.stack().getHoverName().getString().toLowerCase(Locale.ROOT).contains(filter));
        }
        list.sort(
            Comparator.comparing(Entry::scrappable)
                .reversed()
                .thenComparing(entry -> entry.stack().getHoverName().getString())
        );
        return list;
    }

    private Rect listArea() {
        return new Rect(left + 8, top + 38, WIDTH - 16, ROWS * ROW_H);
    }

    private Rect depositButton() {
        return new Rect(left + 8, top + 132, (WIDTH - 20) / 2, 16);
    }

    private Rect scrapButton() {
        var half = (WIDTH - 20) / 2;
        return new Rect(left + 12 + half, top + 132, half, 16);
    }

    private int gridX() {
        return left + (WIDTH - 9 * SLOT) / 2;
    }

    private int gridY() {
        return top + 176;
    }

    private int slotAt(double mouseX, double mouseY) {
        var x = (int) Math.floor((mouseX - gridX()) / SLOT);
        if (x < 0 || x >= 9 || mouseX < gridX()) {
            return -1;
        }
        var localY = mouseY - gridY();
        if (localY >= 0 && localY < 3 * SLOT) {
            return 9 + (int) (localY / SLOT) * 9 + x;
        }
        var hotbarY = localY - 3 * SLOT - 4;
        if (hotbarY >= 0 && hotbarY < SLOT) {
            return x;
        }
        return -1;
    }

    private int slotY(int slot) {
        return slot < 9 ? gridY() + 3 * SLOT + 4 : gridY() + ((slot - 9) / 9) * SLOT;
    }

    private int slotX(int slot) {
        return gridX() + (slot % 9) * SLOT;
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        if (!inRange()) {
            return;
        }
        var player = minecraft.player;
        PipBoyStyle.window(graphics, left, top, WIDTH, HEIGHT);
        PipBoyStyle.title(graphics, font, title, left + 8, top + 6, WIDTH - 16);

        var area = listArea();
        PipBoyStyle.panel(graphics, area.x() - 2, area.y() - 2, area.width() + 4, area.height() + 4);
        var entries = entries();
        scroll = Math.clamp(scroll, 0, Math.max(0, entries.size() - ROWS));
        if (entries.isEmpty()) {
            PipBoyStyle.fitted(
                graphics,
                font,
                Component.translatable(key("empty")),
                area.x() + 4,
                area.y() + 4,
                area.width() - 8,
                PipBoyStyle.text()
            );
            PipBoyStyle.fitted(
                graphics,
                font,
                Component.translatable(key("empty_hint")),
                area.x() + 4,
                area.y() + 16,
                area.width() - 8,
                PipBoyStyle.dim()
            );
        }
        ItemStack hovered = null;
        for (var row = 0; row < ROWS && scroll + row < entries.size(); row++) {
            var entry = entries.get(scroll + row);
            var rowRect = new Rect(area.x(), area.y() + row * ROW_H, area.width() - 6, ROW_H);
            if (rowRect.contains(mouseX, mouseY)) {
                PipBoyDraw.fill(graphics, rowRect, PipBoyTheme.selection());
                hovered = entry.stack();
            }
            graphics.item(entry.stack(), rowRect.x() + 1, rowRect.y() + 1);
            var count = Component.literal(format(entry.count()));
            var countW = font.width(count);
            PipBoyStyle.fitted(
                graphics,
                font,
                entry.stack().getHoverName(),
                rowRect.x() + 20,
                rowRect.y() + 5,
                rowRect.width() - 30 - countW,
                stash() || entry.scrappable() ? PipBoyStyle.text() : PipBoyStyle.dim()
            );
            graphics.text(font, count, rowRect.right() - countW - 2, rowRect.y() + 5, PipBoyStyle.text(), false);
        }
        if (entries.size() > ROWS) {
            var trackH = area.height();
            var thumbH = Math.max(8, trackH * ROWS / entries.size());
            var thumbY = area.y() + (trackH - thumbH) * scroll / Math.max(1, entries.size() - ROWS);
            graphics.fill(area.right() - 4, area.y(), area.right() - 2, area.bottom(), PipBoyTheme.faint());
            graphics.fill(area.right() - 4, thumbY, area.right() - 2, thumbY + thumbH, PipBoyTheme.text());
        }

        if (stash()) {
            var stash = CampStash.of(player);
            var limit = Math.max(1, CampStash.limit());
            var bar = new Rect(left + 8, top + 132, WIDTH - 16, 16);
            PipBoyDraw.fill(graphics, bar, PipBoyTheme.panel());
            var fill = (int) Math.min(bar.width(), bar.width() * stash.total() / limit);
            graphics.fill(
                bar.x(),
                bar.y(),
                bar.x() + fill,
                bar.bottom(),
                stash.total() >= limit ? PipBoyTheme.bad() : PipBoyTheme.selection()
            );
            PipBoyDraw.outline(graphics, bar, PipBoyTheme.border());
            var capacity = Component.translatable(key("capacity"), stash.total(), CampStash.limit());
            graphics.text(
                font,
                capacity,
                bar.x() + (bar.width() - font.width(capacity)) / 2,
                bar.y() + 4,
                PipBoyStyle.text(),
                false
            );
        } else {
            button(graphics, depositButton(), Component.translatable(key("deposit")), mouseX, mouseY, true);
            var anyJunk = entries.stream().anyMatch(Entry::scrappable);
            button(graphics, scrapButton(), Component.translatable(key("scrap_all")), mouseX, mouseY, anyJunk);
            var total = Component.translatable(key("total"), format(CampScrapbox.of(player).total()));
            graphics.text(font, total, left + 8, top + 152, PipBoyStyle.dim(), false);
        }

        graphics.text(
            font,
            Component.translatable("screen.atomicdawn.storage.inventory"),
            gridX(),
            top + 165,
            PipBoyStyle.dim(),
            false
        );
        var inventory = player.getInventory();
        var hoveredSlot = slotAt(mouseX, mouseY);
        var access = player.level().registryAccess();
        for (var slot = 0; slot < 36; slot++) {
            var x = slotX(slot);
            var y = slotY(slot);
            PipBoyStyle.slot(graphics, x, y);
            var stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            var accepted = stash() || ScrapRecipes.storable(access, stack);
            if (slot == hoveredSlot) {
                graphics.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, PipBoyTheme.selection());
                hovered = stack;
            }
            graphics.item(stack, x + 1, y + 1);
            if (!accepted) {
                graphics.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, 0x99000000);
            }
            if (stack.getCount() > 1) {
                var count = Component.literal(String.valueOf(stack.getCount()));
                graphics.text(font, count, x + SLOT - 1 - font.width(count), y + SLOT - 9, 0xFFFFFFFF, true);
            }
        }

        PipBoyStyle.fitted(
            graphics,
            font,
            Component.translatable(key("hint")),
            left + 8,
            top + HEIGHT - 11,
            WIDTH - 16,
            PipBoyStyle.dim()
        );
        PipBoyStyle.crt(graphics, left, top, WIDTH, HEIGHT);
        if (hovered != null) {
            tooltip(graphics, hovered.getHoverName(), mouseX, mouseY);
        }
    }

    private void tooltip(GuiGraphicsExtractor graphics, Component text, int mouseX, int mouseY) {
        var w = font.width(text);
        var x = Math.min(mouseX + 10, width - w - 8);
        var y = Math.max(4, mouseY - 14);
        var box = new Rect(x - 3, y - 3, w + 6, 14);
        PipBoyDraw.fill(graphics, box, PipBoyTheme.withAlpha(PipBoyTheme.glass(), 0xF2));
        PipBoyDraw.outline(graphics, box, PipBoyTheme.text());
        graphics.text(font, text, x, y, PipBoyTheme.text(), false);
    }

    private void button(
        GuiGraphicsExtractor graphics,
        Rect rect,
        Component label,
        int mouseX,
        int mouseY,
        boolean active
    ) {
        var hover = active && rect.contains(mouseX, mouseY);
        PipBoyDraw.fill(graphics, rect, hover ? PipBoyTheme.text() : PipBoyTheme.panel());
        PipBoyDraw.outline(graphics, rect, active ? PipBoyTheme.border() : PipBoyTheme.faint());
        var color = hover ? PipBoyTheme.ink() : active ? PipBoyTheme.text() : PipBoyTheme.disabled();
        var w = Math.min(font.width(label), rect.width() - 4);
        PipBoyStyle.fitted(
            graphics,
            font,
            label,
            rect.x() + (rect.width() - w) / 2,
            rect.y() + 4,
            rect.width() - 4,
            color
        );
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        if (!inRange()) {
            return super.mouseClicked(event, doubleClick);
        }
        var x = event.x();
        var y = event.y();
        var right = event.button() == InputConstants.MOUSE_BUTTON_RIGHT;
        if (!stash() && depositButton().contains(x, y)) {
            send(StorageActionPayload.of(mode, pos, StorageActionPayload.Action.DEPOSIT_ALL));
            return true;
        }
        if (!stash() && scrapButton().contains(x, y)) {
            send(StorageActionPayload.of(mode, pos, StorageActionPayload.Action.SCRAP_ALL));
            return true;
        }
        var slot = slotAt(x, y);
        if (slot >= 0) {
            if (!minecraft.player.getInventory().getItem(slot).isEmpty()) {
                send(
                    new StorageActionPayload(
                        mode,
                        pos,
                        StorageActionPayload.Action.DEPOSIT_SLOT,
                        StorageActionPayload.NONE,
                        slot,
                        right ? 1 : 2
                    )
                );
            }
            return true;
        }
        var area = listArea();
        if (area.contains(x, y)) {
            var entry = entryAt((int) y - area.y());
            if (entry != null) {
                var id = BuiltInRegistries.ITEM.getKey(entry.stack().getItem());
                if (right && !stash()) {
                    if (entry.scrappable()) {
                        send(new StorageActionPayload(mode, pos, StorageActionPayload.Action.SCRAP, id, -1, 0));
                    }
                } else {
                    var max = entry.stack().getMaxStackSize();
                    var amount = event.hasShiftDown() ? max * 36 : right ? 1 : max;
                    send(
                        new StorageActionPayload(
                            mode,
                            pos,
                            StorageActionPayload.Action.WITHDRAW,
                            id,
                            entry.index(),
                            amount
                        )
                    );
                }
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private @Nullable Entry entryAt(int localY) {
        var index = scroll + localY / ROW_H;
        var entries = entries();
        return index >= 0 && index < entries.size() ? entries.get(index) : null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0 && listArea().contains(mouseX, mouseY)) {
            scroll = Math.max(0, scroll + (scrollY > 0 ? -1 : 1));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private static void send(StorageActionPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }

    private static String format(long count) {
        if (count >= 1_000_000_000L) {
            return String.format(Locale.ROOT, "%.1fB", count / 1_000_000_000D);
        }
        if (count >= 1_000_000L) {
            return String.format(Locale.ROOT, "%.1fM", count / 1_000_000D);
        }
        if (count >= 10_000L) {
            return String.format(Locale.ROOT, "%.1fK", count / 1_000D);
        }
        return Long.toString(count);
    }
}
