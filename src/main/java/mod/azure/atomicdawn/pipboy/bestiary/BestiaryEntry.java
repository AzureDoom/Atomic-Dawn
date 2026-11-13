package mod.azure.atomicdawn.pipboy.bestiary;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import mod.azure.atomicdawn.registry.ADRegistries;

public record BestiaryEntry(
    Identifier entity,
    String category,
    int threat,
    List<String> weaknesses,
    List<String> resistances,
    int studyKills,
    Optional<Identifier> icon,
    int sortOrder
) {

    public static final Codec<BestiaryEntry> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Identifier.CODEC.fieldOf("entity").forGetter(BestiaryEntry::entity),
            Codec.STRING.optionalFieldOf("category", "creature").forGetter(BestiaryEntry::category),
            Codec.intRange(1, 5).optionalFieldOf("threat", 1).forGetter(BestiaryEntry::threat),
            Codec.STRING.listOf().optionalFieldOf("weaknesses", List.of()).forGetter(BestiaryEntry::weaknesses),
            Codec.STRING.listOf().optionalFieldOf("resistances", List.of()).forGetter(BestiaryEntry::resistances),
            Codec.intRange(1, 10_000).optionalFieldOf("study_kills", 5).forGetter(BestiaryEntry::studyKills),
            Identifier.CODEC.optionalFieldOf("icon").forGetter(BestiaryEntry::icon),
            Codec.INT.optionalFieldOf("sort", 0).forGetter(BestiaryEntry::sortOrder)
        ).apply(instance, BestiaryEntry::new)
    );

    private static final Comparator<Holder.Reference<BestiaryEntry>> ORDER =
        Comparator.comparing(
            (Holder.Reference<BestiaryEntry> holder) -> holder.value().category()
        )
            .thenComparingInt(holder -> holder.value().sortOrder())
            .thenComparing(holder -> holder.key().identifier().toString());

    public static List<Holder.Reference<BestiaryEntry>> all(RegistryAccess access) {
        return access.lookup(ADRegistries.BESTIARY)
            .map(registry -> registry.listElements().sorted(ORDER).toList())
            .orElse(List.of());
    }

    public static Optional<Holder.Reference<BestiaryEntry>> forEntity(RegistryAccess access, Identifier entity) {
        return access.lookup(ADRegistries.BESTIARY)
            .flatMap(
                registry -> registry.listElements().filter(holder -> holder.value().entity().equals(entity)).findFirst()
            );
    }
}
