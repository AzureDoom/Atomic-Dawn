package mod.azure.atomicdawn.pipboy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import mod.azure.atomicdawn.AtomicDawn;

public record PipBoyData(
    int version,
    boolean initialized,
    UUID deviceId,
    Identifier model,
    List<Identifier> favorites
) {

    public static final int CURRENT_VERSION = 1;

    public static final int MAX_FAVORITES = 256;

    public static final Identifier DEFAULT_MODEL = AtomicDawn.id("pipboy_2000_mk_vi");

    public static final PipBoyData EMPTY = new PipBoyData(
        CURRENT_VERSION,
        false,
        new UUID(0L, 0L),
        DEFAULT_MODEL,
        List.of()
    );

    public static final MapCodec<PipBoyData> MAP_CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Codec.INT.optionalFieldOf("version", CURRENT_VERSION).forGetter(PipBoyData::version),
            Codec.BOOL.optionalFieldOf("initialized", false).forGetter(PipBoyData::initialized),
            UUIDUtil.CODEC.optionalFieldOf("device_id", new UUID(0L, 0L)).forGetter(PipBoyData::deviceId),
            Identifier.CODEC.optionalFieldOf("model", DEFAULT_MODEL).forGetter(PipBoyData::model),
            Identifier.CODEC.listOf().optionalFieldOf("favorites", List.of()).forGetter(PipBoyData::favorites)
        ).apply(instance, PipBoyData::new)
    );

    public static final StreamCodec<ByteBuf, PipBoyData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        PipBoyData::version,
        ByteBufCodecs.BOOL,
        PipBoyData::initialized,
        UUIDUtil.STREAM_CODEC,
        PipBoyData::deviceId,
        Identifier.STREAM_CODEC,
        PipBoyData::model,
        Identifier.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_FAVORITES)),
        PipBoyData::favorites,
        PipBoyData::new
    );

    public PipBoyData {
        var unique = new LinkedHashSet<>(favorites);
        favorites = unique.stream().limit(MAX_FAVORITES).toList();
    }

    public boolean isFavorite(Identifier item) {
        return favorites.contains(item);
    }

    public PipBoyData initializedWith(UUID id) {
        return new PipBoyData(CURRENT_VERSION, true, id, model, favorites);
    }

    public PipBoyData withFavorites(Set<Identifier> value) {
        return new PipBoyData(version, initialized, deviceId, model, List.copyOf(value));
    }
}
