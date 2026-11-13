package mod.azure.atomicdawn.pipboy.location;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import mod.azure.atomicdawn.special.SpecialLoadout;

public record Waypoint(
    String name,
    ResourceKey<Level> dimension,
    BlockPos pos,
    int color
) {

    public static final int[] COLORS = {
        0xFF1AFF80,
        0xFFFFB13C,
        0xFFFF6C5A,
        0xFF5AB4FF,
        0xFFE070FF,
        0xFFFFF15A,
        0xFFFFFFFF,
        0xFF5AFFF0 };

    public static final Codec<Waypoint> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Codec.STRING.optionalFieldOf("name", "").forGetter(Waypoint::name),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Waypoint::dimension),
            BlockPos.CODEC.fieldOf("pos").forGetter(Waypoint::pos),
            Codec.INT.optionalFieldOf("color", 0).forGetter(Waypoint::color)
        ).apply(instance, Waypoint::new)
    );

    public static final StreamCodec<ByteBuf, Waypoint> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.stringUtf8(SpecialLoadout.MAX_NAME_LENGTH * 4),
        Waypoint::name,
        ResourceKey.streamCodec(Registries.DIMENSION),
        Waypoint::dimension,
        BlockPos.STREAM_CODEC,
        Waypoint::pos,
        ByteBufCodecs.VAR_INT,
        Waypoint::color,
        Waypoint::new
    );

    public Waypoint {
        name = SpecialLoadout.sanitizeName(name);
        color = Math.floorMod(color, COLORS.length);
    }

    public int argb() {
        return COLORS[color];
    }

    public Waypoint withName(String value) {
        return new Waypoint(value, dimension, pos, color);
    }

    public Waypoint withColor(int value) {
        return new Waypoint(name, dimension, pos, value);
    }
}
