package mod.azure.atomicdawn.weapon.legendary;

import com.mojang.serialization.MapCodec;

public interface LegendaryBehavior {

    MapCodec<? extends LegendaryBehavior> codec();

    default float damageMultiplier(LegendaryContext context) {
        return 1F;
    }

    default float armorPenetration() {
        return 0F;
    }

    default void onHit(LegendaryContext context, float damageDealt) {}
}
