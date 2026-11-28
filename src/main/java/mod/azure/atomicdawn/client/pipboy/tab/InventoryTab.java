package mod.azure.atomicdawn.client.pipboy.tab;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import mod.azure.atomicdawn.ammunition.AmmoContainerStorage;
import mod.azure.atomicdawn.ammunition.AmmoInventory;
import mod.azure.atomicdawn.ammunition.AmmoItem;
import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.client.pipboy.PipBoyClientState;
import mod.azure.atomicdawn.client.pipboy.PipBoyScreen;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.client.pipboy.Rect;
import mod.azure.atomicdawn.network.PipBoyActionPayload;
import mod.azure.atomicdawn.network.PipBoyItemPayload;
import mod.azure.atomicdawn.pipboy.PipBoyCategory;
import mod.azure.atomicdawn.pipboy.PipBoyData;
import mod.azure.atomicdawn.pipboy.PipBoyInventoryActions;
import mod.azure.atomicdawn.pipboy.PipBoyService;
import mod.azure.atomicdawn.powerarmor.PowerArmorChassisItem;
import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;
import mod.azure.atomicdawn.powerarmor.PowerArmorPieceItem;
import mod.azure.atomicdawn.powerarmor.PowerArmorSlot;
import mod.azure.atomicdawn.registry.ADItems;
import mod.azure.atomicdawn.weapon.instance.Weapons;

public final class InventoryTab implements PipBoyTab {

    private static final int ROW = 13;

    private static final int TOOLBAR = 15;

    private static final List<String> SUBTABS = Arrays.stream(PipBoyCategory.values()).map(Enum::name).toList();

    private static String selectedKey;

    private static int signature;

    private record Entry(
        String key,
        Identifier id,
        ItemStack sample,
        int count,
        PipBoyCategory category,
        int slot,
        int powerArmorSlot,
        boolean equipped,
        boolean held
    ) {}

    private record ItemAction(
        String labelKey,
        PipBoyItemPayload.Action action,
        int slot
    ) {}

    @Override
    public String id() {
        return "inv";
    }

    @Override
    public List<String> subtabs() {
        return SUBTABS;
    }

    @Override
    public Component subtabLabel(String subtab) {
        return Component.translatable(PipBoyCategory.valueOf(subtab).translationKey());
    }

    private static List<Entry> entries(LocalPlayer player, PipBoyCategory category) {
        var favorites = PipBoyService.data(player);
        var grouped = new LinkedHashMap<String, Entry>();
        var inventory = player.getInventory();
        var selected = inventory.getSelectedSlot();
        if (player.getVehicle() instanceof PowerArmorEntity armor) {
            for (var pieceSlot : PowerArmorSlot.all()) {
                var piece = armor.getPiece(pieceSlot);
                if (!piece.isEmpty()) {
                    collect(grouped, piece, category, favorites, -1, pieceSlot.ordinal(), selected);
                }
            }
        }
        for (var slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            collect(grouped, stack, category, favorites, slot, -1, selected);
            if (stack.is(ADItems.AMMO_CONTAINER.get())) {
                for (var contained : AmmoContainerStorage.read(stack)) {
                    if (!contained.isEmpty()) {
                        collect(grouped, contained, category, favorites, -1, -1, selected);
                    }
                }
            }
        }
        var list = new ArrayList<>(grouped.values());
        Comparator<Entry> order = PipBoyClientState.invSort == PipBoyClientState.InvSort.COUNT
            ? Comparator.comparingInt(Entry::count).reversed()
            : Comparator.comparing(e -> e.sample.getHoverName().getString().toLowerCase(Locale.ROOT));
        list.sort(order);
        return list;
    }

    private static void collect(
        Map<String, Entry> grouped,
        ItemStack stack,
        PipBoyCategory category,
        PipBoyData favorites,
        int slot,
        int powerArmorSlot,
        int selectedSlot
    ) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        var primary = PipBoyCategory.classify(stack);
        var include = category == PipBoyCategory.FAVORITES ? favorites.isFavorite(id) : primary == category;
        if (!include) {
            return;
        }
        var amount = stack.getItem() instanceof AmmoItem ? AmmoInventory.rounds(stack) : stack.getCount();
        var equipped = powerArmorSlot >= 0 || PipBoyInventoryActions.isArmorSlot(slot);
        var held = slot >= 0 && slot == selectedSlot && primary == PipBoyCategory.WEAPONS;
        var unique = stack.getMaxStackSize() == 1 || equipped || held;
        var key = powerArmorSlot >= 0
            ? id + "#pa" + powerArmorSlot
            : unique && slot >= 0 ? id + "#" + slot : id.toString();
        grouped.merge(
            key,
            new Entry(key, id, stack, amount, primary, slot, powerArmorSlot, equipped, held),
            (a, b) -> new Entry(
                a.key,
                a.id,
                a.sample,
                a.count + b.count,
                a.category,
                a.slot >= 0 ? a.slot : b.slot,
                a.powerArmorSlot,
                a.equipped,
                a.held
            )
        );
    }

    @Nullable
    private static ItemAction action(LocalPlayer player, Entry entry) {
        if (entry.powerArmorSlot >= 0) {
            return new ItemAction(
                "pipboy.atomicdawn.inv.unequip",
                PipBoyItemPayload.Action.UNEQUIP_POWER_ARMOR,
                entry.powerArmorSlot
            );
        }
        if (entry.slot < 0) {
            return null;
        }
        var stack = entry.sample;
        if (stack.getItem() instanceof PowerArmorPieceItem) {
            return player.getVehicle() instanceof PowerArmorEntity
                ? new ItemAction("pipboy.atomicdawn.inv.equip", PipBoyItemPayload.Action.EQUIP_POWER_ARMOR, entry.slot)
                : null;
        }
        if (stack.getItem() instanceof PowerArmorChassisItem) {
            return player.isPassenger()
                ? null
                : new ItemAction("pipboy.atomicdawn.inv.deploy", PipBoyItemPayload.Action.DEPLOY_CHASSIS, entry.slot);
        }
        if (PipBoyInventoryActions.armorSlot(stack) != null) {
            if (!entry.equipped && player.getVehicle() instanceof PowerArmorEntity) {
                return null;
            }
            return entry.equipped
                ? new ItemAction("pipboy.atomicdawn.inv.unequip", PipBoyItemPayload.Action.UNEQUIP, entry.slot)
                : new ItemAction("pipboy.atomicdawn.inv.equip", PipBoyItemPayload.Action.EQUIP, entry.slot);
        }
        if (PipBoyInventoryActions.usable(stack)) {
            return new ItemAction("pipboy.atomicdawn.inv.use", PipBoyItemPayload.Action.USE, entry.slot);
        }
        if (entry.category == PipBoyCategory.WEAPONS && !entry.held) {
            return new ItemAction("pipboy.atomicdawn.inv.hold", PipBoyItemPayload.Action.HOLD, entry.slot);
        }
        return null;
    }

    private static int signature(LocalPlayer player) {
        var inventory = player.getInventory();
        var hash = inventory.getSelectedSlot();
        for (var slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            hash = hash * 31 + (stack.isEmpty() ? 0 : ItemStack.hashItemAndComponents(stack) * 7 + stack.getCount());
        }
        if (player.getVehicle() instanceof PowerArmorEntity armor) {
            hash = hash * 31 + 1;
            for (var pieceSlot : PowerArmorSlot.all()) {
                var piece = armor.getPiece(pieceSlot);
                hash = hash * 31 + (piece.isEmpty() ? 0 : ItemStack.hashItemAndComponents(piece));
            }
        }
        return hash;
    }

    @Override
    public void tick(PipBoyScreen screen, String subtab) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        var current = signature(player);
        if (current != signature) {
            signature = current;
            screen.refresh();
        }
    }

    private static String scrollKey(String subtab) {
        return "inv." + subtab;
    }

    private static int listWidth(Rect area) {
        return Math.max(120, area.width() / 2);
    }

    private static int rows(Rect area) {
        return Math.max(1, (area.height() - TOOLBAR) / ROW);
    }

    @Override
    public void init(PipBoyScreen screen, Rect area, String subtab) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        var category = PipBoyCategory.valueOf(subtab);
        var sortLabel = Component.translatable(
            "pipboy.atomicdawn.inv.sort",
            Component.translatable(
                "pipboy.atomicdawn.inv.sort." + PipBoyClientState.invSort.name().toLowerCase(Locale.ROOT)
            )
        );
        screen.button(sortLabel, area.x(), area.y(), screen.font().width(sortLabel) + 10, 12, true, () -> {
            PipBoyClientState.invSort = PipBoyClientState.InvSort.values()[(PipBoyClientState.invSort.ordinal() + 1)
                % PipBoyClientState.InvSort.values().length];
            screen.refresh();
        });

        var entries = entries(player, category);
        var first = Math.clamp(entries.size() - 1, 0, PipBoyClientState.scroll(scrollKey(subtab)));
        var listW = listWidth(area);
        var device = PipBoyService.data(player);
        for (var r = 0; r < rows(area) && first + r < entries.size(); r++) {
            var entry = entries.get(first + r);
            var label = Component.literal(device.isFavorite(entry.id) ? "* " : "  ");
            if (entry.equipped) {
                label.append(Component.translatable("pipboy.atomicdawn.inv.equipped_tag"));
            } else if (entry.held) {
                label.append(Component.translatable("pipboy.atomicdawn.inv.held_tag"));
            }
            label.append(entry.sample.getHoverName());
            if (entry.count > 1) {
                label.append(" (" + entry.count + ")");
            }
            var selected = entry.id.equals(PipBoyClientState.selectedItem) && entry.key.equals(selectedKey);
            screen.row(label, area.x() + 20, area.y() + TOOLBAR + r * ROW, listW - 20, ROW - 1, selected, () -> {
                PipBoyClientState.selectedItem = entry.id;
                selectedKey = entry.key;
                screen.refresh();
            });
        }

        var selected = selected(entries);
        if (selected != null) {
            var favorite = device.isFavorite(selected.id);
            var label = Component.translatable(
                favorite ? "pipboy.atomicdawn.inv.unfavorite" : "pipboy.atomicdawn.inv.favorite"
            );
            var detail = area.below(TOOLBAR).rightOf(listW, 8);
            var favoriteWidth = screen.font().width(label) + 10;
            screen.button(
                label,
                detail.x(),
                detail.bottom() - 13,
                favoriteWidth,
                13,
                true,
                () -> ClientPacketDistributor.sendToServer(
                    new PipBoyActionPayload(PipBoyActionPayload.Action.TOGGLE_FAVORITE, selected.id)
                )
            );
            var action = action(player, selected);
            if (action != null) {
                var actionLabel = Component.translatable(action.labelKey());
                screen.button(
                    actionLabel,
                    detail.x() + favoriteWidth + 4,
                    detail.bottom() - 13,
                    screen.font().width(actionLabel) + 10,
                    13,
                    true,
                    () -> ClientPacketDistributor.sendToServer(
                        new PipBoyItemPayload(action.action(), action.slot(), selected.id)
                    )
                );
            }
        }
    }

    private static Entry selected(List<Entry> entries) {
        var id = PipBoyClientState.selectedItem;
        if (id == null) {
            return null;
        }
        for (var entry : entries) {
            if (entry.id.equals(id) && entry.key.equals(selectedKey)) {
                return entry;
            }
        }
        for (var entry : entries) {
            if (entry.id.equals(id)) {
                selectedKey = entry.key;
                return entry;
            }
        }
        return null;
    }

    @Override
    public boolean scroll(PipBoyScreen screen, String subtab, int direction) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        var count = entries(player, PipBoyCategory.valueOf(subtab)).size();
        var key = scrollKey(subtab);
        var next = Math.clamp(PipBoyClientState.scroll(key) + direction, 0, Math.max(0, count - 1));
        if (next == PipBoyClientState.scroll(key)) {
            return false;
        }
        PipBoyClientState.setScroll(key, next);
        return true;
    }

    @Override
    public void render(
        ADGuiGraphics graphics,
        PipBoyScreen screen,
        Rect area,
        String subtab,
        int mouseX,
        int mouseY
    ) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        var entries = entries(player, PipBoyCategory.valueOf(subtab));
        var listW = listWidth(area);
        if (entries.isEmpty()) {
            screen.text(
                graphics,
                Component.translatable("pipboy.atomicdawn.inv.empty"),
                area.x() + 4,
                area.y() + TOOLBAR + 3,
                PipBoyTheme.dim()
            );
        }
        var first = Math.clamp(entries.size() - 1, 0, PipBoyClientState.scroll(scrollKey(subtab)));
        for (var r = 0; r < rows(area) && first + r < entries.size(); r++) {
            graphics.item(entries.get(first + r).sample, area.x() + 2, area.y() + TOOLBAR + r * ROW - 2);
        }

        var selected = selected(entries);
        var detail = area.below(TOOLBAR).rightOf(listW, 8);
        graphics.fill(detail.x() - 4, detail.y(), detail.x() - 3, detail.bottom(), PipBoyTheme.faint());
        if (selected == null) {
            screen.wrapped(
                graphics,
                Component.translatable("pipboy.atomicdawn.inv.select"),
                detail.x(),
                detail.y() + 2,
                detail.width(),
                detail.bottom(),
                PipBoyTheme.dim()
            );
            return;
        }
        var x = detail.x();
        var y = detail.y() + 2;
        graphics.push();
        graphics.translate(x, y);
        graphics.scale(2F, 2F);
        graphics.item(selected.sample, 0, 0);
        graphics.pop();
        screen.fitted(graphics, selected.sample.getHoverName(), x + 38, y + 4, detail.width() - 40, PipBoyTheme.text());
        screen.text(
            graphics,
            Component.translatable("pipboy.atomicdawn.inv.count", selected.count),
            x + 38,
            y + 16,
            PipBoyTheme.dim()
        );
        y += 38;
        if (selected.equipped || selected.held) {
            screen.text(
                graphics,
                Component.translatable(
                    selected.powerArmorSlot >= 0
                        ? "pipboy.atomicdawn.inv.equipped_power_armor"
                        : selected.equipped ? "pipboy.atomicdawn.inv.equipped" : "pipboy.atomicdawn.inv.held"
                ),
                x,
                y,
                PipBoyTheme.text()
            );
            y += 12;
        }
        screen.text(
            graphics,
            Component.translatable(
                "pipboy.atomicdawn.inv.category",
                Component.translatable(selected.category.translationKey())
            ),
            x,
            y,
            PipBoyTheme.dim()
        );
        y += 12;
        var definition = Weapons.definition(minecraft.level, selected.sample);
        if (definition != null) {
            if (definition.condition().enabled()) {
                var condition = Weapons.condition(selected.sample, definition);
                screen.text(
                    graphics,
                    Component.translatable("pipboy.atomicdawn.status.condition"),
                    x,
                    y,
                    PipBoyTheme.dim()
                );
                screen.bar(
                    graphics,
                    x + 58,
                    y + 2,
                    Math.max(20, detail.width() - 62),
                    4,
                    condition.fraction(),
                    condition.fraction() < 0.25F ? PipBoyTheme.bad() : PipBoyTheme.text()
                );
                y += 12;
            }
            screen.text(
                graphics,
                Component.translatable(
                    "pipboy.atomicdawn.inv.damage",
                    String.format(
                        Locale.ROOT,
                        "%.1f",
                        definition.damage().base() * Math.max(1, definition.damage().pellets())
                    )
                ),
                x,
                y,
                PipBoyTheme.dim()
            );
            y += 12;
            if (definition.usesAmmo()) {
                var state = Weapons.state(selected.sample, definition);
                screen.text(
                    graphics,
                    Component.translatable(
                        "pipboy.atomicdawn.inv.loaded",
                        state.loadedAmmo(),
                        definition.ammo().magazineSize()
                    ),
                    x,
                    y,
                    PipBoyTheme.dim()
                );
                y += 12;
            }
        }
        if (selected.sample.isDamageableItem()) {
            var max = selected.sample.getMaxDamage();
            var left = max - selected.sample.getDamageValue();
            screen.text(
                graphics,
                Component.translatable("pipboy.atomicdawn.inv.durability", left, max),
                x,
                y,
                PipBoyTheme.dim()
            );
        }
    }
}
