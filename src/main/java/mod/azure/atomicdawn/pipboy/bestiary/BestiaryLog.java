package mod.azure.atomicdawn.pipboy.bestiary;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public record BestiaryLog(Map<Identifier, Integer> kills) {

    public static final int MAX_ENTRIES = 1024;

    public static final BestiaryLog EMPTY = new BestiaryLog(Map.of());

    public static final MapCodec<BestiaryLog> MAP_CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Codec.unboundedMap(Identifier.CODEC, Codec.INT)
                .optionalFieldOf("kills", Map.of())
                .forGetter(BestiaryLog::kills)
        ).apply(instance, BestiaryLog::new)
    );

    public static final StreamCodec<ByteBuf, BestiaryLog> STREAM_CODEC = ByteBufCodecs.map(
        HashMap::new,
        Identifier.STREAM_CODEC,
        ByteBufCodecs.VAR_INT,
        MAX_ENTRIES
    ).map(BestiaryLog::new, log -> new HashMap<>(log.kills()));

    public BestiaryLog {
        kills = Map.copyOf(kills);
    }

    public boolean encountered(Identifier entity) {
        return kills.containsKey(entity);
    }

    public int killCount(Identifier entity) {
        return kills.getOrDefault(entity, 0);
    }

    public BestiaryLog with(Identifier entity, int value) {
        var copy = new HashMap<>(kills);
        copy.put(entity, Math.max(0, value));
        return new BestiaryLog(copy);
    }
}
