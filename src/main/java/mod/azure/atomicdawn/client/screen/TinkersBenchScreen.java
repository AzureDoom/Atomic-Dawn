package mod.azure.atomicdawn.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

import mod.azure.atomicdawn.client.helper.ADContainerScreen;
import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.crafting.TinkerRecipe;
import mod.azure.atomicdawn.crafting.TinkerService;
import mod.azure.atomicdawn.crafting.TinkersBenchMenu;
import mod.azure.atomicdawn.network.TinkerCraftPayload;
import mod.azure.atomicdawn.weapon.modification.WeaponModding;

public class TinkersBenchScreen extends ADContainerScreen<TinkersBenchMenu> {

    private static final int LIST_X = 8;

    private static final int LIST_Y = 20;

    private static final int LIST_W = 178;

    private static final int LIST_ROWS = 6;

    private static final int ROW_H = 15;

    private static final int RIGHT_X = 198;

    private static final int PREVIEW_SIZE = 32;

    private static final int INFO_Y = 56;

    private static final int COST_Y = 78;

    private static final int MAX_COST_LINES = 3;

    private static final int ACTION_Y = 119;

    private static final int BATCH = 5;

    private static final int STATUS_Y = TinkersBenchMenu.INVENTORY_Y - 11;

    private TinkerRecipe.Category category = TinkerRecipe.Category.AMMO;

    @Nullable
    private ResourceKey<TinkerRecipe> selected;

    private int page;

    private int costScroll;

    @Nullable
    private Component status;

    private int statusColor = PipBoyStyle.text();

    private boolean lastAffordable;

    public TinkersBenchScreen(TinkersBenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TinkersBenchMenu.IMAGE_WIDTH, TinkersBenchMenu.IMAGE_HEIGHT);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = TinkersBenchMenu.INVENTORY_X;
        this.inventoryLabelY = TinkersBenchMenu.INVENTORY_Y - 11;
    }

    private List<Holder.Reference<TinkerRecipe>> recipes() {
        return minecraft.level == null ? List.of() : TinkerService.recipes(minecraft.level, category);
    }

    private @Nullable TinkerRecipe selectedRecipe() {
        if (selected == null) {
            return null;
        }
        for (var holder : recipes()) {
            if (holder.key().equals(selected)) {
                return holder.value();
            }
        }
        return null;
    }

    @Override
    protected void init() {
        super.init();

        var categories = TinkerRecipe.Category.values();
        var tabsX = Math.max(RIGHT_X - 112, titleLabelX + font.width(title) + 6);
        var tabW = (imageWidth - tabsX - 4) / categories.length;
        for (var i = 0; i < categories.length; i++) {
            var c = categories[i];
            var button = Button.builder(Component.translatable(c.translationKey()), ignored -> switchCategory(c))
                .pos(leftPos + tabsX + i * tabW, topPos + 3)
                .size(tabW - 2, 12)
                .build();
            button.active = c != category;
            addRenderableWidget(PipBoyStyle.tab(button));
        }

        var recipes = recipes();
        if ((selected == null || selectedRecipe() == null) && !recipes.isEmpty()) {
            selected = recipes.getFirst().key();
            costScroll = 0;
        }
        var pages = Math.max(1, (recipes.size() + LIST_ROWS - 1) / LIST_ROWS);
        page = Math.clamp(page, 0, pages - 1);
        for (var row = 0; row < LIST_ROWS; row++) {
            var index = page * LIST_ROWS + row;
            if (index >= recipes.size()) {
                break;
            }
            var holder = recipes.get(index);
            var recipe = holder.value();
            var label = Component.literal(recipe.count() + "× ").append(recipe.resultStack().getHoverName());
            var button = Button.builder(label, ignored -> {
                selected = holder.key();
                costScroll = 0;
                rebuildWidgets();
            })
                .pos(leftPos + LIST_X + 18, topPos + LIST_Y + row * ROW_H)
                .size(LIST_W - 18, ROW_H - 2)
                .build();
            button.active = !holder.key().equals(selected);
            PipBoyStyle.row(button);
            addRenderableWidget(button);
        }
        if (pages > 1) {
            var pagerY = topPos + LIST_Y + LIST_ROWS * ROW_H;
            var prev = Button.builder(Component.literal("<"), ignored -> changePage(-1))
                .pos(leftPos + LIST_X, pagerY)
                .size(20, 12)
                .build();
            prev.active = page > 0;
            addRenderableWidget(prev);
            var next = Button.builder(Component.literal(">"), ignored -> changePage(1))
                .pos(leftPos + LIST_X + LIST_W - 20, pagerY)
                .size(20, 12)
                .build();
            next.active = page < pages - 1;
            addRenderableWidget(next);
        }

        var recipe = selectedRecipe();
        if (recipe != null && selected != null) {
            var affordable = minecraft.player.hasInfiniteMaterials()
                || WeaponModding.hasMaterials(minecraft.player, recipe.cost());
            var width = imageWidth - RIGHT_X - 8;
            var one = Button.builder(
                Component.translatable("screen.atomicdawn.tinkers_bench.craft"),
                ignored -> craft(1)
            )
                .pos(leftPos + RIGHT_X, topPos + ACTION_Y)
                .size(width / 2 - 1, 14)
                .build();
            one.active = affordable;
            addRenderableWidget(one);
            var many = Button.builder(
                Component.translatable("screen.atomicdawn.tinkers_bench.craft_many", BATCH),
                ignored -> craft(BATCH)
            )
                .pos(leftPos + RIGHT_X + width / 2 + 1, topPos + ACTION_Y)
                .size(width / 2 - 1, 14)
                .build();
            many.active = affordable;
            addRenderableWidget(many);
        }
    }

    private void switchCategory(TinkerRecipe.Category newCategory) {
        category = newCategory;
        selected = null;
        page = 0;
        status = null;
        rebuildWidgets();
    }

    private void changePage(int delta) {
        page += delta;
        rebuildWidgets();
    }

    private void craft(int times) {
        if (selected != null) {
            status = null;
            ClientPacketDistributor.sendToServer(
                new TinkerCraftPayload(menu.containerId, selected.identifier(), times)
            );
        }
    }

    public void showResult(WeaponModding.Result result) {
        status = Component.translatable(
            "screen.atomicdawn.workbench.result." + result.name().toLowerCase(Locale.ROOT)
        );
        statusColor = result == WeaponModding.Result.CRAFTED ? PipBoyStyle.good() : PipBoyStyle.bad();
        rebuildWidgets();
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        var recipe = selectedRecipe();
        var affordable = recipe != null && minecraft.player != null
            && (minecraft.player.hasInfiniteMaterials() || WeaponModding.hasMaterials(minecraft.player, recipe.cost()));
        if (affordable != lastAffordable) {
            lastAffordable = affordable;
            rebuildWidgets();
        }
    }

    @Override
    protected void drawBackground(ADGuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.drawBackground(graphics, mouseX, mouseY, partialTick);
        var x = leftPos;
        var y = topPos;
        var panelHeight = TinkersBenchMenu.INVENTORY_Y - 30;

        PipBoyStyle.window(graphics, x, y, imageWidth, imageHeight);
        panel(graphics, x + 4, y + 17, LIST_W + 8, panelHeight);
        panel(graphics, x + RIGHT_X - 4, y + 17, imageWidth - RIGHT_X, panelHeight);

        for (var slot : menu.slots) {
            PipBoyStyle.slot(graphics, x + slot.x, y + slot.y);
        }
        PipBoyStyle.crt(graphics, x, y, imageWidth, imageHeight);

        var recipes = recipes();
        for (var row = 0; row < LIST_ROWS; row++) {
            var index = page * LIST_ROWS + row;
            if (index >= recipes.size()) {
                break;
            }
            graphics.item(recipes.get(index).value().resultStack(), x + LIST_X, y + LIST_Y + row * ROW_H - 2);
        }
        if (recipes.isEmpty()) {
            graphics.text(
                font,
                Component.translatable("screen.atomicdawn.tinkers_bench.no_recipes"),
                x + LIST_X + 2,
                y + LIST_Y + 4,
                PipBoyStyle.dim(),
                false
            );
        }

        if (status != null) {
            graphics.text(font, status, x + RIGHT_X, y + STATUS_Y, statusColor, false);
        }

        var recipe = selectedRecipe();
        if (recipe == null) {
            return;
        }
        var stack = recipe.resultStack();
        var previewX = x + RIGHT_X + (imageWidth - RIGHT_X - 8 - PREVIEW_SIZE) / 2;
        graphics.push();
        graphics.translate(previewX, y + 20);
        graphics.scale(PREVIEW_SIZE / 16F, PREVIEW_SIZE / 16F);
        graphics.item(stack, 0, 0);
        graphics.pop();

        graphics.text(
            font,
            Component.translatable("screen.atomicdawn.tinkers_bench.owned", countOwned(stack)),
            x + RIGHT_X,
            y + INFO_Y,
            PipBoyStyle.dim(),
            false
        );
        graphics.text(
            font,
            Component.translatable("screen.atomicdawn.tinkers_bench.cost"),
            x + RIGHT_X,
            y + COST_Y - 11,
            PipBoyStyle.text(),
            false
        );
        drawCost(graphics, x, y, recipe);
    }

    private void drawCost(ADGuiGraphics graphics, int x, int y, TinkerRecipe recipe) {
        var costY = y + COST_Y;
        if (recipe.cost().isEmpty() || minecraft.player.hasInfiniteMaterials()) {
            graphics.text(
                font,
                Component.translatable("screen.atomicdawn.workbench.free"),
                x + RIGHT_X,
                costY,
                PipBoyStyle.dim(),
                false
            );
            return;
        }
        var cost = recipe.cost();

        costScroll = Math.clamp(costScroll, 0, Math.max(0, cost.size() - MAX_COST_LINES));
        var width = costWidth() - 8;
        for (var i = costScroll; i < Math.min(cost.size(), costScroll + MAX_COST_LINES); i++) {
            var entry = cost.get(i);
            var icon = entry.ingredient().items().findFirst().map(ItemStack::new).orElse(ItemStack.EMPTY);
            var have = WeaponModding.count(minecraft.player, entry);
            var line = Component.literal(entry.count() + "× ")
                .append(icon.getHoverName())
                .append(Component.literal(" (" + have + ")"));
            var color = have >= entry.count() ? PipBoyStyle.text() : PipBoyStyle.bad();
            PipBoyStyle.fitted(graphics, font, line, x + RIGHT_X, costY, width, color);
            costY += 10;
        }
        var arrowX = x + RIGHT_X + costWidth() - 6;
        if (costScroll > 0) {
            graphics.text(font, Component.literal("▲"), arrowX, y + COST_Y, PipBoyStyle.dim(), false);
        }
        if (costScroll + MAX_COST_LINES < cost.size()) {
            graphics.text(
                font,
                Component.literal("▼"),
                arrowX,
                y + COST_Y + (MAX_COST_LINES - 1) * 10,
                PipBoyStyle.dim(),
                false
            );
        }
    }

    private int costWidth() {
        return imageWidth - RIGHT_X - 8;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        var localX = mouseX - leftPos - RIGHT_X;
        var localY = mouseY - topPos - COST_Y;
        var recipe = selectedRecipe();
        if (
            scrollY != 0 && recipe != null && localX >= 0 && localX < costWidth() && localY >= -11
                && localY < MAX_COST_LINES * 10 && recipe.cost().size() > MAX_COST_LINES
        ) {
            costScroll = Math.clamp(
                costScroll + (scrollY > 0 ? -1 : 1),
                0,
                recipe.cost().size() - MAX_COST_LINES
            );
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private int countOwned(ItemStack stack) {
        var inventory = minecraft.player.getInventory();
        var total = 0;
        for (var i = 0; i < inventory.getContainerSize(); i++) {
            var item = inventory.getItem(i);
            if (ItemStack.isSameItem(item, stack)) {
                total += item.getCount();
            }
        }
        return total;
    }

    @Override
    protected void drawLabels(ADGuiGraphics graphics, int mouseX, int mouseY) {
        PipBoyStyle.skinButtons(graphics, font, children(), leftPos, topPos);
        graphics.text(font, title, titleLabelX, titleLabelY, PipBoyStyle.text(), false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, PipBoyStyle.dim(), false);
    }

    private static void panel(ADGuiGraphics graphics, int x, int y, int width, int height) {
        PipBoyStyle.panel(graphics, x, y, width, height);
    }
}
