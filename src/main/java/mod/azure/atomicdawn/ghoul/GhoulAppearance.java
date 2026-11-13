package mod.azure.atomicdawn.ghoul;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

@SuppressWarnings("unused")
public record GhoulAppearance(
    boolean ghoul,
    int variant,
    boolean visible,
    boolean fullGlow
) {

    public static final GhoulAppearance NONE = new GhoulAppearance(false, 0, true, false);

    public static final MapCodec<GhoulAppearance> MAP_CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Codec.BOOL.optionalFieldOf("ghoul", false).forGetter(GhoulAppearance::ghoul),
            Codec.INT.optionalFieldOf("variant", 0).forGetter(GhoulAppearance::variant),
            Codec.BOOL.optionalFieldOf("visible", true).forGetter(GhoulAppearance::visible),
            Codec.BOOL.optionalFieldOf("full_glow", false).forGetter(GhoulAppearance::fullGlow)
        ).apply(instance, GhoulAppearance::new)
    );

    public static final StreamCodec<ByteBuf, GhoulAppearance> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        GhoulAppearance::ghoul,
        ByteBufCodecs.VAR_INT.map(GhoulState::clampVariant, GhoulState::clampVariant),
        GhoulAppearance::variant,
        ByteBufCodecs.BOOL,
        GhoulAppearance::visible,
        ByteBufCodecs.BOOL,
        GhoulAppearance::fullGlow,
        GhoulAppearance::new
    );

    public boolean rendersCosmetics() {
        return ghoul && visible;
    }

    public boolean glowing() {
        return ghoul && fullGlow;
    }
}
