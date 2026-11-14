package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;

public record LockpickStatePayload(
    Status status,
    float maxTurn,
    int pins,
    float pinHealth
) implements CustomPacketPayload {

    public enum Status {

        PROBE,
        STRAIN,
        PIN_BROKEN,
        REJECTED,
        SUCCESS,
        FAILED;

        private static final Status[] VALUES = values();

        static Status byIndex(int index) {
            return VALUES[Math.floorMod(index, VALUES.length)];
        }
    }

    public static final Type<LockpickStatePayload> TYPE = new Type<>(AtomicDawn.id("lockpick_state"));

    public static final StreamCodec<ByteBuf, LockpickStatePayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT.map(Status::byIndex, Status::ordinal),
        LockpickStatePayload::status,
        ByteBufCodecs.FLOAT,
        LockpickStatePayload::maxTurn,
        ByteBufCodecs.INT,
        LockpickStatePayload::pins,
        ByteBufCodecs.FLOAT,
        LockpickStatePayload::pinHealth,
        LockpickStatePayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
