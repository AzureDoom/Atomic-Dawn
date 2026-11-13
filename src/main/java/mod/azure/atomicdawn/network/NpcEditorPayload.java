package mod.azure.atomicdawn.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.npc.NpcConfig;

public record NpcEditorPayload(
    int entityId,
    NpcConfig config
) implements CustomPacketPayload {

    public static final Type<NpcEditorPayload> TYPE = new Type<>(AtomicDawn.id("npc_editor"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NpcEditorPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        NpcEditorPayload::entityId,
        NpcConfig.STREAM_CODEC,
        NpcEditorPayload::config,
        NpcEditorPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
