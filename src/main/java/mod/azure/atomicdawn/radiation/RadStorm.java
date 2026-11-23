package mod.azure.atomicdawn.radiation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record RadStorm(
    boolean raining,
    boolean active
) {

    public static final RadStorm NONE = new RadStorm(false, false);

    public static final MapCodec<RadStorm> MAP_CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Codec.BOOL.optionalFieldOf("raining", false).forGetter(RadStorm::raining),
            Codec.BOOL.optionalFieldOf("active", false).forGetter(RadStorm::active)
        ).apply(instance, RadStorm::new)
    );
}
