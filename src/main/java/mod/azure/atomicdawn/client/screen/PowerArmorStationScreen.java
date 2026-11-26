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
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.crafting.WorkbenchService;
import mod.azure.atomicdawn.network.PowerArmorActionPayload;
import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;
import mod.azure.atomicdawn.powerarmor.PowerArmorPieceItem;
import mod.azure.atomicdawn.powerarmor.PowerArmorRecipe;
import mod.azure.atomicdawn.powerarmor.PowerArmorService;
import mod.azure.atomicdawn.powerarmor.PowerArmorSlot;
import mod.azure.atomicdawn.powerarmor.PowerArmorStationMenu;
import mod.azure.atomicdawn.powerarmor.PowerCoreItem;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorLegendary;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorMod;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorUpgrades;
import mod.azure.atomicdawn.registry.ADItems;
import mod.azure.atomicdawn.util.RepairKits;

public class PowerArmorStationScreen extends AbstractContainerScreen<PowerArmorStationMenu> {

    private enum Tab {

        CRAFT,
        REPAIR,
        PAINT,
        MODS,
        LEGENDARY,
        SCRAP;

        String translationKey() {
            return "power_armor.atomicdawn.station.tab." + name().toLowerCase(Locale.ROOT);
        }
    }

    private static final int PREVIEW_X = 90;

    private static final int PREVIEW_Y = 14;

    private static final int PREVIEW_W = 70;

    private static final int PREVIEW_H = 100;

    private static final int PANEL_X = 166;

    private static final int PANEL_W = 158;

    private static final int TABS_X = 110;

    private static final int CONTENT_Y = 18;

    private static final int ROW_H = 16;

    private static final int CRAFT_ROWS = 5;

    private static final int LIST_ROWS = 6;

    private static final float ROTATE_STEP = 30F;

    private static Tab tab = Tab.CRAFT;

    private static float previewYaw;

    private static boolean autoRotate;

    private static int craftScroll;

    private static int scrapScroll;

    @Nullable
    private static Identifier selectedRecipe;

    private static final int MOD_ROWS = 4;

    private static final int LEGENDARY_ROWS = 3;

    private static PowerArmorSlot upgradeSlot = PowerArmorSlot.HELMET;

    private static int modScroll;

    @Nullable
    private static Identifier selectedMod;

    private static int legendaryStar = 1;

    private static int legendaryScroll;

    @Nullable
    private static Identifier selectedLegendary;

    private final List<ItemStack> lastItems = new ArrayList<>();

    public PowerArmorStationScreen(PowerArmorStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, PowerArmorStationMenu.WIDTH, PowerArmorStationMenu.HEIGHT);
        titleLabelX = 8;
        titleLabelY = 5;
        inventoryLabelX = PowerArmorStationMenu.INVENTORY_X;
        inventoryLabelY = PowerArmorStationMenu.INVENTORY_Y - 11;
    }

    private List<Holder.Reference<PowerArmorRecipe>> recipes() {
        return minecraft.level == null
            ? List.of()
            : PowerArmorRecipe.all(minecraft.level.registryAccess());
    }

    private List<Integer> scrapSlots() {
        var slots = new ArrayList<Integer>();
        var inventory = playerInventory();
        for (var i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).getItem() instanceof PowerArmorPieceItem) {
                slots.add(i);
            }
        }
        return slots;
    }

    private Inventory playerInventory() {
        return minecraft.player.getInventory();
    }

    @Override
    protected void init() {
        super.init();
        var tabs = Tab.values();

        var tabW = (PANEL_X + PANEL_W - TABS_X) / tabs.length;
        for (var i = 0; i < tabs.length; i++) {
            var value = tabs[i];
            var button = Button.builder(Component.translatable(value.translationKey()), _ -> {
                tab = value;
                rebuildWidgets();
            }).pos(leftPos + TABS_X + i * tabW, topPos + 2).size(tabW - 2, 11).build();
            button.active = value != tab;
            addRenderableWidget(PipBoyStyle.tab(button));
        }

        var rotateY = topPos + PREVIEW_Y + PREVIEW_H + 2;
        addRenderableWidget(
            Button.builder(Component.literal("◀"), _ -> previewYaw -= ROTATE_STEP)
                .pos(leftPos + PREVIEW_X, rotateY)
                .size(16, 12)
                .tooltip(Tooltip.create(Component.translatable("power_armor.atomicdawn.station.rotate_left")))
                .build()
        );
        addRenderableWidget(
            Button.builder(
                Component.translatable(
                    autoRotate ? "power_armor.atomicdawn.station.auto_on" : "power_armor.atomicdawn.station.auto_off"
                ),
                _ -> {
                    autoRotate = !autoRotate;
                    rebuildWidgets();
                }
            ).pos(leftPos + PREVIEW_X + 18, rotateY).size(34, 12).build()
        );
        addRenderableWidget(
            Button.builder(Component.literal("▶"), _ -> previewYaw += ROTATE_STEP)
                .pos(leftPos + PREVIEW_X + PREVIEW_W - 16, rotateY)
                .size(16, 12)
                .tooltip(Tooltip.create(Component.translatable("power_armor.atomicdawn.station.rotate_right")))
                .build()
        );

        var armor = menu.armor();
        if (armor == null) {
            return;
        }
        switch (tab) {
            case CRAFT -> initCraft(armor);
            case REPAIR -> initRepair(armor);
            case PAINT -> initPaint(armor);
            case MODS -> initMods(armor);
            case LEGENDARY -> initLegendary(armor);
            case SCRAP -> initScrap(armor);
        }
    }

    private void initCraft(PowerArmorEntity armor) {
        var recipes = recipes();
        craftScroll = Math.clamp(craftScroll, 0, Math.max(0, recipes.size() - CRAFT_ROWS));
        if (selectedRecipe == null && !recipes.isEmpty()) {
            selectedRecipe = recipes.getFirst().key().identifier();
        }
        for (var r = 0; r < CRAFT_ROWS && craftScroll + r < recipes.size(); r++) {
            var holder = recipes.get(craftScroll + r);
            var id = holder.key().identifier();
            var button = Button.builder(holder.value().resultStack().getHoverName(), _ -> {
                selectedRecipe = id;
                rebuildWidgets();
            }).pos(leftPos + PANEL_X + 18, topPos + CONTENT_Y + r * ROW_H).size(PANEL_W - 18, 14).build();
            button.active = !id.equals(selectedRecipe);
            PipBoyStyle.row(button);
            addRenderableWidget(button);
        }
        var selected = selectedHolder(recipes);
        if (selected == null) {
            return;
        }
        var label = Component.translatable("power_armor.atomicdawn.station.fabricate_button");
        var button = Button.builder(
            label,
            _ -> ClientPacketDistributor.sendToServer(
                PowerArmorActionPayload.fabricate(armor.getId(), selected.key().identifier())
            )
        ).pos(leftPos + PANEL_X + PANEL_W - 70, topPos + 127).size(70, 13).build();
        button.active = canAfford(selected.value());
        addRenderableWidget(button);
    }

    @Nullable
    private Holder.Reference<PowerArmorRecipe> selectedHolder(List<Holder.Reference<PowerArmorRecipe>> recipes) {
        for (var holder : recipes) {
            if (holder.key().identifier().equals(selectedRecipe)) {
                return holder;
            }
        }
        return null;
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

    private boolean canAfford(PowerArmorRecipe recipe) {
        if (minecraft.player.hasInfiniteMaterials()) {
            return true;
        }
        for (var ingredient : recipe.cost()) {
            if (countMatching(ingredient) < ingredient.count()) {
                return false;
            }
        }
        return true;
    }

    private int repairMaterials() {
        var total = 0;
        var inventory = playerInventory();
        for (var i = 0; i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (stack.is(PowerArmorService.REPAIR_MATERIALS)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private void initRepair(PowerArmorEntity armor) {
        var hasMaterials = minecraft.player.hasInfiniteMaterials() || repairMaterials() > 0;
        var hasKit = RepairKits.available(minecraft.player);
        var kitTooltip = Tooltip.create(
            Component.translatable("power_armor.atomicdawn.station.kit_tooltip", RepairKits.count(minecraft.player))
        );
        for (var slot : PowerArmorSlot.all()) {
            var piece = armor.getPiece(slot);
            var index = slot.ordinal();
            var y = topPos + CONTENT_Y + index * ROW_H;
            var damaged = !piece.isEmpty() && piece.isDamaged();
            var kit = Button.builder(
                Component.translatable("power_armor.atomicdawn.station.kit_button"),
                _ -> send(PowerArmorActionPayload.Action.REPAIR_KIT, index)
            ).pos(leftPos + PANEL_X + PANEL_W - 72, y).size(30, 14).tooltip(kitTooltip).build();
            kit.active = damaged && hasKit;
            addRenderableWidget(kit);
            var button = Button.builder(
                Component.translatable("power_armor.atomicdawn.station.repair_button"),
                _ -> send(PowerArmorActionPayload.Action.REPAIR, index)
            ).pos(leftPos + PANEL_X + PANEL_W - 40, y).size(40, 14).build();
            button.active = damaged && hasMaterials;
            addRenderableWidget(button);
        }
    }

    private void initPaint(PowerArmorEntity armor) {
        var hasDye = !menu.dyeStack().isEmpty();
        for (var slot : PowerArmorSlot.all()) {
            var piece = armor.getPiece(slot);
            var index = slot.ordinal();
            var y = topPos + CONTENT_Y + index * ROW_H;
            var paint = Button.builder(
                Component.translatable("power_armor.atomicdawn.station.paint_button"),
                _ -> send(PowerArmorActionPayload.Action.PAINT, index)
            ).pos(leftPos + PANEL_X + PANEL_W - 86, y).size(42, 14).build();
            paint.active = !piece.isEmpty() && hasDye;
            addRenderableWidget(paint);
            var clear = Button.builder(
                Component.translatable("power_armor.atomicdawn.station.clear_button"),
                _ -> send(PowerArmorActionPayload.Action.CLEAR_PAINT, index)
            ).pos(leftPos + PANEL_X + PANEL_W - 42, y).size(42, 14).build();
            clear.active = !piece.isEmpty() && PowerArmorPieceItem.paint(piece) != PowerArmorPieceItem.NO_PAINT;
            addRenderableWidget(clear);
        }
    }

    private void initScrap(PowerArmorEntity armor) {
        var slots = scrapSlots();
        scrapScroll = Math.clamp(scrapScroll, 0, Math.max(0, slots.size() - LIST_ROWS));
        var access = minecraft.level.registryAccess();
        for (var r = 0; r < LIST_ROWS && scrapScroll + r < slots.size(); r++) {
            var inventorySlot = slots.get(scrapScroll + r);
            var stack = playerInventory().getItem(inventorySlot);
            var returns = PowerArmorService.scrapYield(access, stack);
            MutableComponent tooltip = Component.translatable("power_armor.atomicdawn.station.scrap_returns");
            if (returns.isEmpty()) {
                tooltip.append("\n").append(Component.translatable("power_armor.atomicdawn.station.scrap_nothing"));
            }
            for (var item : returns) {
                tooltip.append("\n" + item.getCount() + "x ").append(item.getHoverName());
            }
            addRenderableWidget(
                Button.builder(
                    Component.translatable("power_armor.atomicdawn.station.scrap_button"),
                    _ -> ClientPacketDistributor.sendToServer(
                        PowerArmorActionPayload.slot(PowerArmorActionPayload.Action.SCRAP, armor.getId(), inventorySlot)
                    )
                )
                    .pos(leftPos + PANEL_X + PANEL_W - 50, topPos + CONTENT_Y + r * ROW_H)
                    .size(50, 14)
                    .tooltip(Tooltip.create(tooltip))
                    .build()
            );
        }
    }

    @Nullable
    private static PowerArmorPieceItem upgradePiece(PowerArmorEntity armor) {
        return armor.getPiece(upgradeSlot).getItem() instanceof PowerArmorPieceItem piece ? piece : null;
    }

    private void initUpgradeSlots() {
        var slots = PowerArmorSlot.all();
        var w = PANEL_W / slots.length;
        for (var i = 0; i < slots.length; i++) {
            var slot = slots[i];
            var button = Button.builder(
                Component.translatable("power_armor.atomicdawn.station.slot_short." + slot.getSerializedName()),
                _ -> {
                    upgradeSlot = slot;
                    selectedMod = null;
                    selectedLegendary = null;
                    modScroll = 0;
                    legendaryScroll = 0;
                    rebuildWidgets();
                }
            )
                .pos(leftPos + PANEL_X + i * w, topPos + CONTENT_Y)
                .size(w - 1, 12)
                .tooltip(Tooltip.create(Component.translatable(slot.translationKey())))
                .build();
            button.active = slot != upgradeSlot;
            addRenderableWidget(PipBoyStyle.tab(button));
        }
    }

    private List<Holder.Reference<PowerArmorMod>> modsFor(@Nullable PowerArmorPieceItem piece) {
        if (piece == null) {
            return List.of();
        }
        return PowerArmorMod.all(minecraft.level.registryAccess())
            .stream()
            .filter(holder -> holder.value().fits(piece))
            .toList();
    }

    private static Component modName(Identifier id) {
        return Component.translatable(PowerArmorMod.translationKey(id));
    }

    private void initMods(PowerArmorEntity armor) {
        initUpgradeSlots();
        var piece = upgradePiece(armor);
        var mods = modsFor(piece);
        if (mods.isEmpty()) {
            return;
        }
        var installed = PowerArmorUpgrades.of(armor.getPiece(upgradeSlot));
        modScroll = Math.clamp(modScroll, 0, Math.max(0, mods.size() - MOD_ROWS));
        if (selectedMod == null || mods.stream().noneMatch(holder -> holder.key().identifier().equals(selectedMod))) {
            selectedMod = mods.getFirst().key().identifier();
        }
        for (var r = 0; r < MOD_ROWS && modScroll + r < mods.size(); r++) {
            var holder = mods.get(modScroll + r);
            var id = holder.key().identifier();
            var isInstalled = id.equals(installed.mods().get(holder.value().group()));
            var label = Component.literal(isInstalled ? "» " : "").append(modName(id));
            var tooltip = Component.translatable(PowerArmorMod.translationKey(id) + ".desc")
                .append("\n")
                .append(
                    Component.translatable(
                        "power_armor.atomicdawn.station.mod_group",
                        Component.translatable("power_armor.atomicdawn.mod_group." + holder.value().group())
                    )
                );
            var button = Button.builder(label, _ -> {
                selectedMod = id;
                rebuildWidgets();
            })
                .pos(leftPos + PANEL_X, topPos + CONTENT_Y + 16 + r * ROW_H)
                .size(PANEL_W, 14)
                .tooltip(Tooltip.create(tooltip))
                .build();
            button.active = !id.equals(selectedMod);
            addRenderableWidget(PipBoyStyle.row(button));
        }
        var selected = mods.stream().filter(holder -> holder.key().identifier().equals(selectedMod)).findFirst();
        if (selected.isEmpty()) {
            return;
        }
        var mod = selected.get();
        var isInstalled = selectedMod.equals(installed.mods().get(mod.value().group()));
        var label = Component.translatable(
            "power_armor.atomicdawn.station." + (isInstalled ? "remove_button" : "install_button")
        );
        var id = selectedMod;
        var slotIndex = upgradeSlot.ordinal();
        var action = isInstalled
            ? PowerArmorActionPayload.Action.REMOVE_MOD
            : PowerArmorActionPayload.Action.INSTALL_MOD;
        var button = Button.builder(
            label,
            _ -> ClientPacketDistributor.sendToServer(
                PowerArmorActionPayload.upgrade(action, armor.getId(), slotIndex, id)
            )
        ).pos(leftPos + PANEL_X + PANEL_W - 70, topPos + 127).size(70, 13).build();
        var limit = AtomicDawn.CONFIG.common.equipment.powerArmor.maintenance.POWER_ARMOR_MODS_PER_PIECE;
        var full = !installed.mods().containsKey(mod.value().group()) && installed.mods().size() >= limit && limit > 1;
        button.active = isInstalled || (!full && canAfford(mod.value().cost()));
        addRenderableWidget(button);
    }

    private void renderMods(GuiGraphicsExtractor graphics, PowerArmorEntity armor) {
        var x = leftPos + PANEL_X;
        var piece = upgradePiece(armor);
        var listY = topPos + CONTENT_Y + 16;
        if (piece == null) {
            wrapped(graphics, Component.translatable("power_armor.atomicdawn.station.no_piece"), x, listY + 2, 3);
            return;
        }
        var mods = modsFor(piece);
        if (mods.isEmpty()) {
            wrapped(graphics, Component.translatable("power_armor.atomicdawn.station.no_mods"), x, listY + 2, 3);
            return;
        }
        var selected = mods.stream().filter(holder -> holder.key().identifier().equals(selectedMod)).findFirst();
        selected.ifPresent(
            powerArmorModReference -> drawCost(
                graphics,
                powerArmorModReference.value().cost(),
                x,
                listY + MOD_ROWS * ROW_H + 2
            )
        );
        var installed = PowerArmorUpgrades.of(armor.getPiece(upgradeSlot)).mods().size();
        graphics.text(
            font,
            Component.translatable(
                "power_armor.atomicdawn.station.mod_count",
                installed,
                AtomicDawn.CONFIG.common.equipment.powerArmor.maintenance.POWER_ARMOR_MODS_PER_PIECE
            ),
            x,
            topPos + 129,
            PipBoyTheme.dim(),
            false
        );
    }

    private void initLegendary(PowerArmorEntity armor) {
        initUpgradeSlots();
        for (var star = 1; star <= 3; star++) {
            var s = star;
            var button = Button.builder(Component.literal("★".repeat(star)), _ -> {
                legendaryStar = s;
                selectedLegendary = null;
                legendaryScroll = 0;
                rebuildWidgets();
            }).pos(leftPos + PANEL_X + (star - 1) * 32, topPos + CONTENT_Y + 15).size(30, 12).build();
            button.active = star != legendaryStar;
            addRenderableWidget(PipBoyStyle.tab(button));
        }
        if (upgradePiece(armor) == null) {
            return;
        }
        var access = minecraft.level.registryAccess();
        var effects = PowerArmorLegendary.all(access, legendaryStar);
        if (effects.isEmpty()) {
            return;
        }
        var current = PowerArmorUpgrades.of(armor.getPiece(upgradeSlot)).legendary(access, legendaryStar);
        legendaryScroll = Math.clamp(legendaryScroll, 0, Math.max(0, effects.size() - LEGENDARY_ROWS));
        if (
            selectedLegendary == null || effects.stream()
                .noneMatch(holder -> holder.key().identifier().equals(selectedLegendary))
        ) {
            selectedLegendary = effects.getFirst().key().identifier();
        }
        for (var r = 0; r < LEGENDARY_ROWS && legendaryScroll + r < effects.size(); r++) {
            var id = effects.get(legendaryScroll + r).key().identifier();
            var label = Component.literal(id.equals(current) ? "» " : "")
                .append(Component.translatable(PowerArmorLegendary.translationKey(id)));
            var button = Button.builder(label, _ -> {
                selectedLegendary = id;
                rebuildWidgets();
            })
                .pos(leftPos + PANEL_X, topPos + CONTENT_Y + 31 + r * ROW_H)
                .size(PANEL_W, 14)
                .tooltip(Tooltip.create(Component.translatable(PowerArmorLegendary.translationKey(id) + ".desc")))
                .build();
            button.active = !id.equals(selectedLegendary);
            addRenderableWidget(PipBoyStyle.row(button));
        }
        var id = selectedLegendary;
        var applied = id.equals(current);
        var slotIndex = upgradeSlot.ordinal();
        var button = Button.builder(
            Component.translatable(
                applied ? "power_armor.atomicdawn.station.installed" : "power_armor.atomicdawn.station.apply_button"
            ),
            _ -> ClientPacketDistributor.sendToServer(
                PowerArmorActionPayload.upgrade(
                    PowerArmorActionPayload.Action.APPLY_LEGENDARY,
                    armor.getId(),
                    slotIndex,
                    id
                )
            )
        ).pos(leftPos + PANEL_X + PANEL_W - 70, topPos + 127).size(70, 13).build();
        button.active = !applied && (minecraft.player.hasInfiniteMaterials() || PowerArmorService.countItem(
            minecraft.player,
            ADItems.LEGENDARY_MODULE.get()
        ) >= WorkbenchService.legendaryCost(legendaryStar));
        addRenderableWidget(button);
    }

    private void renderLegendary(GuiGraphicsExtractor graphics, PowerArmorEntity armor) {
        var x = leftPos + PANEL_X;
        var listY = topPos + CONTENT_Y + 31;
        if (upgradePiece(armor) == null) {
            wrapped(graphics, Component.translatable("power_armor.atomicdawn.station.no_piece"), x, listY + 2, 3);
            return;
        }
        var access = minecraft.level.registryAccess();
        var current = PowerArmorUpgrades.of(armor.getPiece(upgradeSlot)).legendary(access, legendaryStar);
        var infoY = listY + LEGENDARY_ROWS * ROW_H + 2;
        var currentName = current == null
            ? Component.translatable("power_armor.atomicdawn.station.legendary_none")
            : Component.translatable(PowerArmorLegendary.translationKey(current));
        graphics.text(
            font,
            Component.translatable("power_armor.atomicdawn.station.legendary_current", currentName),
            x,
            infoY,
            PipBoyTheme.dim(),
            false
        );
        var need = WorkbenchService.legendaryCost(legendaryStar);
        var have = PowerArmorService.countItem(minecraft.player, ADItems.LEGENDARY_MODULE.get());
        var infinite = minecraft.player.hasInfiniteMaterials();
        graphics.text(
            font,
            Component.translatable("power_armor.atomicdawn.station.legendary_cost", have, need),
            x,
            infoY + 10,
            infinite || have >= need ? PipBoyTheme.text() : PipBoyTheme.bad(),
            false
        );
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
        var title = Component.translatable("power_armor.atomicdawn.station.cost");
        graphics.text(font, title, x, y, PipBoyTheme.dim(), false);
        var infinite = minecraft.player.hasInfiniteMaterials();
        for (var i = 0; i < Math.min(3, cost.size()); i++) {
            var ingredient = cost.get(i);
            var items = ingredient.ingredient().items().toList();
            var name = items.isEmpty() ? Component.literal("?") : new ItemStack(items.getFirst()).getHoverName();
            var have = countMatching(ingredient);
            var line = Component.literal(have + "/" + ingredient.count() + " ").append(name);
            var color = infinite || have >= ingredient.count() ? PipBoyTheme.text() : PipBoyTheme.bad();
            graphics.text(font, line, x + 30, y + i * 9, color, false);
        }
    }

    private void send(PowerArmorActionPayload.Action action, int slot) {
        var armor = menu.armor();
        if (armor != null) {
            ClientPacketDistributor.sendToServer(PowerArmorActionPayload.slot(action, armor.getId(), slot));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        var current = new ArrayList<ItemStack>();
        for (var slot : menu.slots) {
            current.add(slot.getItem());
        }
        var armor = menu.armor();
        if (armor != null) {
            for (var i = 0; i < PowerArmorEntity.SLOT_COUNT; i++) {
                current.add(armor.getSlotItem(i));
            }
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
            if (localX >= PANEL_X && localX < PANEL_X + PANEL_W && localY >= CONTENT_Y && localY < 140) {
                var direction = scrollY > 0 ? -1 : 1;
                if (tab == Tab.CRAFT) {
                    craftScroll += direction;
                } else if (tab == Tab.SCRAP) {
                    scrapScroll += direction;
                } else if (tab == Tab.MODS) {
                    modScroll += direction;
                } else if (tab == Tab.LEGENDARY) {
                    legendaryScroll += direction;
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
            var edge = i <= PowerArmorStationMenu.DYE_SLOT ? PipBoyTheme.text() : PipBoyStyle.slotEdge();
            graphics.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, edge);
            graphics.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, PipBoyStyle.slotBackground());
        }
        PipBoyStyle.crt(graphics, x, y, imageWidth, imageHeight);
        graphics.text(
            font,
            Component.translatable("power_armor.atomicdawn.station.core"),
            x + PowerArmorStationMenu.CORE_X - 2,
            y + PowerArmorStationMenu.CORE_Y - 10,
            PipBoyTheme.dim(),
            false
        );
        graphics.text(
            font,
            Component.translatable("power_armor.atomicdawn.station.dye"),
            x + PowerArmorStationMenu.DYE_X + 1,
            y + PowerArmorStationMenu.DYE_Y - 10,
            PipBoyTheme.dim(),
            false
        );
        graphics.fill(x + PANEL_X - 4, y + 3, x + PANEL_X - 3, y + 140, PipBoyTheme.faint());

        var armor = menu.armor();
        if (armor == null) {
            return;
        }
        var core = armor.getCore();
        if (!core.isEmpty()) {
            var fraction = PowerCoreItem.fraction(core);
            var cx = x + PowerArmorStationMenu.CORE_X;
            var cy = y + PowerArmorStationMenu.CORE_Y + 19;
            graphics.fill(cx, cy, cx + 16, cy + 3, PipBoyTheme.faint());
            graphics.fill(cx, cy, cx + Math.round(16 * fraction), cy + 3, PipBoyTheme.text());
        }
        renderPreview(graphics, armor);
        switch (tab) {
            case CRAFT -> renderCraft(graphics);
            case REPAIR -> renderRepair(graphics, armor);
            case PAINT -> renderPaint(graphics, armor);
            case MODS -> renderMods(graphics, armor);
            case LEGENDARY -> renderLegendary(graphics, armor);
            case SCRAP -> renderScrap(graphics);
        }
    }

    @Override
    protected void extractLabels(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        PipBoyStyle.skinButtons(graphics, font, children(), leftPos, topPos);
        graphics.text(font, title, titleLabelX, titleLabelY, PipBoyStyle.text(), false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, PipBoyStyle.dim(), false);
    }

    private float currentYaw() {
        var spin = autoRotate ? (System.currentTimeMillis() % 7200L) / 20F : 0F;
        return previewYaw + spin;
    }

    private void renderPreview(GuiGraphicsExtractor graphics, PowerArmorEntity armor) {
        var px = leftPos + PREVIEW_X;
        var py = topPos + PREVIEW_Y;
        graphics.fill(px - 1, py - 1, px + PREVIEW_W + 1, py + PREVIEW_H + 1, PipBoyTheme.faint());
        graphics.fill(px, py, px + PREVIEW_W, py + PREVIEW_H, PipBoyTheme.glass());

        var bodyRot = armor.yBodyRot;
        var bodyRotO = armor.yBodyRotO;
        var yRot = armor.getYRot();
        var yRotO = armor.yRotO;
        var xRot = armor.getXRot();
        var xRotO = armor.xRotO;
        var headRot = armor.yHeadRot;
        var headRotO = armor.yHeadRotO;

        var yaw = 180F + currentYaw();
        armor.yBodyRot = yaw;
        armor.yBodyRotO = yaw;
        armor.setYRot(yaw);
        armor.yRotO = yaw;
        armor.setXRot(0F);
        armor.xRotO = 0F;
        armor.yHeadRot = yaw;
        armor.yHeadRotO = yaw;

        var pose = new Quaternionf().rotateZ((float) Math.PI);
        var tilt = new Quaternionf().rotateX(-0.12F);
        pose.mul(tilt);
        var scale = 36F / armor.getScale();
        InventoryScreen.renderEntityInInventoryFollowsAngle(
            graphics,
            px,
            py,
            px + PREVIEW_W,
            py + PREVIEW_H,
            (int) scale,
            0.0F,
            0.0F,
            0.0F,
            armor
        );

        armor.yBodyRot = bodyRot;
        armor.yBodyRotO = bodyRotO;
        armor.setYRot(yRot);
        armor.yRotO = yRotO;
        armor.setXRot(xRot);
        armor.xRotO = xRotO;
        armor.yHeadRot = headRot;
        armor.yHeadRotO = headRotO;
    }

    private void renderCraft(GuiGraphicsExtractor graphics) {
        var x = leftPos + PANEL_X;
        var y = topPos;
        var recipes = recipes();
        for (var r = 0; r < CRAFT_ROWS && craftScroll + r < recipes.size(); r++) {
            graphics.item(recipes.get(craftScroll + r).value().resultStack(), x, y + CONTENT_Y + r * ROW_H - 1);
        }
        if (recipes.size() > CRAFT_ROWS) {
            var trackY = y + CONTENT_Y;
            var trackH = CRAFT_ROWS * ROW_H - 2;
            var thumbH = Math.max(6, trackH * CRAFT_ROWS / recipes.size());
            var thumbY = trackY + (trackH - thumbH) * craftScroll / Math.max(1, recipes.size() - CRAFT_ROWS);
            graphics.fill(x + PANEL_W + 1, trackY, x + PANEL_W + 3, trackY + trackH, PipBoyTheme.faint());
            graphics.fill(x + PANEL_W + 1, thumbY, x + PANEL_W + 3, thumbY + thumbH, PipBoyTheme.text());
        }
        var selected = selectedHolder(recipes);
        if (selected == null) {
            wrapped(
                graphics,
                Component.translatable("power_armor.atomicdawn.station.no_recipes"),
                x,
                y + CONTENT_Y + 2,
                3
            );
            return;
        }
        var costY = y + CONTENT_Y + CRAFT_ROWS * ROW_H + 1;
        graphics.text(
            font,
            Component.translatable("power_armor.atomicdawn.station.cost"),
            x,
            costY,
            PipBoyTheme.dim(),
            false
        );
        var infinite = minecraft.player.hasInfiniteMaterials();
        var cost = selected.value().cost();
        for (var i = 0; i < Math.min(3, cost.size()); i++) {
            var ingredient = cost.get(i);
            var items = ingredient.ingredient().items().toList();
            var name = items.isEmpty() ? Component.literal("?") : new ItemStack(items.getFirst()).getHoverName();
            var have = countMatching(ingredient);
            var line = Component.literal(have + "/" + ingredient.count() + " ").append(name);
            var color = infinite || have >= ingredient.count() ? PipBoyTheme.text() : PipBoyTheme.bad();
            graphics.text(font, line, x + 30, costY + i * 9, color, false);
        }
    }

    private void renderRepair(GuiGraphicsExtractor graphics, PowerArmorEntity armor) {
        var x = leftPos + PANEL_X;
        for (var slot : PowerArmorSlot.all()) {
            var rowY = topPos + CONTENT_Y + slot.ordinal() * ROW_H;
            var piece = armor.getPiece(slot);
            graphics.text(
                font,
                Component.translatable(slot.translationKey()),
                x,
                rowY + 3,
                piece.isEmpty() ? PipBoyTheme.faint() : PipBoyTheme.text(),
                false
            );
            if (piece.isEmpty()) {
                continue;
            }
            var condition = PowerArmorPieceItem.condition(piece);
            var color = PowerArmorPieceItem.isBroken(piece)
                ? PipBoyTheme.bad()
                : condition > 0.6F ? PipBoyTheme.text() : condition > 0.25F ? PipBoyTheme.warn() : PipBoyTheme.bad();
            var barX = x + 54;
            graphics.fill(barX, rowY + 5, barX + 30, rowY + 9, PipBoyTheme.faint());
            graphics.fill(barX, rowY + 5, barX + Math.round(30 * condition), rowY + 9, color);
        }
        var materials = minecraft.player.hasInfiniteMaterials()
            ? Component.translatable("power_armor.atomicdawn.station.repair_creative")
            : Component.translatable("power_armor.atomicdawn.station.repair_materials", repairMaterials());
        wrapped(graphics, materials, x, topPos + CONTENT_Y + LIST_ROWS * ROW_H + 2, 2);
    }

    private void renderPaint(GuiGraphicsExtractor graphics, PowerArmorEntity armor) {
        var x = leftPos + PANEL_X;
        for (var slot : PowerArmorSlot.all()) {
            var rowY = topPos + CONTENT_Y + slot.ordinal() * ROW_H;
            var piece = armor.getPiece(slot);
            graphics.text(
                font,
                Component.translatable(slot.translationKey()),
                x,
                rowY + 3,
                piece.isEmpty() ? PipBoyTheme.faint() : PipBoyTheme.text(),
                false
            );
            if (piece.isEmpty()) {
                continue;
            }
            var paint = PowerArmorPieceItem.paint(piece);
            var swatchX = x + 56;
            graphics.fill(swatchX, rowY + 2, swatchX + 12, rowY + 12, PipBoyTheme.faint());
            if (paint != PowerArmorPieceItem.NO_PAINT) {
                graphics.fill(swatchX + 1, rowY + 3, swatchX + 11, rowY + 11, 0xFF000000 | paint);
            }
        }
        var dye = menu.dyeStack();
        var info = dye.isEmpty()
            ? Component.translatable("power_armor.atomicdawn.station.paint_hint")
            : Component.translatable("power_armor.atomicdawn.station.paint_using", dye.getHoverName());
        wrapped(graphics, info, x, topPos + CONTENT_Y + LIST_ROWS * ROW_H + 2, 2);
    }

    private void renderScrap(GuiGraphicsExtractor graphics) {
        var x = leftPos + PANEL_X;
        var slots = scrapSlots();
        if (slots.isEmpty()) {
            wrapped(
                graphics,
                Component.translatable("power_armor.atomicdawn.station.scrap_empty"),
                x,
                topPos + CONTENT_Y + 2,
                3
            );
        }
        for (var r = 0; r < LIST_ROWS && scrapScroll + r < slots.size(); r++) {
            var stack = playerInventory().getItem(slots.get(scrapScroll + r));
            var rowY = topPos + CONTENT_Y + r * ROW_H;
            graphics.item(stack, x, rowY - 1);
            var condition = " " + Math.round(PowerArmorPieceItem.condition(stack) * 100F) + "%";
            var available = PANEL_W - 50 - 24 - font.width(condition);
            var name = font.plainSubstrByWidth(stack.getHoverName().getString(), Math.max(10, available));
            graphics.text(font, Component.literal(name + condition), x + 20, rowY + 3, PipBoyTheme.text(), false);
        }
        wrapped(
            graphics,
            Component.translatable("power_armor.atomicdawn.station.scrap_hint"),
            x,
            topPos + CONTENT_Y + LIST_ROWS * ROW_H + 2,
            2
        );
    }

    private void wrapped(GuiGraphicsExtractor graphics, Component text, int x, int y, int maxLines) {
        var lines = font.split(text, PANEL_W);
        for (var i = 0; i < Math.min(maxLines, lines.size()); i++) {
            graphics.text(font, lines.get(i), x, y + i * 9, PipBoyTheme.dim(), false);
        }
    }
}
