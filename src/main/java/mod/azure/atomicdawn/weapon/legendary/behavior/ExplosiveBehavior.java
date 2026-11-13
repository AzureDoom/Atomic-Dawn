package mod.azure.atomicdawn.weapon.legendary.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import mod.azure.atomicdawn.registry.ADDamageTypes;
import mod.azure.atomicdawn.weapon.legendary.LegendaryBehavior;
import mod.azure.atomicdawn.weapon.legendary.LegendaryContext;

public record ExplosiveBehavior(
    float fraction,
    float radius
) implements LegendaryBehavior {

    public static final MapCodec<ExplosiveBehavior> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Codec.floatRange(0, 10).optionalFieldOf("fraction", 0.2F).forGetter(ExplosiveBehavior::fraction),
            Codec.floatRange(0.1F, 8F).optionalFieldOf("radius", 1.5F).forGetter(ExplosiveBehavior::radius)
        ).apply(instance, ExplosiveBehavior::new)
    );

    @Override
    public MapCodec<ExplosiveBehavior> codec() {
        return CODEC;
    }

    @Override
    public void onHit(LegendaryContext context, float damageDealt) {
        var level = context.level();
        var pos = context.hitPosition();
        var amount = damageDealt * fraction;
        if (amount <= 0F) {
            return;
        }
        var source = ADDamageTypes.source(level, ADDamageTypes.LEGENDARY_EXPLOSION, context.attacker());
        var area = new AABB(pos, pos).inflate(radius);
        for (var entity : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != context.attacker())) {
            if (entity.position().distanceToSqr(pos) <= radius * radius + entity.getBbWidth()) {
                entity.setInvulnerableTime(0);
                entity.hurtServer(level, source, amount);
            }
        }
        level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        level.playSound(
            null,
            pos.x,
            pos.y,
            pos.z,
            SoundEvents.GENERIC_EXPLODE.value(),
            SoundSource.PLAYERS,
            0.35F,
            1.6F
        );
    }
}
