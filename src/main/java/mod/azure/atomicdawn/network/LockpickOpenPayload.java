package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;

public record LockpickOpenPayload(
    BlockPos pos,
    int level,
    int pins
) implements CustomPacketPayload {

    public static final Type<LockpickOpenPayload> TYPE = new Type<>(AtomicDawn.id("lockpick_open"));

    public static final StreamCodec<ByteBuf, LockpickOpenPayload> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        LockpickOpenPayload::pos,
        ByteBufCodecs.VAR_INT,
        LockpickOpenPayload::level,
        ByteBufCodecs.INT,
        LockpickOpenPayload::pins,
        LockpickOpenPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
