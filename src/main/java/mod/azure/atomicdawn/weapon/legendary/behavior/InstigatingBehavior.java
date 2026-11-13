package mod.azure.atomicdawn.weapon.legendary.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.LivingEntity;

import mod.azure.atomicdawn.weapon.legendary.LegendaryBehavior;
import mod.azure.atomicdawn.weapon.legendary.LegendaryContext;

public record InstigatingBehavior(float multiplier) implements LegendaryBehavior {

    public static final MapCodec<InstigatingBehavior> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Codec.floatRange(1, 10).optionalFieldOf("multiplier", 2F).forGetter(InstigatingBehavior::multiplier)
        ).apply(instance, InstigatingBehavior::new)
    );

    @Override
    public MapCodec<InstigatingBehavior> codec() {
        return CODEC;
    }

    @Override
    public float damageMultiplier(LegendaryContext context) {
        if (context.target() instanceof LivingEntity living && living.getHealth() >= living.getMaxHealth()) {
            return multiplier;
        }
        return 1F;
    }
}
