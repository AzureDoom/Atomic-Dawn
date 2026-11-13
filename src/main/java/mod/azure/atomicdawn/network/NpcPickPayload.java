package mod.azure.atomicdawn.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.npc.NpcService;

public record NpcPickPayload(
    int entityId
) implements CustomPacketPayload {

    public static final Type<NpcPickPayload> TYPE = new Type<>(AtomicDawn.id("npc_pick"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NpcPickPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        NpcPickPayload::entityId,
        NpcPickPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handleOnServer(NpcPickPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            NpcService.handlePick(player, payload.entityId());
        }
    }
}
