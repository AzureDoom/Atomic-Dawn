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
import mod.azure.atomicdawn.powerarmor.PowerArmorService;

public record PowerArmorActionPayload(
    Action action,
    int entityId,
    int slot,
    Identifier recipe
) implements CustomPacketPayload {

    public enum Action {

        REPAIR,
        PAINT,
        CLEAR_PAINT,
        FABRICATE,
        SCRAP,

        INSTALL_MOD,

        APPLY_LEGENDARY,

        REMOVE_MOD,

        EXIT,

        SET_FLYING,

        REPAIR_KIT;

        private static final Action[] VALUES = values();

        static Action byIndex(int index) {
            return VALUES[Math.floorMod(index, VALUES.length)];
        }
    }

    public static final Type<PowerArmorActionPayload> TYPE = new Type<>(AtomicDawn.id("power_armor_action"));

    public static final StreamCodec<ByteBuf, PowerArmorActionPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT.map(Action::byIndex, Action::ordinal),
        PowerArmorActionPayload::action,
        ByteBufCodecs.VAR_INT,
        PowerArmorActionPayload::entityId,
        ByteBufCodecs.VAR_INT,
        PowerArmorActionPayload::slot,
        Identifier.STREAM_CODEC,
        PowerArmorActionPayload::recipe,
        PowerArmorActionPayload::new
    );

    public static PowerArmorActionPayload slot(Action action, int entityId, int slot) {
        return new PowerArmorActionPayload(action, entityId, slot, SpecialActionPayload.NONE);
    }

    public static PowerArmorActionPayload flying(int entityId, boolean flying) {
        return new PowerArmorActionPayload(Action.SET_FLYING, entityId, flying ? 1 : 0, SpecialActionPayload.NONE);
    }

    public static PowerArmorActionPayload upgrade(Action action, int entityId, int slot, Identifier id) {
        return new PowerArmorActionPayload(action, entityId, slot, id);
    }

    public static PowerArmorActionPayload fabricate(int entityId, Identifier recipe) {
        return new PowerArmorActionPayload(Action.FABRICATE, entityId, -1, recipe);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handleOnServer(PowerArmorActionPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            PowerArmorService.handle(player, payload);
        }
    }
}
