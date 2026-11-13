package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;

public record HolotapePlayPayload(Identifier holotape) implements CustomPacketPayload {

    public static final Type<HolotapePlayPayload> TYPE = new Type<>(AtomicDawn.id("holotape_play"));

    public static final StreamCodec<ByteBuf, HolotapePlayPayload> STREAM_CODEC = Identifier.STREAM_CODEC.map(
        HolotapePlayPayload::new,
        HolotapePlayPayload::holotape
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
