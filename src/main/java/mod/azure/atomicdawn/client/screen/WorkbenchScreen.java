package mod.azure.atomicdawn.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import mod.azure.atomicdawn.client.helper.ADContainerScreen;
import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.crafting.WorkbenchMenu;
import mod.azure.atomicdawn.crafting.WorkbenchService;
import mod.azure.atomicdawn.item.WeaponItem;
import mod.azure.atomicdawn.network.WorkbenchActionPayload;
import mod.azure.atomicdawn.network.WorkbenchInstallPayload;
import mod.azure.atomicdawn.progression.ModUnlocks;
import mod.azure.atomicdawn.registry.ADDataComponents;
import mod.azure.atomicdawn.registry.ADItems;
import mod.azure.atomicdawn.util.RepairKits;
import mod.azure.atomicdawn.weapon.definition.WeaponDefinition;
import mod.azure.atomicdawn.weapon.instance.Weapons;
import mod.azure.atomicdawn.weapon.legendary.LegendaryEffect;
import mod.azure.atomicdawn.weapon.modification.ModNames;
import mod.azure.atomicdawn.weapon.modification.ModSlot;
import mod.azure.atomicdawn.weapon.modification.ModificationResolver;
import mod.azure.atomicdawn.weapon.modification.StatModifier;
import mod.azure.atomicdawn.weapon.modification.WeaponModding;
import mod.azure.atomicdawn.weapon.modification.WeaponModification;
import mod.azure.atomicdawn.weapon.modification.WeaponMods;
import mod.azure.atomicdawn.weapon.modification.WeaponStat;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class WorkbenchScreen extends ADContainerScreen<WorkbenchMenu> {

    private static final int LEFT_X = 8;

    private static final int LEFT_LIST_Y = 40;

    private static final int LEFT_W = 72;

    private static final int ROW_H = 12;

    private static final int MID_X = 86;

    private static final int MID_Y = 20;

    private static final int MID_W = 104;

    private static final int MID_ROWS = 5;

    private static final int MID_ROW_H = 15;

    private static final int COST_Y = 110;

    private static final int RIGHT_X = 198;

    private static final int PREVIEW_SIZE = 32;

    private static final int STATS_Y = 55;

    private static final int STATUS_Y = WorkbenchMenu.INVENTORY_Y - 11;

    private enum Tab {

        MODIFY,
        REPAIR,
        SCRAP,
        CRAFT,
        LEGENDARY;

        Component label() {
            return Component.translatable("screen.atomicdawn.workbench.tab." + name().toLowerCase(Locale.ROOT));
        }
    }

    private record StatRow(
        WeaponStat stat,
        String key,
        boolean higherIsBetter
    ) {}

    private static final List<StatRow> STATS = List.of(
        new StatRow(WeaponStat.DAMAGE, "damage", true),
        new StatRow(WeaponStat.FIRE_INTERVAL, "fire_rate", true),
        new StatRow(WeaponStat.RANGE, "range", true),
        new StatRow(WeaponStat.BASE_SPREAD, "accuracy", true),
        new StatRow(WeaponStat.RECOIL_VERTICAL, "recoil", false),
        new StatRow(WeaponStat.MAGAZINE_SIZE, "magazine", true),
        new StatRow(WeaponStat.RELOAD_TICKS, "reload", false)
    );

    private Tab tab = Tab.MODIFY;

    @Nullable
    private ModSlot selectedSlot;

    @Nullable
    private Optional<ResourceKey<WeaponModification>> selectedMod;

    @Nullable
    private Item selectedCraft;

    private int selectedStar = 1;

    @Nullable
    private ResourceKey<LegendaryEffect> selectedEffect;

    private boolean scrapArmed;

    private int page;

    private ItemStack lastWeapon = ItemStack.EMPTY;

    @Nullable
    private Component status;

    private int statusColor = PipBoyStyle.text();

    public WorkbenchScreen(WorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WorkbenchMenu.IMAGE_WIDTH, WorkbenchMenu.IMAGE_HEIGHT);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = WorkbenchMenu.INVENTORY_X;
        this.inventoryLabelY = WorkbenchMenu.INVENTORY_Y - 11;
    }

    private ItemStack weapon() {
        return menu.weapon();
    }

    @Nullable
    private WeaponItem weaponItem() {
        return weapon().getItem() instanceof WeaponItem item ? item : null;
    }

    @Nullable
    private WeaponDefinition base() {
        return minecraft.level == null ? null : Weapons.baseDefinition(minecraft.level, weapon());
    }

    @Nullable
    private WeaponDefinition effective() {
        return minecraft.level == null ? null : Weapons.definition(minecraft.level, weapon());
    }

    private List<ModSlot> slots() {
        var base = base();
        return base == null ? List.of() : base.modifications().slots();
    }

    private List<Optional<Holder.Reference<WeaponModification>>> candidates() {
        var item = weaponItem();
        var base = base();
        if (item == null || base == null || selectedSlot == null || minecraft.level == null) {
            return List.of();
        }
        var list = new ArrayList<Optional<Holder.Reference<WeaponModification>>>();
        if (!base.modifications().defaults().containsKey(selectedSlot)) {
            list.add(Optional.empty());
        }
        ModificationResolver.compatible(minecraft.level.registryAccess(), item.definitionKey(), selectedSlot)
            .forEach(holder -> list.add(Optional.of(holder)));
        return list;
    }

    private Optional<ResourceKey<WeaponModification>> installedIn(ModSlot slot) {
        var base = base();
        return base == null ? Optional.empty() : ModificationResolver.installed(weapon(), base).get(slot);
    }

    private List<Item> craftables() {
        if (minecraft.level == null) {
            return List.of();
        }
        var level = minecraft.level;
        return ADItems.weapons()
            .filter(item -> {
                var definition = Weapons.baseDefinition(level, new ItemStack(item));
                return definition != null && definition.crafting().isPresent();
            })
            .toList();
    }

    private List<Holder.Reference<LegendaryEffect>> effectsForStar() {
        var base = base();
        if (base == null || minecraft.level == null) {
            return List.of();
        }
        return ModificationResolver.legendaryEffectsForStar(minecraft.level.registryAccess(), selectedStar)
            .stream()
            .filter(effect -> effect.value().appliesTo(base.category()))
            .toList();
    }

    @Override
    protected void init() {
        super.init();

        var tabs = Tab.values();
        var tabsX = Math.max(MID_X, titleLabelX + font.width(title) + 6);
        var tabW = (imageWidth - tabsX - 4) / tabs.length;
        for (var i = 0; i < tabs.length; i++) {
            var t = tabs[i];
            var button = Button.builder(t.label(), ignored -> switchTab(t))
                .pos(leftPos + tabsX + i * tabW, topPos + 3)
                .size(tabW - 2, 12)
                .build();
            button.active = t != tab;
            PipBoyStyle.tab(button);
            addRenderableWidget(button);
        }

        switch (tab) {
            case MODIFY -> initModify();
            case REPAIR -> initRepair();
            case SCRAP -> initScrap();
            case CRAFT -> initCraft();
            case LEGENDARY -> initLegendary();
        }
    }

    private void initModify() {
        var slots = slots();
        if (selectedSlot == null || !slots.contains(selectedSlot)) {
            selectedSlot = slots.isEmpty() ? null : slots.getFirst();
            selectedMod = Optional.empty();
            page = 0;
        }
        if (selectedSlot != null && selectedMod == null) {
            selectedMod = installedIn(selectedSlot);
        }

        for (var i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            var button = Button.builder(Component.translatable(slot.translationKey()), ignored -> selectSlot(slot))
                .pos(leftPos + LEFT_X, topPos + LEFT_LIST_Y + i * ROW_H)
                .size(LEFT_W, ROW_H - 1)
                .build();
            button.active = slot != selectedSlot;
            PipBoyStyle.row(button);
            addRenderableWidget(button);
        }

        var candidates = candidates();
        var installed = selectedSlot == null
            ? Optional.<ResourceKey<WeaponModification>>empty()
            : installedIn(selectedSlot);
        pagedList(candidates.size(), (row, index) -> {
            var candidate = candidates.get(index);
            var key = candidate.map(Holder.Reference::key);
            var locked = candidate.isPresent() && !ModUnlocks.isUnlocked(minecraft.player, candidate.get());
            MutableComponent label = key.map(ModNames::mod)
                .orElse(Component.translatable("screen.atomicdawn.workbench.none"));
            if (key.equals(installed)) {
                label = Component.literal("» ").append(label);
            } else if (locked) {
                label = label.withStyle(ChatFormatting.DARK_GRAY);
            }
            var tooltip = describe(candidate.map(Holder::value));
            if (locked) {
                tooltip = Component.translatable("screen.atomicdawn.workbench.locked").append("\n").append(tooltip);
            }
            var button = Button.builder(label, ignored -> selectMod(key))
                .pos(leftPos + MID_X, topPos + MID_Y + row * MID_ROW_H)
                .size(MID_W, MID_ROW_H - 2)
                .tooltip(Tooltip.create(tooltip))
                .build();
            button.active = !key.equals(selectedMod);
            PipBoyStyle.row(button);
            addRenderableWidget(button);
        });

        if (selectedSlot != null && selectedMod != null) {
            var isInstalled = selectedMod.equals(installed);
            var locked = selectedMod.flatMap(this::lookupMod)
                .map(holder -> !ModUnlocks.isUnlocked(minecraft.player, holder))
                .orElse(false);
            var affordable = isInstalled || WeaponModding.hasMaterials(minecraft.player, modCost(selectedMod));
            var label = Component.translatable(
                isInstalled
                    ? "screen.atomicdawn.workbench.installed"
                    : locked ? "screen.atomicdawn.workbench.locked_short" : "screen.atomicdawn.workbench.install"
            );
            actionButton(label, !isInstalled && !locked && affordable, this::install);
        }
    }

    private void initRepair() {
        var definition = effective();
        if (definition == null || !definition.condition().enabled()) {
            return;
        }
        var condition = Weapons.condition(weapon(), definition);
        var cost = WorkbenchService.repairCost(definition, condition);
        var damaged = condition.missing() > 0;
        var repair = Button.builder(
            Component.translatable("screen.atomicdawn.workbench.repair"),
            ignored -> action(WorkbenchActionPayload.Action.REPAIR, Optional.empty())
        )
            .pos(leftPos + MID_X, topPos + MID_Y + 28)
            .size(MID_W, 14)
            .build();
        repair.active = damaged && WeaponModding.hasMaterials(minecraft.player, cost);
        addRenderableWidget(repair);

        var kits = RepairKits.count(minecraft.player);
        var kit = Button.builder(
            Component.translatable("screen.atomicdawn.workbench.repair_kit", kits),
            ignored -> action(WorkbenchActionPayload.Action.REPAIR_WITH_KIT, Optional.empty())
        )
            .pos(leftPos + MID_X, topPos + MID_Y + 46)
            .size(MID_W, 14)
            .build();
        kit.active = damaged && RepairKits.available(minecraft.player);
        addRenderableWidget(kit);
    }

    private void initScrap() {
        if (weaponItem() == null) {
            return;
        }
        var label = Component.translatable(
            scrapArmed ? "screen.atomicdawn.workbench.scrap_confirm" : "screen.atomicdawn.workbench.scrap"
        );
        actionButton(label, true, () -> {
            if (scrapArmed) {
                scrapArmed = false;
                action(WorkbenchActionPayload.Action.SCRAP, Optional.empty());
            } else {
                scrapArmed = true;
                rebuildWidgets();
            }
        });
    }

    private void initCraft() {
        var craftables = craftables();
        if (selectedCraft == null && !craftables.isEmpty()) {
            selectedCraft = craftables.getFirst();
        }
        pagedList(craftables.size(), (row, index) -> {
            var item = craftables.get(index);
            var button = Button.builder(item.getName(new ItemStack(item)), ignored -> {
                selectedCraft = item;
                rebuildWidgets();
            })
                .pos(leftPos + MID_X, topPos + MID_Y + row * MID_ROW_H)
                .size(MID_W, MID_ROW_H - 2)
                .build();
            button.active = item != selectedCraft;
            PipBoyStyle.row(button);
            addRenderableWidget(button);
        });
        if (selectedCraft != null) {
            var item = (WeaponItem) selectedCraft;
            var cost = craftCost(selectedCraft);
            actionButton(
                Component.translatable("screen.atomicdawn.workbench.craft"),
                WeaponModding.hasMaterials(minecraft.player, cost),
                () -> action(WorkbenchActionPayload.Action.CRAFT, Optional.of(item.definitionKey().identifier()))
            );
        }
    }

    private void initLegendary() {
        if (weaponItem() == null) {
            return;
        }
        for (var star = 1; star <= 3; star++) {
            var s = star;
            var button = Button.builder(Component.literal("★".repeat(star)), ignored -> {
                selectedStar = s;
                selectedEffect = null;
                page = 0;
                rebuildWidgets();
            })
                .pos(leftPos + LEFT_X, topPos + LEFT_LIST_Y + (star - 1) * ROW_H)
                .size(LEFT_W, ROW_H - 1)
                .build();
            button.active = star != selectedStar;
            PipBoyStyle.row(button);
            addRenderableWidget(button);
        }

        var effects = effectsForStar();
        var current = ModificationResolver.legendaryKeys(weapon()).effects();
        pagedList(effects.size(), (row, index) -> {
            var effect = effects.get(index);
            MutableComponent label = ModNames.legendary(effect.key());
            if (current.contains(effect.key())) {
                label = Component.literal("» ").append(label);
            }
            var button = Button.builder(label, ignored -> {
                selectedEffect = effect.key();
                rebuildWidgets();
            })
                .pos(leftPos + MID_X, topPos + MID_Y + row * MID_ROW_H)
                .size(MID_W, MID_ROW_H - 2)
                .tooltip(Tooltip.create(ModNames.legendaryDescription(effect.key())))
                .build();
            button.active = !effect.key().equals(selectedEffect);
            PipBoyStyle.row(button);
            addRenderableWidget(button);
        });

        if (selectedEffect != null) {
            var key = selectedEffect;
            var applied = current.contains(key);
            var modules = WorkbenchService.legendaryCost(selectedStar);
            var affordable = minecraft.player.hasInfiniteMaterials() || countItem(
                ADItems.LEGENDARY_MODULE.get()
            ) >= modules;
            actionButton(
                Component.translatable(
                    applied ? "screen.atomicdawn.workbench.installed" : "screen.atomicdawn.workbench.apply"
                ),
                !applied && affordable,
                () -> action(WorkbenchActionPayload.Action.APPLY_LEGENDARY, Optional.of(key.identifier()))
            );
        }
    }

    private void pagedList(int size, RowBuilder rowBuilder) {
        var pages = Math.max(1, (size + MID_ROWS - 1) / MID_ROWS);
        page = Math.clamp(page, 0, pages - 1);
        for (var row = 0; row < MID_ROWS; row++) {
            var index = page * MID_ROWS + row;
            if (index >= size) {
                break;
            }
            rowBuilder.build(row, index);
        }
        if (pages > 1) {
            var pagerY = topPos + MID_Y + MID_ROWS * MID_ROW_H;
            var prev = Button.builder(Component.literal("<"), ignored -> changePage(-1))
                .pos(leftPos + MID_X, pagerY)
                .size(20, 12)
                .build();
            prev.active = page > 0;
            addRenderableWidget(prev);
            var next = Button.builder(Component.literal(">"), ignored -> changePage(1))
                .pos(leftPos + MID_X + MID_W - 20, pagerY)
                .size(20, 12)
                .build();
            next.active = page < pages - 1;
            addRenderableWidget(next);
        }
    }

    @FunctionalInterface
    private interface RowBuilder {

        void build(int row, int index);
    }

    private void actionButton(Component label, boolean active, Runnable onPress) {
        var button = Button.builder(label, ignored -> onPress.run())
            .pos(leftPos + 198, topPos + 119)
            .size(imageWidth - 198 - 8, 14)
            .build();
        button.active = active;
        addRenderableWidget(button);
    }

    private void switchTab(Tab newTab) {
        tab = newTab;
        page = 0;
        scrapArmed = false;
        status = null;
        rebuildWidgets();
    }

    private void selectSlot(ModSlot slot) {
        selectedSlot = slot;
        selectedMod = installedIn(slot);
        page = 0;
        rebuildWidgets();
    }

    private void selectMod(Optional<ResourceKey<WeaponModification>> mod) {
        selectedMod = mod;
        rebuildWidgets();
    }

    private void changePage(int delta) {
        page += delta;
        rebuildWidgets();
    }

    private void install() {
        if (selectedSlot != null && selectedMod != null) {
            status = null;
            ClientPacketDistributor.sendToServer(
                new WorkbenchInstallPayload(menu.containerId, selectedSlot, selectedMod)
            );
        }
    }

    private void action(WorkbenchActionPayload.Action action, Optional<Identifier> target) {
        status = null;
        ClientPacketDistributor.sendToServer(new WorkbenchActionPayload(menu.containerId, action, target));
    }

    public void showResult(WeaponModding.Result result) {
        status = Component.translatable(
            "screen.atomicdawn.workbench.result." + result.name().toLowerCase(Locale.ROOT)
        );
        statusColor = switch (result) {
            case INSTALLED, REPAIRED, SCRAPPED, CRAFTED, LEGENDARY_APPLIED -> PipBoyStyle.good();
            default -> PipBoyStyle.bad();
        };
        rebuildWidgets();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        var current = weapon();
        if (!ItemStack.matches(current, lastWeapon)) {
            if (!ItemStack.isSameItem(current, lastWeapon)) {
                selectedSlot = null;
                selectedMod = null;
                selectedEffect = null;
                scrapArmed = false;
            }
            lastWeapon = current.copy();
            rebuildWidgets();
        }
    }

    @Override
    protected void drawBackground(ADGuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.drawBackground(graphics, mouseX, mouseY, partialTick);
        var x = leftPos;
        var y = topPos;
        var panelHeight = WorkbenchMenu.INVENTORY_Y - 30;

        PipBoyStyle.window(graphics, x, y, imageWidth, imageHeight);
        panel(graphics, x + 4, y + 17, MID_X - 8, panelHeight);
        panel(graphics, x + MID_X - 2, y + 17, MID_W + 4, panelHeight);
        panel(graphics, x + RIGHT_X - 4, y + 17, imageWidth - RIGHT_X, panelHeight);

        for (var slot : menu.slots) {
            PipBoyStyle.slot(graphics, x + slot.x, y + slot.y);
        }
        PipBoyStyle.crt(graphics, x, y, imageWidth, imageHeight);

        if (status != null) {
            graphics.text(font, status, x + RIGHT_X, y + STATUS_Y, statusColor, false);
        }

        if (tab == Tab.CRAFT) {
            extractCraft(graphics, x, y);
            return;
        }

        var item = weaponItem();
        var base = base();
        if (item == null || base == null || minecraft.level == null) {
            hint(graphics, x, y, "screen.atomicdawn.workbench.insert_weapon");
            return;
        }

        switch (tab) {
            case MODIFY -> extractModify(graphics, x, y, item, base);
            case REPAIR -> extractRepair(graphics, x, y);
            case SCRAP -> extractScrap(graphics, x, y, base);
            case LEGENDARY -> extractLegendary(graphics, x, y);
            default -> {}
        }
    }

    private void extractModify(ADGuiGraphics graphics, int x, int y, WeaponItem item, WeaponDefinition base) {
        if (base.modifications().slots().isEmpty()) {
            hint(graphics, x, y, "screen.atomicdawn.workbench.no_slots");
        }
        var access = minecraft.level.registryAccess();
        var legendary = ModificationResolver.legendary(access, weapon());
        var installedMods = ModificationResolver.installed(weapon(), base);
        var current = ModificationResolver.effective(access, item.definitionKey(), base, installedMods, legendary);
        WeaponMods previewMods = selectedSlot != null && selectedMod != null
            ? installedMods.with(selectedSlot, selectedMod)
            : installedMods;
        var preview = ModificationResolver.effective(access, item.definitionKey(), base, previewMods, legendary);

        var previewStack = weapon().copy();
        previewStack.set(ADDataComponents.WEAPON_MODS.get(), previewMods);
        drawPreview(graphics, x, y, previewStack);
        drawStats(graphics, x, y, current, preview);

        if (selectedMod != null && selectedSlot != null && !selectedMod.equals(installedMods.get(selectedSlot))) {
            drawCost(graphics, x, y, modCost(selectedMod));
        }
    }

    private void extractRepair(ADGuiGraphics graphics, int x, int y) {
        var definition = effective();
        drawPreview(graphics, x, y, weapon());
        if (definition == null || !definition.condition().enabled()) {
            hint(graphics, x, y, "screen.atomicdawn.workbench.indestructible");
            return;
        }
        var condition = Weapons.condition(weapon(), definition);
        graphics.text(
            font,
            Component.translatable("tooltip.atomicdawn.condition", condition.current(), condition.max()),
            x + MID_X + 2,
            y + MID_Y + 4,
            condition.isBroken() ? PipBoyStyle.bad() : PipBoyStyle.text(),
            false
        );
        var barWidth = MID_W - 4;
        graphics.fill(
            x + MID_X + 2,
            y + MID_Y + 16,
            x + MID_X + 2 + barWidth,
            y + MID_Y + 19,
            PipBoyStyle.slotBackground()
        );
        graphics.fill(
            x + MID_X + 2,
            y + MID_Y + 16,
            x + MID_X + 2 + Math.round(barWidth * condition.fraction()),
            y + MID_Y + 19,
            condition.fraction() < 0.25F ? PipBoyStyle.bad() : PipBoyStyle.good()
        );
        drawCost(graphics, x, y, WorkbenchService.repairCost(definition, condition));
    }

    private void extractScrap(ADGuiGraphics graphics, int x, int y, WeaponDefinition base) {
        drawPreview(graphics, x, y, weapon());
        var lineY = y + MID_Y + 4;
        var textX = x + MID_X + 2;
        var availableWidth = MID_W - 4;
        var bottom = y + WorkbenchMenu.INVENTORY_Y - 14;

        lineY = drawWrappedText(
            graphics,
            Component.translatable("screen.atomicdawn.workbench.scrap_yields"),
            textX,
            lineY,
            availableWidth,
            bottom,
            PipBoyStyle.text()
        ) + 3;

        if (base.crafting().isPresent()) {
            var crafting = base.crafting().get();
            var scrapEntries = crafting.scrap();
            for (var i = 0; i < Math.min(scrapEntries.size(), 4); i++) {
                var entry = scrapEntries.get(i);
                if (lineY + 42 > bottom) {
                    break;
                }
                var stack = new ItemStack(entry.item(), entry.count());
                graphics.push();
                graphics.translate(textX, lineY);
                graphics.scale(0.5F, 0.5F);
                graphics.item(stack, 0, 0);
                graphics.pop();

                drawFittedText(
                    graphics,
                    Component.literal(entry.count() + "× ").append(stack.getHoverName()),
                    textX + 11,
                    lineY,
                    availableWidth - 11,
                    PipBoyStyle.dim()
                );
                lineY += 10;
            }
            lineY += 2;
            lineY = drawWrappedText(
                graphics,
                Component.translatable("screen.atomicdawn.workbench.scrap_xp", crafting.scrapExperience()),
                textX,
                lineY,
                availableWidth,
                bottom,
                PipBoyStyle.dim()
            ) + 2;
        }

        var stars = ModificationResolver.legendaryKeys(weapon()).effects().size();
        if (stars > 0) {
            lineY = drawWrappedText(
                graphics,
                Component.translatable("screen.atomicdawn.workbench.scrap_modules", stars),
                textX,
                lineY,
                availableWidth,
                bottom,
                PipBoyStyle.highlight()
            ) + 2;
        }
        drawWrappedText(
            graphics,
            Component.translatable("screen.atomicdawn.workbench.scrap_learn"),
            textX,
            lineY,
            availableWidth,
            bottom,
            PipBoyStyle.dim()
        );
    }

    private void extractCraft(ADGuiGraphics graphics, int x, int y) {
        if (selectedCraft == null) {
            hint(graphics, x, y, "screen.atomicdawn.workbench.nothing_craftable");
            return;
        }
        drawPreview(graphics, x, y, new ItemStack(selectedCraft));
        drawCraftCost(graphics, x, y, craftCost(selectedCraft));
    }

    private void extractLegendary(ADGuiGraphics graphics, int x, int y) {
        drawPreview(graphics, x, y, weapon());
        var lineY = y + 55;
        for (var effect : ModificationResolver.legendary(minecraft.level.registryAccess(), weapon())) {
            drawFittedText(
                graphics,
                Component.literal("★".repeat(effect.value().star()) + " ").append(ModNames.legendary(effect.key())),
                x + RIGHT_X + 2,
                lineY,
                imageWidth - RIGHT_X - 10,
                PipBoyStyle.highlight()
            );
            lineY += 10;
        }
        if (selectedEffect != null) {
            var modules = WorkbenchService.legendaryCost(selectedStar);
            var have = countItem(ADItems.LEGENDARY_MODULE.get());
            var free = minecraft.player.hasInfiniteMaterials() || modules == 0;
            var icon = new ItemStack(ADItems.LEGENDARY_MODULE.get());
            var line = free
                ? Component.translatable("screen.atomicdawn.workbench.free")
                : Component.literal(modules + "× ").append(icon.getHoverName()).append(" (" + have + ")");
            drawWrappedText(
                graphics,
                line,
                x + MID_X + 2,
                y + COST_Y - 2,
                MID_W - 4,
                y + WorkbenchMenu.INVENTORY_Y - 14,
                free || have >= modules ? PipBoyStyle.text() : PipBoyStyle.bad()
            );
        }
    }

    private void drawPreview(ADGuiGraphics graphics, int x, int y, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        var previewX = x + RIGHT_X + (imageWidth - RIGHT_X - 8 - PREVIEW_SIZE) / 2;
        var previewY = y + 20;
        graphics.push();
        graphics.translate(previewX, previewY);
        graphics.scale(PREVIEW_SIZE / 16F, PREVIEW_SIZE / 16F);
        graphics.item(stack, 0, 0);
        graphics.pop();
    }

    private void drawStats(
        ADGuiGraphics graphics,
        int x,
        int y,
        WeaponDefinition current,
        WeaponDefinition preview
    ) {
        var statY = y + STATS_Y;
        for (var row : STATS) {
            if (current.isMelee() && row.stat() != WeaponStat.DAMAGE && row.stat() != WeaponStat.FIRE_INTERVAL) {
                continue;
            }
            if (
                !current.usesAmmo() && (row.stat() == WeaponStat.MAGAZINE_SIZE || row.stat() == WeaponStat.RELOAD_TICKS)
            ) {
                continue;
            }
            var now = displayValue(row.stat(), current);
            var next = displayValue(row.stat(), preview);
            graphics.text(
                font,
                Component.translatable("screen.atomicdawn.workbench.stat." + row.key()),
                x + RIGHT_X,
                statY,
                PipBoyStyle.dim(),
                false
            );
            var valueText = format(now);
            var valueX = x + imageWidth - 10 - font.width(valueText);
            var delta = next - now;
            if (Math.abs(delta) > 0.005F) {
                var better = (delta > 0) == row.higherIsBetter();
                var deltaText = (delta > 0 ? "+" : "") + format(delta);
                graphics.text(
                    font,
                    Component.literal(deltaText),
                    valueX - 4 - font.width(deltaText),
                    statY,
                    better ? PipBoyStyle.good() : PipBoyStyle.bad(),
                    false
                );
            }
            graphics.text(font, Component.literal(valueText), valueX, statY, PipBoyStyle.text(), false);
            statY += 9;
        }
    }

    private void drawFittedText(
        ADGuiGraphics graphics,
        Component message,
        int x,
        int y,
        int maxWidth,
        int color
    ) {
        var width = font.width(message);
        if (width <= maxWidth) {
            graphics.text(font, message, x, y, color, false);
            return;
        }
        var scale = Math.min(1F, (float) maxWidth / width);
        graphics.push();
        graphics.translate(x, y);
        graphics.scale(scale, scale);
        graphics.text(font, message, 0, 0, color, false);
        graphics.pop();
    }

    private int drawWrappedText(
        ADGuiGraphics graphics,
        Component message,
        int x,
        int y,
        int maxWidth,
        int bottom,
        int color
    ) {
        for (var line : font.split(message, maxWidth)) {
            if (y + 9 > bottom) {
                break;
            }
            graphics.text(font, line, x, y, color, false);
            y += 10;
        }
        return y;
    }

    private void drawCraftCost(ADGuiGraphics graphics, int x, int y, List<SizedIngredient> cost) {
        var textX = x + RIGHT_X + 3;
        var width = imageWidth - RIGHT_X - 12;
        var lineY = y + 56;
        var bottom = y + 116;
        if (cost.isEmpty() || minecraft.player.hasInfiniteMaterials()) {
            graphics.text(
                font,
                Component.translatable("screen.atomicdawn.workbench.free"),
                textX,
                lineY,
                PipBoyStyle.dim(),
                false
            );
            return;
        }
        for (var i = 0; i < cost.size(); i++) {
            if (lineY + 9 > bottom || (cost.size() > 6 && i == 5)) {
                drawFittedText(
                    graphics,
                    Component.literal("+" + (cost.size() - i) + " more"),
                    textX,
                    Math.min(lineY, bottom - 10),
                    width,
                    PipBoyStyle.dim()
                );
                break;
            }
            var entry = cost.get(i);
            var icon = entry.ingredient().items().findFirst().map(ItemStack::new).orElse(ItemStack.EMPTY);
            var have = WeaponModding.count(minecraft.player, entry);
            var line = Component.literal(entry.count() + "× ")
                .append(icon.getHoverName())
                .append(" (" + have + ")");
            drawFittedText(
                graphics,
                line,
                textX,
                lineY,
                width,
                have >= entry.count() ? PipBoyStyle.text() : PipBoyStyle.bad()
            );
            lineY += 10;
        }
    }

    private void drawCost(ADGuiGraphics graphics, int x, int y, List<SizedIngredient> cost) {
        var lineY = y + COST_Y - 3;
        if (cost.isEmpty() || minecraft.player.hasInfiniteMaterials()) {
            graphics.text(
                font,
                Component.translatable("screen.atomicdawn.workbench.free"),
                x + MID_X + 2,
                lineY,
                PipBoyStyle.dim(),
                false
            );
            return;
        }
        var visible = Math.min(cost.size() > 3 ? 2 : 3, cost.size());
        for (var i = 0; i < visible; i++) {
            var entry = cost.get(i);
            var icon = entry.ingredient().items().findFirst().map(ItemStack::new).orElse(ItemStack.EMPTY);
            var have = WeaponModding.count(minecraft.player, entry);
            var line = Component.literal(entry.count() + "× ")
                .append(icon.getHoverName())
                .append(" (" + have + ")");
            drawFittedText(
                graphics,
                line,
                x + MID_X + 2,
                lineY,
                MID_W - 4,
                have >= entry.count() ? PipBoyStyle.text() : PipBoyStyle.bad()
            );
            lineY += 10;
        }
        if (cost.size() > visible) {
            drawFittedText(
                graphics,
                Component.literal("+" + (cost.size() - visible) + " more"),
                x + MID_X + 2,
                lineY - 1,
                MID_W - 4,
                PipBoyStyle.dim()
            );
        }
    }

    private void hint(ADGuiGraphics graphics, int x, int y, String key) {
        drawWrappedText(
            graphics,
            Component.translatable(key),
            x + MID_X + 2,
            y + MID_Y + 4,
            MID_W - 4,
            y + WorkbenchMenu.INVENTORY_Y - 14,
            PipBoyStyle.dim()
        );
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

    private Optional<Holder.Reference<WeaponModification>> lookupMod(ResourceKey<WeaponModification> key) {
        return minecraft.level == null
            ? Optional.empty()
            : ModificationResolver.lookup(minecraft.level.registryAccess(), key);
    }

    private List<SizedIngredient> modCost(Optional<ResourceKey<WeaponModification>> mod) {
        if (mod.isEmpty()) {
            return List.of();
        }
        var base = base();
        if (
            base != null && selectedSlot != null && mod.equals(
                Optional.ofNullable(base.modifications().defaults().get(selectedSlot))
            )
        ) {
            return List.of();
        }
        return lookupMod(mod.get()).map(holder -> holder.value().cost()).orElse(List.of());
    }

    private List<SizedIngredient> craftCost(Item item) {
        if (minecraft.level == null) {
            return List.of();
        }
        var definition = Weapons.baseDefinition(minecraft.level, new ItemStack(item));
        return definition == null ? List.of() : WorkbenchService.craftCost(definition);
    }

    private int countItem(Item item) {
        var inventory = minecraft.player.getInventory();
        var total = 0;
        for (var i = 0; i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static float displayValue(WeaponStat stat, WeaponDefinition definition) {
        var raw = ModificationResolver.value(definition, stat);
        return switch (stat) {
            case FIRE_INTERVAL -> 1200F / Math.max(1F, raw);
            case RELOAD_TICKS -> raw / 20F;
            case BASE_SPREAD -> Math.max(0F, 100F - raw * 10F);
            default -> raw;
        };
    }

    private static String format(float value) {
        return Math.abs(value - Math.round(value)) < 0.05F
            ? Integer.toString(Math.round(value))
            : String.format(Locale.ROOT, "%.1f", value);
    }

    private static Component describe(Optional<WeaponModification> mod) {
        if (mod.isEmpty()) {
            return Component.translatable("screen.atomicdawn.workbench.none.desc");
        }
        var lines = new ArrayList<Component>();
        for (var modifier : mod.get().modifiers()) {
            var amount = modifier.amount();
            var percent = modifier.operation() != StatModifier.Operation.ADD;
            var number = percent ? format(amount * 100F) + "%" : format(amount);
            lines.add(
                Component.literal((amount >= 0 ? "+" : "") + number + " ")
                    .append(Component.translatable(modifier.stat().translationKey()))
                    .withStyle(ChatFormatting.GRAY)
            );
        }
        mod.get()
            .fireModes()
            .filter(modes -> !modes.isEmpty())
            .ifPresent(
                modes -> lines.add(
                    Component.translatable("screen.atomicdawn.workbench.fire_modes")
                        .append(" ")
                        .append(Component.translatable(modes.getFirst().translationKey()))
                        .withStyle(ChatFormatting.GRAY)
                )
            );
        if (lines.isEmpty()) {
            return Component.translatable("screen.atomicdawn.workbench.cosmetic");
        }
        var text = Component.empty();
        for (var i = 0; i < lines.size(); i++) {
            if (i > 0) {
                text.append("\n");
            }
            text.append(lines.get(i));
        }
        return text;
    }
}
