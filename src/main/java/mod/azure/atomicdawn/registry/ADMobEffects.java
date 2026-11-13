package mod.azure.atomicdawn.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import mod.azure.atomicdawn.AtomicDawn;

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

    private ADMobEffects() {}

    private static final class SimpleEffect extends MobEffect {

        SimpleEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
