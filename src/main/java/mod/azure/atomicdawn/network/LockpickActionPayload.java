package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.lock.LockpickService;

public record LockpickActionPayload(
    Action action,
    float angle
) implements CustomPacketPayload {

    public enum Action {

        PROBE,
        STRAIN,
        OPEN,
        CANCEL;

        private static final Action[] VALUES = values();

        static Action byIndex(int index) {
            return VALUES[Math.floorMod(index, VALUES.length)];
        }
    }

    public static final Type<LockpickActionPayload> TYPE = new Type<>(AtomicDawn.id("lockpick_action"));

    public static final StreamCodec<ByteBuf, LockpickActionPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT.map(Action::byIndex, Action::ordinal),
        LockpickActionPayload::action,
        ByteBufCodecs.FLOAT,
        LockpickActionPayload::angle,
        LockpickActionPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handleOnServer(LockpickActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        switch (payload.action()) {
            case PROBE -> LockpickService.probe(player, payload.angle());
            case STRAIN -> LockpickService.strain(player);
            case OPEN -> LockpickService.open(player);
            case CANCEL -> LockpickService.cancel(player);
        }
    }
}
