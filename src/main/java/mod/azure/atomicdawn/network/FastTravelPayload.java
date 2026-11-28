package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.pipboy.location.DiscoveredLocation;
import mod.azure.atomicdawn.pipboy.location.FastTravelService;

public record FastTravelPayload(DiscoveredLocation location) implements CustomPacketPayload {

    public static final Type<FastTravelPayload> TYPE = new Type<>(AtomicDawn.id("fast_travel"));

    public static final StreamCodec<ByteBuf, FastTravelPayload> STREAM_CODEC = DiscoveredLocation.STREAM_CODEC.map(
        FastTravelPayload::new,
        FastTravelPayload::location
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handleOnServer(FastTravelPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            FastTravelService.travel(player, payload.location());
        }
    }
}
