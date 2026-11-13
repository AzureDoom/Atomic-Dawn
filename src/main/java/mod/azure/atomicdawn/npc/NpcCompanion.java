package mod.azure.atomicdawn.npc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public record NpcCompanion(
    boolean hireable,
    Optional<Identifier> costItem,
    int costCount,
    int durationMinutes
) {

    public static final NpcCompanion DEFAULT = new NpcCompanion(false, Optional.empty(), 1, 20);

    public static final Codec<NpcCompanion> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Codec.BOOL.optionalFieldOf("hireable", false).forGetter(NpcCompanion::hireable),
            Identifier.CODEC.optionalFieldOf("cost_item").forGetter(NpcCompanion::costItem),
            Codec.INT.optionalFieldOf("cost_count", 1).forGetter(NpcCompanion::costCount),
            Codec.INT.optionalFieldOf("duration_minutes", 20).forGetter(NpcCompanion::durationMinutes)
        ).apply(instance, NpcCompanion::new)
    );

    public NpcCompanion sanitized() {
        return new NpcCompanion(
            hireable,
            costItem == null ? Optional.empty() : costItem,
            Math.clamp(costCount, 1, 64 * 36),
            Math.clamp(durationMinutes, 0, 60 * 24 * 7)
        );
    }
}
