package mod.azure.atomicdawn.npc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record NpcShop(
    List<Trade> trades,
    int stock,
    int restockMinutes
) {

    public static final int MAX_TRADES = 12;

    public record Trade(
        ItemStack costA,
        ItemStack costB,
        ItemStack result
    ) {

        public static final Codec<Trade> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                ItemStack.CODEC.fieldOf("cost").forGetter(Trade::costA),
                ItemStack.OPTIONAL_CODEC.optionalFieldOf("cost_b", ItemStack.EMPTY).forGetter(Trade::costB),
                ItemStack.CODEC.fieldOf("result").forGetter(Trade::result)
            ).apply(instance, Trade::new)
        );
    }

    public static final NpcShop EMPTY = new NpcShop(List.of(), 0, 0);

    public static final Codec<NpcShop> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Trade.CODEC.listOf().optionalFieldOf("trades", List.of()).forGetter(NpcShop::trades),
            Codec.INT.optionalFieldOf("stock", 0).forGetter(NpcShop::stock),
            Codec.INT.optionalFieldOf("restock_minutes", 0).forGetter(NpcShop::restockMinutes)
        ).apply(instance, NpcShop::new)
    );

    public boolean isEmpty() {
        return trades.isEmpty();
    }

    public NpcShop sanitized() {
        var list = trades == null ? List.<Trade>of() : trades;
        return new NpcShop(
            list.size() > MAX_TRADES ? List.copyOf(list.subList(0, MAX_TRADES)) : List.copyOf(list),
            Math.clamp(stock, 0, 9999),
            Math.clamp(restockMinutes, 0, 60 * 24 * 7)
        );
    }

    public NpcShop withTrades(List<Trade> value) {
        return new NpcShop(value, stock, restockMinutes);
    }

    public NpcShop withLimits(int newStock, int newRestock) {
        return new NpcShop(trades, newStock, newRestock);
    }
}
