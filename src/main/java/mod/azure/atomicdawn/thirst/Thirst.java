package mod.azure.atomicdawn.thirst;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record Thirst(float value) {

    public static final float MAX = 20F;

    public static final Thirst FULL = new Thirst(MAX);

    public static final MapCodec<Thirst> MAP_CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(Codec.FLOAT.optionalFieldOf("value", MAX).forGetter(Thirst::value))
            .apply(instance, Thirst::new)
    );

    public static final StreamCodec<ByteBuf, Thirst> STREAM_CODEC = ByteBufCodecs.FLOAT.map(
        Thirst::new,
        Thirst::value
    );

    public Thirst {
        value = Math.clamp(value, 0F, MAX);
    }
}
