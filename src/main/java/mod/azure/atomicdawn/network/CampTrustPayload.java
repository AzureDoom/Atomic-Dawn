package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.camp.CampService;

public record CampTrustPayload(String name) implements CustomPacketPayload {

    public static final Type<CampTrustPayload> TYPE = new Type<>(AtomicDawn.id("camp_trust"));

    public static final StreamCodec<ByteBuf, CampTrustPayload> STREAM_CODEC = ByteBufCodecs.stringUtf8(16)
        .map(CampTrustPayload::new, CampTrustPayload::name);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handleOnServer(CampTrustPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            CampService.trust(player, payload.name());
        }
    }
}
