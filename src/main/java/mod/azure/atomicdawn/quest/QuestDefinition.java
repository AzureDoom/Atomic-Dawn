package mod.azure.atomicdawn.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import mod.azure.atomicdawn.helper.ADRegistryUtil;
import mod.azure.atomicdawn.registry.ADRegistries;

public record QuestDefinition(
    QuestCategory category,
    boolean autoStart,
    List<Identifier> prerequisites,
    boolean sequential,
    List<QuestObjective> objectives,
    QuestReward rewards,
    boolean enabled,
    int sortOrder,
    Map<String, QuestOutcome> outcomes,
    Optional<String> defaultOutcome
) {

    public static final Codec<QuestDefinition> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            QuestCategory.CODEC.optionalFieldOf("category", QuestCategory.SIDE).forGetter(QuestDefinition::category),
            Codec.BOOL.optionalFieldOf("auto_start", false).forGetter(QuestDefinition::autoStart),
            Identifier.CODEC.listOf()
                .optionalFieldOf("prerequisites", List.of())
                .forGetter(QuestDefinition::prerequisites),
            Codec.BOOL.optionalFieldOf("sequential", true).forGetter(QuestDefinition::sequential),
            ExtraCodecs.nonEmptyList(QuestObjective.CODEC.listOf())
                .fieldOf("objectives")
                .forGetter(QuestDefinition::objectives),
            QuestReward.CODEC.optionalFieldOf("rewards", QuestReward.NONE).forGetter(QuestDefinition::rewards),
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(QuestDefinition::enabled),
            Codec.INT.optionalFieldOf("sort", 0).forGetter(QuestDefinition::sortOrder),
            Codec.unboundedMap(Codec.STRING, QuestOutcome.CODEC)
                .optionalFieldOf("outcomes", Map.of())
                .forGetter(QuestDefinition::outcomes),
            Codec.STRING.optionalFieldOf("default_outcome").forGetter(QuestDefinition::defaultOutcome)
        ).apply(instance, QuestDefinition::new)
    );

    private static final Comparator<Holder.Reference<QuestDefinition>> ORDER =
        Comparator.<Holder.Reference<QuestDefinition>>comparingInt(holder -> holder.value().category().ordinal())
            .thenComparingInt(holder -> holder.value().sortOrder())
            .thenComparing(holder -> holder.key().identifier().toString());

    public static List<Holder.Reference<QuestDefinition>> all(RegistryAccess access) {
        return ADRegistryUtil.optionalRegistry(access, ADRegistries.QUEST)
            .map(registry -> registry.listElements().filter(holder -> holder.value().enabled()).sorted(ORDER).toList())
            .orElse(List.of());
    }

    public static Optional<QuestDefinition> get(RegistryAccess access, Identifier id) {
        return ADRegistryUtil.optionalRegistry(access, ADRegistries.QUEST)
            .flatMap(registry -> registry.get(ResourceKey.create(ADRegistries.QUEST, id)))
            .map(Holder::value)
            .filter(QuestDefinition::enabled);
    }
}
