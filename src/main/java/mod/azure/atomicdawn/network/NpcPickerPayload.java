package mod.azure.atomicdawn.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import mod.azure.atomicdawn.AtomicDawn;

public record NpcPickerPayload(
    List<Entry> entries
) implements CustomPacketPayload {

    public static final int MAX_ENTRIES = 128;

    public record Entry(
        int entityId,
        String name,
        int distance,
        int respawnSeconds
    ) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            Entry::entityId,
            ByteBufCodecs.stringUtf8(256),
            Entry::name,
            ByteBufCodecs.VAR_INT,
            Entry::distance,
            ByteBufCodecs.VAR_INT,
            Entry::respawnSeconds,
            Entry::new
        );

        public boolean downed() {
            return respawnSeconds > 0;
        }
    }

    public static final Type<NpcPickerPayload> TYPE = new Type<>(AtomicDawn.id("npc_picker"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NpcPickerPayload> STREAM_CODEC = StreamCodec.composite(
        Entry.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ENTRIES)),
        NpcPickerPayload::entries,
        NpcPickerPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
