package mod.azure.atomicdawn.weapon.legendary.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import mod.azure.atomicdawn.weapon.legendary.LegendaryBehavior;

public record AntiArmorBehavior(float penetration) implements LegendaryBehavior {

    public static final MapCodec<AntiArmorBehavior> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Codec.floatRange(0, 1).optionalFieldOf("penetration", 0.5F).forGetter(AntiArmorBehavior::penetration)
        ).apply(instance, AntiArmorBehavior::new)
    );

    @Override
    public MapCodec<AntiArmorBehavior> codec() {
        return CODEC;
    }

    @Override
    public float armorPenetration() {
        return penetration;
    }
}
