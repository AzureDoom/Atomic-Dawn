package mod.azure.atomicdawn.client.camp;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mod.azure.atomicdawn.camp.CampBuildable;
import mod.azure.atomicdawn.camp.CampMaterials;
import mod.azure.atomicdawn.camp.CampPlans;
import mod.azure.atomicdawn.client.input.ADKeyMappings;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.network.CampSyncPayload;

public final class CampCatalogScreen extends Screen {

    private static final int MIN_PANEL_WIDTH = 300;

    private static final int MAX_PANEL_WIDTH = 480;

    private static final int TAB_HEIGHT = 13;

    private static final int TAB_GAP = 3;

    private static final int ROW = 22;

    private static final int SEARCH_TOP = 18;

    private static final int TABS_TOP = 36;

    @Nullable
    private static CampBuildable.Category lastCategory;

    @Nullable
    private CampBuildable.Category category = lastCategory;

    private String query = "";

    private int scroll;

    @Nullable
    private EditBox search;

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

    public CampCatalogScreen() {
        super(Component.translatable("camp.atomicdawn.catalog.title"));
    }

    private record Tab(
        int x,
        int y,
        int w,
        Component label,
        CampBuildable.@Nullable Category value
    ) {}

    private int panelWidth() {
        return Math.clamp(width - 40, MIN_PANEL_WIDTH, MAX_PANEL_WIDTH);
    }

    private int left() {
        return width / 2 - panelWidth() / 2;
    }

    private List<Tab> tabs() {
        var tabs = new ArrayList<Tab>();
        var startX = left() + 6;
        var maxX = left() + panelWidth() - 6;
        var x = startX;
        var y = top() + TABS_TOP;
        var labels = new ArrayList<Component>();
        var values = new ArrayList<CampBuildable.Category>();
        labels.add(Component.translatable("camp.atomicdawn.category.all"));
        values.add(null);
        for (var entry : categories()) {
            labels.add(entry.label());
            values.add(entry);
        }
        for (var i = 0; i < labels.size(); i++) {
            var w = font.width(labels.get(i)) + 8;
            if (x + w > maxX && x > startX) {
                x = startX;
                y += TAB_HEIGHT + TAB_GAP;
            }
            tabs.add(new Tab(x, y, w, labels.get(i), values.get(i)));
            x += w + TAB_GAP;
        }
        return tabs;
    }

    private int listTop() {
        var tabs = tabs();
        var lastY = tabs.isEmpty() ? top() + TABS_TOP : tabs.getLast().y();
        return lastY + TAB_HEIGHT + 6;
    }

    private int top() {
        return 20;
    }

    private int bottom() {
        return height - 20;
    }

    private int visibleRows() {
        return Math.max(1, (bottom() - listTop() - 6) / ROW);
    }

    @Override
    protected void init() {
        var box = new EditBox(
            font,
            left() + 6,
            top() + SEARCH_TOP,
            panelWidth() - 12,
            12,
            Component.translatable("camp.atomicdawn.catalog.search")
        );
        box.setMaxLength(32);
        box.setValue(query);
        box.setResponder(value -> {
            query = value.toLowerCase(Locale.ROOT).trim();
            scroll = 0;
        });
        search = addRenderableWidget(box);
    }

    private List<Holder.Reference<CampBuildable>> entries() {
        if (minecraft == null || minecraft.level == null) {
            return List.of();
        }
        return CampBuildable.all(minecraft.level.registryAccess())
            .stream()
            .filter(holder -> category == null || holder.value().category() == category)
            .filter(
                holder -> query.isEmpty() || holder.value()
                    .name()
                    .getString()
                    .toLowerCase(Locale.ROOT)
                    .contains(query)
            )
            .toList();
    }

    private List<CampBuildable.Category> categories() {
        if (minecraft == null || minecraft.level == null) {
            return List.of();
        }
        return CampBuildable.all(minecraft.level.registryAccess())
            .stream()
            .map(holder -> holder.value().category())
            .distinct()
            .toList();
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        hits.clear();
        var player = minecraft == null ? null : minecraft.player;
        if (player == null) {
            return;
        }
        var x = left();
        var y = top();
        var panelWidth = panelWidth();
        graphics.fill(x, y, x + panelWidth, bottom(), PipBoyTheme.withAlpha(PipBoyTheme.background(), 0xE0));
        graphics.fill(x, y, x + panelWidth, y + 1, PipBoyTheme.border());
        graphics.fill(x, bottom() - 1, x + panelWidth, bottom(), PipBoyTheme.border());
        graphics.text(font, title, x + 6, y + 6, PipBoyTheme.text(), false);
        var camp = CampWorkshopClient.workshopCamp(player);
        if (camp != null) {
            var budget = Component.translatable(
                "camp.atomicdawn.catalog.budget",
                camp.budgetLimit() - camp.budgetUsed(),
                camp.budgetLimit()
            );
            graphics.text(font, budget, x + panelWidth - 6 - font.width(budget), y + 6, PipBoyTheme.dim(), false);
        }

        for (var tab : tabs()) {
            tab(graphics, tab, tab.value() == category);
        }

        var entries = entries();
        var visible = visibleRows();
        scroll = Math.clamp(scroll, 0, Math.max(0, entries.size() - visible));
        var rowY = listTop();
        if (entries.isEmpty()) {
            var empty = Component.translatable("camp.atomicdawn.catalog.empty");
            graphics.text(font, empty, x + 8, rowY + 4, PipBoyTheme.dim(), false);
            return;
        }
        for (var i = scroll; i < Math.min(entries.size(), scroll + visible); i++) {
            row(graphics, entries.get(i), x + 4, rowY, mouseX, mouseY, camp);
            rowY += ROW;
        }
        if (entries.size() > visible) {
            var trackTop = listTop();
            var trackHeight = visible * ROW;
            var thumb = Math.max(8, trackHeight * visible / entries.size());
            var thumbY = trackTop + (trackHeight - thumb) * scroll / Math.max(1, entries.size() - visible);
            var trackX = x + panelWidth - 3;
            graphics.fill(trackX, trackTop, trackX + 1, trackTop + trackHeight, PipBoyTheme.faint());
            graphics.fill(trackX - 1, thumbY, trackX + 2, thumbY + thumb, PipBoyTheme.dim());
        }
    }

    private void tab(GuiGraphicsExtractor graphics, Tab tab, boolean active) {
        var x = tab.x();
        var y = tab.y();
        graphics.fill(x, y, x + tab.w(), y + TAB_HEIGHT, active ? PipBoyTheme.highlight() : PipBoyTheme.panel());
        graphics.text(font, tab.label(), x + 4, y + 3, active ? PipBoyTheme.text() : PipBoyTheme.dim(), false);
        hits.add(new Hit(x, y, x + tab.w(), y + TAB_HEIGHT, () -> {
            category = tab.value();
            lastCategory = tab.value();
            scroll = 0;
        }));
    }

    private void row(
        GuiGraphicsExtractor graphics,
        Holder.Reference<CampBuildable> holder,
        int x,
        int y,
        int mouseX,
        int mouseY,
        CampSyncPayload.@Nullable CampView camp
    ) {
        var player = minecraft == null ? null : minecraft.player;
        if (player == null) {
            return;
        }
        var definition = holder.value();
        var id = holder.key().identifier();
        var w = panelWidth() - 12;
        var hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + ROW - 2;
        var selected = id.equals(CampWorkshopClient.selected());
        if (selected || hovered) {
            graphics.fill(x, y, x + w, y + ROW - 2, selected ? PipBoyTheme.highlight() : PipBoyTheme.panel());
        }
        graphics.item(definition.icon(), x + 2, y + 2);
        var unlocked = CampPlans.unlocked(player, definition);
        var affordable = CampMaterials.has(player, definition.cost());
        var nameColor = !unlocked ? PipBoyTheme.disabled() : affordable ? PipBoyTheme.text() : PipBoyTheme.dim();
        var cost = Component.translatable("camp.atomicdawn.catalog.cost", definition.budget());
        var limit = definition.limit() > 0 && camp != null
            ? Component.literal(camp.count(id) + "/" + definition.limit())
            : Component.empty();
        var textWidth = w - 30 - Math.max(font.width(cost), font.width(limit)) - 8;
        graphics.text(font, fit(definition.name(), textWidth), x + 22, y + 2, nameColor, false);
        var detail = !unlocked
            ? Component.translatable("camp.atomicdawn.catalog.locked")
            : materials(definition);
        var detailColor = unlocked ? PipBoyTheme.faint() : PipBoyTheme.bad();
        graphics.text(font, fit(detail, textWidth), x + 22, y + 11, detailColor, false);
        graphics.text(font, cost, x + w - font.width(cost) - 4, y + 2, PipBoyTheme.dim(), false);
        graphics.text(font, limit, x + w - font.width(limit) - 4, y + 11, PipBoyTheme.faint(), false);
        hits.add(new Hit(x, y, x + w, y + ROW - 2, () -> {
            CampWorkshopClient.select(id);
            onClose();
        }));
    }

    private Component fit(Component text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        var ellipsis = "...";
        var trimmed = font.plainSubstrByWidth(text.getString(), Math.max(0, maxWidth - font.width(ellipsis)));
        return Component.literal(trimmed + ellipsis).withStyle(text.getStyle());
    }

    private static Component materials(CampBuildable definition) {
        if (definition.cost().isEmpty()) {
            return Component.translatable("camp.atomicdawn.catalog.free");
        }
        var text = Component.empty();
        var first = true;
        for (var ingredient : definition.cost()) {
            var item = ingredient.ingredient().items().findFirst().orElse(null);
            if (item == null) {
                continue;
            }
            if (!first) {
                text.append(", ");
            }
            text.append(ingredient.count() + "x ").append(new ItemStack(item).getHoverName());
            first = false;
        }
        return text;
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll -= (int) Math.signum(scrollY);
        return true;
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        var typing = search != null && search.isFocused();
        if (!typing && ADKeyMappings.WORKSHOP_CATALOG.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
