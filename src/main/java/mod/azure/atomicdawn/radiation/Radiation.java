package mod.azure.atomicdawn.radiation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record Radiation(float rads) {

    public static final Radiation NONE = new Radiation(0F);

    public static final MapCodec<Radiation> MAP_CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(Codec.FLOAT.fieldOf("rads").forGetter(Radiation::rads))
            .apply(instance, Radiation::new)
    );

    public static final StreamCodec<ByteBuf, Radiation> STREAM_CODEC = ByteBufCodecs.FLOAT.map(
        Radiation::new,
        Radiation::rads
    );

    public Radiation withRads(float value, float max) {
        return new Radiation(Math.clamp(value, 0F, max));
    }
}
