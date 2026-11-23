package mod.azure.atomicdawn.weapon.legendary;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

import mod.azure.atomicdawn.special.SpecialAttribute;

public interface LegendaryBehavior {

    MapCodec<? extends LegendaryBehavior> codec();

    default float damageMultiplier(LegendaryContext context) {
        return 1F;
    }

    default float armorPenetration() {
        return 0F;
    }

    default boolean redirect(LegendaryContext context, float amount) {
        return false;
    }

    default void onHit(LegendaryContext context, float damageDealt) {}

    default void onKill(LegendaryContext context, float damageDealt) {}

    default int specialBonus(Player holder, SpecialAttribute attribute) {
        return 0;
    }

    default float criticalGainMultiplier() {
        return 1F;
    }

    default float incomingDamageMultiplier(Player holder, DamageSource source) {
        return 1F;
    }

    default float reflectFraction(Player holder, DamageSource source) {
        return 0F;
    }

    default float bashMultiplier() {
        return 1F;
    }

    default float aimingSpeedBonus() {
        return 0F;
    }
}
