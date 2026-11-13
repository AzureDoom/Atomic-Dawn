package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.npc.NpcDialogueService;

public record NpcDialogueChoicePayload(
    int entityId,
    int choice
) implements CustomPacketPayload {

    public static final int CLOSE = -1;

    public static final Type<NpcDialogueChoicePayload> TYPE = new Type<>(AtomicDawn.id("npc_dialogue_choice"));

    public static final StreamCodec<ByteBuf, NpcDialogueChoicePayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        NpcDialogueChoicePayload::entityId,
        ByteBufCodecs.VAR_INT,
        NpcDialogueChoicePayload::choice,
        NpcDialogueChoicePayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handleOnServer(NpcDialogueChoicePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (payload.choice() == CLOSE) {
            NpcDialogueService.close(player);
        } else {
            NpcDialogueService.choose(player, payload.entityId(), payload.choice());
        }
    }
}
