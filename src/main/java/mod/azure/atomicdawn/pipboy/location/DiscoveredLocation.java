package mod.azure.atomicdawn.pipboy.location;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record DiscoveredLocation(
    Identifier structure,
    ResourceKey<Level> dimension,
    BlockPos pos,
    long discoveredAt
) {

    public static final Codec<DiscoveredLocation> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Identifier.CODEC.fieldOf("structure").forGetter(DiscoveredLocation::structure),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(DiscoveredLocation::dimension),
            BlockPos.CODEC.fieldOf("pos").forGetter(DiscoveredLocation::pos),
            Codec.LONG.optionalFieldOf("discovered_at", 0L).forGetter(DiscoveredLocation::discoveredAt)
        ).apply(instance, DiscoveredLocation::new)
    );

    public static final StreamCodec<ByteBuf, DiscoveredLocation> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC,
        DiscoveredLocation::structure,
        ResourceKey.streamCodec(Registries.DIMENSION),
        DiscoveredLocation::dimension,
        BlockPos.STREAM_CODEC,
        DiscoveredLocation::pos,
        ByteBufCodecs.VAR_LONG,
        DiscoveredLocation::discoveredAt,
        DiscoveredLocation::new
    );
}
