package mod.azure.atomicdawn.pipboy.holotape;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record HolotapeLog(List<Identifier> collected) {

    public static final int MAX_HOLOTAPES = 512;

    public static final HolotapeLog EMPTY = new HolotapeLog(List.of());

    public static final MapCodec<HolotapeLog> MAP_CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Identifier.CODEC.listOf().optionalFieldOf("collected", List.of()).forGetter(HolotapeLog::collected)
        ).apply(instance, HolotapeLog::new)
    );

    public static final StreamCodec<ByteBuf, HolotapeLog> STREAM_CODEC = Identifier.STREAM_CODEC
        .apply(ByteBufCodecs.list(MAX_HOLOTAPES))
        .map(HolotapeLog::new, HolotapeLog::collected);

    public HolotapeLog {
        collected = List.copyOf(collected);
    }

    public boolean has(Identifier id) {
        return collected.contains(id);
    }

    public HolotapeLog with(Identifier id) {
        if (has(id) || collected.size() >= MAX_HOLOTAPES) {
            return this;
        }
        var copy = new ArrayList<>(collected);
        copy.add(id);
        return new HolotapeLog(copy);
    }
}
