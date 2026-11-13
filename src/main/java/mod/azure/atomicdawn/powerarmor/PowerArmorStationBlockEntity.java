package mod.azure.atomicdawn.powerarmor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import mod.azure.atomicdawn.registry.ADBlockEntities;

public class PowerArmorStationBlockEntity extends BlockEntity {

    public PowerArmorStationBlockEntity(BlockPos pos, BlockState state) {
        super(ADBlockEntities.POWER_ARMOR_STATION.get(), pos, state);
    }
}
