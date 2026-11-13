package mod.azure.atomicdawn.pipboy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record PipBoyStats(
    long shotsFired,
    long vatsHits,
    long vatsMisses,
    long criticalHits,
    long radsAbsorbed
) {

    public static final PipBoyStats EMPTY = new PipBoyStats(0, 0, 0, 0, 0);

    public static final MapCodec<PipBoyStats> MAP_CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Codec.LONG.optionalFieldOf("shots_fired", 0L).forGetter(PipBoyStats::shotsFired),
            Codec.LONG.optionalFieldOf("vats_hits", 0L).forGetter(PipBoyStats::vatsHits),
            Codec.LONG.optionalFieldOf("vats_misses", 0L).forGetter(PipBoyStats::vatsMisses),
            Codec.LONG.optionalFieldOf("critical_hits", 0L).forGetter(PipBoyStats::criticalHits),
            Codec.LONG.optionalFieldOf("rads_absorbed", 0L).forGetter(PipBoyStats::radsAbsorbed)
        ).apply(instance, PipBoyStats::new)
    );

    public PipBoyStats withShot() {
        return new PipBoyStats(shotsFired + 1, vatsHits, vatsMisses, criticalHits, radsAbsorbed);
    }

    public PipBoyStats withVats(boolean hit, boolean critical) {
        return new PipBoyStats(
            shotsFired,
            vatsHits + (hit ? 1 : 0),
            vatsMisses + (hit ? 0 : 1),
            criticalHits + (critical ? 1 : 0),
            radsAbsorbed
        );
    }

    public PipBoyStats withRads(long rads) {
        return new PipBoyStats(shotsFired, vatsHits, vatsMisses, criticalHits, radsAbsorbed + Math.max(0, rads));
    }
}
