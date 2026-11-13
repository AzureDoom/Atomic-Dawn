package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;

public record HealthClampPayload(float health) implements CustomPacketPayload {

    public static final Type<HealthClampPayload> TYPE = new Type<>(AtomicDawn.id("health_clamp"));

    public static final StreamCodec<ByteBuf, HealthClampPayload> STREAM_CODEC = ByteBufCodecs.FLOAT.map(
        HealthClampPayload::new,
        HealthClampPayload::health
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
