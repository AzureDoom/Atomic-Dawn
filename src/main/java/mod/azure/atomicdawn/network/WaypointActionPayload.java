package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.pipboy.location.LocationService;
import mod.azure.atomicdawn.special.SpecialLoadout;

public record WaypointActionPayload(
    Action action,
    int index,
    BlockPos pos,
    String name,
    int color
) implements CustomPacketPayload {

    public enum Action {

        ADD,
        REMOVE,
        RENAME,
        RECOLOR;

        private static final Action[] VALUES = values();

        static Action byIndex(int index) {
            return VALUES[Math.floorMod(index, VALUES.length)];
        }
    }

    public static final Type<WaypointActionPayload> TYPE = new Type<>(AtomicDawn.id("waypoint_action"));

    public static final StreamCodec<ByteBuf, WaypointActionPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT.map(Action::byIndex, Action::ordinal),
        WaypointActionPayload::action,
        ByteBufCodecs.VAR_INT,
        WaypointActionPayload::index,
        BlockPos.STREAM_CODEC,
        WaypointActionPayload::pos,
        ByteBufCodecs.stringUtf8(SpecialLoadout.MAX_NAME_LENGTH * 4),
        WaypointActionPayload::name,
        ByteBufCodecs.VAR_INT,
        WaypointActionPayload::color,
        WaypointActionPayload::new
    );

    public static WaypointActionPayload add(BlockPos pos, String name, int color) {
        return new WaypointActionPayload(Action.ADD, -1, pos, name, color);
    }

    public static WaypointActionPayload edit(Action action, int index, String name, int color) {
        return new WaypointActionPayload(action, index, BlockPos.ZERO, name, color);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handleOnServer(WaypointActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        switch (payload.action()) {
            case ADD -> LocationService.addWaypoint(player, payload.pos(), payload.name(), payload.color());
            case REMOVE -> LocationService.removeWaypoint(player, payload.index());
            case RENAME -> LocationService.renameWaypoint(player, payload.index(), payload.name());
            case RECOLOR -> LocationService.recolorWaypoint(player, payload.index(), payload.color());
        }
    }
}
