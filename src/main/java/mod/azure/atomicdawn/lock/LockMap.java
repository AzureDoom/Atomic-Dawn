package mod.azure.atomicdawn.lock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record LockMap(Map<BlockPos, Lock> locks) {

    public static final LockMap EMPTY = new LockMap(Map.of());

    private record Entry(
        BlockPos pos,
        Lock lock
    ) {

        static final Codec<Entry> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
                Lock.CODEC.fieldOf("lock").forGetter(Entry::lock)
            ).apply(instance, Entry::new)
        );
    }

    public static final MapCodec<LockMap> MAP_CODEC = Entry.CODEC.listOf()
        .fieldOf("locks")
        .xmap(LockMap::fromEntries, LockMap::toEntries);

    public LockMap {
        locks = Map.copyOf(locks);
    }

    private static LockMap fromEntries(List<Entry> entries) {
        var map = new HashMap<BlockPos, Lock>();
        for (var entry : entries) {
            map.put(entry.pos().immutable(), entry.lock());
        }
        return new LockMap(map);
    }

    private List<Entry> toEntries() {
        return locks.entrySet().stream().map(entry -> new Entry(entry.getKey(), entry.getValue())).toList();
    }

    public @Nullable Lock get(BlockPos pos) {
        return locks.get(pos);
    }

    public boolean isEmpty() {
        return locks.isEmpty();
    }

    public LockMap with(BlockPos pos, Lock lock) {
        var copy = new HashMap<>(locks);
        copy.put(pos.immutable(), lock);
        return new LockMap(copy);
    }

    public LockMap without(BlockPos pos) {
        if (!locks.containsKey(pos)) {
            return this;
        }
        var copy = new HashMap<>(locks);
        copy.remove(pos);
        return new LockMap(copy);
    }
}
