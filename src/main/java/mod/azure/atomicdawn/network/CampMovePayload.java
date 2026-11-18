package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.camp.CampPlacementService;
import mod.azure.atomicdawn.camp.CampStorageService;

public record CampMovePayload(
    Action action,
    UUID id,
    BlockPos pos,
    Direction facing,
    int nonce
) implements CustomPacketPayload {

    public enum Action {

        STORE,
        PLACE_STORED,
        SCRAP_STORED,
        MOVE_MODULE;

        private static final Action[] VALUES = values();

        static Action byIndex(int index) {
            return VALUES[Math.floorMod(index, VALUES.length)];
        }
    }

    private static final UUID NONE = new UUID(0L, 0L);

    private static final Direction[] DIRECTIONS = Direction.values();

    public static final Type<CampMovePayload> TYPE = new Type<>(AtomicDawn.id("camp_move"));

    public static final StreamCodec<ByteBuf, CampMovePayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            ByteBufCodecs.VAR_INT.encode(buf, payload.action().ordinal());
            UUIDUtil.STREAM_CODEC.encode(buf, payload.id());
            BlockPos.STREAM_CODEC.encode(buf, payload.pos());
            ByteBufCodecs.VAR_INT.encode(buf, payload.facing().ordinal());
            ByteBufCodecs.VAR_INT.encode(buf, payload.nonce());
        },
        buf -> new CampMovePayload(
            Action.byIndex(ByteBufCodecs.VAR_INT.decode(buf)),
            UUIDUtil.STREAM_CODEC.decode(buf),
            BlockPos.STREAM_CODEC.decode(buf),
            DIRECTIONS[Math.floorMod(ByteBufCodecs.VAR_INT.decode(buf), DIRECTIONS.length)],
            ByteBufCodecs.VAR_INT.decode(buf)
        )
    );

    public static CampMovePayload store(BlockPos pos, UUID id, int nonce) {
        return new CampMovePayload(Action.STORE, id, pos, Direction.NORTH, nonce);
    }

    public static CampMovePayload placeStored(UUID id, BlockPos target, Direction facing, int nonce) {
        return new CampMovePayload(Action.PLACE_STORED, id, target, facing, nonce);
    }

    public static CampMovePayload scrapStored(UUID id) {
        return new CampMovePayload(Action.SCRAP_STORED, id, BlockPos.ZERO, Direction.NORTH, 0);
    }

    public static CampMovePayload moveModule(BlockPos target, int nonce) {
        return new CampMovePayload(Action.MOVE_MODULE, NONE, target, Direction.NORTH, nonce);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handleOnServer(CampMovePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        switch (payload.action()) {
            case STORE -> CampStorageService.store(player, payload.pos(), payload.id(), payload.nonce());
            case PLACE_STORED -> CampStorageService.place(
                player,
                payload.id(),
                payload.pos(),
                payload.facing(),
                payload.nonce()
            );
            case SCRAP_STORED -> CampStorageService.scrap(player, payload.id());
            case MOVE_MODULE -> CampPlacementService.moveModule(player, payload.pos(), payload.nonce());
        }
    }
}
