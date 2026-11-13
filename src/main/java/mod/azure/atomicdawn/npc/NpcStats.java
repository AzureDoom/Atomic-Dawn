package mod.azure.atomicdawn.npc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record NpcStats(
    float maxHealth,
    boolean invulnerable
) {

    public static final NpcStats DEFAULT = new NpcStats(20F, true);

    public static final Codec<NpcStats> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("max_health", 20F).forGetter(NpcStats::maxHealth),
            Codec.BOOL.optionalFieldOf("invulnerable", true).forGetter(NpcStats::invulnerable)
        ).apply(instance, NpcStats::new)
    );

    public NpcStats sanitized() {
        return new NpcStats(Float.isFinite(maxHealth) ? Math.clamp(maxHealth, 1F, 10_000F) : 20F, invulnerable);
    }
}
