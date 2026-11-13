package mod.azure.atomicdawn.npc;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class NpcMerchant implements Merchant {

    private static final double MAX_DISTANCE_SQR = 8.0 * 8.0;

    private final NpcEntity npc;

    @Nullable
    private Player trading;

    @Nullable
    private MerchantOffers offers;

    NpcMerchant(NpcEntity npc) {
        this.npc = npc;
    }

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        trading = player;
        if (player == null) {
            offers = null;
        }
    }

    @Override
    public @Nullable Player getTradingPlayer() {
        return trading;
    }

    @Override
    public @NotNull MerchantOffers getOffers() {
        if (offers == null) {
            offers = build();
        }
        return offers;
    }

    private MerchantOffers build() {
        var result = new MerchantOffers();
        var shop = npc.config().shop();
        var maxUses = shop.stock() > 0 ? shop.stock() : Integer.MAX_VALUE;
        for (var i = 0; i < shop.trades().size(); i++) {
            var trade = shop.trades().get(i);
            var costB = trade.costB().isEmpty()
                ? Optional.<ItemCost>empty()
                : Optional.of(new ItemCost(trade.costB().getItem(), trade.costB().getCount()));
            result.add(
                new MerchantOffer(
                    new ItemCost(trade.costA().getItem(), trade.costA().getCount()),
                    costB,
                    trade.result().copy(),
                    npc.shopUses(i),
                    maxUses,
                    0,
                    0F
                )
            );
        }
        return result;
    }

    @Override
    public void overrideOffers(@NotNull MerchantOffers value) {}

    @Override
    public void notifyTrade(@NotNull MerchantOffer offer) {
        offer.increaseUses();
        var index = getOffers().indexOf(offer);
        npc.recordShopUse(index, offer.getUses());
        npc.emote("swing");
    }

    @Override
    public void notifyTradeUpdated(@NotNull ItemStack stack) {}

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {}

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public @NotNull SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    @Override
    public boolean isClientSide() {
        return npc.level().isClientSide();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return trading == player && npc.isAlive() && !npc.isDowned() && player.distanceToSqr(npc) <= MAX_DISTANCE_SQR;
    }
}
