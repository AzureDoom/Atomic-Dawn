package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.ghoul.GhoulService;
import mod.azure.atomicdawn.ghoul.GhoulTreatmentService;

public record GhoulActionPayload(
    Action action,
    int value
) implements CustomPacketPayload {

    public enum Action {

        CONFIRM_TRANSFORM,
        CONFIRM_REVERT,
        CANCEL,
        SET_VARIANT,
        SET_COSMETIC_VISIBLE;

        private static final Action[] VALUES = values();

        static Action byIndex(int index) {
            return VALUES[Math.floorMod(index, VALUES.length)];
        }
    }

    public static final Type<GhoulActionPayload> TYPE = new Type<>(AtomicDawn.id("ghoul_action"));

    public static final StreamCodec<ByteBuf, GhoulActionPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT.map(Action::byIndex, Action::ordinal),
        GhoulActionPayload::action,
        ByteBufCodecs.VAR_INT,
        GhoulActionPayload::value,
        GhoulActionPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handleOnServer(GhoulActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        switch (payload.action()) {
            case CONFIRM_TRANSFORM -> GhoulTreatmentService.confirm(player, GhoulTreatmentService.Kind.GHOULIFY);
            case CONFIRM_REVERT -> GhoulTreatmentService.confirm(player, GhoulTreatmentService.Kind.RESTORE);
            case CANCEL -> GhoulTreatmentService.cancel(player);
            case SET_VARIANT -> {
                if (GhoulService.isGhoul(player)) {
                    GhoulService.setAppearance(
                        player,
                        payload.value(),
                        GhoulService.state(player).cosmeticVisible()
                    );
                }
            }
            case SET_COSMETIC_VISIBLE -> {
                if (GhoulService.isGhoul(player) && (payload.value() == 0 || payload.value() == 1)) {
                    GhoulService.setAppearance(
                        player,
                        GhoulService.state(player).appearanceVariant(),
                        payload.value() == 1
                    );
                }
            }
        }
    }
}
