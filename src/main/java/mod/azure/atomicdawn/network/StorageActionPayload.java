package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.camp.CampStashService;
import mod.azure.atomicdawn.helper.ADRegistryUtil;
import mod.azure.atomicdawn.junk.ScrapboxService;

public record StorageActionPayload(
    StorageOpenPayload.Mode mode,
    BlockPos pos,
    Action action,
    Identifier item,
    int index,
    int amount
) implements CustomPacketPayload {

    public enum Action {

        DEPOSIT_ALL,
        DEPOSIT_SLOT,
        WITHDRAW,
        SCRAP,
        SCRAP_ALL;

        private static final Action[] VALUES = values();

        static Action byIndex(int index) {
            return VALUES[Math.floorMod(index, VALUES.length)];
        }
    }

    public static final Identifier NONE = ADRegistryUtil.vanillaId("air");

    public static final Type<StorageActionPayload> TYPE = new Type<>(AtomicDawn.id("storage_action"));

    public static final StreamCodec<ByteBuf, StorageActionPayload> STREAM_CODEC = StreamCodec.composite(
        StorageOpenPayload.MODE_CODEC,
        StorageActionPayload::mode,
        BlockPos.STREAM_CODEC,
        StorageActionPayload::pos,
        ByteBufCodecs.VAR_INT.map(Action::byIndex, Action::ordinal),
        StorageActionPayload::action,
        Identifier.STREAM_CODEC,
        StorageActionPayload::item,
        ByteBufCodecs.VAR_INT,
        StorageActionPayload::index,
        ByteBufCodecs.VAR_INT,
        StorageActionPayload::amount,
        StorageActionPayload::new
    );

    public static StorageActionPayload of(StorageOpenPayload.Mode mode, BlockPos pos, Action action) {
        return new StorageActionPayload(mode, pos, action, NONE, 0, 0);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handleOnServer(StorageActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        var pos = payload.pos();
        if (payload.mode() == StorageOpenPayload.Mode.STASH) {
            switch (payload.action()) {
                case DEPOSIT_SLOT -> CampStashService.depositSlot(player, pos, payload.index(), payload.amount() > 1);
                case WITHDRAW -> CampStashService.withdraw(
                    player,
                    pos,
                    payload.index(),
                    payload.item(),
                    payload.amount()
                );
                default -> {}
            }
            return;
        }
        switch (payload.action()) {
            case DEPOSIT_ALL -> ScrapboxService.depositAll(player, pos);
            case DEPOSIT_SLOT -> ScrapboxService.depositSlot(player, pos, payload.index());
            case WITHDRAW -> ScrapboxService.withdraw(player, pos, payload.item(), payload.amount());
            case SCRAP -> ScrapboxService.scrap(player, pos, payload.item());
            case SCRAP_ALL -> ScrapboxService.scrap(player, pos, null);
        }
    }
}
