package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.special.SpecialResult;

public record SpecialResultPayload(SpecialResult result) implements CustomPacketPayload {

    public static final Type<SpecialResultPayload> TYPE = new Type<>(AtomicDawn.id("special_result"));

    public static final StreamCodec<ByteBuf, SpecialResultPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
        index -> new SpecialResultPayload(SpecialResult.byIndex(index)),
        payload -> payload.result().ordinal()
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
