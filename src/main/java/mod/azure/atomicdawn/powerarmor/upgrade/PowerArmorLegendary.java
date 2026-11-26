package mod.azure.atomicdawn.powerarmor.upgrade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import mod.azure.atomicdawn.helper.ADRegistryUtil;
import mod.azure.atomicdawn.registry.ADRegistries;

public record PowerArmorLegendary(
    int star,
    List<PowerArmorEffect> effects
) {

    public static final Codec<PowerArmorLegendary> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Codec.intRange(1, 3).fieldOf("star").forGetter(PowerArmorLegendary::star),
            PowerArmorEffect.CODEC.listOf().fieldOf("effects").forGetter(PowerArmorLegendary::effects)
        ).apply(instance, PowerArmorLegendary::new)
    );

    private static final Comparator<Holder.Reference<PowerArmorLegendary>> ORDER =
        Comparator.comparing(holder -> holder.key().identifier().getPath());

    public static List<Holder.Reference<PowerArmorLegendary>> all(RegistryAccess access, int star) {
        return ADRegistryUtil.optionalRegistry(access, ADRegistries.POWER_ARMOR_LEGENDARY)
            .map(
                registry -> registry.listElements()
                    .filter(holder -> holder.value().star() == star)
                    .sorted(ORDER)
                    .toList()
            )
            .orElse(List.of());
    }

    public static Optional<Holder.Reference<PowerArmorLegendary>> get(RegistryAccess access, Identifier id) {
        return ADRegistryUtil.optionalRegistry(access, ADRegistries.POWER_ARMOR_LEGENDARY)
            .flatMap(registry -> registry.get(ADRegistries.key(ADRegistries.POWER_ARMOR_LEGENDARY, id)));
    }

    public static String translationKey(Identifier id) {
        return "power_armor_legendary." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }
}
