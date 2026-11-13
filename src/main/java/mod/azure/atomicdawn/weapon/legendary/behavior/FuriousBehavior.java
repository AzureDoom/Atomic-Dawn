package mod.azure.atomicdawn.weapon.legendary.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import mod.azure.atomicdawn.weapon.legendary.LegendaryBehavior;
import mod.azure.atomicdawn.weapon.legendary.LegendaryContext;

public record FuriousBehavior(
    float perHit,
    float max
) implements LegendaryBehavior {

    public static final MapCodec<FuriousBehavior> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Codec.floatRange(0, 10).optionalFieldOf("per_hit", 0.05F).forGetter(FuriousBehavior::perHit),
            Codec.floatRange(0, 10).optionalFieldOf("max", 0.45F).forGetter(FuriousBehavior::max)
        ).apply(instance, FuriousBehavior::new)
    );

    @Override
    public MapCodec<FuriousBehavior> codec() {
        return CODEC;
    }

    @Override
    public float damageMultiplier(LegendaryContext context) {
        var streak = context.shooter().furiousStreak(context.target().getUUID());
        return 1F + Math.min(max, perHit * streak);
    }

    @Override
    public void onHit(LegendaryContext context, float damageDealt) {
        context.shooter().recordFuriousHit(context.target().getUUID());
    }
}
