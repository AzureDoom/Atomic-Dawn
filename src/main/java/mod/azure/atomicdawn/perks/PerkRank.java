package mod.azure.atomicdawn.perks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;

public record PerkRank(
    Optional<Integer> explicitCost,
    int minLevel,
    List<PerkEffect> effects
) {

    public static final Codec<PerkRank> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Codec.intRange(0, 15).optionalFieldOf("cost").forGetter(PerkRank::explicitCost),
            Codec.intRange(1, 10_000).optionalFieldOf("min_level", 1).forGetter(PerkRank::minLevel),
            PerkEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(PerkRank::effects)
        ).apply(instance, PerkRank::new)
    );

    public int cost(int rank) {
        return explicitCost.orElse(rank);
    }
}
