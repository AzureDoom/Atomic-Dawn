package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

import mod.azure.atomicdawn.AtomicDawn;

public record PartySyncPayload(
    int maxSize,
    List<Member> members
) implements CustomPacketPayload {

    public record Member(
        UUID id,
        String name,
        boolean leader,
        boolean online,
        float health,
        float maxHealth,
        Identifier dimension,
        BlockPos pos
    ) {

        public static final StreamCodec<ByteBuf, Member> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            Member::id,
            ByteBufCodecs.stringUtf8(64),
            Member::name,
            ByteBufCodecs.BOOL,
            Member::leader,
            ByteBufCodecs.BOOL,
            Member::online,
            ByteBufCodecs.FLOAT,
            Member::health,
            ByteBufCodecs.FLOAT,
            Member::maxHealth,
            Identifier.STREAM_CODEC,
            Member::dimension,
            BlockPos.STREAM_CODEC,
            Member::pos,
            Member::new
        );
    }

    public static final PartySyncPayload NONE = new PartySyncPayload(0, List.of());

    public static final Type<PartySyncPayload> TYPE = new Type<>(AtomicDawn.id("party_sync"));

    public static final StreamCodec<ByteBuf, PartySyncPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        PartySyncPayload::maxSize,
        Member.STREAM_CODEC.apply(ByteBufCodecs.list(64)),
        PartySyncPayload::members,
        PartySyncPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
