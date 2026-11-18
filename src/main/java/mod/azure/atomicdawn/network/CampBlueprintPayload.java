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
import mod.azure.atomicdawn.camp.CampBlueprint;
import mod.azure.atomicdawn.camp.CampBlueprintService;

public record CampBlueprintPayload(
    Action action,
    Identifier blueprint,
    String name,
    BlockPos from,
    BlockPos to,
    Direction facing,
    int nonce
) implements CustomPacketPayload {

    public enum Action {

        SAVE_PREFAB,
        SAVE_CAMP,
        PLACE,
        DELETE;

        private static final Action[] VALUES = values();

        static Action byIndex(int index) {
            return VALUES[Math.floorMod(index, VALUES.length)];
        }
    }

    private static final int MAX_NAME = 64;

    private static final Direction[] DIRECTIONS = Direction.values();

    private static final Identifier NONE = AtomicDawn.id("none");

    public static final Type<CampBlueprintPayload> TYPE = new Type<>(AtomicDawn.id("camp_blueprint"));

    public static final StreamCodec<ByteBuf, CampBlueprintPayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            ByteBufCodecs.VAR_INT.encode(buf, payload.action().ordinal());
            Identifier.STREAM_CODEC.encode(buf, payload.blueprint());
            ByteBufCodecs.stringUtf8(MAX_NAME).encode(buf, payload.name());
            BlockPos.STREAM_CODEC.encode(buf, payload.from());
            BlockPos.STREAM_CODEC.encode(buf, payload.to());
            ByteBufCodecs.VAR_INT.encode(buf, payload.facing().ordinal());
            ByteBufCodecs.VAR_INT.encode(buf, payload.nonce());
        },
        buf -> new CampBlueprintPayload(
            Action.byIndex(ByteBufCodecs.VAR_INT.decode(buf)),
            Identifier.STREAM_CODEC.decode(buf),
            ByteBufCodecs.stringUtf8(MAX_NAME).decode(buf),
            BlockPos.STREAM_CODEC.decode(buf),
            BlockPos.STREAM_CODEC.decode(buf),
            DIRECTIONS[Math.floorMod(ByteBufCodecs.VAR_INT.decode(buf), DIRECTIONS.length)],
            ByteBufCodecs.VAR_INT.decode(buf)
        )
    );

    public static CampBlueprintPayload savePrefab(
        String name,
        BlockPos from,
        BlockPos to,
        Direction facing,
        int nonce
    ) {
        return new CampBlueprintPayload(Action.SAVE_PREFAB, NONE, name, from, to, facing, nonce);
    }

    public static CampBlueprintPayload saveCamp(String name, BlockPos at, Direction facing, int nonce) {
        return new CampBlueprintPayload(Action.SAVE_CAMP, NONE, name, at, at, facing, nonce);
    }

    public static CampBlueprintPayload place(Identifier blueprint, BlockPos target, Direction facing, int nonce) {
        return new CampBlueprintPayload(Action.PLACE, blueprint, "", target, target, facing, nonce);
    }

    public static CampBlueprintPayload delete(Identifier blueprint) {
        return new CampBlueprintPayload(Action.DELETE, blueprint, "", BlockPos.ZERO, BlockPos.ZERO, Direction.NORTH, 0);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handleOnServer(CampBlueprintPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        switch (payload.action()) {
            case SAVE_PREFAB -> CampBlueprintService.save(
                player,
                payload.name(),
                CampBlueprint.Kind.PREFAB,
                payload.from(),
                payload.to(),
                payload.facing(),
                payload.nonce()
            );
            case SAVE_CAMP -> CampBlueprintService.save(
                player,
                payload.name(),
                CampBlueprint.Kind.CAMP,
                payload.from(),
                payload.to(),
                payload.facing(),
                payload.nonce()
            );
            case PLACE -> CampBlueprintService.place(
                player,
                payload.blueprint(),
                payload.from(),
                payload.facing(),
                payload.nonce()
            );
            case DELETE -> CampBlueprintService.delete(player, payload.blueprint());
        }
    }
}
