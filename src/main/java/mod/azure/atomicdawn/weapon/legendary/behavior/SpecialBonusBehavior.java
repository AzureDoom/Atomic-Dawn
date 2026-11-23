package mod.azure.atomicdawn.weapon.legendary.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.player.Player;

import mod.azure.atomicdawn.special.SpecialAttribute;
import mod.azure.atomicdawn.weapon.action.WeaponActions;
import mod.azure.atomicdawn.weapon.legendary.LegendaryBehavior;

public record SpecialBonusBehavior(
    SpecialAttribute attribute,
    int amount,
    boolean perKill,
    int max
) implements LegendaryBehavior {

    public static final MapCodec<SpecialBonusBehavior> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            SpecialAttribute.CODEC.fieldOf("attribute").forGetter(SpecialBonusBehavior::attribute),
            Codec.intRange(-10, 10).optionalFieldOf("amount", 3).forGetter(SpecialBonusBehavior::amount),
            Codec.BOOL.optionalFieldOf("per_kill", false).forGetter(SpecialBonusBehavior::perKill),
            Codec.intRange(0, 20).optionalFieldOf("max", 3).forGetter(SpecialBonusBehavior::max)
        ).apply(instance, SpecialBonusBehavior::new)
    );

    @Override
    public MapCodec<SpecialBonusBehavior> codec() {
        return CODEC;
    }

    @Override
    public int specialBonus(Player holder, SpecialAttribute attribute) {
        if (attribute != this.attribute) {
            return 0;
        }
        if (!perKill) {
            return amount;
        }
        var streak = WeaponActions.shooter(holder).killStreak(holder.level().getGameTime());
        return Math.min(max, streak * amount);
    }
}
