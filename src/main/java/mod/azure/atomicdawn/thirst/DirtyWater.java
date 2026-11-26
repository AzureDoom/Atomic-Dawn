package mod.azure.atomicdawn.thirst;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;

import mod.azure.atomicdawn.helper.ADWorldgenUtil;
import mod.azure.atomicdawn.radiation.BlastZoneService;
import mod.azure.atomicdawn.registry.ADFluids;
import mod.azure.atomicdawn.worldgen.ADBiomes;

public final class DirtyWater {

    private DirtyWater() {}

    public static boolean isIrradiated(FluidState fluid) {
        return fluid.is(ADFluids.IRRADIATED_WATER.get()) || fluid.is(ADFluids.FLOWING_IRRADIATED_WATER.get());
    }

    public static boolean isDirty(Level level, BlockPos pos, FluidState fluid) {
        if (isIrradiated(fluid)) {
            return true;
        }
        if (!fluid.is(FluidTags.WATER)) {
            return false;
        }
        if (ADWorldgenUtil.biomeIs(level, pos, ADBiomes.WASTELAND)) {
            return true;
        }
        return level instanceof ServerLevel serverLevel && BlastZoneService.inZone(serverLevel, pos);
    }
}
