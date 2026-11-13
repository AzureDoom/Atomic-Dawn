package mod.azure.atomicdawn.armor.upgrade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import mod.azure.atomicdawn.registry.ADRegistries;

public record ArmorLegendary(
    int star,
    List<ArmorEffect> effects
) {

    public static final Codec<ArmorLegendary> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Codec.intRange(1, 3).fieldOf("star").forGetter(ArmorLegendary::star),
            ArmorEffect.CODEC.listOf().fieldOf("effects").forGetter(ArmorLegendary::effects)
        ).apply(instance, ArmorLegendary::new)
    );

    private static final Comparator<Holder.Reference<ArmorLegendary>> ORDER =
        Comparator.comparing(holder -> holder.key().identifier().getPath());

    public static List<Holder.Reference<ArmorLegendary>> all(RegistryAccess access, int star) {
        return access.lookup(ADRegistries.ARMOR_LEGENDARY)
            .map(
                registry -> registry.listElements()
                    .filter(holder -> holder.value().star() == star)
                    .sorted(ORDER)
                    .toList()
            )
            .orElse(List.of());
    }

    public static Optional<Holder.Reference<ArmorLegendary>> get(RegistryAccess access, Identifier id) {
        return access.lookup(ADRegistries.ARMOR_LEGENDARY)
            .flatMap(registry -> registry.get(ADRegistries.key(ADRegistries.ARMOR_LEGENDARY, id)));
    }

    public static String translationKey(Identifier id) {
        return "armor_legendary." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }
}
