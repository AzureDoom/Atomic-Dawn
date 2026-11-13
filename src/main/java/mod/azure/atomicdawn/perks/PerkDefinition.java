package mod.azure.atomicdawn.perks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;

import java.util.List;
import java.util.Optional;

import mod.azure.atomicdawn.special.SpecialAttribute;

public record PerkDefinition(
    SpecialAttribute special,
    List<PerkRank> ranks,
    List<String> tags,
    Optional<String> exclusiveGroup,
    boolean enabled,
    int sortOrder
) {

    public static final Codec<PerkDefinition> CODEC = RecordCodecBuilder.<PerkDefinition>create(
        instance -> instance.group(
            SpecialAttribute.CODEC.fieldOf("special").forGetter(PerkDefinition::special),
            ExtraCodecs.nonEmptyList(PerkRank.CODEC.listOf()).fieldOf("ranks").forGetter(PerkDefinition::ranks),
            Codec.STRING.listOf().optionalFieldOf("tags", List.of()).forGetter(PerkDefinition::tags),
            Codec.STRING.optionalFieldOf("exclusive_group").forGetter(PerkDefinition::exclusiveGroup),
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(PerkDefinition::enabled),
            Codec.INT.optionalFieldOf("sort", 0).forGetter(PerkDefinition::sortOrder)
        ).apply(instance, PerkDefinition::new)
    ).validate(PerkDefinition::validate);

    private static DataResult<PerkDefinition> validate(PerkDefinition definition) {
        for (var i = 0; i < definition.ranks.size(); i++) {
            var rank = definition.ranks.get(i);
            if (rank.cost(i + 1) < 0) {
                return DataResult.error(() -> "Perk rank costs must be non-negative");
            }
            if (i > 0 && rank.minLevel() < definition.ranks.get(i - 1).minLevel()) {
                return DataResult.error(() -> "Perk rank min_level must not decrease between ranks");
            }
        }
        return DataResult.success(definition);
    }

    public int maxRank() {
        return ranks.size();
    }

    public PerkRank rank(int rank) {
        return ranks.get(Math.clamp(rank, 1, ranks.size()) - 1);
    }

    public int cost(int rank) {
        return rank(rank).cost(Math.clamp(rank, 1, ranks.size()));
    }

    public static String translationKey(Identifier id) {
        return "perk." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    public static String translationKey(ResourceKey<PerkDefinition> key) {
        return translationKey(key.identifier());
    }
}
