package mod.azure.atomicdawn.weapon.legendary.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.registry.ADDataComponents;
import mod.azure.atomicdawn.vats.BodyPart;
import mod.azure.atomicdawn.weapon.legendary.LegendaryBehavior;
import mod.azure.atomicdawn.weapon.legendary.LegendaryContext;

public record ConditionalBonusBehavior(
    Condition condition,
    float bonus,
    float threshold,
    float chance
) implements LegendaryBehavior {

    public static final MapCodec<ConditionalBonusBehavior> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Condition.CODEC.fieldOf("condition").forGetter(ConditionalBonusBehavior::condition),
            Codec.floatRange(0, 10).optionalFieldOf("bonus", 0.25F).forGetter(ConditionalBonusBehavior::bonus),
            Codec.FLOAT.optionalFieldOf("threshold", 0F).forGetter(ConditionalBonusBehavior::threshold),
            Codec.floatRange(0, 1).optionalFieldOf("chance", 1F).forGetter(ConditionalBonusBehavior::chance)
        ).apply(instance, ConditionalBonusBehavior::new)
    );

    @Override
    public MapCodec<ConditionalBonusBehavior> codec() {
        return CODEC;
    }

    @Override
    public float damageMultiplier(LegendaryContext context) {
        if (!condition.test(context, threshold)) {
            return 1F;
        }
        if (chance < 1F && context.attacker().getRandom().nextFloat() >= chance) {
            return 1F;
        }
        return 1F + bonus;
    }

    public enum Condition implements StringRepresentable {

        AIMING("aiming"),
        STATIONARY("stationary"),
        SNEAK_ATTACK("sneak_attack"),
        POWER_ATTACK("power_attack"),
        LIMB("limb"),
        LAST_ROUND("last_round"),
        LONG_RANGE("long_range"),
        LOW_HEALTH_TARGET("low_health_target");

        public static final Codec<Condition> CODEC = StringRepresentable.fromEnum(Condition::values);

        private final String name;

        Condition(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }

        boolean test(LegendaryContext context, float threshold) {
            var attacker = context.attacker();
            var shooter = context.shooter();
            return switch (this) {
                case AIMING -> shooter.aimingDownSights();
                case STATIONARY -> {
                    var dx = attacker.getX() - attacker.xo;
                    var dz = attacker.getZ() - attacker.zo;
                    yield dx * dx + dz * dz < 1.0E-4 && attacker.onGround();
                }
                case SNEAK_ATTACK -> attacker.isCrouching()
                    && !(context.target() instanceof Mob mob && mob.getTarget() == attacker);
                case POWER_ATTACK -> shooter.chargeMultiplier() > Math.max(1F, threshold);
                case LIMB -> shooter.vatsActive() && shooter.vatsPart() != BodyPart.TORSO
                    && shooter.vatsPart() != BodyPart.HEAD;
                case LAST_ROUND -> {
                    var state = context.weapon().get(ADDataComponents.WEAPON_STATE.get());
                    yield state != null && state.loadedAmmo() <= 0;
                }
                case LONG_RANGE -> attacker.getEyePosition().distanceTo(context.hitPosition()) >= threshold;
                case LOW_HEALTH_TARGET -> context.target() instanceof LivingEntity living
                    && living.getHealth() <= living.getMaxHealth() * threshold;
            };
        }
    }
}
