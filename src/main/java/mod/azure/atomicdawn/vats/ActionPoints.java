package mod.azure.atomicdawn.vats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ActionPoints(
    float points,
    float critical
) {

    public static final float CRITICAL_FULL = 100F;

    public static final ActionPoints FULL_DEFAULT = new ActionPoints(100F, 0F);

    public static final MapCodec<ActionPoints> MAP_CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Codec.FLOAT.fieldOf("points").forGetter(ActionPoints::points),
            Codec.FLOAT.fieldOf("critical").forGetter(ActionPoints::critical)
        ).apply(instance, ActionPoints::new)
    );

    public static final StreamCodec<ByteBuf, ActionPoints> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT,
        ActionPoints::points,
        ByteBufCodecs.FLOAT,
        ActionPoints::critical,
        ActionPoints::new
    );

    public boolean criticalReady() {
        return critical >= CRITICAL_FULL;
    }

    public ActionPoints withPoints(float value, float max) {
        return new ActionPoints(Math.clamp(value, 0F, max), critical);
    }

    public ActionPoints withCritical(float value) {
        return new ActionPoints(points, Math.clamp(value, 0F, CRITICAL_FULL));
    }
}
