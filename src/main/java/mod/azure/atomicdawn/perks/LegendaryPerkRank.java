package mod.azure.atomicdawn.perks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record LegendaryPerkRank(
    int coinCost,
    int minLevel,
    List<PerkEffect> effects
) {

    public static final Codec<LegendaryPerkRank> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Codec.intRange(0, 100_000).optionalFieldOf("coin_cost", 0).forGetter(LegendaryPerkRank::coinCost),
            Codec.intRange(1, 10_000).optionalFieldOf("min_level", 1).forGetter(LegendaryPerkRank::minLevel),
            PerkEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(LegendaryPerkRank::effects)
        ).apply(instance, LegendaryPerkRank::new)
    );
}
