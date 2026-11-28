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
import mod.azure.atomicdawn.pipboy.PipBoyInventoryActions;

public record PipBoyItemPayload(
    Action action,
    int slot,
    Identifier item
) implements CustomPacketPayload {

    public enum Action {

        USE,
        EQUIP,
        UNEQUIP,
        HOLD,
        EQUIP_POWER_ARMOR,
        UNEQUIP_POWER_ARMOR,
        DEPLOY_CHASSIS;

        private static final Action[] VALUES = values();

        static Action byIndex(int index) {
            return VALUES[Math.floorMod(index, VALUES.length)];
        }
    }

    public static final Type<PipBoyItemPayload> TYPE = new Type<>(AtomicDawn.id("pipboy_item"));

    public static final StreamCodec<ByteBuf, PipBoyItemPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT.map(Action::byIndex, Action::ordinal),
        PipBoyItemPayload::action,
        ByteBufCodecs.VAR_INT,
        PipBoyItemPayload::slot,
        Identifier.STREAM_CODEC,
        PipBoyItemPayload::item,
        PipBoyItemPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handleOnServer(PipBoyItemPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || player.isSpectator() || !player.isAlive()) {
            return;
        }
        switch (payload.action()) {
            case USE -> PipBoyInventoryActions.use(player, payload.slot(), payload.item());
            case EQUIP -> PipBoyInventoryActions.equip(player, payload.slot(), payload.item());
            case UNEQUIP -> PipBoyInventoryActions.unequip(player, payload.slot(), payload.item());
            case HOLD -> PipBoyInventoryActions.hold(player, payload.slot(), payload.item());
            case EQUIP_POWER_ARMOR -> PipBoyInventoryActions.equipPowerArmorPiece(
                player,
                payload.slot(),
                payload.item()
            );
            case UNEQUIP_POWER_ARMOR -> PipBoyInventoryActions.unequipPowerArmorPiece(player, payload.slot());
            case DEPLOY_CHASSIS -> PipBoyInventoryActions.deployChassis(player, payload.slot(), payload.item());
        }
    }
}
