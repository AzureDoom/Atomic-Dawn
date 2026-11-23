package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;

public record StorageOpenPayload(
    Mode mode,
    BlockPos pos
) implements CustomPacketPayload {

    public enum Mode {

        SCRAPBOX,
        STASH;

        private static final Mode[] VALUES = values();

        static Mode byIndex(int index) {
            return VALUES[Math.floorMod(index, VALUES.length)];
        }
    }

    public static final Type<StorageOpenPayload> TYPE = new Type<>(AtomicDawn.id("storage_open"));

    public static final StreamCodec<ByteBuf, Mode> MODE_CODEC = ByteBufCodecs.VAR_INT.map(Mode::byIndex, Mode::ordinal);

    public static final StreamCodec<ByteBuf, StorageOpenPayload> STREAM_CODEC = StreamCodec.composite(
        MODE_CODEC,
        StorageOpenPayload::mode,
        BlockPos.STREAM_CODEC,
        StorageOpenPayload::pos,
        StorageOpenPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
