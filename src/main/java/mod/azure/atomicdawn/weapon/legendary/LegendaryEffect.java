package mod.azure.atomicdawn.weapon.legendary;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceKey;

import java.util.List;
import java.util.Optional;

import mod.azure.atomicdawn.weapon.definition.WeaponCategory;
import mod.azure.atomicdawn.weapon.modification.StatModifier;

public record LegendaryEffect(
    int star,
    List<WeaponCategory> categories,
    List<StatModifier> modifiers,
    Optional<LegendaryBehavior> behavior
) {

    public static final Codec<LegendaryEffect> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Codec.intRange(1, 4).fieldOf("star").forGetter(LegendaryEffect::star),
            WeaponCategory.CODEC.listOf()
                .optionalFieldOf("categories", List.of())
                .forGetter(LegendaryEffect::categories),
            StatModifier.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(LegendaryEffect::modifiers),
            LegendaryBehaviors.CODEC.optionalFieldOf("behavior").forGetter(LegendaryEffect::behavior)
        ).apply(instance, LegendaryEffect::new)
    );

    public boolean appliesTo(WeaponCategory category) {
        return categories.isEmpty() || categories.contains(category);
    }

    public static String translationKey(ResourceKey<LegendaryEffect> key) {
        var id = key.identifier();
        return "legendary." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }
}
