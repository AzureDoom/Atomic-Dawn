package mod.azure.atomicdawn.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import mod.azure.atomicdawn.AtomicDawn;

public final class ADFluids {

    public static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(
        NeoForgeRegistries.Keys.FLUID_TYPES,
        AtomicDawn.MOD_ID
    );

    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, AtomicDawn.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> IRRADIATED_WATER_TYPE = TYPES.register(
        "irradiated_water",
        () -> new FluidType(
            FluidType.Properties.create()
                .descriptionId("block.atomicdawn.irradiated_water")
                .fallDistanceModifier(0F)
                .canExtinguish(true)
                .canConvertToSource(false)
                .supportsBoating(true)
                .canHydrate(false)
                .isWaterLike(true)
                .lightLevel(2)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
        )
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> IRRADIATED_WATER = FLUIDS.register(
        "irradiated_water",
        () -> new BaseFlowingFluid.Source(properties())
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_IRRADIATED_WATER = FLUIDS.register(
        "flowing_irradiated_water",
        () -> new BaseFlowingFluid.Flowing(properties())
    );

    private ADFluids() {}

    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(IRRADIATED_WATER_TYPE, IRRADIATED_WATER, FLOWING_IRRADIATED_WATER)
            .bucket(ADItems.IRRADIATED_WATER_BUCKET)
            .block(ADBlocks.IRRADIATED_WATER);
    }
}
