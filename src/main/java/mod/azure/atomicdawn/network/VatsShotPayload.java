package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.vats.BodyPart;

public record VatsShotPayload(
    boolean hit,
    boolean critical,
    BodyPart part,
    int chancePercent
) implements CustomPacketPayload {

    public static final Type<VatsShotPayload> TYPE = new Type<>(AtomicDawn.id("vats_shot"));

    public static final StreamCodec<ByteBuf, VatsShotPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        VatsShotPayload::hit,
        ByteBufCodecs.BOOL,
        VatsShotPayload::critical,
        ByteBufCodecs.VAR_INT.map(BodyPart::byIndex, BodyPart::ordinal),
        VatsShotPayload::part,
        ByteBufCodecs.VAR_INT,
        VatsShotPayload::chancePercent,
        VatsShotPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
