package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;

public record NukeExplosionPayload(
    Vec3 position,
    float radius
) implements CustomPacketPayload {

    public static final Type<NukeExplosionPayload> TYPE = new Type<>(AtomicDawn.id("nuke_explosion"));

    public static final StreamCodec<ByteBuf, NukeExplosionPayload> STREAM_CODEC = StreamCodec.composite(
        Vec3.STREAM_CODEC,
        NukeExplosionPayload::position,
        ByteBufCodecs.FLOAT,
        NukeExplosionPayload::radius,
        NukeExplosionPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
