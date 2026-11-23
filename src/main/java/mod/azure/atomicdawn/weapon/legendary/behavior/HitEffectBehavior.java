package mod.azure.atomicdawn.weapon.legendary.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.registry.ADDamageTypes;
import mod.azure.atomicdawn.weapon.instance.Weapons;
import mod.azure.atomicdawn.weapon.legendary.LegendaryBehavior;
import mod.azure.atomicdawn.weapon.legendary.LegendaryContext;

public record HitEffectBehavior(
    Effect effect,
    float amount,
    float chance,
    int duration
) implements LegendaryBehavior {

    public static final MapCodec<HitEffectBehavior> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Effect.CODEC.fieldOf("effect").forGetter(HitEffectBehavior::effect),
            Codec.floatRange(0, 10).optionalFieldOf("amount", 0.25F).forGetter(HitEffectBehavior::amount),
            Codec.floatRange(0, 1).optionalFieldOf("chance", 1F).forGetter(HitEffectBehavior::chance),
            Codec.intRange(0, 72000).optionalFieldOf("duration", 60).forGetter(HitEffectBehavior::duration)
        ).apply(instance, HitEffectBehavior::new)
    );

    @Override
    public MapCodec<HitEffectBehavior> codec() {
        return CODEC;
    }

    @Override
    public void onHit(LegendaryContext context, float damageDealt) {
        var attacker = context.attacker();
        if (chance < 1F && attacker.getRandom().nextFloat() >= chance) {
            return;
        }
        var level = context.level();
        var target = context.target();
        switch (effect) {
            case VAMPIRE -> attacker.heal(attacker.getMaxHealth() * amount);
            case SUPPRESS -> {
                if (target instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0), attacker);
                }
            }
            case GHOST -> attacker.addEffect(
                new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, true, false, true)
            );
            case POISON -> {
                if (target instanceof LivingEntity living && living.isAlive()) {
                    living.addEffect(new MobEffectInstance(MobEffects.POISON, duration, 0), attacker);
                    living.setInvulnerableTime(0);
                    var source = level.damageSources().indirectMagic(attacker, attacker);
                    living.hurtServer(level, source, damageDealt * amount);
                }
            }
            case FIRE -> {
                if (target instanceof LivingEntity living && living.isAlive()) {
                    living.igniteForSeconds(duration / 20F);
                    living.setInvulnerableTime(0);
                    living.hurtServer(level, level.damageSources().onFire(), damageDealt * amount);
                }
            }
            case TWO_SHOT -> {
                if (target instanceof LivingEntity living && living.isAlive()) {
                    var definition = Weapons.definition(level, context.weapon());
                    if (definition == null) {
                        return;
                    }
                    var type = ADDamageTypes.forTarget(definition.damage().type(), target);
                    living.setInvulnerableTime(0);
                    living.hurtServer(level, ADDamageTypes.source(level, type, attacker), damageDealt * amount);
                    var pos = context.hitPosition();
                    level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 4, 0.1, 0.1, 0.1, 0.1);
                }
            }
        }
    }

    public enum Effect implements StringRepresentable {

        VAMPIRE("vampire"),
        SUPPRESS("suppress"),
        GHOST("ghost"),
        POISON("poison"),
        FIRE("fire"),
        TWO_SHOT("two_shot");

        public static final Codec<Effect> CODEC = StringRepresentable.fromEnum(Effect::values);

        private final String name;

        Effect(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
