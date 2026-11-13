package mod.azure.atomicdawn.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.npc.NpcConfig;
import mod.azure.atomicdawn.npc.NpcService;

public record NpcEditPayload(
    int entityId,
    Action action,
    NpcConfig config
) implements CustomPacketPayload {

    public enum Action {

        SAVE,
        DELETE,
        MOVE_HERE,
        SET_HOME,
        CLEAR_EQUIPMENT,
        EDIT_SHOP,
        RELEASE_COMPANION,
        EDIT_GEAR,
        RESPAWN_NOW;

        private static final Action[] VALUES = values();

        static Action byIndex(int index) {
            return VALUES[Math.floorMod(index, VALUES.length)];
        }
    }

    public static final Type<NpcEditPayload> TYPE = new Type<>(AtomicDawn.id("npc_edit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NpcEditPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        NpcEditPayload::entityId,
        ByteBufCodecs.VAR_INT.map(Action::byIndex, Action::ordinal),
        NpcEditPayload::action,
        NpcConfig.STREAM_CODEC,
        NpcEditPayload::config,
        NpcEditPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handleOnServer(NpcEditPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            NpcService.handleEdit(player, payload);
        }
    }
}
