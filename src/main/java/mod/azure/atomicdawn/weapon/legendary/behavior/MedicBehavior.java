package mod.azure.atomicdawn.weapon.legendary.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;

import mod.azure.atomicdawn.weapon.legendary.LegendaryBehavior;
import mod.azure.atomicdawn.weapon.legendary.LegendaryContext;
import mod.azure.atomicdawn.weapon.legendary.LegendaryHooks;

public record MedicBehavior(float healFraction) implements LegendaryBehavior {

    public static final MapCodec<MedicBehavior> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Codec.floatRange(0, 10).optionalFieldOf("heal_fraction", 0.5F).forGetter(MedicBehavior::healFraction)
        ).apply(instance, MedicBehavior::new)
    );

    @Override
    public MapCodec<MedicBehavior> codec() {
        return CODEC;
    }

    @Override
    public boolean redirect(LegendaryContext context, float amount) {
        if (!(context.target() instanceof LivingEntity ally) || !LegendaryHooks.friendly(context.attacker(), ally)) {
            return false;
        }
        ally.heal(amount * healFraction);
        var top = ally.getY() + ally.getBbHeight();
        context.level().sendParticles(ParticleTypes.HEART, ally.getX(), top, ally.getZ(), 2, 0.2, 0.2, 0.2, 0);
        return true;
    }
}
