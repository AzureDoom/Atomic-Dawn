package mod.azure.atomicdawn.limb;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.world.entity.EntityType;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import mod.azure.atomicdawn.vats.BodyPart;

public record LimbProfile(
    HolderSet<EntityType<?>> entities,
    List<BodyPart> parts,
    Map<BodyPart, List<String>> bones,
    float health,
    boolean immune,
    int priority
) {

    public static final Codec<BodyPart> PART_CODEC = Codec.STRING.comapFlatMap(
        name -> Arrays.stream(BodyPart.values())
            .filter(part -> part.name().equalsIgnoreCase(name))
            .findFirst()
            .map(DataResult::success)
            .orElseGet(() -> DataResult.error(() -> "Unknown body part: " + name)),
        part -> part.name().toLowerCase(Locale.ROOT)
    );

    @SuppressWarnings("NullableProblems")
    public static final Codec<LimbProfile> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            RegistryCodecs.holderSet(Registries.ENTITY_TYPE).fieldOf("entities").forGetter(LimbProfile::entities),
            PART_CODEC.listOf()
                .optionalFieldOf("parts", List.of(BodyPart.values()))
                .forGetter(LimbProfile::parts),
            Codec.unboundedMap(PART_CODEC, Codec.STRING.listOf())
                .optionalFieldOf("bones", Map.of())
                .forGetter(LimbProfile::bones),
            Codec.floatRange(0.01F, 100F).optionalFieldOf("health", 1F).forGetter(LimbProfile::health),
            Codec.BOOL.optionalFieldOf("immune", false).forGetter(LimbProfile::immune),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(LimbProfile::priority)
        ).apply(instance, LimbProfile::new)
    );

    public static final LimbProfile DEFAULT = new LimbProfile(
        HolderSet.empty(),
        List.of(BodyPart.values()),
        Map.of(),
        1F,
        false,
        Integer.MIN_VALUE
    );

    public boolean has(BodyPart part) {
        return parts.contains(part);
    }

    public BodyPart boneMatch(String name, String bone) {
        var lowerName = name.toLowerCase(Locale.ROOT);
        var lowerBone = bone.toLowerCase(Locale.ROOT);
        for (var entry : bones.entrySet()) {
            for (var candidate : entry.getValue()) {
                var lower = candidate.toLowerCase(Locale.ROOT);
                if (lower.equals(lowerName) || lower.equals(lowerBone)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }
}
