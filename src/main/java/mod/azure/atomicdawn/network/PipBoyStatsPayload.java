package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.AtomicDawn;

public record PipBoyStatsPayload(
    long playTimeTicks,
    long deaths,
    long mobKills,
    long playerKills,
    long distanceCm,
    long shotsFired,
    long vatsHits,
    long vatsMisses,
    long criticalHits,
    long radsAbsorbed
) implements CustomPacketPayload {

    public static final Type<PipBoyStatsPayload> TYPE = new Type<>(AtomicDawn.id("pipboy_stats"));

    public static final StreamCodec<ByteBuf, PipBoyStatsPayload> STREAM_CODEC = StreamCodec.of(
        (buf, p) -> {
            for (
                var value : new long[] {
                    p.playTimeTicks,
                    p.deaths,
                    p.mobKills,
                    p.playerKills,
                    p.distanceCm,
                    p.shotsFired,
                    p.vatsHits,
                    p.vatsMisses,
                    p.criticalHits,
                    p.radsAbsorbed }
            ) {
                ByteBufCodecs.VAR_LONG.encode(buf, value);
            }
        },
        buf -> new PipBoyStatsPayload(
            ByteBufCodecs.VAR_LONG.decode(buf),
            ByteBufCodecs.VAR_LONG.decode(buf),
            ByteBufCodecs.VAR_LONG.decode(buf),
            ByteBufCodecs.VAR_LONG.decode(buf),
            ByteBufCodecs.VAR_LONG.decode(buf),
            ByteBufCodecs.VAR_LONG.decode(buf),
            ByteBufCodecs.VAR_LONG.decode(buf),
            ByteBufCodecs.VAR_LONG.decode(buf),
            ByteBufCodecs.VAR_LONG.decode(buf),
            ByteBufCodecs.VAR_LONG.decode(buf)
        )
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
