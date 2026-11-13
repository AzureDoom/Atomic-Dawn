package mod.azure.atomicdawn.weapon.legendary.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import mod.azure.atomicdawn.weapon.legendary.LegendaryBehavior;
import mod.azure.atomicdawn.weapon.legendary.LegendaryContext;

public record BloodiedBehavior(float maxBonus) implements LegendaryBehavior {

    public static final MapCodec<BloodiedBehavior> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Codec.floatRange(0, 10).optionalFieldOf("max_bonus", 0.95F).forGetter(BloodiedBehavior::maxBonus)
        ).apply(instance, BloodiedBehavior::new)
    );

    @Override
    public MapCodec<BloodiedBehavior> codec() {
        return CODEC;
    }

    @Override
    public float damageMultiplier(LegendaryContext context) {
        var attacker = context.attacker();
        var missing = 1F - attacker.getHealth() / Math.max(1F, attacker.getMaxHealth());
        return 1F + maxBonus * Math.clamp(missing, 0F, 1F);
    }
}
