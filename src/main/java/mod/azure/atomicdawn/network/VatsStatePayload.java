package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.vats.BodyPart;
import mod.azure.atomicdawn.weapon.action.VatsActions;

public record VatsStatePayload(
    boolean active,
    int targetId,
    BodyPart part
) implements CustomPacketPayload {

    public static final Type<VatsStatePayload> TYPE = new Type<>(AtomicDawn.id("vats_state"));

    public static final StreamCodec<ByteBuf, VatsStatePayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        VatsStatePayload::active,
        ByteBufCodecs.VAR_INT,
        VatsStatePayload::targetId,
        ByteBufCodecs.VAR_INT.map(BodyPart::byIndex, BodyPart::ordinal),
        VatsStatePayload::part,
        VatsStatePayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handleOnServer(VatsStatePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            VatsActions.setState(player, payload.active(), payload.targetId(), payload.part());
        }
    }
}
