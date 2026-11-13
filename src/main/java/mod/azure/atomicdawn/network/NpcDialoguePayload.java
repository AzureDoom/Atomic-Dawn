package mod.azure.atomicdawn.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.npc.NpcDialogueService;

public record NpcDialoguePayload(
    boolean open,
    int entityId,
    Component name,
    Component title,
    Component text,
    List<Component> options
) implements CustomPacketPayload {

    public static final Type<NpcDialoguePayload> TYPE = new Type<>(AtomicDawn.id("npc_dialogue"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NpcDialoguePayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        NpcDialoguePayload::open,
        ByteBufCodecs.VAR_INT,
        NpcDialoguePayload::entityId,
        ComponentSerialization.STREAM_CODEC,
        NpcDialoguePayload::name,
        ComponentSerialization.STREAM_CODEC,
        NpcDialoguePayload::title,
        ComponentSerialization.STREAM_CODEC,
        NpcDialoguePayload::text,
        ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs.list(NpcDialogueService.MAX_OPTIONS)),
        NpcDialoguePayload::options,
        NpcDialoguePayload::new
    );

    public static NpcDialoguePayload closed(int entityId) {
        return new NpcDialoguePayload(
            false,
            entityId,
            Component.empty(),
            Component.empty(),
            Component.empty(),
            List.of()
        );
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
