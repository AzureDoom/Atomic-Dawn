package mod.azure.atomicdawn.npc;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public final class NpcShops {

    private static final int ROWS = 6;

    private static final int CONTAINER_SIZE = ROWS * 9;

    private static final int TRADES_PER_ROW = 2;

    private static final int COST_A = 0;

    private static final int COST_B = 1;

    private static final int ARROW = 2;

    private static final int RESULT = 3;

    private static final int SEPARATOR = 4;

    private NpcShops() {}

    public static boolean open(ServerPlayer player, NpcEntity npc) {
        if (npc.config().shop().isEmpty()) {
            player.sendSystemMessage(
                Component.translatable("npc.atomicdawn.shop.empty").withStyle(ChatFormatting.GRAY),
                true
            );
            return false;
        }
        var merchant = npc.merchant();
        if (merchant.getTradingPlayer() != null && merchant.getTradingPlayer() != player) {
            player.sendSystemMessage(
                Component.translatable("npc.atomicdawn.shop.busy").withStyle(ChatFormatting.GRAY),
                true
            );
            return false;
        }
        merchant.setTradingPlayer(player);
        merchant.openTradingScreen(player, npc.getDisplayName(), 0);
        return true;
    }

    private static int base(int trade) {
        var row = trade / TRADES_PER_ROW;
        var column = trade % TRADES_PER_ROW;
        return row * 9 + column * 5;
    }

    private static boolean isMarker(int slot) {
        if (slot < 0 || slot >= CONTAINER_SIZE) {
            return false;
        }
        var column = slot % 9;
        return column == ARROW || column == SEPARATOR || column == 5 + ARROW;
    }

    private static ItemStack marker(Item item, String key) {
        var stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable(key).withStyle(ChatFormatting.YELLOW));
        return stack;
    }

    public static void openEditor(ServerPlayer player, NpcEntity npc) {
        var container = new SimpleContainer(CONTAINER_SIZE);
        for (var slot = 0; slot < CONTAINER_SIZE; slot++) {
            var column = slot % 9;
            if (column == SEPARATOR) {
                container.setItem(
                    slot,
                    marker(Items.STAINED_GLASS_PANE.pick(DyeColor.BLACK), "npc.atomicdawn.shop.separator")
                );
            } else if (column == ARROW || column == 5 + ARROW) {
                container.setItem(
                    slot,
                    marker(Items.STAINED_GLASS_PANE.pick(DyeColor.LIME), "npc.atomicdawn.shop.arrow")
                );
            }
        }
        var trades = npc.config().shop().trades();
        for (var i = 0; i < Math.min(trades.size(), NpcShop.MAX_TRADES); i++) {
            var base = base(i);
            var trade = trades.get(i);
            container.setItem(base + COST_A, trade.costA().copy());
            container.setItem(base + COST_B, trade.costB().copy());
            container.setItem(base + RESULT, trade.result().copy());
        }
        player.openMenu(
            new SimpleMenuProvider(
                (id, inventory, _) -> new ChestMenu(MenuType.GENERIC_9x6, id, inventory, container, ROWS) {

                    @Override
                    public void clicked(
                        int slotIndex,
                        int buttonNum,
                        @NotNull ContainerInput containerInput,
                        @NotNull Player clicker
                    ) {
                        if (isMarker(slotIndex)) {
                            return;
                        }
                        super.clicked(slotIndex, buttonNum, containerInput, clicker);
                    }

                    @Override
                    public @NotNull ItemStack quickMoveStack(@NotNull Player mover, int index) {
                        if (isMarker(index)) {
                            return ItemStack.EMPTY;
                        }
                        return super.quickMoveStack(mover, index);
                    }

                    @Override
                    public void removed(@NotNull Player closing) {
                        super.removed(closing);
                        if (closing instanceof ServerPlayer serverPlayer) {
                            save(serverPlayer, npc, container);
                        }
                    }
                },
                Component.translatable("npc.atomicdawn.shop.editor_title")
            )
        );
    }

    private static void save(ServerPlayer player, NpcEntity npc, SimpleContainer container) {
        var trades = new ArrayList<NpcShop.Trade>();
        var leftovers = new ArrayList<ItemStack>();
        for (var i = 0; i < NpcShop.MAX_TRADES; i++) {
            var base = base(i);
            var costA = container.getItem(base + COST_A);
            var costB = container.getItem(base + COST_B);
            var result = container.getItem(base + RESULT);
            if (costA.isEmpty() && !costB.isEmpty()) {
                costA = costB;
                costB = ItemStack.EMPTY;
            }
            if (!costA.isEmpty() && !result.isEmpty()) {
                trades.add(new NpcShop.Trade(costA.copy(), costB.copy(), result.copy()));
                continue;
            }
            for (var stack : new ItemStack[] { costA, costB, result }) {
                if (!stack.isEmpty()) {
                    leftovers.add(stack.copy());
                }
            }
        }
        for (var stack : leftovers) {
            player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
        }
        if (!NpcService.canEdit(player) || !npc.isAlive() || npc.isRemoved()) {
            return;
        }
        npc.applyConfig(npc.config().withShop(npc.config().shop().withTrades(trades)));
        npc.resetShopUses();
        var message = leftovers.isEmpty()
            ? Component.translatable("npc.atomicdawn.shop.saved", trades.size())
            : Component.translatable("npc.atomicdawn.shop.saved_returned", trades.size());
        player.sendSystemMessage(message, true);
    }
}
