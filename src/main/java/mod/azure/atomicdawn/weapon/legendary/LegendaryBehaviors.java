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
import mod.azure.atomicdawn.weapon.legendary.behavior.ExplosiveBehavior;
import mod.azure.atomicdawn.weapon.legendary.behavior.FuriousBehavior;
import mod.azure.atomicdawn.weapon.legendary.behavior.InstigatingBehavior;

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

    private LegendaryBehaviors() {}

    public static void registerRegistry(NewRegistryEvent event) {
        event.register(TYPES);
    }
}
