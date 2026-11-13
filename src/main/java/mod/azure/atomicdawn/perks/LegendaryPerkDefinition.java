package mod.azure.atomicdawn.perks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

import java.util.List;

public record LegendaryPerkDefinition(
    List<LegendaryPerkRank> ranks,
    List<String> tags,
    boolean enabled,
    int sortOrder
) {

    public static final int MAX_RANKS = 8;

    public static final Codec<LegendaryPerkDefinition> CODEC = RecordCodecBuilder.<LegendaryPerkDefinition>create(
        instance -> instance.group(
            ExtraCodecs.nonEmptyList(LegendaryPerkRank.CODEC.listOf())
                .fieldOf("ranks")
                .forGetter(LegendaryPerkDefinition::ranks),
            Codec.STRING.listOf().optionalFieldOf("tags", List.of()).forGetter(LegendaryPerkDefinition::tags),
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(LegendaryPerkDefinition::enabled),
            Codec.INT.optionalFieldOf("sort", 0).forGetter(LegendaryPerkDefinition::sortOrder)
        ).apply(instance, LegendaryPerkDefinition::new)
    ).validate(LegendaryPerkDefinition::validate);

    private static DataResult<LegendaryPerkDefinition> validate(LegendaryPerkDefinition definition) {
        if (definition.ranks.size() > MAX_RANKS) {
            return DataResult.error(() -> "Legendary perks support at most " + MAX_RANKS + " ranks");
        }
        for (var i = 1; i < definition.ranks.size(); i++) {
            if (definition.ranks.get(i).minLevel() < definition.ranks.get(i - 1).minLevel()) {
                return DataResult.error(() -> "Legendary perk rank min_level must not decrease between ranks");
            }
        }
        return DataResult.success(definition);
    }

    public int maxRank() {
        return ranks.size();
    }

    public LegendaryPerkRank rank(int rank) {
        return ranks.get(Math.clamp(rank, 1, ranks.size()) - 1);
    }

    public int upgradeCost(int rank) {
        return rank(rank).coinCost();
    }
}
