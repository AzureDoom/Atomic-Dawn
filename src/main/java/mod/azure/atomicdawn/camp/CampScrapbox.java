package mod.azure.atomicdawn.camp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public record CampScrapbox(Map<Identifier, Long> counts) {

    public static final CampScrapbox EMPTY = new CampScrapbox(Map.of());

    public static final MapCodec<CampScrapbox> MAP_CODEC = Codec.unboundedMap(Identifier.CODEC, Codec.LONG)
        .optionalFieldOf("counts", Map.of())
        .xmap(CampScrapbox::new, CampScrapbox::counts);

    public static final StreamCodec<ByteBuf, CampScrapbox> STREAM_CODEC = ByteBufCodecs
        .map(HashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.VAR_LONG)
        .map(CampScrapbox::new, scrapbox -> new HashMap<>(scrapbox.counts()));

    public CampScrapbox {
        var copy = new HashMap<Identifier, Long>();
        counts.forEach((id, count) -> {
            if (count > 0L) {
                copy.put(id, count);
            }
        });
        counts = Map.copyOf(copy);
    }

    public static CampScrapbox of(Player player) {
        return player.getData(CampAttachments.SCRAPBOX.get());
    }

    public static void set(Player player, CampScrapbox scrapbox) {
        player.setData(CampAttachments.SCRAPBOX.get(), scrapbox);
    }

    public static CampScrapbox fromItems(Map<Item, Long> items) {
        var counts = new HashMap<Identifier, Long>();
        items.forEach((item, count) -> counts.merge(BuiltInRegistries.ITEM.getKey(item), count, CampScrapbox::sum));
        return new CampScrapbox(counts);
    }

    public Map<Item, Long> items() {
        var items = new LinkedHashMap<Item, Long>();
        counts.forEach((id, count) -> BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> items.put(item, count)));
        return items;
    }

    public long count(Item item) {
        return counts.getOrDefault(BuiltInRegistries.ITEM.getKey(item), 0L);
    }

    public long total() {
        var total = 0L;
        for (var count : counts.values()) {
            total = sum(total, count);
        }
        return total;
    }

    public CampScrapbox add(Item item, long amount) {
        if (amount <= 0L) {
            return this;
        }
        var copy = new HashMap<>(counts);
        copy.merge(BuiltInRegistries.ITEM.getKey(item), amount, CampScrapbox::sum);
        return new CampScrapbox(copy);
    }

    public CampScrapbox remove(Item item, long amount) {
        var copy = new HashMap<>(counts);
        var id = BuiltInRegistries.ITEM.getKey(item);
        var left = copy.getOrDefault(id, 0L) - Math.max(0L, amount);
        if (left > 0L) {
            copy.put(id, left);
        } else {
            copy.remove(id);
        }
        return new CampScrapbox(copy);
    }

    private static long sum(long a, long b) {
        return a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b;
    }
}
