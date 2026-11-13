package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.weapon.action.VatsActions;
import mod.azure.atomicdawn.weapon.action.WeaponActions;

public record WeaponActionPayload(Action action) implements CustomPacketPayload {

    public static final Type<WeaponActionPayload> TYPE = new Type<>(AtomicDawn.id("weapon_action"));

    public static final StreamCodec<ByteBuf, WeaponActionPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
        index -> new WeaponActionPayload(Action.byIndex(index)),
        payload -> payload.action().ordinal()
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handleOnServer(WeaponActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        switch (payload.action()) {
            case FIRE -> WeaponActions.tryFire(player);
            case RELOAD -> WeaponActions.tryReload(player);
            case CYCLE_FIRE_MODE -> WeaponActions.cycleFireMode(player);
            case INSPECT -> WeaponActions.inspect(player);
            case TRIGGER_PRESS -> WeaponActions.triggerPress(player);
            case TRIGGER_RELEASE -> WeaponActions.triggerRelease(player);
            case ARM_CRITICAL -> VatsActions.armCritical(player);
        }
    }

    public enum Action {

        FIRE,
        RELOAD,
        CYCLE_FIRE_MODE,
        INSPECT,
        TRIGGER_PRESS,
        TRIGGER_RELEASE,
        ARM_CRITICAL;

        private static final Action[] VALUES = values();

        static Action byIndex(int index) {
            return VALUES[Math.floorMod(index, VALUES.length)];
        }
    }
}
