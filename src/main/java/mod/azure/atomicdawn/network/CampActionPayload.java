package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.camp.CampService;

public record CampActionPayload(
    Action action,
    UUID campId,
    int value
) implements CustomPacketPayload {

    public enum Action {

        REQUEST,
        STORE,
        PIN,
        SELECT,
        REBUILD,
        DELETE,
        TOGGLE_ACCESS,
        UNTRUST,
        MODULE_FACING,
        CLEAR_REBUILD;

        private static final Action[] VALUES = values();

        static Action byIndex(int index) {
            return VALUES[Math.floorMod(index, VALUES.length)];
        }
    }

    public static final Type<CampActionPayload> TYPE = new Type<>(AtomicDawn.id("camp_action"));

    public static final StreamCodec<ByteBuf, CampActionPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT.map(Action::byIndex, Action::ordinal),
        CampActionPayload::action,
        UUIDUtil.STREAM_CODEC,
        CampActionPayload::campId,
        ByteBufCodecs.VAR_INT,
        CampActionPayload::value,
        CampActionPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handleOnServer(CampActionPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            CampService.handle(player, payload.action(), payload.campId(), payload.value());
        }
    }
}
