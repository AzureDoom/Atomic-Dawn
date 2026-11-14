package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.camp.CampWorkshopService;
import mod.azure.atomicdawn.camp.PlacementMode;

public record CampWorkshopPayload(
    Action action,
    Identifier definition,
    BlockPos pos,
    Direction facing,
    PlacementMode mode,
    int nonce
) implements CustomPacketPayload {

    public enum Action {

        PLACE,
        SCRAP;

        private static final Action[] VALUES = values();

        static Action byIndex(int index) {
            return VALUES[Math.floorMod(index, VALUES.length)];
        }
    }

    private static final Direction[] DIRECTIONS = Direction.values();

    public static final Type<CampWorkshopPayload> TYPE = new Type<>(AtomicDawn.id("camp_workshop"));

    public static final StreamCodec<ByteBuf, CampWorkshopPayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            ByteBufCodecs.VAR_INT.encode(buf, payload.action().ordinal());
            Identifier.STREAM_CODEC.encode(buf, payload.definition());
            BlockPos.STREAM_CODEC.encode(buf, payload.pos());
            ByteBufCodecs.VAR_INT.encode(buf, payload.facing().ordinal());
            ByteBufCodecs.VAR_INT.encode(buf, payload.mode().ordinal());
            ByteBufCodecs.VAR_INT.encode(buf, payload.nonce());
        },
        buf -> new CampWorkshopPayload(
            Action.byIndex(ByteBufCodecs.VAR_INT.decode(buf)),
            Identifier.STREAM_CODEC.decode(buf),
            BlockPos.STREAM_CODEC.decode(buf),
            DIRECTIONS[Math.floorMod(ByteBufCodecs.VAR_INT.decode(buf), DIRECTIONS.length)],
            PlacementMode.byIndex(ByteBufCodecs.VAR_INT.decode(buf)),
            ByteBufCodecs.VAR_INT.decode(buf)
        )
    );

    public static CampWorkshopPayload place(
        Identifier definition,
        BlockPos pos,
        Direction facing,
        PlacementMode mode,
        int nonce
    ) {
        return new CampWorkshopPayload(Action.PLACE, definition, pos, facing, mode, nonce);
    }

    public static CampWorkshopPayload scrap(BlockPos pos, int nonce) {
        return new CampWorkshopPayload(
            Action.SCRAP,
            AtomicDawn.id("none"),
            pos,
            Direction.NORTH,
            PlacementMode.COLLISION,
            nonce
        );
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handleOnServer(CampWorkshopPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        switch (payload.action()) {
            case PLACE -> CampWorkshopService.place(
                player,
                payload.definition(),
                payload.pos(),
                payload.facing(),
                payload.mode(),
                payload.nonce()
            );
            case SCRAP -> CampWorkshopService.scrap(player, payload.pos(), payload.nonce());
        }
    }
}
