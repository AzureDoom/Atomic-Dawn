package mod.azure.atomicdawn.story;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public record StoryState(
    Map<Identifier, Integer> reputation,
    Map<Identifier, Integer> affinity,
    Map<String, String> flags,
    Map<Identifier, String> outcomes,
    List<String> attempted
) {

    public static final StoryState EMPTY = new StoryState(Map.of(), Map.of(), Map.of(), Map.of(), List.of());

    public static final MapCodec<StoryState> MAP_CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Codec.unboundedMap(Identifier.CODEC, Codec.INT)
                .optionalFieldOf("reputation", Map.of())
                .forGetter(StoryState::reputation),
            Codec.unboundedMap(Identifier.CODEC, Codec.INT)
                .optionalFieldOf("affinity", Map.of())
                .forGetter(StoryState::affinity),
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                .optionalFieldOf("flags", Map.of())
                .forGetter(StoryState::flags),
            Codec.unboundedMap(Identifier.CODEC, Codec.STRING)
                .optionalFieldOf("outcomes", Map.of())
                .forGetter(StoryState::outcomes),
            Codec.STRING.listOf().optionalFieldOf("attempted", List.of()).forGetter(StoryState::attempted)
        ).apply(instance, StoryState::new)
    );

    public static final StreamCodec<ByteBuf, StoryState> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.VAR_INT),
        StoryState::reputation,
        ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.VAR_INT),
        StoryState::affinity,
        ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8),
        StoryState::flags,
        ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.STRING_UTF8),
        StoryState::outcomes,
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
        StoryState::attempted,
        StoryState::new
    );

    public StoryState {
        reputation = Map.copyOf(reputation);
        affinity = Map.copyOf(affinity);
        flags = Map.copyOf(flags);
        outcomes = Map.copyOf(outcomes);
        attempted = List.copyOf(new LinkedHashSet<>(attempted));
    }

    public StoryState withReputation(Identifier faction, int value) {
        var copy = new HashMap<>(reputation);
        copy.put(faction, value);
        return new StoryState(copy, affinity, flags, outcomes, attempted);
    }

    public StoryState withAffinity(Identifier npc, int value) {
        var copy = new HashMap<>(affinity);
        copy.put(npc, value);
        return new StoryState(reputation, copy, flags, outcomes, attempted);
    }

    public StoryState withFlag(String key, String value) {
        var copy = new HashMap<>(flags);
        if (value == null) {
            copy.remove(key);
        } else {
            copy.put(key, value);
        }
        return new StoryState(reputation, affinity, copy, outcomes, attempted);
    }

    public StoryState withOutcome(Identifier quest, String outcome) {
        var copy = new HashMap<>(outcomes);
        if (outcome == null) {
            copy.remove(quest);
        } else {
            copy.put(quest, outcome);
        }
        return new StoryState(reputation, affinity, flags, copy, attempted);
    }

    public StoryState withAttempt(String key) {
        var copy = new ArrayList<>(attempted);
        copy.add(key);
        return new StoryState(reputation, affinity, flags, outcomes, copy);
    }
}
