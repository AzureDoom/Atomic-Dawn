package mod.azure.atomicdawn.pipboy.holotape;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import mod.azure.atomicdawn.helper.ADRegistryUtil;
import mod.azure.atomicdawn.registry.ADRegistries;

public record Holotape(
    Identifier sound,
    int lengthSeconds,
    Optional<String> author,
    int sortOrder
) {

    public static final Codec<Holotape> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Identifier.CODEC.fieldOf("sound").forGetter(Holotape::sound),
            Codec.intRange(0, 86_400).optionalFieldOf("length_seconds", 0).forGetter(Holotape::lengthSeconds),
            Codec.STRING.optionalFieldOf("author").forGetter(Holotape::author),
            Codec.INT.optionalFieldOf("sort", 0).forGetter(Holotape::sortOrder)
        ).apply(instance, Holotape::new)
    );

    private static final Comparator<Holder.Reference<Holotape>> ORDER =
        Comparator.<Holder.Reference<Holotape>>comparingInt(holder -> holder.value().sortOrder())
            .thenComparing(holder -> holder.key().identifier().toString());

    public static List<Holder.Reference<Holotape>> all(RegistryAccess access) {
        return ADRegistryUtil.optionalRegistry(access, ADRegistries.HOLOTAPE)
            .map(registry -> registry.listElements().sorted(ORDER).toList())
            .orElse(List.of());
    }

    public static Optional<Holotape> get(RegistryAccess access, Identifier id) {
        return ADRegistryUtil.optionalRegistry(access, ADRegistries.HOLOTAPE)
            .flatMap(registry -> registry.get(ResourceKey.create(ADRegistries.HOLOTAPE, id)))
            .map(Holder::value);
    }
}
