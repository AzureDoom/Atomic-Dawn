package mod.azure.atomicdawn.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

public record QuestProgress(
    boolean completed,
    int stage,
    List<Integer> counts
) {

    public static final int MAX_OBJECTIVES = 64;

    public static final Codec<QuestProgress> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Codec.BOOL.optionalFieldOf("completed", false).forGetter(QuestProgress::completed),
            Codec.INT.optionalFieldOf("stage", 0).forGetter(QuestProgress::stage),
            Codec.INT.listOf().optionalFieldOf("counts", List.of()).forGetter(QuestProgress::counts)
        ).apply(instance, QuestProgress::new)
    );

    public static final StreamCodec<ByteBuf, QuestProgress> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        QuestProgress::completed,
        ByteBufCodecs.VAR_INT,
        QuestProgress::stage,
        ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(MAX_OBJECTIVES)),
        QuestProgress::counts,
        QuestProgress::new
    );

    public QuestProgress {
        stage = Math.max(0, stage);
        counts = List.copyOf(counts);
    }

    public static QuestProgress start(int objectives) {
        var counts = new ArrayList<Integer>();
        for (var i = 0; i < Math.min(objectives, MAX_OBJECTIVES); i++) {
            counts.add(0);
        }
        return new QuestProgress(false, 0, counts);
    }

    public int count(int index) {
        return index < counts.size() ? counts.get(index) : 0;
    }

    public QuestProgress withCount(int index, int value) {
        var copy = new ArrayList<>(counts);
        while (copy.size() <= index) {
            copy.add(0);
        }
        copy.set(index, value);
        return new QuestProgress(completed, stage, copy);
    }

    public QuestProgress withStage(int value) {
        return new QuestProgress(completed, value, counts);
    }

    public QuestProgress complete() {
        return new QuestProgress(true, stage, counts);
    }
}
