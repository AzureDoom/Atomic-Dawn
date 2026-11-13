package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;

public record ReloadStatusPayload(int durationTicks) implements CustomPacketPayload {

    public static final Type<ReloadStatusPayload> TYPE = new Type<>(AtomicDawn.id("reload_status"));

    public static final StreamCodec<ByteBuf, ReloadStatusPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
        ReloadStatusPayload::new,
        ReloadStatusPayload::durationTicks
    );

    public static final ReloadStatusPayload IDLE = new ReloadStatusPayload(0);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
