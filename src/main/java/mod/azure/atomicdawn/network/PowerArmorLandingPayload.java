package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;

public record PowerArmorLandingPayload(
    float intensity,
    int ticks
) implements CustomPacketPayload {

    public static final Type<PowerArmorLandingPayload> TYPE = new Type<>(AtomicDawn.id("power_armor_landing"));

    public static final StreamCodec<ByteBuf, PowerArmorLandingPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT,
        PowerArmorLandingPayload::intensity,
        ByteBufCodecs.VAR_INT,
        PowerArmorLandingPayload::ticks,
        PowerArmorLandingPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
