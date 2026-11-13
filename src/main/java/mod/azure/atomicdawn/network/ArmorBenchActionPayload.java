package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.armor.ArmorBenchService;

public record ArmorBenchActionPayload(
    Action action,
    Identifier id
) implements CustomPacketPayload {

    public enum Action {

        FABRICATE,
        INSTALL_MOD,
        REMOVE_MOD,
        APPLY_LEGENDARY,
        REPAIR,
        REPAIR_KIT,
        SCRAP,
        JET_PACK;

        private static final Action[] VALUES = values();

        static Action byIndex(int index) {
            return VALUES[Math.floorMod(index, VALUES.length)];
        }
    }

    public static final Type<ArmorBenchActionPayload> TYPE = new Type<>(AtomicDawn.id("armor_bench_action"));

    public static final StreamCodec<ByteBuf, ArmorBenchActionPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT.map(Action::byIndex, Action::ordinal),
        ArmorBenchActionPayload::action,
        Identifier.STREAM_CODEC,
        ArmorBenchActionPayload::id,
        ArmorBenchActionPayload::new
    );

    public static ArmorBenchActionPayload of(Action action) {
        return new ArmorBenchActionPayload(action, SpecialActionPayload.NONE);
    }

    public static ArmorBenchActionPayload of(Action action, Identifier id) {
        return new ArmorBenchActionPayload(action, id);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handleOnServer(ArmorBenchActionPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            ArmorBenchService.handle(player, payload);
        }
    }
}
