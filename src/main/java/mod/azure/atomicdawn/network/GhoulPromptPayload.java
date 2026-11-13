package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.ghoul.GhoulTreatmentService;

public record GhoulPromptPayload(GhoulTreatmentService.Kind kind) implements CustomPacketPayload {

    public static final Type<GhoulPromptPayload> TYPE = new Type<>(AtomicDawn.id("ghoul_prompt"));

    public static final StreamCodec<ByteBuf, GhoulPromptPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
        index -> new GhoulPromptPayload(GhoulTreatmentService.Kind.byIndex(index)),
        payload -> payload.kind().ordinal()
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
