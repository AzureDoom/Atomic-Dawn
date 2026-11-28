package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.thirst.SourceDrinking;

public record DrinkSourcePayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<DrinkSourcePayload> TYPE = new Type<>(AtomicDawn.id("drink_source"));

    public static final StreamCodec<ByteBuf, DrinkSourcePayload> STREAM_CODEC = BlockPos.STREAM_CODEC.map(
        DrinkSourcePayload::new,
        DrinkSourcePayload::pos
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handleOnServer(DrinkSourcePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            SourceDrinking.sip(player, payload.pos());
        }
    }
}
