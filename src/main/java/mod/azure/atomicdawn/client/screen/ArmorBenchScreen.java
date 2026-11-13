package mod.azure.atomicdawn.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mod.azure.atomicdawn.armor.ArmorBenchService;
import mod.azure.atomicdawn.armor.ArmorPiece;
import mod.azure.atomicdawn.armor.ArmorPieceItem;
import mod.azure.atomicdawn.armor.ArmorRecipe;
import mod.azure.atomicdawn.armor.upgrade.ArmorLegendary;
import mod.azure.atomicdawn.armor.upgrade.ArmorMod;
import mod.azure.atomicdawn.armor.upgrade.ArmorModKind;
import mod.azure.atomicdawn.armor.upgrade.ArmorUpgrades;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.crafting.ArmorBenchMenu;
import mod.azure.atomicdawn.crafting.WorkbenchService;
import mod.azure.atomicdawn.network.ArmorBenchActionPayload;
import mod.azure.atomicdawn.powerarmor.PowerArmorService;
import mod.azure.atomicdawn.registry.ADItems;
import mod.azure.atomicdawn.util.RepairKits;

public class ArmorBenchScreen extends AbstractContainerScreen<ArmorBenchMenu> {

    private enum Tab {

        CRAFT,
        MODS,
        LEGENDARY,
        REPAIR,
        SCRAP;

        String translationKey() {
            return "armor.atomicdawn.bench.tab." + name().toLowerCase(Locale.ROOT);
        }
    }

    private static final int PREVIEW_X = 36;

    private static final int PREVIEW_Y = 14;

    private static final int PREVIEW_W = 70;

    private static final int PREVIEW_H = 100;

    private static final int PANEL_X = 116;

    private static final int PANEL_W = 208;

    private static final int CONTENT_Y = 18;

    private static final int LIST_Y = CONTENT_Y + 15;

    private static final int ROW_H = 16;

    private static final int CRAFT_ROWS = 5;

    private static final int LIST_ROWS = 4;

    private static final int ACTION_Y = 127;

    private static final int ACTION_W = 70;

    private static final float ROTATE_STEP = 30F;

    private static Tab tab = Tab.CRAFT;

    private static ArmorRecipe.Category category = ArmorRecipe.Category.VANILLA;

    private static ArmorModKind modKind = ArmorModKind.MATERIAL;

    private static int legendaryStar = 1;

    private static int craftScroll;

    private static int modScroll;

    private static int legendaryScroll;

    private static float previewYaw;

    @Nullable
    private static Identifier selectedRecipe;

    @Nullable
    private static Identifier selectedMod;

    @Nullable
    private static Identifier selectedLegendary;

    private final List<ItemStack> lastItems = new ArrayList<>();

    public ArmorBenchScreen(ArmorBenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ArmorBenchMenu.WIDTH, ArmorBenchMenu.HEIGHT);
        titleLabelX = 8;
        titleLabelY = 5;
        inventoryLabelX = ArmorBenchMenu.INVENTORY_X;
        inventoryLabelY = ArmorBenchMenu.INVENTORY_Y - 11;
    }

    private Inventory playerInventory() {
        return minecraft.player.getInventory();
    }

    private ItemStack work() {
        return menu.work();
    }

    @Nullable
    private ArmorPieceItem workPiece() {
        return work().getItem() instanceof ArmorPieceItem item ? item : null;
    }

    private void send(ArmorBenchActionPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }

    @Override
    protected void init() {
        super.init();
        var tabs = Tab.values();
        var tabW = PANEL_W / tabs.length;
        for (var i = 0; i < tabs.length; i++) {
            var value = tabs[i];
            var button = Button.builder(Component.translatable(value.translationKey()), _ -> {
                tab = value;
                rebuildWidgets();
            }).pos(leftPos + PANEL_X + i * tabW, topPos + 2).size(tabW - 2, 11).build();
            button.active = value != tab;
            addRenderableWidget(PipBoyStyle.tab(button));
        }

        var rotateY = topPos + PREVIEW_Y + PREVIEW_H + 2;
        addRenderableWidget(
            Button.builder(Component.literal("◀"), _ -> previewYaw -= ROTATE_STEP)
                .pos(leftPos + PREVIEW_X, rotateY)
                .size(16, 12)
                .build()
        );
        addRenderableWidget(
            Button.builder(Component.literal("▶"), _ -> previewYaw += ROTATE_STEP)
                .pos(leftPos + PREVIEW_X + PREVIEW_W - 16, rotateY)
                .size(16, 12)
                .build()
        );

        switch (tab) {
            case CRAFT -> initCraft();
            case MODS -> initMods();
            case LEGENDARY -> initLegendary();
            case REPAIR -> initRepair();
            case SCRAP -> initScrap();
        }
    }

    private Button actionButton(Component label, Runnable action, boolean active) {
        var button = Button.builder(label, _ -> action.run())
            .pos(leftPos + PANEL_X + PANEL_W - ACTION_W, topPos + ACTION_Y)
            .size(ACTION_W, 13)
            .build();
        button.active = active;
        return addRenderableWidget(button);
    }

    private List<Holder.Reference<ArmorRecipe>> recipes() {
        return minecraft == null || minecraft.level == null
            ? List.of()
            : ArmorRecipe.all(minecraft.level.registryAccess(), category);
    }

    @Nullable
    private Holder.Reference<ArmorRecipe> selectedRecipe(List<Holder.Reference<ArmorRecipe>> recipes) {
        for (var holder : recipes) {
            if (holder.key().identifier().equals(selectedRecipe)) {
                return holder;
            }
        }
        return null;
    }

    private void initCraft() {
        var categories = ArmorRecipe.Category.values();
        var w = PANEL_W / categories.length;
        for (var i = 0; i < categories.length; i++) {
            var value = categories[i];
            var button = Button.builder(Component.translatable(value.translationKey()), _ -> {
                category = value;
                craftScroll = 0;
                selectedRecipe = null;
                rebuildWidgets();
            }).pos(leftPos + PANEL_X + i * w, topPos + CONTENT_Y).size(w - 1, 12).build();
            button.active = value != category;
            addRenderableWidget(PipBoyStyle.tab(button));
        }
        var recipes = recipes();
        craftScroll = Math.clamp(craftScroll, 0, Math.max(0, recipes.size() - CRAFT_ROWS));
        if (selectedHolderMissing(recipes)) {
            selectedRecipe = recipes.isEmpty() ? null : recipes.getFirst().key().identifier();
        }
        for (var r = 0; r < CRAFT_ROWS && craftScroll + r < recipes.size(); r++) {
            var holder = recipes.get(craftScroll + r);
            var id = holder.key().identifier();
            var button = Button.builder(holder.value().resultStack().getHoverName(), _ -> {
                selectedRecipe = id;
                rebuildWidgets();
            }).pos(leftPos + PANEL_X + 18, topPos + LIST_Y + r * ROW_H).size(PANEL_W - 18, 14).build();
            button.active = !id.equals(selectedRecipe);
            addRenderableWidget(PipBoyStyle.row(button));
        }
        var selected = selectedRecipe(recipes);
        if (selected != null) {
            var id = selected.key().identifier();
            actionButton(
                Component.translatable("armor.atomicdawn.bench.craft_button"),
                () -> send(ArmorBenchActionPayload.of(ArmorBenchActionPayload.Action.FABRICATE, id)),
                canAfford(selected.value().cost())
            );
        }
    }

    private boolean selectedHolderMissing(List<Holder.Reference<ArmorRecipe>> recipes) {
        return selectedRecipe == null || selectedRecipe(recipes) == null;
    }

    private List<Holder.Reference<ArmorMod>> mods() {
        var item = workPiece();
        return item == null ? List.of() : ArmorMod.fitting(minecraft.level.registryAccess(), item, modKind);
    }

    private void initMods() {
        var kinds = ArmorModKind.values();
        var w = PANEL_W / kinds.length;
        for (var i = 0; i < kinds.length; i++) {
            var value = kinds[i];
            var button = Button.builder(Component.translatable(value.translationKey()), _ -> {
                modKind = value;
                modScroll = 0;
                selectedMod = null;
                rebuildWidgets();
            }).pos(leftPos + PANEL_X + i * w, topPos + CONTENT_Y).size(w - 1, 12).build();
            button.active = value != modKind;
            addRenderableWidget(PipBoyStyle.tab(button));
        }
        var mods = mods();
        if (mods.isEmpty()) {
            return;
        }
        var installed = ArmorUpgrades.of(work()).mod(modKind).orElse(null);
        modScroll = Math.clamp(modScroll, 0, Math.max(0, mods.size() - LIST_ROWS));
        if (selectedMod == null || mods.stream().noneMatch(holder -> holder.key().identifier().equals(selectedMod))) {
            selectedMod = installed != null ? installed : mods.getFirst().key().identifier();
        }
        for (var r = 0; r < LIST_ROWS && modScroll + r < mods.size(); r++) {
            var id = mods.get(modScroll + r).key().identifier();
            var label = Component.literal(id.equals(installed) ? "» " : "")
                .append(Component.translatable(ArmorMod.translationKey(id)));
            var button = Button.builder(label, _ -> {
                selectedMod = id;
                rebuildWidgets();
            })
                .pos(leftPos + PANEL_X, topPos + LIST_Y + r * ROW_H)
                .size(PANEL_W, 14)
                .tooltip(Tooltip.create(Component.translatable(ArmorMod.translationKey(id) + ".desc")))
                .build();
            button.active = !id.equals(selectedMod);
            addRenderableWidget(PipBoyStyle.row(button));
        }
        var selected = mods.stream().filter(holder -> holder.key().identifier().equals(selectedMod)).findFirst();
        if (selected.isEmpty()) {
            return;
        }
        var id = selectedMod;
        var isInstalled = id.equals(installed);
        actionButton(
            Component.translatable(
                isInstalled ? "armor.atomicdawn.bench.remove_button" : "armor.atomicdawn.bench.install_button"
            ),
            () -> send(
                ArmorBenchActionPayload.of(
                    isInstalled
                        ? ArmorBenchActionPayload.Action.REMOVE_MOD
                        : ArmorBenchActionPayload.Action.INSTALL_MOD,
                    id
                )
            ),
            isInstalled || canAfford(selected.get().value().cost())
        );
    }

    private void initLegendary() {
        for (var star = 1; star <= 3; star++) {
            var s = star;
            var button = Button.builder(Component.literal("★".repeat(star)), _ -> {
                legendaryStar = s;
                selectedLegendary = null;
                legendaryScroll = 0;
                rebuildWidgets();
            }).pos(leftPos + PANEL_X + (star - 1) * 32, topPos + CONTENT_Y).size(30, 12).build();
            button.active = star != legendaryStar;
            addRenderableWidget(PipBoyStyle.tab(button));
        }
        if (!ArmorPiece.isArmor(work())) {
            return;
        }
        var access = minecraft.level.registryAccess();
        var effects = ArmorLegendary.all(access, legendaryStar);
        if (effects.isEmpty()) {
            return;
        }
        var current = ArmorUpgrades.of(work()).legendary(access, legendaryStar);
        legendaryScroll = Math.clamp(legendaryScroll, 0, Math.max(0, effects.size() - LIST_ROWS));
        if (
            selectedLegendary == null || effects.stream()
                .noneMatch(holder -> holder.key().identifier().equals(selectedLegendary))
        ) {
            selectedLegendary = current != null ? current : effects.getFirst().key().identifier();
        }
        for (var r = 0; r < LIST_ROWS && legendaryScroll + r < effects.size(); r++) {
            var id = effects.get(legendaryScroll + r).key().identifier();
            var label = Component.literal(id.equals(current) ? "» " : "")
                .append(Component.translatable(ArmorLegendary.translationKey(id)));
            var button = Button.builder(label, _ -> {
                selectedLegendary = id;
                rebuildWidgets();
            })
                .pos(leftPos + PANEL_X, topPos + LIST_Y + r * ROW_H)
                .size(PANEL_W, 14)
                .tooltip(Tooltip.create(Component.translatable(ArmorLegendary.translationKey(id) + ".desc")))
                .build();
            button.active = !id.equals(selectedLegendary);
            addRenderableWidget(PipBoyStyle.row(button));
        }
        var id = selectedLegendary;
        var applied = id.equals(current);
        actionButton(
            Component.translatable(
                applied ? "armor.atomicdawn.bench.installed" : "armor.atomicdawn.bench.apply_button"
            ),
            () -> send(ArmorBenchActionPayload.of(ArmorBenchActionPayload.Action.APPLY_LEGENDARY, id)),
            !applied && (minecraft.player.hasInfiniteMaterials() || modules() >= WorkbenchService.legendaryCost(
                legendaryStar
            ))
        );
    }

    private int modules() {
        return PowerArmorService.countItem(minecraft.player, ADItems.LEGENDARY_MODULE.get());
    }

    private void initRepair() {
        var stack = work();
        var hasMaterials = minecraft.player.hasInfiniteMaterials() || ArmorBenchService.repairMaterials(
            minecraft.player,
            stack
        ) > 0;
        var damaged = ArmorPiece.isArmor(stack) && stack.isDamaged();
        actionButton(
            Component.translatable("armor.atomicdawn.bench.repair_button"),
            () -> send(ArmorBenchActionPayload.of(ArmorBenchActionPayload.Action.REPAIR)),
            damaged && hasMaterials
        );
        var kit = Button.builder(
            Component.translatable("armor.atomicdawn.bench.kit_button", RepairKits.count(minecraft.player)),
            _ -> send(ArmorBenchActionPayload.of(ArmorBenchActionPayload.Action.REPAIR_KIT))
        )
            .pos(leftPos + PANEL_X + PANEL_W - ACTION_W * 2 - 4, topPos + ACTION_Y)
            .size(ACTION_W, 13)
            .build();
        kit.active = damaged && RepairKits.available(minecraft.player);
        addRenderableWidget(kit);
    }

    private void initScrap() {
        var stack = work();
        var returns = ArmorBenchService.scrapYield(minecraft.level.registryAccess(), stack);
        MutableComponent tooltip = Component.translatable("armor.atomicdawn.bench.scrap_returns");
        if (returns.isEmpty()) {
            tooltip.append("\n").append(Component.translatable("armor.atomicdawn.bench.scrap_nothing"));
        }
        for (var item : returns) {
            tooltip.append("\n" + item.getCount() + "x ").append(item.getHoverName());
        }
        actionButton(
            Component.translatable("armor.atomicdawn.bench.scrap_button"),
            () -> send(ArmorBenchActionPayload.of(ArmorBenchActionPayload.Action.SCRAP)),
            ArmorPiece.isArmor(stack)
        ).setTooltip(Tooltip.create(tooltip));
    }

    private int countMatching(SizedIngredient ingredient) {
        var total = 0;
        var inventory = playerInventory();
        for (var i = 0; i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (ingredient.ingredient().test(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private boolean canAfford(List<SizedIngredient> cost) {
        if (minecraft.player.hasInfiniteMaterials()) {
            return true;
        }
        for (var ingredient : cost) {
            if (countMatching(ingredient) < ingredient.count()) {
                return false;
            }
        }
        return true;
    }

    private void drawCost(GuiGraphicsExtractor graphics, List<SizedIngredient> cost, int x, int y) {
        graphics.text(font, Component.translatable("armor.atomicdawn.bench.cost"), x, y, PipBoyTheme.dim(), false);
        var infinite = minecraft.player.hasInfiniteMaterials();
        var width = PANEL_W - ACTION_W - 34;
        for (var i = 0; i < Math.min(3, cost.size()); i++) {
            var ingredient = cost.get(i);
            var items = ingredient.ingredient().items().toList();
            var name = items.isEmpty() ? Component.literal("?") : new ItemStack(items.getFirst()).getHoverName();
            var have = countMatching(ingredient);
            var line = Component.literal(have + "/" + ingredient.count() + " ").append(name);
            var color = infinite || have >= ingredient.count() ? PipBoyTheme.text() : PipBoyTheme.bad();
            PipBoyStyle.fitted(graphics, font, line, x + 30, y + i * 9, width, color);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        var current = new ArrayList<ItemStack>();
        for (var slot : menu.slots) {
            current.add(slot.getItem());
        }
        var changed = current.size() != lastItems.size();
        for (var i = 0; !changed && i < current.size(); i++) {
            changed = !ItemStack.matches(current.get(i), lastItems.get(i));
        }
        if (changed) {
            lastItems.clear();
            current.forEach(stack -> lastItems.add(stack.copy()));
            rebuildWidgets();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        var localX = mouseX - leftPos;
        var localY = mouseY - topPos;
        if (scrollY != 0) {
            if (
                localX >= PREVIEW_X && localX < PREVIEW_X + PREVIEW_W && localY >= PREVIEW_Y && localY < PREVIEW_Y
                    + PREVIEW_H
            ) {
                previewYaw += (float) Math.signum(scrollY) * 15F;
                return true;
            }
            if (localX >= PANEL_X && localX < PANEL_X + PANEL_W && localY >= LIST_Y && localY < ACTION_Y) {
                var direction = scrollY > 0 ? -1 : 1;
                switch (tab) {
                    case CRAFT -> craftScroll += direction;
                    case MODS -> modScroll += direction;
                    case LEGENDARY -> legendaryScroll += direction;
                    default -> {
                        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
                    }
                }
                rebuildWidgets();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        var x = leftPos;
        var y = topPos;
        PipBoyStyle.window(graphics, x, y, imageWidth, imageHeight);
        for (var i = 0; i < menu.slots.size(); i++) {
            var slot = menu.slots.get(i);
            var edge = i == ArmorBenchMenu.WORK_SLOT ? PipBoyTheme.text() : PipBoyStyle.slotEdge();
            graphics.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, edge);
            graphics.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, PipBoyStyle.slotBackground());
        }
        PipBoyStyle.crt(graphics, x, y, imageWidth, imageHeight);
        graphics.fill(x + PANEL_X - 4, y + 3, x + PANEL_X - 3, y + 140, PipBoyTheme.faint());
        renderWorkInfo(graphics);
        renderPreview(graphics);
        switch (tab) {
            case CRAFT -> renderCraft(graphics);
            case MODS -> renderMods(graphics);
            case LEGENDARY -> renderLegendary(graphics);
            case REPAIR -> renderRepair(graphics);
            case SCRAP -> renderScrap(graphics);
        }
    }

    @Override
    protected void extractLabels(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        PipBoyStyle.skinButtons(graphics, font, children(), leftPos, topPos);
        graphics.text(font, title, titleLabelX, titleLabelY, PipBoyStyle.text(), false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, PipBoyStyle.dim(), false);
    }

    private void renderWorkInfo(GuiGraphicsExtractor graphics) {
        var x = leftPos + ArmorBenchMenu.WORK_X - 2;
        var y = topPos + ArmorBenchMenu.WORK_Y + 20;
        var stack = work();
        if (stack.isEmpty()) {
            return;
        }
        var piece = ArmorPiece.of(stack);
        if (piece != null) {
            PipBoyStyle.fitted(
                graphics,
                font,
                Component.translatable(piece.translationKey()),
                x,
                y,
                PREVIEW_X - x + leftPos - 2,
                PipBoyTheme.dim()
            );
        }
        if (stack.getItem() instanceof ArmorPieceItem item) {
            PipBoyStyle.fitted(
                graphics,
                font,
                Component.translatable(item.set().weight().translationKey()),
                x,
                y + 10,
                PREVIEW_X - x + leftPos - 2,
                PipBoyTheme.dim()
            );
        }
        var condition = Math.round(ArmorPieceItem.condition(stack) * 100F) + "%";
        PipBoyStyle.fitted(
            graphics,
            font,
            Component.literal(condition),
            x,
            y + 20,
            PREVIEW_X - x + leftPos - 2,
            PipBoyTheme.text()
        );
    }

    private void renderPreview(GuiGraphicsExtractor graphics) {
        var px = leftPos + PREVIEW_X;
        var py = topPos + PREVIEW_Y;
        graphics.fill(px - 1, py - 1, px + PREVIEW_W + 1, py + PREVIEW_H + 1, PipBoyTheme.faint());
        graphics.fill(px, py, px + PREVIEW_W, py + PREVIEW_H, PipBoyTheme.glass());
        var player = minecraft.player;
        var bodyRot = player.yBodyRot;
        var bodyRotO = player.yBodyRotO;
        var yRot = player.getYRot();
        var yRotO = player.yRotO;
        var xRot = player.getXRot();
        var xRotO = player.xRotO;
        var headRot = player.yHeadRot;
        var headRotO = player.yHeadRotO;

        var yaw = 180F + previewYaw;
        player.yBodyRot = yaw;
        player.yBodyRotO = yaw;
        player.setYRot(yaw);
        player.yRotO = yaw;
        player.setXRot(0F);
        player.xRotO = 0F;
        player.yHeadRot = yaw;
        player.yHeadRotO = yaw;

        InventoryScreen.renderEntityInInventoryFollowsAngle(
            graphics,
            px,
            py,
            px + PREVIEW_W,
            py + PREVIEW_H,
            42,
            0.0F,
            0.0F,
            0.0F,
            player
        );

        player.yBodyRot = bodyRot;
        player.yBodyRotO = bodyRotO;
        player.setYRot(yRot);
        player.yRotO = yRotO;
        player.setXRot(xRot);
        player.xRotO = xRotO;
        player.yHeadRot = headRot;
        player.yHeadRotO = headRotO;
    }

    private void renderCraft(GuiGraphicsExtractor graphics) {
        var x = leftPos + PANEL_X;
        var recipes = recipes();
        for (var r = 0; r < CRAFT_ROWS && craftScroll + r < recipes.size(); r++) {
            graphics.item(recipes.get(craftScroll + r).value().resultStack(), x, topPos + LIST_Y + r * ROW_H - 1);
        }
        scrollbar(graphics, recipes.size(), CRAFT_ROWS, craftScroll);
        var selected = selectedRecipe(recipes);
        if (selected == null) {
            wrapped(graphics, Component.translatable("armor.atomicdawn.bench.no_recipes"), x, topPos + LIST_Y + 2, 3);
            return;
        }
        drawCost(graphics, selected.value().cost(), x, topPos + LIST_Y + CRAFT_ROWS * ROW_H + 1);
    }

    private void scrollbar(GuiGraphicsExtractor graphics, int size, int rows, int scroll) {
        if (size <= rows) {
            return;
        }
        var x = leftPos + PANEL_X + PANEL_W;
        var trackY = topPos + LIST_Y;
        var trackH = rows * ROW_H - 2;
        var thumbH = Math.max(6, trackH * rows / size);
        var thumbY = trackY + (trackH - thumbH) * scroll / Math.max(1, size - rows);
        graphics.fill(x + 1, trackY, x + 3, trackY + trackH, PipBoyTheme.faint());
        graphics.fill(x + 1, thumbY, x + 3, thumbY + thumbH, PipBoyTheme.text());
    }

    private boolean renderNoArmor(GuiGraphicsExtractor graphics, int y) {
        if (ArmorPiece.isArmor(work())) {
            return false;
        }
        wrapped(graphics, Component.translatable("armor.atomicdawn.bench.no_armor"), leftPos + PANEL_X, y, 3);
        return true;
    }

    private void renderMods(GuiGraphicsExtractor graphics) {
        var x = leftPos + PANEL_X;
        var y = topPos + LIST_Y + 2;
        if (renderNoArmor(graphics, y)) {
            return;
        }
        if (workPiece() == null) {
            wrapped(graphics, Component.translatable("armor.atomicdawn.bench.vanilla_no_mods"), x, y, 3);
            return;
        }
        var mods = mods();
        if (mods.isEmpty()) {
            wrapped(graphics, Component.translatable("armor.atomicdawn.bench.no_mods"), x, y, 3);
            return;
        }
        scrollbar(graphics, mods.size(), LIST_ROWS, modScroll);
        var selected = mods.stream().filter(holder -> holder.key().identifier().equals(selectedMod)).findFirst();
        if (selected.isPresent() && !ArmorUpgrades.of(work()).hasMod(selectedMod)) {
            drawCost(graphics, selected.get().value().cost(), x, topPos + LIST_Y + LIST_ROWS * ROW_H + 2);
        }
    }

    private void renderLegendary(GuiGraphicsExtractor graphics) {
        var x = leftPos + PANEL_X;
        if (renderNoArmor(graphics, topPos + LIST_Y + 2)) {
            return;
        }
        var access = minecraft.level.registryAccess();
        scrollbar(graphics, ArmorLegendary.all(access, legendaryStar).size(), LIST_ROWS, legendaryScroll);
        var current = ArmorUpgrades.of(work()).legendary(access, legendaryStar);
        var infoY = topPos + LIST_Y + LIST_ROWS * ROW_H + 2;
        var currentName = current == null
            ? Component.translatable("armor.atomicdawn.bench.legendary_none")
            : Component.translatable(ArmorLegendary.translationKey(current));
        PipBoyStyle.fitted(
            graphics,
            font,
            Component.translatable("armor.atomicdawn.bench.legendary_current", currentName),
            x,
            infoY,
            PANEL_W,
            PipBoyTheme.dim()
        );
        var need = WorkbenchService.legendaryCost(legendaryStar);
        var have = modules();
        graphics.text(
            font,
            Component.translatable("armor.atomicdawn.bench.legendary_cost", have, need),
            x,
            infoY + 10,
            minecraft.player.hasInfiniteMaterials() || have >= need ? PipBoyTheme.text() : PipBoyTheme.bad(),
            false
        );
    }

    private void renderRepair(GuiGraphicsExtractor graphics) {
        var x = leftPos + PANEL_X;
        var y = topPos + CONTENT_Y + 2;
        if (renderNoArmor(graphics, y)) {
            return;
        }
        var stack = work();
        graphics.item(stack, x, y);
        PipBoyStyle.fitted(graphics, font, stack.getHoverName(), x + 20, y + 4, PANEL_W - 20, PipBoyTheme.text());
        var condition = ArmorPieceItem.condition(stack);
        var color = condition > 0.6F ? PipBoyTheme.text() : condition > 0.25F ? PipBoyTheme.warn() : PipBoyTheme.bad();
        var barY = y + 22;
        graphics.fill(x, barY, x + PANEL_W, barY + 4, PipBoyTheme.faint());
        graphics.fill(x, barY, x + Math.round(PANEL_W * condition), barY + 4, color);
        var info = minecraft.player.hasInfiniteMaterials()
            ? Component.translatable("armor.atomicdawn.bench.repair_creative")
            : Component.translatable(
                "armor.atomicdawn.bench.repair_materials",
                ArmorBenchService.repairMaterials(minecraft.player, stack)
            );
        wrapped(graphics, info, x, barY + 10, 2);
        wrapped(graphics, Component.translatable("armor.atomicdawn.bench.repair_hint"), x, barY + 30, 3);
    }

    private void renderScrap(GuiGraphicsExtractor graphics) {
        var x = leftPos + PANEL_X;
        var y = topPos + CONTENT_Y + 2;
        if (renderNoArmor(graphics, y)) {
            return;
        }
        var stack = work();
        graphics.item(stack, x, y);
        PipBoyStyle.fitted(graphics, font, stack.getHoverName(), x + 20, y + 4, PANEL_W - 20, PipBoyTheme.text());
        var returns = ArmorBenchService.scrapYield(minecraft.level.registryAccess(), stack);
        var lineY = y + 22;
        graphics.text(
            font,
            Component.translatable("armor.atomicdawn.bench.scrap_returns"),
            x,
            lineY,
            PipBoyTheme.dim(),
            false
        );
        if (returns.isEmpty()) {
            graphics.text(
                font,
                Component.translatable("armor.atomicdawn.bench.scrap_nothing"),
                x,
                lineY + 10,
                PipBoyTheme.dim(),
                false
            );
        }
        for (var i = 0; i < Math.min(4, returns.size()); i++) {
            var item = returns.get(i);
            var line = Component.literal(item.getCount() + "x ").append(item.getHoverName());
            PipBoyStyle.fitted(graphics, font, line, x, lineY + 10 + i * 9, PANEL_W, PipBoyTheme.text());
        }
        if (!ArmorUpgrades.of(stack).isEmpty()) {
            wrapped(graphics, Component.translatable("armor.atomicdawn.bench.scrap_warning"), x, lineY + 50, 2);
        }
    }

    private void wrapped(GuiGraphicsExtractor graphics, Component text, int x, int y, int maxLines) {
        var lines = font.split(text, PANEL_W);
        for (var i = 0; i < Math.min(maxLines, lines.size()); i++) {
            graphics.text(font, lines.get(i), x, y + i * 9, PipBoyTheme.dim(), false);
        }
    }
}
