package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.pipboy.PipBoyNotifications;

public record PipBoyNotificationPayload(
    PipBoyNotifications.Kind kind,
    int value,
    String detail
) implements CustomPacketPayload {

    public static final Type<PipBoyNotificationPayload> TYPE = new Type<>(AtomicDawn.id("pipboy_notification"));

    public static final StreamCodec<ByteBuf, PipBoyNotificationPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT.map(PipBoyNotifications.Kind::byIndex, PipBoyNotifications.Kind::ordinal),
        PipBoyNotificationPayload::kind,
        ByteBufCodecs.VAR_INT,
        PipBoyNotificationPayload::value,
        ByteBufCodecs.stringUtf8(256),
        PipBoyNotificationPayload::detail,
        PipBoyNotificationPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
