package mod.azure.atomicdawn.special;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import mod.azure.atomicdawn.AtomicDawn;

@SuppressWarnings("unused")
public record LegendaryProgress(
    int coins,
    Map<Identifier, Integer> owned,
    List<Identifier> slots
) {

    public static final int MAX_SLOTS = 6;

    public static final int MAX_OWNED = 128;

    public static final Identifier EMPTY_SLOT = AtomicDawn.id("empty");

    public static final LegendaryProgress EMPTY = new LegendaryProgress(0, Map.of(), List.of());

    public static final Codec<LegendaryProgress> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Codec.INT.optionalFieldOf("coins", 0).forGetter(LegendaryProgress::coins),
            Codec.unboundedMap(Identifier.CODEC, Codec.INT)
                .optionalFieldOf("owned", Map.of())
                .forGetter(LegendaryProgress::owned),
            Identifier.CODEC.listOf().optionalFieldOf("slots", List.of()).forGetter(LegendaryProgress::slots)
        ).apply(instance, LegendaryProgress::new)
    );

    public static final StreamCodec<ByteBuf, LegendaryProgress> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        LegendaryProgress::coins,
        ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.VAR_INT, MAX_OWNED),
        LegendaryProgress::owned,
        Identifier.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_SLOTS)),
        LegendaryProgress::slots,
        LegendaryProgress::new
    );

    public LegendaryProgress {
        coins = Math.max(0, coins);
        var cleanOwned = new HashMap<Identifier, Integer>();
        owned.forEach((id, rank) -> {
            if (rank != null && rank > 0) {
                cleanOwned.put(id, rank);
            }
        });
        owned = Map.copyOf(cleanOwned);
        var cleanSlots = new ArrayList<Identifier>(MAX_SLOTS);
        for (var i = 0; i < MAX_SLOTS; i++) {
            var id = i < slots.size() ? slots.get(i) : null;
            cleanSlots.add(id == null || (!id.equals(EMPTY_SLOT) && cleanSlots.contains(id)) ? EMPTY_SLOT : id);
        }
        slots = List.copyOf(cleanSlots);
    }

    public int ownedRank(Identifier perk) {
        return owned.getOrDefault(perk, 0);
    }

    public Optional<Identifier> slot(int index) {
        if (index < 0 || index >= slots.size()) {
            return Optional.empty();
        }
        var id = slots.get(index);
        return id.equals(EMPTY_SLOT) ? Optional.empty() : Optional.of(id);
    }

    public int slotOf(Identifier perk) {
        return perk.equals(EMPTY_SLOT) ? -1 : slots.indexOf(perk);
    }

    public boolean equipped(Identifier perk) {
        return slotOf(perk) >= 0;
    }

    public int equippedRank(Identifier perk) {
        return equipped(perk) ? ownedRank(perk) : 0;
    }

    public List<Identifier> equippedPerks() {
        var list = new ArrayList<Identifier>();
        for (var id : slots) {
            if (!id.equals(EMPTY_SLOT)) {
                list.add(id);
            }
        }
        return list;
    }

    public LegendaryProgress withCoins(int value) {
        return new LegendaryProgress(value, owned, slots);
    }

    public LegendaryProgress addCoins(int amount) {
        return withCoins((int) Math.clamp((long) coins + amount, 0L, Integer.MAX_VALUE));
    }

    public LegendaryProgress withOwned(Identifier perk, int rank) {
        var copy = new HashMap<>(owned);
        if (rank <= 0) {
            copy.remove(perk);
        } else {
            copy.put(perk, rank);
        }
        return new LegendaryProgress(coins, copy, slots);
    }

    public LegendaryProgress withSlot(int index, Identifier perk) {
        var copy = new ArrayList<>(slots);
        for (var i = 0; i < copy.size(); i++) {
            if (copy.get(i).equals(perk)) {
                copy.set(i, EMPTY_SLOT);
            }
        }
        copy.set(index, perk);
        return new LegendaryProgress(coins, owned, copy);
    }

    public LegendaryProgress clearSlot(int index) {
        if (index < 0 || index >= slots.size()) {
            return this;
        }
        var copy = new ArrayList<>(slots);
        copy.set(index, EMPTY_SLOT);
        return new LegendaryProgress(coins, owned, copy);
    }
}
