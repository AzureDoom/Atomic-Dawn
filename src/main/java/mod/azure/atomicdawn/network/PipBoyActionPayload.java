package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.pipboy.PipBoyService;
import mod.azure.atomicdawn.pipboy.light.PipBoyLightService;
import mod.azure.atomicdawn.quest.QuestService;

public record PipBoyActionPayload(
    Action action,
    Identifier target
) implements CustomPacketPayload {

    public enum Action {

        TOGGLE_FAVORITE,
        REQUEST_STATS,
        TRACK_QUEST,
        TOGGLE_LIGHT;

        private static final Action[] VALUES = values();

        static Action byIndex(int index) {
            return VALUES[Math.floorMod(index, VALUES.length)];
        }
    }

    public static final Type<PipBoyActionPayload> TYPE = new Type<>(AtomicDawn.id("pipboy_action"));

    public static final StreamCodec<ByteBuf, PipBoyActionPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT.map(Action::byIndex, Action::ordinal),
        PipBoyActionPayload::action,
        Identifier.STREAM_CODEC,
        PipBoyActionPayload::target,
        PipBoyActionPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handleOnServer(PipBoyActionPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            switch (payload.action()) {
                case TOGGLE_FAVORITE -> PipBoyService.toggleFavorite(player, payload.target());
                case REQUEST_STATS -> PipBoyService.sendStats(player);
                case TRACK_QUEST -> QuestService.track(
                    player,
                    payload.target().equals(SpecialActionPayload.NONE)
                        ? Optional.empty()
                        : Optional.of(payload.target())
                );
                case TOGGLE_LIGHT -> PipBoyLightService.toggle(player);
            }
        }
    }
}
