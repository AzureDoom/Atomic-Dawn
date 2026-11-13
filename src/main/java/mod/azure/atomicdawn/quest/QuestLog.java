package mod.azure.atomicdawn.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record QuestLog(
    Map<Identifier, QuestProgress> quests,
    Optional<Identifier> tracked
) {

    public static final int MAX_QUESTS = 1024;

    public static final QuestLog EMPTY = new QuestLog(Map.of(), Optional.empty());

    public static final MapCodec<QuestLog> MAP_CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Codec.unboundedMap(Identifier.CODEC, QuestProgress.CODEC)
                .optionalFieldOf("quests", Map.of())
                .forGetter(QuestLog::quests),
            Identifier.CODEC.optionalFieldOf("tracked").forGetter(QuestLog::tracked)
        ).apply(instance, QuestLog::new)
    );

    public static final StreamCodec<ByteBuf, QuestLog> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, QuestProgress.STREAM_CODEC, MAX_QUESTS),
        QuestLog::quests,
        ByteBufCodecs.optional(Identifier.STREAM_CODEC),
        QuestLog::tracked,
        QuestLog::new
    );

    public QuestLog {
        quests = Map.copyOf(quests);
    }

    public Optional<QuestProgress> get(Identifier id) {
        return Optional.ofNullable(quests.get(id));
    }

    public boolean isCompleted(Identifier id) {
        var progress = quests.get(id);
        return progress != null && progress.completed();
    }

    public QuestLog with(Identifier id, QuestProgress progress) {
        var copy = new HashMap<>(quests);
        copy.put(id, progress);
        return new QuestLog(copy, tracked);
    }

    public QuestLog without(Identifier id) {
        var copy = new HashMap<>(quests);
        copy.remove(id);
        return new QuestLog(copy, tracked.filter(value -> !value.equals(id)));
    }

    public QuestLog withTracked(Optional<Identifier> value) {
        return new QuestLog(quests, value);
    }
}
