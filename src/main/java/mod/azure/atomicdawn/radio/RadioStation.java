package mod.azure.atomicdawn.radio;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import mod.azure.atomicdawn.helper.ADRegistryUtil;
import mod.azure.atomicdawn.registry.ADRegistries;

public record RadioStation(
    float frequency,
    List<Identifier> tracks,
    boolean shuffle,
    boolean enabled,
    int sortOrder
) {

    public static final Codec<RadioStation> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Codec.floatRange(0F, 10_000F).fieldOf("frequency").forGetter(RadioStation::frequency),
            ExtraCodecs.nonEmptyList(Identifier.CODEC.listOf()).fieldOf("tracks").forGetter(RadioStation::tracks),
            Codec.BOOL.optionalFieldOf("shuffle", true).forGetter(RadioStation::shuffle),
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(RadioStation::enabled),
            Codec.INT.optionalFieldOf("sort", 0).forGetter(RadioStation::sortOrder)
        ).apply(instance, RadioStation::new)
    );

    private static final Comparator<Holder.Reference<RadioStation>> ORDER =
        Comparator.<Holder.Reference<RadioStation>>comparingInt(holder -> holder.value().sortOrder())
            .thenComparingDouble(holder -> holder.value().frequency())
            .thenComparing(holder -> holder.key().identifier().toString());

    public static List<Holder.Reference<RadioStation>> all(RegistryAccess access) {
        return ADRegistryUtil.optionalRegistry(access, ADRegistries.RADIO_STATION)
            .map(registry -> registry.listElements().filter(holder -> holder.value().enabled()).sorted(ORDER).toList())
            .orElse(List.of());
    }

    public static Optional<RadioStation> get(RegistryAccess access, Identifier id) {
        return ADRegistryUtil.optionalRegistry(access, ADRegistries.RADIO_STATION)
            .flatMap(registry -> registry.get(ResourceKey.create(ADRegistries.RADIO_STATION, id)))
            .map(Holder::value)
            .filter(RadioStation::enabled);
    }

    public static String translationKey(Identifier id) {
        return "radio_station." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    public static String trackKey(Identifier track) {
        return "subtitles." + track.getNamespace() + "." + track.getPath();
    }
}
