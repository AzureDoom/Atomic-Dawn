package mod.azure.atomicdawn.npc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;

import mod.azure.atomicdawn.helper.ADRegistryUtil;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public record NpcAppearance(
    Kind kind,
    Identifier model,
    CompoundTag data,
    float scale,
    Optional<Identifier> texture
) {

    public enum Kind implements StringRepresentable {

        ENTITY,
        AZURE;

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final float MIN_SCALE = 0.1F;

    public static final float MAX_SCALE = 5F;

    public static final Identifier DEFAULT_MODEL = ADRegistryUtil.vanillaId("mannequin");

    public static final NpcAppearance DEFAULT = new NpcAppearance(
        Kind.ENTITY,
        DEFAULT_MODEL,
        new CompoundTag(),
        1F,
        Optional.empty()
    );

    public static final Codec<NpcAppearance> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Kind.CODEC.optionalFieldOf("kind", Kind.ENTITY).forGetter(NpcAppearance::kind),
            Identifier.CODEC.optionalFieldOf("model", DEFAULT_MODEL).forGetter(NpcAppearance::model),
            CompoundTag.CODEC.optionalFieldOf("data", new CompoundTag()).forGetter(NpcAppearance::data),
            Codec.FLOAT.optionalFieldOf("scale", 1F).forGetter(NpcAppearance::scale),
            Identifier.CODEC.optionalFieldOf("texture").forGetter(NpcAppearance::texture)
        ).apply(instance, NpcAppearance::new)
    );

    public NpcAppearance {
        data = data == null ? new CompoundTag() : data.copy();
        scale = Float.isFinite(scale) ? Math.clamp(scale, MIN_SCALE, MAX_SCALE) : 1F;
        texture = texture == null ? Optional.empty() : texture;
    }

    public CompoundTag toTag() {
        return CODEC.encodeStart(NbtOps.INSTANCE, this)
            .result()
            .filter(CompoundTag.class::isInstance)
            .map(CompoundTag.class::cast)
            .orElseGet(CompoundTag::new);
    }

    public static NpcAppearance fromTag(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return DEFAULT;
        }
        return CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(DEFAULT);
    }

    public NpcAppearance withKind(Kind value, Identifier newModel) {
        return new NpcAppearance(value, newModel, value == kind ? data : new CompoundTag(), scale, Optional.empty());
    }

    public NpcAppearance withModel(Identifier value) {
        return new NpcAppearance(kind, value, value.equals(model) ? data : new CompoundTag(), scale, texture);
    }

    public NpcAppearance withData(CompoundTag value) {
        return new NpcAppearance(kind, model, value, scale, texture);
    }

    public NpcAppearance withScale(float value) {
        return new NpcAppearance(kind, model, data, value, texture);
    }

    public NpcAppearance withTexture(Optional<Identifier> value) {
        return new NpcAppearance(kind, model, data, scale, value);
    }
}
