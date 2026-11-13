package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.special.SpecialAttribute;
import mod.azure.atomicdawn.special.SpecialLoadout;
import mod.azure.atomicdawn.special.SpecialService;

public record SpecialActionPayload(
    Action action,
    int revision,
    Identifier perk,
    int rank,
    int loadout,
    List<Integer> special,
    String name
) implements CustomPacketPayload {

    public enum Action {

        ALLOCATE,
        LEARN_PERK,
        EQUIP_PERK,
        UNEQUIP_PERK,
        SWITCH_LOADOUT,
        RENAME_LOADOUT,
        SCRAP_PERK,
        LEGENDARY_EQUIP,
        LEGENDARY_UNEQUIP,
        LEGENDARY_UPGRADE;

        private static final Action[] VALUES = values();

        static Action byIndex(int index) {
            return VALUES[Math.floorMod(index, VALUES.length)];
        }
    }

    public static final Identifier NONE = AtomicDawn.id("none");

    public static final Type<SpecialActionPayload> TYPE = new Type<>(AtomicDawn.id("special_action"));

    private static final StreamCodec<ByteBuf, List<Integer>> SPECIAL_CODEC = ByteBufCodecs.VAR_INT.apply(
        ByteBufCodecs.list(SpecialAttribute.COUNT)
    );

    private static final StreamCodec<ByteBuf, String> NAME_CODEC = ByteBufCodecs.stringUtf8(
        SpecialLoadout.MAX_NAME_LENGTH * 4
    );

    public static final StreamCodec<ByteBuf, SpecialActionPayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            ByteBufCodecs.VAR_INT.encode(buf, payload.action().ordinal());
            ByteBufCodecs.VAR_INT.encode(buf, payload.revision());
            Identifier.STREAM_CODEC.encode(buf, payload.perk());
            ByteBufCodecs.VAR_INT.encode(buf, payload.rank());
            ByteBufCodecs.VAR_INT.encode(buf, payload.loadout());
            SPECIAL_CODEC.encode(buf, payload.special());
            NAME_CODEC.encode(buf, payload.name());
        },
        buf -> new SpecialActionPayload(
            Action.byIndex(ByteBufCodecs.VAR_INT.decode(buf)),
            ByteBufCodecs.VAR_INT.decode(buf),
            Identifier.STREAM_CODEC.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf),
            SPECIAL_CODEC.decode(buf),
            NAME_CODEC.decode(buf)
        )
    );

    public SpecialActionPayload {
        special = List.copyOf(special);
    }

    public static SpecialActionPayload allocate(int revision, List<Integer> special) {
        return new SpecialActionPayload(Action.ALLOCATE, revision, NONE, 0, 0, special, "");
    }

    public static SpecialActionPayload perk(Action action, int revision, Identifier perk, int rank) {
        return new SpecialActionPayload(action, revision, perk, rank, 0, List.of(), "");
    }

    public static SpecialActionPayload loadout(Action action, int revision, int index, String name) {
        return new SpecialActionPayload(action, revision, NONE, 0, index, List.of(), name);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handleOnServer(SpecialActionPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            var result = SpecialService.handle(player, payload);
            ADNetwork.sendTo(player, new SpecialResultPayload(result));
        }
    }
}
