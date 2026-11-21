package mod.azure.atomicdawn.npc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityDimensions;

import java.util.Map;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public record NpcModelDefinition(
    Identifier geo,
    Identifier texture,
    Identifier animations,
    float width,
    float height,
    Optional<Float> eyeHeight,
    Map<String, String> animationNames,
    Map<String, Identifier> variants
) {

    public static final Codec<NpcModelDefinition> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Identifier.CODEC.fieldOf("geo").forGetter(NpcModelDefinition::geo),
            Identifier.CODEC.fieldOf("texture").forGetter(NpcModelDefinition::texture),
            Identifier.CODEC.fieldOf("animations").forGetter(NpcModelDefinition::animations),
            Codec.floatRange(0.1F, 8F).optionalFieldOf("width", 0.6F).forGetter(NpcModelDefinition::width),
            Codec.floatRange(0.1F, 8F).optionalFieldOf("height", 1.95F).forGetter(NpcModelDefinition::height),
            Codec.floatRange(0.05F, 8F).optionalFieldOf("eye_height").forGetter(NpcModelDefinition::eyeHeight),
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                .optionalFieldOf("animation_names", Map.of())
                .forGetter(NpcModelDefinition::animationNames),
            Codec.unboundedMap(Codec.STRING, Identifier.CODEC)
                .optionalFieldOf("variants", Map.of())
                .forGetter(NpcModelDefinition::variants)
        ).apply(instance, NpcModelDefinition::new)
    );

    public String animation(NpcAnimationState state) {
        return animationNames.getOrDefault(state.id(), state.defaultAnimation());
    }

    public String emote(String emote) {
        return animationNames.getOrDefault("emote." + emote, emote);
    }

    public EntityDimensions dimensions() {
        var dimensions = EntityDimensions.scalable(width, height);
        return eyeHeight.map(dimensions::withEyeHeight).orElse(dimensions);
    }

    public Identifier texture(Optional<Identifier> override) {
        if (override.isEmpty()) {
            return texture;
        }
        var variant = variants.get(override.get().toString());
        return variant != null ? variant : override.get();
    }
}
