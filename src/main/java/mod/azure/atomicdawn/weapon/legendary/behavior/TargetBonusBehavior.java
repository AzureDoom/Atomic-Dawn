package mod.azure.atomicdawn.weapon.legendary.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import mod.azure.atomicdawn.weapon.legendary.LegendaryBehavior;
import mod.azure.atomicdawn.weapon.legendary.LegendaryContext;
import mod.azure.atomicdawn.weapon.legendary.LegendaryHooks;

public record TargetBonusBehavior(
    String target,
    float bonus
) implements LegendaryBehavior {

    public static final MapCodec<TargetBonusBehavior> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Codec.STRING.fieldOf("target").forGetter(TargetBonusBehavior::target),
            Codec.floatRange(0, 10).optionalFieldOf("bonus", 0.3F).forGetter(TargetBonusBehavior::bonus)
        ).apply(instance, TargetBonusBehavior::new)
    );

    @Override
    public MapCodec<TargetBonusBehavior> codec() {
        return CODEC;
    }

    @Override
    public float damageMultiplier(LegendaryContext context) {
        return LegendaryHooks.matches(context.target(), target) ? 1F + bonus : 1F;
    }
}
