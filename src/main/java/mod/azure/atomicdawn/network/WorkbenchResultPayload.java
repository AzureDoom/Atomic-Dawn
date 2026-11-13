package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.weapon.modification.WeaponModding;

public record WorkbenchResultPayload(WeaponModding.Result result) implements CustomPacketPayload {

    private static final WeaponModding.Result[] RESULTS = WeaponModding.Result.values();

    public static final Type<WorkbenchResultPayload> TYPE = new Type<>(AtomicDawn.id("workbench_result"));

    public static final StreamCodec<ByteBuf, WorkbenchResultPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
        index -> new WorkbenchResultPayload(RESULTS[Math.floorMod(index, RESULTS.length)]),
        payload -> payload.result().ordinal()
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
