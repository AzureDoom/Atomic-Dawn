package mod.azure.atomicdawn.weapon.legendary.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import mod.azure.atomicdawn.weapon.legendary.LegendaryBehavior;

public final class SimpleBehaviors {

    private SimpleBehaviors() {}

    public record Bash(float multiplier) implements LegendaryBehavior {

        public static final MapCodec<Bash> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                Codec.floatRange(1, 10).optionalFieldOf("multiplier", 1.5F).forGetter(Bash::multiplier)
            ).apply(instance, Bash::new)
        );

        @Override
        public MapCodec<Bash> codec() {
            return CODEC;
        }

        @Override
        public float bashMultiplier() {
            return multiplier;
        }
    }

    public record AimingSpeed(float bonus) implements LegendaryBehavior {

        public static final MapCodec<AimingSpeed> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                Codec.floatRange(0, 2).optionalFieldOf("bonus", 0.25F).forGetter(AimingSpeed::bonus)
            ).apply(instance, AimingSpeed::new)
        );

        @Override
        public MapCodec<AimingSpeed> codec() {
            return CODEC;
        }

        @Override
        public float aimingSpeedBonus() {
            return bonus;
        }
    }

    public record CriticalCharge(float multiplier) implements LegendaryBehavior {

        public static final MapCodec<CriticalCharge> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                Codec.floatRange(0, 10).optionalFieldOf("multiplier", 1.5F).forGetter(CriticalCharge::multiplier)
            ).apply(instance, CriticalCharge::new)
        );

        @Override
        public MapCodec<CriticalCharge> codec() {
            return CODEC;
        }

        @Override
        public float criticalGainMultiplier() {
            return multiplier;
        }
    }
}
