package mod.azure.atomicdawn.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.special.SpecialAttribute;

public final class ADMobEffects {

    public static final DeferredRegister<MobEffect> REGISTER = DeferredRegister.create(
        Registries.MOB_EFFECT,
        AtomicDawn.MOD_ID
    );

    public static final DeferredHolder<MobEffect, MobEffect> RAD_RESISTANCE = REGISTER.register(
        "rad_resistance",
        () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0xFFE07A2A)
    );

    public static final DeferredHolder<MobEffect, MobEffect> BLEEDING = REGISTER.register(
        "bleeding",
        () -> new SimpleEffect(MobEffectCategory.HARMFUL, 0xFF9E1B1B)
    );

    public static final DeferredHolder<MobEffect, MobEffect> NUKA_CRANBERRY = REGISTER.register(
        "nuka_cranberry",
        () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0xFF4A3B2C)
    );

    public static final DeferredHolder<MobEffect, MobEffect> NUKA_DARK = REGISTER.register(
        "nuka_dark",
        () -> new SimpleEffect(MobEffectCategory.NEUTRAL, 0xFF34302C)
    );

    public static final Map<SpecialAttribute, DeferredHolder<MobEffect, MobEffect>> NUKA_TWIST = twist();

    private ADMobEffects() {}

    private static Map<SpecialAttribute, DeferredHolder<MobEffect, MobEffect>> twist() {
        var effects = new EnumMap<SpecialAttribute, DeferredHolder<MobEffect, MobEffect>>(SpecialAttribute.class);
        for (var attribute : SpecialAttribute.all()) {
            effects.put(
                attribute,
                REGISTER.register(
                    "nuka_twist_" + attribute.getSerializedName(),
                    () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0xFFF95B81)
                )
            );
        }
        return Collections.unmodifiableMap(effects);
    }

    private static final class SimpleEffect extends MobEffect {

        SimpleEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
