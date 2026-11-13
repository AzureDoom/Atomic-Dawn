package mod.azure.atomicdawn.mutation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public record Mutations(Map<Mutation, Long> suppressedUntil) {

    public static final Mutations NONE = new Mutations(Map.of());

    public static final MapCodec<Mutations> MAP_CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Codec.unboundedMap(Mutation.CODEC, Codec.LONG)
                .optionalFieldOf("mutations", Map.of())
                .forGetter(Mutations::suppressedUntil)
        ).apply(instance, Mutations::new)
    );

    public static final StreamCodec<ByteBuf, Mutations> STREAM_CODEC = ByteBufCodecs.map(
        HashMap::new,
        ByteBufCodecs.VAR_INT.map(Mutation::byIndex, Mutation::ordinal),
        ByteBufCodecs.VAR_LONG,
        Mutation.values().length
    ).map(Mutations::new, mutations -> new HashMap<>(mutations.suppressedUntil()));

    public Mutations {
        suppressedUntil = suppressedUntil.isEmpty() ? Map.of() : new EnumMap<>(suppressedUntil);
    }

    public boolean has(Mutation mutation) {
        return suppressedUntil.containsKey(mutation);
    }

    public Set<Mutation> all() {
        return suppressedUntil.keySet();
    }

    public int size() {
        return suppressedUntil.size();
    }

    public boolean suppressed(Mutation mutation, long now) {
        return suppressedUntil.getOrDefault(mutation, 0L) > now;
    }

    public Mutations with(Mutation mutation, long suppressedUntilTime) {
        var copy = new EnumMap<Mutation, Long>(Mutation.class);
        copy.putAll(suppressedUntil);
        copy.put(mutation, suppressedUntilTime);
        return new Mutations(copy);
    }

    public Mutations without(Mutation mutation) {
        var copy = new EnumMap<Mutation, Long>(Mutation.class);
        copy.putAll(suppressedUntil);
        copy.remove(mutation);
        return new Mutations(copy);
    }
}
