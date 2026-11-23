package mod.azure.atomicdawn.weapon.legendary.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import mod.azure.atomicdawn.weapon.action.WeaponActions;
import mod.azure.atomicdawn.weapon.legendary.LegendaryBehavior;

public record DefenseBehavior(
    Stance stance,
    float reduction,
    float reflect
) implements LegendaryBehavior {

    public static final MapCodec<DefenseBehavior> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Stance.CODEC.fieldOf("stance").forGetter(DefenseBehavior::stance),
            Codec.floatRange(0, 0.95F).optionalFieldOf("reduction", 0F).forGetter(DefenseBehavior::reduction),
            Codec.floatRange(0, 10).optionalFieldOf("reflect", 0F).forGetter(DefenseBehavior::reflect)
        ).apply(instance, DefenseBehavior::new)
    );

    @Override
    public MapCodec<DefenseBehavior> codec() {
        return CODEC;
    }

    @Override
    public float incomingDamageMultiplier(Player holder, DamageSource source) {
        return stance.active(holder, source) ? 1F - reduction : 1F;
    }

    @Override
    public float reflectFraction(Player holder, DamageSource source) {
        return stance.active(holder, source) ? reflect : 0F;
    }

    public static boolean melee(DamageSource source) {
        return source.getEntity() instanceof LivingEntity && source.getDirectEntity() == source.getEntity()
            && !source.is(DamageTypeTags.IS_PROJECTILE) && !source.is(DamageTypeTags.IS_EXPLOSION);
    }

    public enum Stance implements StringRepresentable {

        RELOADING("reloading"),
        AIMING("aiming"),
        GUARDING("guarding");

        public static final Codec<Stance> CODEC = StringRepresentable.fromEnum(Stance::values);

        private final String name;

        Stance(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }

        boolean active(Player holder, DamageSource source) {
            var shooter = WeaponActions.shooter(holder);
            return switch (this) {
                case RELOADING -> shooter.isReloading();
                case AIMING -> shooter.aimingDownSights();
                case GUARDING -> holder.isCrouching() && melee(source);
            };
        }
    }
}
