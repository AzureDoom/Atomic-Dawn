package mod.azure.atomicdawn.camp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import mod.azure.atomicdawn.AtomicDawn;

public record CampStash(List<Entry> entries) {

    public record Entry(
        ItemStack stack,
        long count
    ) {

        static final Codec<Entry> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                ItemStack.CODEC.fieldOf("item").forGetter(Entry::stack),
                Codec.LONG.fieldOf("count").forGetter(Entry::count)
            ).apply(instance, Entry::new)
        );

        static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            Entry::stack,
            ByteBufCodecs.VAR_LONG,
            Entry::count,
            Entry::new
        );

        public Entry {
            stack = stack.copyWithCount(1);
        }

        public ItemStack display() {
            return stack.copyWithCount((int) Math.min(count, stack.getMaxStackSize()));
        }
    }

    private record LegacySlot(
        int slot,
        ItemStack stack
    ) {

        static final Codec<LegacySlot> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                Codec.INT.fieldOf("slot").forGetter(LegacySlot::slot),
                ItemStack.CODEC.fieldOf("item").forGetter(LegacySlot::stack)
            ).apply(instance, LegacySlot::new)
        );
    }

    public static final CampStash EMPTY = new CampStash(List.of());

    public static final MapCodec<CampStash> MAP_CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Entry.CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(CampStash::entries),
            LegacySlot.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(ignored -> List.of())
        ).apply(instance, CampStash::migrate)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, CampStash> STREAM_CODEC = Entry.STREAM_CODEC
        .apply(ByteBufCodecs.list())
        .map(CampStash::new, CampStash::entries);

    public CampStash {
        var merged = new ArrayList<Entry>();
        for (var entry : entries) {
            if (entry.stack().isEmpty() || entry.count() <= 0L) {
                continue;
            }
            var index = indexOf(merged, entry.stack());
            if (index >= 0) {
                var existing = merged.get(index);
                merged.set(index, new Entry(existing.stack(), existing.count() + entry.count()));
            } else {
                merged.add(entry);
            }
        }
        entries = List.copyOf(merged);
    }

    private static CampStash migrate(List<Entry> entries, List<LegacySlot> legacy) {
        var all = new ArrayList<>(entries);
        for (var slot : legacy) {
            if (!slot.stack().isEmpty()) {
                all.add(new Entry(slot.stack(), slot.stack().getCount()));
            }
        }
        return new CampStash(all);
    }

    private static int indexOf(List<Entry> entries, ItemStack stack) {
        for (var i = 0; i < entries.size(); i++) {
            if (ItemStack.isSameItemSameComponents(entries.get(i).stack(), stack)) {
                return i;
            }
        }
        return -1;
    }

    public static CampStash of(Player player) {
        return player.getData(CampAttachments.STASH.get());
    }

    public static void set(Player player, CampStash stash) {
        player.setData(CampAttachments.STASH.get(), stash);
    }

    public static int limit() {
        return AtomicDawn.CONFIG.common.settlement.camp.storage.STASH_LIMIT;
    }

    public long total() {
        var total = 0L;
        for (var entry : entries) {
            total += entry.count();
        }
        return total;
    }

    public long space() {
        return Math.max(0L, limit() - total());
    }

    public CampStash add(ItemStack stack, long amount) {
        var list = new ArrayList<>(entries);
        list.add(new Entry(stack, amount));
        return new CampStash(list);
    }

    public CampStash remove(int index, long amount) {
        var list = new ArrayList<>(entries);
        var entry = list.get(index);
        var left = entry.count() - amount;
        if (left > 0L) {
            list.set(index, new Entry(entry.stack(), left));
        } else {
            list.remove(index);
        }
        return new CampStash(list);
    }
}
