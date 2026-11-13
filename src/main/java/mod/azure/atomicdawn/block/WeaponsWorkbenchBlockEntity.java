package mod.azure.atomicdawn.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.crafting.WorkbenchMenu;
import mod.azure.atomicdawn.registry.ADBlockEntities;

public class WeaponsWorkbenchBlockEntity extends BenchBlockEntity {

    public WeaponsWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(ADBlockEntities.WEAPONS_WORKBENCH.get(), pos, state);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.atomicdawn.weapons_workbench");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(
        int containerId,
        @NotNull Inventory inventory,
        @NotNull Player player
    ) {
        if (level == null) {
            return null;
        }
        return new WorkbenchMenu(containerId, inventory, ContainerLevelAccess.create(level, worldPosition), this);
    }
}
