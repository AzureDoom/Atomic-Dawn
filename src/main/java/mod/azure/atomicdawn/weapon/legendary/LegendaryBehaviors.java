package mod.azure.atomicdawn.weapon.legendary;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

import java.util.function.Function;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.weapon.legendary.behavior.AntiArmorBehavior;
import mod.azure.atomicdawn.weapon.legendary.behavior.BloodiedBehavior;
import mod.azure.atomicdawn.weapon.legendary.behavior.ConditionalBonusBehavior;
import mod.azure.atomicdawn.weapon.legendary.behavior.DefenseBehavior;
import mod.azure.atomicdawn.weapon.legendary.behavior.ExplosiveBehavior;
import mod.azure.atomicdawn.weapon.legendary.behavior.FuriousBehavior;
import mod.azure.atomicdawn.weapon.legendary.behavior.HitEffectBehavior;
import mod.azure.atomicdawn.weapon.legendary.behavior.InstigatingBehavior;
import mod.azure.atomicdawn.weapon.legendary.behavior.KillEffectBehavior;
import mod.azure.atomicdawn.weapon.legendary.behavior.MedicBehavior;
import mod.azure.atomicdawn.weapon.legendary.behavior.ScalingBonusBehavior;
import mod.azure.atomicdawn.weapon.legendary.behavior.SimpleBehaviors;
import mod.azure.atomicdawn.weapon.legendary.behavior.SpecialBonusBehavior;
import mod.azure.atomicdawn.weapon.legendary.behavior.TargetBonusBehavior;

@SuppressWarnings("unused")
public final class LegendaryBehaviors {

    public static final ResourceKey<Registry<MapCodec<? extends LegendaryBehavior>>> TYPES_KEY = ResourceKey
        .createRegistryKey(AtomicDawn.id("legendary_behavior_type"));

    public static final Registry<MapCodec<? extends LegendaryBehavior>> TYPES = new RegistryBuilder<>(TYPES_KEY)
        .sync(true)
        .create();

    public static final Codec<LegendaryBehavior> CODEC = TYPES.byNameCodec()
        .dispatch(LegendaryBehavior::codec, Function.identity());

    public static final DeferredRegister<MapCodec<? extends LegendaryBehavior>> REGISTER = DeferredRegister.create(
        TYPES,
        AtomicDawn.MOD_ID
    );

    public static final DeferredHolder<MapCodec<? extends LegendaryBehavior>, MapCodec<BloodiedBehavior>> BLOODIED =
        REGISTER.register("bloodied", () -> BloodiedBehavior.CODEC);

    public static final DeferredHolder<MapCodec<? extends LegendaryBehavior>, MapCodec<FuriousBehavior>> FURIOUS =
        REGISTER.register("furious", () -> FuriousBehavior.CODEC);

    public static final DeferredHolder<MapCodec<? extends LegendaryBehavior>, MapCodec<InstigatingBehavior>> INSTIGATING =
        REGISTER.register("instigating", () -> InstigatingBehavior.CODEC);

    public static final DeferredHolder<MapCodec<? extends LegendaryBehavior>, MapCodec<AntiArmorBehavior>> ANTI_ARMOR =
        REGISTER.register("anti_armor", () -> AntiArmorBehavior.CODEC);

    public static final DeferredHolder<MapCodec<? extends LegendaryBehavior>, MapCodec<ExplosiveBehavior>> EXPLOSIVE =
        REGISTER.register("explosive", () -> ExplosiveBehavior.CODEC);

    public static final DeferredHolder<MapCodec<? extends LegendaryBehavior>, MapCodec<TargetBonusBehavior>> TARGET_BONUS =
        REGISTER.register("target_bonus", () -> TargetBonusBehavior.CODEC);

    public static final DeferredHolder<MapCodec<? extends LegendaryBehavior>, MapCodec<ConditionalBonusBehavior>> CONDITIONAL_BONUS =
        REGISTER.register("conditional_bonus", () -> ConditionalBonusBehavior.CODEC);

    public static final DeferredHolder<MapCodec<? extends LegendaryBehavior>, MapCodec<ScalingBonusBehavior>> SCALING_BONUS =
        REGISTER.register("scaling_bonus", () -> ScalingBonusBehavior.CODEC);

    public static final DeferredHolder<MapCodec<? extends LegendaryBehavior>, MapCodec<HitEffectBehavior>> HIT_EFFECT =
        REGISTER.register("hit_effect", () -> HitEffectBehavior.CODEC);

    public static final DeferredHolder<MapCodec<? extends LegendaryBehavior>, MapCodec<KillEffectBehavior>> KILL_EFFECT =
        REGISTER.register("kill_effect", () -> KillEffectBehavior.CODEC);

    public static final DeferredHolder<MapCodec<? extends LegendaryBehavior>, MapCodec<MedicBehavior>> MEDIC =
        REGISTER.register("medic", () -> MedicBehavior.CODEC);

    public static final DeferredHolder<MapCodec<? extends LegendaryBehavior>, MapCodec<SpecialBonusBehavior>> SPECIAL_BONUS =
        REGISTER.register("special_bonus", () -> SpecialBonusBehavior.CODEC);

    public static final DeferredHolder<MapCodec<? extends LegendaryBehavior>, MapCodec<DefenseBehavior>> DEFENSE =
        REGISTER.register("defense", () -> DefenseBehavior.CODEC);

    public static final DeferredHolder<MapCodec<? extends LegendaryBehavior>, MapCodec<SimpleBehaviors.Bash>> BASH =
        REGISTER.register("bash", () -> SimpleBehaviors.Bash.CODEC);

    public static final DeferredHolder<MapCodec<? extends LegendaryBehavior>, MapCodec<SimpleBehaviors.AimingSpeed>> AIMING_SPEED =
        REGISTER.register("aiming_speed", () -> SimpleBehaviors.AimingSpeed.CODEC);

    public static final DeferredHolder<MapCodec<? extends LegendaryBehavior>, MapCodec<SimpleBehaviors.CriticalCharge>> CRITICAL_CHARGE =
        REGISTER.register("critical_charge", () -> SimpleBehaviors.CriticalCharge.CODEC);

    private LegendaryBehaviors() {}

    public static void registerRegistry(NewRegistryEvent event) {
        event.register(TYPES);
    }
}
