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
import java.util.Map;

public record CampScrapbox(Map<Identifier, Integer> counts) {

    public static final CampScrapbox EMPTY = new CampScrapbox(Map.of());

    public static final MapCodec<CampScrapbox> MAP_CODEC = Codec.unboundedMap(Identifier.CODEC, Codec.INT)
        .optionalFieldOf("counts", Map.of())
        .xmap(CampScrapbox::new, CampScrapbox::counts);

    public static final StreamCodec<ByteBuf, CampScrapbox> STREAM_CODEC = ByteBufCodecs
        .map(HashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.VAR_INT)
        .map(CampScrapbox::new, scrapbox -> new HashMap<>(scrapbox.counts()));

    public CampScrapbox {
        var copy = new HashMap<Identifier, Integer>();
        counts.forEach((id, count) -> {
            if (count > 0) {
                copy.put(id, count);
            }
        });
        counts = Map.copyOf(copy);
    }

    public static CampScrapbox of(Player player) {
        return player.getData(CampAttachments.SCRAPBOX.get());
    }

    public int count(Item item) {
        return counts.getOrDefault(BuiltInRegistries.ITEM.getKey(item), 0);
    }

    public int total() {
        var total = 0;
        for (var count : counts.values()) {
            total += count;
        }
        return total;
    }

    public CampScrapbox add(Item item, int amount) {
        var copy = new HashMap<>(counts);
        copy.merge(BuiltInRegistries.ITEM.getKey(item), amount, Integer::sum);
        return new CampScrapbox(copy);
    }
}
